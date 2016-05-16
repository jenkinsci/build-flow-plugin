/*
 * The MIT License
 *
 * Copyright (c) 2013, CloudBees, Inc., Nicolas De Loof.
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

import hudson.Extension;
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.model.Queue.FlyweightTask;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Publisher;
import hudson.Util;
import hudson.util.AlternativeUiTextProvider;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.triggers.SCMTriggerItem;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import groovy.lang.GroovyShell;

/**
 * Defines the orchestration logic for a build flow as a succession of jobs to be executed and chained together
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildFlow extends Project<BuildFlow, FlowRun> implements TopLevelItem, FlyweightTask, SCMTriggerItem {

    private final FlowIcon icon = new FlowIcon();

    private String dsl;
    private String dslFile;

    private boolean buildNeedsWorkspace;


    public BuildFlow(ItemGroup parent, String name) {
        super(parent, name);
    }

    public String getDsl() {
        return dsl;
    }

    public void setDsl(String dsl) {
        this.dsl = dsl;
    }

    public boolean getBuildNeedsWorkspace() {
        return buildNeedsWorkspace;
    }

    public void setBuildNeedsWorkspace(boolean buildNeedsWorkspace) {
        this.buildNeedsWorkspace = buildNeedsWorkspace;
    }

    public String getDslFile() {
        return dslFile;
    }

    public void setDslFile(String dslFile) {
        this.dslFile = dslFile;
    }

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        super.submit(req, rsp);
        JSONObject json = req.getSubmittedForm();
        this.buildNeedsWorkspace = json.containsKey("buildNeedsWorkspace");
        if (Jenkins.getInstance().hasPermission(Jenkins.RUN_SCRIPTS)) {
            this.dsl = json.getString("dsl");
            if (this.buildNeedsWorkspace) {
                JSONObject o = json.getJSONObject("buildNeedsWorkspace");
                this.dslFile = Util.fixEmptyAndTrim(o.getString("dslFile"));
            } else {
                this.dslFile = null;
            }
        }
    }

    @Extension
    public static final BuildFlowDescriptor DESCRIPTOR = new BuildFlowDescriptor();

    public BuildFlowDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, Messages.BuildFlow_Messages());
    }


    public static class BuildFlowDescriptor extends AbstractProjectDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.BuildFlow_Messages();
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new BuildFlow(parent, name);
        }

        public FormValidation doCheckDsl(@QueryParameter String value) {
            // Require RUN_SCRIPTS permission, otherwise print a warning that no edits are possible
            if (!Jenkins.getInstance().hasPermission(Jenkins.RUN_SCRIPTS)) { 
                return FormValidation.warning(Messages.BuildFlow_InsufficientPermissions());
            }
            
            try {
            	new GroovyShell().parse(value);
			} 
			catch (MultipleCompilationErrorsException e) {
				return FormValidation.error( e.getMessage());
			} 
            return FormValidation.ok();
        }
      
    }

    @Override
    protected Class<FlowRun> getBuildClass() {
        return FlowRun.class;
    }

}
