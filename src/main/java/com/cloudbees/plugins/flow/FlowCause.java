package com.cloudbees.plugins.flow;

import hudson.model.Cause;
import jenkins.model.Jenkins;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FlowCause extends Cause {

    private transient FlowRun flowRun;

    private final String cause;

    public FlowCause(FlowRun flowRun) {
        this.flowRun = flowRun;
        this.cause = flowRun.getParent().getFullName() + "#" + flowRun.getNumber();
    }

    public FlowRun getFlowRun() {
        if (this.flowRun == null) {
            int i = cause.lastIndexOf('#');
            BuildFlow flow = Jenkins.getInstance().getItemByFullName(cause.substring(0,i), BuildFlow.class);
            flowRun = flow.getBuildByNumber(Integer.parseInt(cause.substring(i+1)));
        }
        return flowRun;
    }

    @Override
    public String getShortDescription() {
        return "Started by build flow " + cause;
    }
}
