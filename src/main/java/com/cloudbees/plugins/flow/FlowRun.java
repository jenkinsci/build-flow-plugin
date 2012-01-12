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

import hudson.model.AbstractBuild;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;

/**
 * Maintain the state of execution of a build flow as a chain of triggered jobs
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class FlowRun extends Run<BuildFlow, FlowRun>{

    public FlowRun(BuildFlow job) throws IOException {
        super(job);
    }

    public FlowRun(BuildFlow project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    public BuildFlow getBuildFlow() {
        return project;
    }

    public void onStarted(AbstractBuild<?, ?> build) {
        // TODO maintain the state of the run as execution of the build flow
        build.addAction(new BuildFlowAction(this));
    }

    public void onCompleted(Run run) {
        // TODO maintain the state of the run as execution of the build flow
    }
}
