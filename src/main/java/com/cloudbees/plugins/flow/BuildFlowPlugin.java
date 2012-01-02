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

import hudson.Extension;
import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildFlowPlugin extends Plugin {

    /**
     * A RunListener to watch Job builds and trigger downstream jobs according to flow definition
     */
    @Extension
    public static class FlowListener extends RunListener<Run> {

        @Override
        public void onCompleted(Run run, TaskListener listener) {

            BuildFlowAction flowAction = run.getAction(BuildFlowAction.class);
            flowAction.getFlow().onCompleted(run);
            // TODO let the flow trigger next step(s);
        }
    }
}
