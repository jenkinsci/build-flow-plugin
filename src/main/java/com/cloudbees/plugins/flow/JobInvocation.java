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

    public JobInvocation(FlowRun run, String name) {
        this.name = name;
        Item item = Jenkins.getInstance().getItem(name, run.getProject().getParent());
        if (item instanceof AbstractProject) {
            project = (AbstractProject<?, ? extends AbstractBuild<?,?>>) item;
        } else {
            throw new JobNotFoundException("Item " + name + "not found (or isn't a job).");
        }
    }

    /* package */ Boolean run(Cause cause, List<Action> actions) {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause, actions);
        return future != null;
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

    public AbstractProject<?, ? extends AbstractBuild<?, ?>> getProject() {
        return project;
    }

    public String toString() {
        return "running job :" + name;
    }
}
