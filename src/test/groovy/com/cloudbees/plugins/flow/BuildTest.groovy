/*
 * The MIT License
 *
 * Copyright (c) 2013, CloudBees, Inc., Nicolas De Loof.
 *                     Cisco Systems, Inc., a California corporation
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
import org.jvnet.hudson.test.Issue

import static hudson.model.Result.SUCCESS
import static hudson.model.Result.FAILURE
import jenkins.model.Jenkins
import hudson.model.Job
import hudson.model.Action
import hudson.model.ParametersAction
import hudson.model.ParameterValue
import hudson.model.StringParameterValue
import hudson.model.ParametersDefinitionProperty
import hudson.model.ParameterDefinition
import hudson.model.StringParameterDefinition
import hudson.model.FreeStyleProject

import static hudson.model.Result.UNSTABLE
import org.junit.Test

class BuildTest extends DSLTestCase {

    @Test
    public void testUnknownJob() {
        def flow = run("""
            build("unknown")
        """)
        assert FAILURE == flow.result
        jenkinsRule.assertLogContains("Item unknown not found (or isn't a job).", flow)
    }

    @Test
    public void testDisabledJob() {
        def disabledJob = createJob("disabledJob")
        disabledJob.disable()

        def flow = run("""
            build("disabledJob")
        """)

        jenkinsRule.assertBuildStatus(FAILURE, flow);
        jenkinsRule.assertLogContains("Could not schedule job disabledJob, ensure it is not already queued with the same parameters or is not disabled", flow)
    }

    @Test
    public void testSingleBuild() {
        Job job1 = createJob("job1")
        def flow = run("""
            build("job1")
        """)
        assertSuccess(job1)
        assert SUCCESS == flow.result
    }

    @Test
    public void testBuildWithParams() {
        Job job1 = createJob("job1")
        def flow = run("""
            build("job1",
                  param1: "one",
                  param2: "two")
        """)
        def build = assertSuccess(job1)
        assertHasParameter(build, "param1", "one")
        assertHasParameter(build, "param2", "two")
        assert SUCCESS == flow.result
    }

    @Test
    public void testBuildWithParamsAsMap() {
        Job job1 = createJob("job1")
        def flow = run("""
            def params = ["param1": "one", "param2": "two"]
            build(params, "job1")
        """)
        def build = assertSuccess(job1)
        assertHasParameter(build, "param1", "one")
        assertHasParameter(build, "param2", "two")
        assert SUCCESS == flow.result
    }

    @Test
    public void testJobFailure() {
        Job willFail = createFailJob("willFail");
        def flow = run("""
            build("willFail")
            build("willNotRun")
        """)
        assertFailure(willFail)
        assert FAILURE == flow.result
    }

    @Test
    public void testJobUnstable() {
        Job unstable = createUnstableJob("unstable");
        def flow = run("""
            build("unstable")
            build("willNotRun")
        """)
        assertUnstable(unstable)
        assert UNSTABLE == flow.result
    }

    @Test
    public void testSequentialBuilds() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def flow = run("""
            build("job1")
            build("job2")
            build("job3")
        """)
        assertAllSuccess(jobs)
        assert SUCCESS == flow.result
        println flow.jobsGraph.edgeSet()
    }

    @Test
    public void testSequentialBuildsWithFailure() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def willFail = createFailJob("willFail")
        def notRan = createJob("notRan")
        def flow = run("""
            build("job1")
            build("job2")
            build("job3")
            build("willFail")
            build("notRan")
        """)
        assertAllSuccess(jobs)
        assertFailure(willFail)
        assertDidNotRun(notRan)
        assert FAILURE == flow.result
        println flow.jobsGraph.edgeSet()
    }

    @Test
    public void testParametersFromBuild() {
        Job job1 = createJob("job1")
        Job job2 = createJob("job2")
        def flow = run("""
            b = build("job1")
            build("job2",
                  param1: b.result.name,
                  param2: b.name)
        """)
        assertSuccess(job1)
        def build = assertSuccess(job2)
        assertHasParameter(build, "param1", "SUCCESS")
        assertHasParameter(build, "param2", "job1")
        assert SUCCESS == flow.result
    }

    @Test
    public void testParametersFromBuildWithDefaultValues() {
        FreeStyleProject job1 = createJob("job1")
        def parametersDefinitions = new ParametersDefinitionProperty(new StringParameterDefinition("param1", "0"), new StringParameterDefinition("param2", "0"))
        job1.addProperty(parametersDefinitions)

        def flow = run("""
            b = build("job1",
                  param1:"1",
                  param3:"3")
        """)

        def build = assertSuccess(job1)
        assertHasParameter(build, "param1", "1")
        assertHasParameter(build, "param2", "0")
        assertHasParameter(build, "param3", "3")
        assert SUCCESS == flow.result
    }

    @Issue("JENKINS-17199")
    @Test
    public void testImportStatement() {
        def flow = run("""
            import java.util.Date;
            println "Hello from date: "+new Date();
        """)
        assert SUCCESS == flow.result
        assert flow.log.contains("Hello from date: ")
    }

    @Test
    public void testParallelThreadName() {
        BuildFlow flow = new BuildFlow(Jenkins.instance, "project_name")
        flow.dsl = """
            parallel(
                    { println Thread.currentThread().name }
            );
        """
        flow.onCreatedFromScratch() // need this to updateTransientActions
        def build = flow.scheduleBuild2(0).get()

        assert SUCCESS == build.result
        assert build.log.contains("BuildFlow parallel statement thread for project_name")
    }
}
