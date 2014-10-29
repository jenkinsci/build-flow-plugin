/*
 * The MIT License
 *
 * Copyright (c) 2013, CloudBees, Inc., Nicolas De Loof.
 *                     Cisco Systems, Inc., a California corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cloudbees.plugins.flow;

import hudson.model.Cause;
import jenkins.model.Jenkins;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FlowCause extends Cause.UpstreamCause {

    private transient FlowRun flowRun;
    
    private JobInvocation associatedJob;

    private final String cause;

    public FlowCause(FlowRun flowRun, JobInvocation associatedJob) {
        super(flowRun);
        this.flowRun = flowRun;
        this.cause = flowRun.getParent().getFullName() + "#" + flowRun.getNumber();
        this.associatedJob = associatedJob;
    }

    public FlowRun getFlowRun() {
        // TODO(mattmoor): If we upgrade to 1.505+ we can replace usage
        // of this with UpstreamCause#getUpstreamRun().
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

    public String getBuildFlow() {
        int i = cause.lastIndexOf('#');
        return cause.substring(0,i);
    }

    public int getBuildNumber() {
        int i = cause.lastIndexOf('#');
        return Integer.parseInt(cause.substring(i+1));
    }

    @Override
    public String getShortDescription() {
        return "Started by build flow " + cause;
    }
}
