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

import com.cloudbees.plugins.flow.dsl.Flow;
import com.cloudbees.plugins.flow.dsl.FlowDSL;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor.FormException;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.RunMap;
import hudson.model.RunMap.Constructor;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.util.AlternativeUiTextProvider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import jenkins.model.Jenkins;

/**
 * Defines the orchestration logic for a build flow as a succession o jobs to be executed and chained together
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildFlow extends Job<BuildFlow, FlowRun> implements TopLevelItem {

    private final FlowIcon icon = new FlowIcon();

    protected transient /*final*/ RunMap<FlowRun> runs = new RunMap<FlowRun>();

    private String dsl;

    public BuildFlow(ItemGroup parent, String name) {
        super(parent, name);
        loadRuns();
    }

    @Override
    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        super.onLoad(parent, name);
        loadRuns();
    }

    private void loadRuns() {
        this.runs = new RunMap<FlowRun>();
        // Load previous flowRuns from history
        this.runs.load(this, new Constructor<FlowRun>() {
            public FlowRun create(File dir) throws IOException {
                return new FlowRun(BuildFlow.this, dir);
            }
        });
    }

    public String getDsl() {
        return dsl;
    }
    
    public Flow getFlow() {
        return FlowDSL.evalScript(getDsl());
    }

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        this.dsl = req.getSubmittedForm().getString("dsl");
    }

    @Override
    public Collection<? extends Job> getAllJobs() {
        return Collections.<Job>singleton(this);
    }

    @Override
    public boolean isBuildable() {
        return true;
    }

    @Override
    protected SortedMap<Integer, ? extends FlowRun> _getRuns() {
        return runs.getView();
    }

    @Override
    protected void removeRun(FlowRun run) {
        runs.remove(run);
    }

    protected synchronized FlowRun newRun() throws IOException {
        FlowRun flowRun = new FlowRun(this);
        runs.put(flowRun);
        return flowRun;
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

    @Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, Messages.BuildFlow_Messages());
    }

    /**
     * Start a new execution of the build flow, running the entry-point job
     */
    public void doBuild() throws IOException {
        newRun().start();
    }

    public static class BuildFlowDescriptor extends TopLevelItemDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.BuildFlow_Messages();
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new BuildFlow(parent, name);
        }
    } 
    
}
