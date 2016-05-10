/*
 * The MIT License
 *
 * Copyright (c) 2013, CloudBees, Inc., Nicolas De Loof.
 *                     Cisco Systems, Inc., a California corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cloudbees.plugins.flow;

import hudson.model.*
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.logging.Logger;

import java.text.DateFormat;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JobInvocation {

    private static final Logger LOGGER = Logger.getLogger(JobInvocation.class.getName());

    private final String name;
    private int buildNumber;

    // The FlowRun build that initiated this job
    private transient final FlowRun run;

    private transient AbstractProject<?, ? extends AbstractBuild<?, ?>> project;

    private transient Run build;

    private transient QueueTaskFuture<? extends AbstractBuild<?, ?>> future;

    private final Lock lock;
    private final Condition finalizedCond;

    // Whether the build has started. If true, this.build should be set.
    private boolean started;
    // Whether the build has completed
    private boolean completed;
    // Whether the build has completed
    private boolean finalized;

    private final int uid

    public JobInvocation(FlowRun run, AbstractProject project) {
        this.uid = run.buildIndex.getAndIncrement()
        this.run = run;
        this.name = project.getFullName();
        this.project = project;
        this.lock = new ReentrantLock();
        this.finalizedCond = lock.newCondition();
    }

    public JobInvocation(FlowRun run, String name) {
        this(run, getProjectByName(run, name));
    }

    private static AbstractProject getProjectByName(FlowRun run, String name) {
        final ItemGroup context = run.getProject().getParent()
        AbstractProject item = Jenkins.getInstance().getItem(name, (ItemGroup) context, AbstractProject.class);
        if (item == null) {
            throw new JobNotFoundException("Item " + name + " not found (or isn't a job).");
        }
        return item;
    }

    /* package */ JobInvocation run(Cause cause, List<Action> actions) {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause, actions);
        if (future == null) {
            // XXX this will mark the build as failed - perhaps aborting would be a better option?
            throw new CouldNotScheduleJobException("Could not schedule job "
                    + project.getName() +", ensure it is not already queued with the same parameters or is not disabled");
        }
        return this;
    }

    /**
     * Makes an attempt to abort the run.
     * If the run has already started then an attempt is made to abort it, if the job has not yet started then
     * it is removed from the queue.
     * @return <code>true</code> if the run was aborted
     */
    /* package */ boolean abort() {
        def aborted = false
        if (!started) {
            // Need to search the queue for the correct job and cancel it in
            // the queue.
            def queue = Jenkins.instance.queue
            for (queueItem in queue.items) {
                if (future == queueItem.getFuture()) {
                    aborted = queue.cancel(queueItem)
                    break;
                }
            }
        }
        else if (!completed) {
            // as the task has already started we want to be kinder in recording the cause.
            def cause = new FlowAbortedCause(flowRun);
            def executor = build.executor ?: build.oneOffExecutor;
            if (executor != null) {
                executor.interrupt(Result.ABORTED, cause)
                aborted = true;
             }
        }
        return aborted;
    }

    /**
     * Delegate method calls we don't implement to the actual {@link Run} so that DSL feels like a Run object has been
     * returned, but we can lazy-resolve the actual Run object and add some helper methods
     */
    def invokeMethod(String name, args) {
        // Calling $name with $args on actual Run object
        getBuild()."$name"(*args)
    }

    def propertyMissing(String name) {
        // Retrieve property $name from actual Run object
        return getBuild()."$name"
    }

    public Result getResult() throws ExecutionException, InterruptedException {
        waitForCompletion();
        return getBuild().getResult();
    }

    public String getResultString() throws ExecutionException, InterruptedException {
        return getResult().toString().toLowerCase();
    }

    /* package */ Run getFlowRun() {
        return run;
    }

    /* package */ void buildStarted(Run build) {
        this.started = true;
        this.build = build;
        this.buildNumber = build.getNumber();
    }

    /* package */ void buildCompleted() {
        this.completed = true;
    }

    /* package */ void buildFinalized() {
        this.lock.lock();
        try {
            this.finalized = true;
            this.finalizedCond.signal();
        } finally {
            this.lock.unlock();
        }
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return (build != null ? build.displayName : "");
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public String getBuildUrl() {
        return this.getBuild() != null ? this.getBuild().getAbsoluteUrl() : null;
    }

    public String getStartTime() {
        String formattedStartTime = "";
        if (getBuild().getTime() != null) {
            formattedStartTime = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT)
                .format(getBuild().getTime());
        }
        return formattedStartTime;
    }

    public Run getBuild() throws ExecutionException, InterruptedException {
        if (build == null) {
            if (future != null) {
                // waiting for build to run
                build = future.get();
                buildNumber = build.getNumber();
            } else if (buildNumber > 0) {
                // loaded from persistent store
                build = getProject().getBuildByNumber(buildNumber);
            }
        }
        return build;
    }

    public AbstractProject<?, ? extends AbstractBuild<?, ?>> getProject() {
        if (project == null)
            project = Jenkins.getInstance().getItemByFullName(name, AbstractProject.class);
        return project;
    }

    public String toString() {
        return "${name} ${displayName}"
    }

    public Run waitForStart() throws ExecutionException, InterruptedException {
        return future.waitForStart();
    }

    public void waitForCompletion() throws ExecutionException, InterruptedException {
        if (!completed) {
            if (future != null) {
                future.get();
            } else {
                throw new RuntimeException("Can't wait for completion.");
            }
        }
    }

    public void waitForFinalization() throws ExecutionException, InterruptedException {
        if (!finalized) {
            this.lock.lock();
            try {
                this.finalizedCond.await();
            } finally {
                this.lock.unlock();
            }
        }
    }

    String getId() {
        return "build-" + uid;
    }

    /**
     * Initial vertex for the build DAG. To be used by FlowRun constructor to initiate the DAG
     */
    static class Start extends JobInvocation {

        public Start(FlowRun run) {
            super(run, run.getProject());
        }
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof JobInvocation)) return false
        JobInvocation other = (JobInvocation) obj
        return hashCode() == other.hashCode();
    }

    @Override
    int hashCode() {
        return uid;
    }
}
