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

package com.cloudbees.plugins.flow

import hudson.model.Result

import static hudson.model.Result.SUCCESS
import static hudson.model.Result.FAILURE
import hudson.model.Job
import jenkins.model.Jenkins
import org.junit.Ignore
import org.junit.Test
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry

class BindingTest extends DSLTestCase {

    @Test
    public void testBindings() {
        Job job1 = createJob("job1")
        def flow = run("""
            out.println("yeah")
            build("job1", param1: build.number)
        """)
        def build = assertSuccess(job1)
        assertHasParameter(build, "param1", "1")
        assert SUCCESS == flow.result
    }

    @Test
    public void testEnvBinding() {
        jenkinsRule.jenkins.globalNodeProperties.add(new EnvironmentVariablesNodeProperty(new Entry("someGlobalProperty", "expectedGlobalPropertyValue")))
        jenkinsRule.jenkins.nodeProperties.add(new EnvironmentVariablesNodeProperty(new Entry("someNodeProperty", "expectedNodePropertyValue")))

        Job job1 = createJob("job1")
        def flow = run("""
            out.println("yeah")
            build("job1", param1: env["someGlobalProperty"], param2: env["someNodeProperty"])
        """)
        def build = assertSuccess(job1)
        assertHasParameter(build, "param1", "expectedGlobalPropertyValue")
        assertHasParameter(build, "param2", "expectedNodePropertyValue")
        assert SUCCESS == flow.result
    }

    @Test
    public void testBuiltinImports() {
        def flow = run("""
            println "test="+SUCCESS.class.name;
        """)
        assert SUCCESS == flow.result
        assert flow.log.contains("test="+ Result.SUCCESS.class.name)
    }
}
