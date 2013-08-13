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

import hudson.model.ParametersDefinitionProperty
import hudson.model.RunParameterDefinition
import jenkins.model.Jenkins

import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS

class ConcurrencyTest extends DSLTestCase {

    /**
     * Test that triggering multiple builds of the same concurrent build is ok.
     * This relies on the default quiet period of 5 seconds.
     */
    public void testConcurrency() {
        Jenkins.instance.numExecutors = 8
        Jenkins.instance.reload()

        File f1 = new File("target/concjob1.block")
        // this will prevent job1 from running.
        f1.createNewFile();

        def concjob1 = createBlockingJob("concjob1", f1)
        concjob1.concurrentBuild = true
        concjob1.properties.put(ParametersDefinitionProperty.DescriptorImpl,
                new ParametersDefinitionProperty(new RunParameterDefinition("param1", getName(), "ignored", null)))

        BuildFlow flow = new BuildFlow(Jenkins.instance, getName())
        flow.concurrentBuild = true;
        flow.dsl = """  build("concjob1", param1: build.number)  """

        def sfr1 = flow.scheduleBuild2(0)
        def fr1 = sfr1.waitForStart()

        def sfr2 = flow.scheduleBuild2(0)
        def fr2 = sfr2.waitForStart()

        def sfr3 = flow.scheduleBuild2(0)
        def fr3 = sfr3.waitForStart()

        def sfr4 = flow.scheduleBuild2(0)
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


    /**
     * Test that triggering multiple builds of the same non-concurrent build gives an appropriate error.
     * This relies on the default quiet period of 5 seconds.
     */
    public void testConcurrencyWithNonConcurrentDownstream() {
        Jenkins.instance.numExecutors = 8
        Jenkins.instance.reload()

        File f1 = new File("target/concjob2.block")
        // this will prevent job1 from running.
        f1.createNewFile();

        def concjob2 = createBlockingJob("concjob2", f1)

        BuildFlow flow = new BuildFlow(Jenkins.instance, getName())
        flow.concurrentBuild = true;
        flow.dsl = """  build("concjob2")  """

        def sfr1 = flow.scheduleBuild2(0)
        def fr1 = sfr1.waitForStart()

        def sfr2 = flow.scheduleBuild2(0)
        def fr2 = sfr2.waitForStart()

        def sfr3 = flow.scheduleBuild2(0)
        def fr3 = sfr3.waitForStart()

        def sfr4 = flow.scheduleBuild2(0)
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

        // all 4 jobs should not have completed successfully as some should have failed to scheduled a job.
        // however there is some timing involved here so it could be a little intermittent.
        assertBuildStatusSuccess(fr1);
        // some of the next three should have failed.
        def atLeastOneRunFailed = false;
        for (run in [fr2, fr3, fr4] ) {
            if (run.result.isWorseOrEqualTo(SUCCESS)) {
                atLeastOneRunFailed = true
                assertLogContains("Could not schedule job concjob2, ensure it is not already queued with the same parameters or is not disabled", run)
            }
        }
        assertTrue("At least on build should have failed", atLeastOneRunFailed)
    }
}
