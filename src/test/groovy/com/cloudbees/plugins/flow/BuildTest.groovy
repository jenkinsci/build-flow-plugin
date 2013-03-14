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

package com.cloudbees.plugins.flow

import hudson.model.Result
import org.jvnet.hudson.test.Bug

import static hudson.model.Result.SUCCESS
import static hudson.model.Result.FAILURE
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

class BuildTest extends DSLTestCase {

    public void testUnknownJob() {
        def flow = run("""
            build("unknown")
        """)
        assert FAILURE == flow.result
    }

    public void testSingleBuild() {
        Job job1 = createJob("job1")
        def flow = run("""
            build("job1")
        """)
        assertSuccess(job1)
        assert SUCCESS == flow.result
    }

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

    public void testJobFailure() {
        Job willFail = createFailJob("willFail");
        def flow = run("""
            build("willFail")
            build("willNotRun")
        """)
        assertFailure(willFail)
        assert FAILURE == flow.result
    }

    public void testJobUnstable() {
        Job unstable = createUnstableJob("unstable");
        def flow = run("""
            build("unstable")
            build("willNotRun")
        """)
        assertUnstable(unstable)
        assert UNSTABLE == flow.result
    }

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

    @Bug(17199)
    public void testImportStatement() {
        def flow = run("""
            import java.util.Date;
            println "Hello from date: "+new Date();
        """)
        assert SUCCESS == flow.result
        assert flow.log.contains("Hello from date: ")
    }
}
