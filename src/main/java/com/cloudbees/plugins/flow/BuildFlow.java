/*
 * Copyright (C) 2011 CloudBees Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.cloudbees.plugins.flow;

import hudson.Extension;
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.model.Queue.FlyweightTask;
import hudson.tasks.Publisher;
import hudson.util.AlternativeUiTextProvider;
import hudson.util.DescribableList;
import java.io.IOException;
import java.util.Hashtable;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Defines the orchestration logic for a build flow as a succession o jobs to be executed and chained together
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildFlow extends AbstractProject<BuildFlow, FlowRun> implements TopLevelItem, FlyweightTask, SCMedItem {

    private final FlowIcon icon = new FlowIcon();

    private String dsl;

    public BuildFlow(ItemGroup parent, String name) {
        super(parent, name);
    }

    public String getDsl() {
        return dsl;
    }

    public void setDsl(String dsl) {
        this.dsl = dsl;
    }

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        super.submit(req, rsp);
        this.dsl = req.getSubmittedForm().getString("dsl");
    }

    @Extension
    public static final BuildFlowDescriptor DESCRIPTOR = new BuildFlowDescriptor();

    public BuildFlowDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public void onCompleted(Run run) {
    }

    @Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, Messages.BuildFlow_Messages());
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

    @Override
    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        return new DescribableList<Publisher,Descriptor<Publisher>>(this);
    }

    @Override
    protected Class<FlowRun> getBuildClass() {
        return FlowRun.class;
    }

    @Override
    public boolean isFingerprintConfigured() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void buildDependencyGraph(DependencyGraph graph) {
        // TODO Auto-generated method stub
        
    }

    public AbstractProject<?, ?> asProject() {
	return (AbstractProject) this;
    } 
        
}
