package com.cloudbees.plugin.flow;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurableFailureBuilder extends Builder {

    private int failures;

    ConfigurableFailureBuilder(int failures) {
        this.failures = failures;
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        build.setResult(failures-- > 0 ? FAILURE : SUCCESS);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public String getDisplayName() {
            return "Configurable failure";
        }
        public ConfigurableFailureBuilder newInstance(StaplerRequest req, JSONObject data) {
            return new ConfigurableFailureBuilder(0);
        }
    }
}
