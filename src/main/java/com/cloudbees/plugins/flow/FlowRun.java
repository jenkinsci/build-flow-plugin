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

import hudson.model.Build;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Maintain the state of a build flow execution, as a chain of triggered jobs
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class FlowRun extends Build<BuildFlow, FlowRun> {

    protected FlowRun(BuildFlow job, Calendar timestamp) {
        super(job, timestamp);
    }

    protected FlowRun(BuildFlow project) throws IOException {
        super(project);
    }

    protected FlowRun(BuildFlow project, File buildDir) throws IOException {
        super(project, buildDir);
    }
}
