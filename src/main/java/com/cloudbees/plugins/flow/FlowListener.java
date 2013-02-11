package com.cloudbees.plugins.flow;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.listeners.RunListener;

@Extension
public class FlowListener extends RunListener<AbstractBuild<?, ?>> {

    @Override
    public synchronized void onStarted(AbstractBuild<?, ?> startedBuild,
            TaskListener listener) {
        FlowCause cause = startedBuild.getCause(FlowCause.class);
        if (cause != null) {
            cause.getAssociatedJob().buildStarted(startedBuild);
        }
    }

    @Override
    public synchronized void onCompleted(AbstractBuild<?, ?> finishedBuild,
            TaskListener listener) {
        FlowCause cause = finishedBuild.getCause(FlowCause.class);
        if (cause != null) {
            cause.getAssociatedJob().buildCompleted();
        }
    }

}
