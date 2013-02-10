package com.cloudbees.plugins.flow;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.listeners.RunListener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Extension
public class FlowListener extends RunListener<AbstractBuild<?, ?>> {

    private List<FlowRun> currentlyRunningFlows = new LinkedList<FlowRun>();

    /**
     * Gets all JobInvocation instances currently associated with a running flow
     */
    private Collection<JobInvocation> getJobs() {
        Collection<JobInvocation> jobs = new LinkedList<JobInvocation>();
        for (FlowRun run : currentlyRunningFlows) {
            jobs.addAll(run.getJobsGraph().vertexSet());
        }
        return jobs;
    }

    /**
     * Determines if a given build was started by the given JobInvocation
     */
    private boolean wasStartedBy(AbstractBuild<?, ?> build, JobInvocation job) {
        FlowCause flowCause = (FlowCause) build.getCause(FlowCause.class);
        return flowCause != null
                && flowCause.getFlowRun().getNumber() == job.getFlowRun()
                        .getNumber();
    }

    @Override
    public synchronized void onStarted(AbstractBuild<?, ?> startedBuild,
            TaskListener listener) {
        if (startedBuild instanceof FlowRun) {
            currentlyRunningFlows.add((FlowRun) startedBuild);
        }
        for (JobInvocation job : getJobs()) {
            if (!job.isStarted()
                    && job.getProject() == startedBuild.getProject()
                    && wasStartedBy(startedBuild, job)) {
                job.buildStarted(startedBuild);
                break;
            }
        }
    }

    @Override
    public synchronized void onCompleted(AbstractBuild<?, ?> finishedBuild,
            TaskListener listener) {
        for (JobInvocation job : getJobs()) {
            try {
                if (job.getBuild() == finishedBuild) {
                    job.buildCompleted();
                    break;
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onFinalized(AbstractBuild<?, ?> finalizedBuild) {
        if (finalizedBuild instanceof FlowRun) {
            currentlyRunningFlows.remove((FlowRun) finalizedBuild);
        }
    }

}
