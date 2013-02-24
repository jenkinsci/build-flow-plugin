package com.cloudbees.plugins.flow;

import java.util.List;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.listeners.RunListener;

@Extension
public class FlowListener extends RunListener<AbstractBuild<?, ?>> {

    @Override
    public synchronized void onStarted(AbstractBuild<?, ?> startedBuild,
            TaskListener listener) {
        List<Cause> causes = startedBuild.getCauses();
        for (Cause cause : causes) {
            if (cause instanceof FlowCause) {
                ((FlowCause) cause).getAssociatedJob().buildStarted(
                        startedBuild);
            }
        }
    }

    @Override
    public synchronized void onCompleted(AbstractBuild<?, ?> finishedBuild,
            TaskListener listener) {
        List<Cause> causes = finishedBuild.getCauses();
        for (Cause cause : causes) {
            if (cause instanceof FlowCause) {
                ((FlowCause) cause).getAssociatedJob().buildCompleted();
            }
        }
    }

}
