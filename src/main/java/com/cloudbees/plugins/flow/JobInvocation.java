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
    private final AbstractProject<?, ? extends AbstractBuild<?, ?>> project;
    private AbstractBuild build;
    private transient Future<? extends AbstractBuild<?, ?>> future;

    public JobInvocation(String name) {
        this.name = name;
        Item item = Jenkins.getInstance().getItemByFullName(name);
        if (item instanceof AbstractProject) {
            project = (AbstractProject<?, ? extends AbstractBuild<?,?>>) item;
        } else {
            throw new JobNotFoundException("Item " + name + "not found (or isn't a job).");
        }
    }

    /**
     * Used to setup the initial vertex for the FlowRun execution Graph
     */
    /* package */ JobInvocation(FlowRun flowRun) {
        this.name = flowRun.getDisplayName();
        this.project = flowRun.getProject();
        this.build = flowRun;
    }

    /* package */ JobInvocation run(Cause cause, List<Action> actions) {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause, actions);
        return this;
    }


    public Result getResult() throws ExecutionException, InterruptedException {
        return getBuild().getResult();
    }

    public Run getBuild() throws ExecutionException, InterruptedException {
        if (build == null) {
            build = future.get();
        }
        return build;
    }

    public String toString() {
        return "running job :" + name;
    }
}
