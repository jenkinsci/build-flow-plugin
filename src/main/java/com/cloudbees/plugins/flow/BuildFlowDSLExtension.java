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
