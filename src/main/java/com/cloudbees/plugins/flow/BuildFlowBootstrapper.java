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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.mail.Folder;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Publisher to let the entry-state job of a build flow bootstrap a new flow run.
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildFlowBootstrapper extends Publisher {

    private final String flow;

    @DataBoundConstructor
    public BuildFlowBootstrapper(String flow) {
        this.flow = flow;
    }
    
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        for (BuildFlow buildFlow : BuildFlow.all()) {
            if (buildFlow.getName().equals(flow)) {
                buildFlow.newRun(build);
                break;
            }
        }
        return true; 
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return Messages.BuildFlowBootstrapper_Messages();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public List<String> doFillFlowItems() {
            List<String> flows = new ArrayList<String>();
            for (BuildFlow flow : BuildFlow.all()) {
                flows.add(flow.getName());
            }
            Collections.sort(flows);
            return flows;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(BuildFlowBootstrapper.class.getName());
}
