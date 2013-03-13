package com.cloudbees.plugin.flow;

import com.cloudbees.plugins.flow.BuildFlowDSLExtension;
import com.cloudbees.plugins.flow.FlowDelegate;
import hudson.Extension;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class TestDSLExtension extends BuildFlowDSLExtension {
    @Override
    public Object createExtension(String extensionName, FlowDelegate dsl) {
        Map m = new TreeMap();
        m.put("dsl",dsl);
        m.put("name",extensionName);
        return m;
    }
}
