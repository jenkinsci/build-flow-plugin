package com.cloudbees.plugins.flow;

import hudson.model.Cause;
import jenkins.model.Jenkins;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FlowCause extends Cause {

    private transient FlowRun flowRun;
    
    private transient JobInvocation associatedJob;

    private final String cause;

    public FlowCause(FlowRun flowRun, JobInvocation associatedJob) {
        this.flowRun = flowRun;
        this.cause = flowRun.getParent().getFullName() + "#" + flowRun.getNumber();
        this.associatedJob = associatedJob;
    }

    public FlowRun getFlowRun() {
        if (this.flowRun == null) {
            int i = cause.lastIndexOf('#');
            BuildFlow flow = Jenkins.getInstance().getItemByFullName(cause.substring(0,i), BuildFlow.class);
            flowRun = flow.getBuildByNumber(Integer.parseInt(cause.substring(i+1)));
        }
        return flowRun;
    }

    public JobInvocation getAssociatedJob() {
    	return associatedJob;
    }

    @Override
    public String getShortDescription() {
        return "Started by build flow " + cause;
    }
}
