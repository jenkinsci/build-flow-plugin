package com.cloudbees.plugins.flow;

import hudson.model.Cause;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FlowCause extends Cause {

    private final FlowRun flowRun;

    public FlowCause(FlowRun flowRun) {
        this.flowRun = flowRun;
    }

    @Override
    public String getShortDescription() {
        return "Started by build flow " + flowRun.toString();
    }
}
