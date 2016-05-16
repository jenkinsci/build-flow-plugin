/*
 * The MIT License
 *
 * Copyright (c) 2013, CloudBees, Inc., Nicolas De Loof.
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

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Maintain the state of execution of a build flow as a chain of triggered jobs
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class FlowRun extends Build<BuildFlow, FlowRun> {

    private static final Logger LOGGER = Logger.getLogger(FlowRun.class.getName());
    
    private String dsl;
    private String dslFile;

    private boolean buildNeedsWorkspace;

    private JobInvocation.Start startJob;

    private DirectedGraph<JobInvocation, JobEdge> jobsGraph;

    private transient ThreadLocal<FlowState> state = new ThreadLocal<FlowState>();
    
    private transient AtomicInteger buildIndex = new AtomicInteger(1);

    public FlowRun(BuildFlow job, File buildDir) throws IOException {
        super(job, buildDir);
        setup(job);
    }

    public FlowRun(BuildFlow job) throws IOException {
        super(job);
        setup(job);
    }

    private void setup(BuildFlow job) {
        if (jobsGraph == null) {
            jobsGraph = new SimpleDirectedGraph<JobInvocation, JobEdge>(JobEdge.class);
        }
        if (startJob == null) {
            startJob = new JobInvocation.Start(this);
        }
        this.dsl = job.getDsl();
        this.dslFile = job.getDslFile();
        this.buildNeedsWorkspace = job.getBuildNeedsWorkspace();
        startJob.buildStarted(this);
        jobsGraph.addVertex(startJob);
        state.set(new FlowState(SUCCESS, startJob));
    }

    /* package */ void schedule(JobInvocation job, List<Action> actions) throws ExecutionException, InterruptedException {
        addBuild(job);
        job.run(new FlowCause(this, job), actions);
    }

    /* package */ Run waitForCompletion(JobInvocation job) throws ExecutionException, InterruptedException {
        job.waitForCompletion();
        getState().setResult(job.getResult());
        return job.getBuild();
    }

    /* package */ Run waitForFinalization(JobInvocation job) throws ExecutionException, InterruptedException {
        job.waitForFinalization();
        getState().setResult(job.getResult());
        return job.getBuild();
    }

    /* package */ FlowState getState() {
        return state.get();
    }

    /* package */ void setState(FlowState s) {
        state.set(s);
    }

    public DirectedGraph<JobInvocation, JobEdge> getJobsGraph() {
        return jobsGraph;
    }

    public JobInvocation getStartJob() {
        return startJob;
    }

    public BuildFlow getBuildFlow() {
        return project;
    }

    public void doGetDot(StaplerRequest req, StaplerResponse rsp) throws IOException {
        new DOTExporter<JobInvocation, JobEdge>().export(rsp.getWriter(), jobsGraph);
    }

    public synchronized void addBuild(JobInvocation job) throws ExecutionException, InterruptedException {
        jobsGraph.addVertex(job);
        for (JobInvocation up : state.get().getLastCompleted()) {
            String edge = up.getId() + " => " + job.getId();
            LOGGER.fine("added build to execution graph " + edge);
            jobsGraph.addEdge(up, job, new JobEdge(up, job));
        }
        state.get().setLastCompleted(job);
    }

    @Override
    public void run() {
        if (buildNeedsWorkspace) {
            execute(new BuildWithWorkspaceRunnerImpl(dsl, dslFile));
        } else {
            execute(new FlyweightTaskRunnerImpl(dsl));
        }
    }

    protected class BuildWithWorkspaceRunnerImpl extends BuildExecution {

        private final String dsl;
        private final String dslFile;

        public BuildWithWorkspaceRunnerImpl(String dsl, String dslFile) {
            this.dsl = dsl;
            this.dslFile = dslFile;
        }

        protected Result doRun(BuildListener listener) throws Exception {
            if(!preBuild(listener, project.getPublishersList()))
                return FAILURE;

            try {
                setResult(SUCCESS);
                if (dslFile != null) {
                    listener.getLogger().printf("[build-flow] reading DSL from file '%s'\n", dslFile);
                    String fileContent = getWorkspace().child(dslFile).readToString();
                    new FlowDSL().executeFlowScript(FlowRun.this, fileContent, listener);
                } else {
                    new FlowDSL().executeFlowScript(FlowRun.this, dsl, listener);
                }
            } finally {
                boolean failed=false;
                for( int i=buildEnvironments.size()-1; i>=0; i-- ) {
                    if (!buildEnvironments.get(i).tearDown(FlowRun.this,listener)) {
                        failed=true;
                    }
                }
                if (failed) return Result.FAILURE;
            }
            return getState().getResult();
        }

        @Override
        public void post2(BuildListener listener) throws IOException, InterruptedException {
            if(!performAllBuildSteps(listener, project.getPublishersList(), true))
                setResult(FAILURE);
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {
            performAllBuildSteps(listener, project.getPublishersList(), false);
            FlowRun.this.startJob.buildCompleted();
            super.cleanUp(listener);
        }
    }

    protected class FlyweightTaskRunnerImpl extends RunExecution {

        private final String dsl;

        public FlyweightTaskRunnerImpl(String dsl) {
            this.dsl = dsl;
        }

        @Override
        public Result run(BuildListener listener) throws Exception, RunnerAbortedException {
            setResult(SUCCESS);
            new FlowDSL().executeFlowScript(FlowRun.this, dsl, listener);
            return getState().getResult();
        }

        @Override
        public void post(BuildListener listener) throws Exception {
            FlowRun.this.startJob.buildCompleted();
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {

        }
    }

    public static class JobEdge {

        private JobInvocation source;
        private JobInvocation target;

        public JobEdge(JobInvocation source, JobInvocation target) {
            this.source = source;
            this.target = target;
        }

        public JobInvocation getSource() {
            return source;
        }

        public JobInvocation getTarget() {
            return target;
        }

    }

}
