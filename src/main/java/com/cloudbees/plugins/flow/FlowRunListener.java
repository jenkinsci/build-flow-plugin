package com.cloudbees.plugins.flow;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class FlowRunListener extends RunListener<Run> {

    @Override
    public void onCompleted(Run run, TaskListener listener) {

        FlowCause cause = (FlowCause) run.getCause(FlowCause.class);
        if (cause == null) return;

        FlowRun flowRun = cause.getFlowRun();
        if (!flowRun.isCompleted())
            flowRun.resume();
    }
}
