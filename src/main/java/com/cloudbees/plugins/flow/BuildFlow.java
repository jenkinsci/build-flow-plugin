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
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.BallColor;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Defines the orchestration logic for a build flow as a succession o jobs to be executed and chained together
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildFlow extends AbstractItem implements TopLevelItem {

    public BuildFlow(ItemGroup parent, String name) {
        super(parent,name);
    }

    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit(StaplerRequest req,
                                            StaplerResponse rsp) throws IOException, ServletException, FormException {
        checkPermission(CONFIGURE);
        requirePOST();

        JSONObject json = req.getSubmittedForm();
        // TODO (re)configure buildFlow

        save();
    }

    @Override
    public Collection<? extends Job> getAllJobs() {
        return Collections.<Job>emptyList();
    }

    public FlowIcon getIconColor() {
        return new FlowIcon();
    }

    public static Collection<BuildFlow> all() {
        return Util.createSubList(Jenkins.getInstance().getItems(), BuildFlow.class);
    }

    @Extension
    public static final BuildFlowDescriptor DESCRIPTOR = new BuildFlowDescriptor();

    public BuildFlowDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public void onCompleted(Run run) {
        // TODO trigger next step of the flow
    }

    /**
     * Start a new execution of the build flow, running the entry-point job
     */
    public void newRun(AbstractBuild<?,?> build) {
        new FlowRun(this).onStarted(build);
    }

    public static class BuildFlowDescriptor extends TopLevelItemDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.BuildFlow_Messages();
        }

        @Override
        public boolean isApplicable(Descriptor descriptor) {
            return descriptor.isSubTypeOf(AbstractFlow.class);
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new BuildFlow(parent, name);
        }
    } 
    
}
