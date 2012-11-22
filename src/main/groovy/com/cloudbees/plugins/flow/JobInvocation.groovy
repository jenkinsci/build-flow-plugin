package com.cloudbees.plugins.flow;

import hudson.model.*;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JobInvocation {

    private static final Logger LOGGER = Logger.getLogger(JobInvocation.class.getName());

    private final String name;
    private int buildNumber;

    private transient AbstractProject<?, ? extends AbstractBuild<?, ?>> project;

    private transient AbstractBuild build;

    private transient Future<? extends AbstractBuild<?, ?>> future;

    public JobInvocation(FlowRun run, AbstractProject project) {
        this.name = project.getFullName();
        this.project = project;
    }

    public JobInvocation(FlowRun run, String name) {
        this(run, getProjectByName(run, name));
    }

    private static AbstractProject getProjectByName(FlowRun run, String name) {
        AbstractProject item = Jenkins.getInstance().getItem(name, run.getProject().getParent(), AbstractProject.class);
        if (item == null) {
            throw new JobNotFoundException("Item " + name + "not found (or isn't a job).");
        }
        return item;
    }

    /* package */ JobInvocation run(Cause cause, List<Action> actions) {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause, actions);
        if (future == null) {
            throw new CouldNotScheduleJobException("Could not schedule job "
                    + project.getName() +", ensure its not already enqueued with same parameters", e);
        }
        return this;
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
        return getBuild().getResult();
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
        return "running job :" + name;
    }

    public void waitForCompletion() throws ExecutionException, InterruptedException {
        Run run = getBuild();
        while(run.isBuilding()) Thread.sleep(1000);
    }

    /**
     * Initial vertex for the build DAG. To be used by FlowRun constructor to initiate the DAG
     */
    static class Start extends JobInvocation {

        public Start(FlowRun run) {
            super(run, run.getProject());
        }
    }
}
