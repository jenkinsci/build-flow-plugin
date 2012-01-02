package com.cloudbees.plugins.flow;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import org.kohsuke.stapler.DataBoundConstructor;

import jenkins.model.Jenkins;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class DslFlow extends AbstractFlow {

    private final String dsl;

    @DataBoundConstructor
    public DslFlow(String dsl) {
        this.dsl = dsl;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AbstractFlow> {

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType.isAssignableFrom(BuildFlow.class);
        }

        @Override
        public String getDisplayName() {
            return "DSL flow";
        }
    }
}
