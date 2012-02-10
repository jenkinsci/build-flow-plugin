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

package com.cloudbees.plugins.flow.dsl;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import hudson.model.AbstractBuild
import hudson.model.Result;

import static org.codehaus.groovy.syntax.Types.*;

public class FlowDSL {

    ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false)
        cl(emc)
        emc.initialize()
        return emc
    }

    String executeFlowScript(String dsl) {
        Script dslScript = new GroovyShell().parse(dsl)
        dslScript.metaClass = createEMC(dslScript.class, {
            ExpandoMetaClass emc ->
            emc.flow = {
                Closure cl ->
                cl.delegate = new FlowDelegate()
                cl.resolveStrategy = Closure.DELEGATE_FIRST
                cl()
            }
        })
        dslScript.run()
        return "Cool"
    }
}

public class FlowDelegate {

    def build(String jobName) {
        executeJenkinsJobWithName(jobName);
    }

    def build(Map args, String jobName) {
        executeJenkinsJobWithNameAndArgs(jobName, args);
    }

    /**def methodMissing(String name, Object args) {
        println "On flowdelegate, missing method '${name}' with args '${args}'"
        if (name == FlowDSLSyntax.BUILD_KEYWORD) {
            String jobName = args[0]
            return executeJenkinsJobWith(jobName)
        }
    }    **/

    private def executeJenkinsJobWithName(String name) {
        return executeJenkinsJobWithNameAndArgs(name, [:])
    }

    private def executeJenkinsJobWithNameAndArgs(String name, Map args) {
        // ask for job with name ${name}
        // execute the job and wait for it
        // if fail, then prevent other instructions
        println "\nJenkins is executing job : ${name} with args : ${args}\n"
        return "Done job ${name}"
    }
}

public class FlowDSLSyntax  {
    public final static String BUILD_KEYWORD = "build"
    public final static String CAUSE_KEYWORD = "cause"
    public final static String PARALLEL_KEYWORD = "parallel"
    public final static String GUARD_KEYWORD = "guard"
    public final static String RESCUE_KEYWORD = "rescue"
    public final static String CLEAN_KEYWORD = "clean"
}