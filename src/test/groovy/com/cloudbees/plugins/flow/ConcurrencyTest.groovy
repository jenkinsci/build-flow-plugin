/*
 * The MIT License
 *
 * Copyright (c) 2013, Cisco Systems, Inc., a California corporation
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

import hudson.model.Cause
import hudson.model.ParametersDefinitionProperty
import hudson.model.RunParameterDefinition
import jenkins.model.Jenkins

import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS
import org.junit.Test
import static org.junit.Assert.assertTrue

class ConcurrencyTest extends DSLTestCase {

    /**
     * Test that triggering multiple builds of the same concurrent build is ok.
     * This relies on the default quiet period of 5 seconds.
     */
    @Test
    public void testConcurrency() {
        Jenkins.instance.numExecutors = 8
        Jenkins.instance.reload()

        File f1 = new File("target", "concjob1.block")
        // this will prevent job1 from running.
        f1.mkdirs();
        f1.createNewFile();

        def concjob1 = createBlockingJob("concjob1", f1)
        concjob1.concurrentBuild = true
        concjob1.properties.put(ParametersDefinitionProperty.DescriptorImpl,
                new ParametersDefinitionProperty(new RunParameterDefinition("param1", name.getMethodName(), "ignored", null)))

        BuildFlow flow = new BuildFlow(Jenkins.instance, name.getMethodName())
        flow.concurrentBuild = true;
        flow.dsl = """  build("concjob1", param1: build.number)  """
        flow.onCreatedFromScratch()

        def sfr1 = flow.scheduleBuild2(0, new Cause.UserIdCause())
        def fr1 = sfr1.waitForStart()

        def sfr2 = flow.scheduleBuild2(0, new Cause.UserIdCause())
        def fr2 = sfr2.waitForStart()

        def sfr3 = flow.scheduleBuild2(0, new Cause.UserIdCause())
        def fr3 = sfr3.waitForStart()

        def sfr4 = flow.scheduleBuild2(0, new Cause.UserIdCause())
        def fr4 = sfr4.waitForStart()

        // we have a 5 second quiet period!
        // This sleep does not add to the execution time of the test as we just wait for the builds to complete anyway.
        Thread.sleep(6000L);
        println("releasing jobs")
        f1.delete();

        // wait for all the flows to finish.
        sfr1.get();
        sfr2.get();
        sfr3.get();
        sfr4.get();

        assertRan(flow, 4, SUCCESS);
        assertRan(concjob1, 4, SUCCESS);
    }

}
