/*
 Copyright (C) 2011 CloudBees Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Affero General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, see <http://www.gnu.org/licenses/>.
*/
package com.cloudbees.plugins.flow;

import com.cloudbees.plugins.flow.dsl.Step;

import hudson.model.Cause.UpstreamCause;
import hudson.model.Run;

/**
 * Cause for a job that get triggered as part of a build flow execution
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildFlowCause extends UpstreamCause {

    private final FlowRun flowRun;
    private final Step step;

    public BuildFlowCause(FlowRun flowRun, Step step) {
        super(flowRun);
        this.flowRun = flowRun;
        this.step = step;
    }

    public FlowRun getFlowRun() {
        return flowRun;
    }    
    
    public Step getStep() {
        return step;
    }
}
