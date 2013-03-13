package com.cloudbees.plugins.flow;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

/**
 * Extends BuildFlow DSL by inserting additional methods/properties.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class BuildFlowDSLExtension implements ExtensionPoint {
    /**
     * Creates a new DSL extension object.
     *
     * @param extensionName
     *      String that identifies the extension.
     *      This string should primarily the artifact ID of the plugin,
     *      such as "external-resource-dispatcher". The consist naming between extension
     *      and plugin name makes it easier for users to use extensions
     *      from an untyped scripting environment of Groovy.
     *
     *      If a plugin needs to expose multiple extensions from a single plugin,
     *      append suffix to the artifact ID, for example "external-resource-dispatcher.manager"
     *
     * @return
     *      null if this extension doesn't understand the specified extension name.
     */
    public abstract Object createExtension(String extensionName, FlowDelegate dsl);

    /**
     * All the installed extensions.
     */
    public static ExtensionList<BuildFlowDSLExtension> all() {
        return Jenkins.getInstance().getExtensionList(BuildFlowDSLExtension.class);
    }
}
