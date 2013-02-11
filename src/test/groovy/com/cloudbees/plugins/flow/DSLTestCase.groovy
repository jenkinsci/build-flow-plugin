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

import com.cloudbees.plugin.flow.UnstableBuilder
import com.cloudbees.plugins.flow.FlowDSL
import hudson.model.Result
import org.jvnet.hudson.test.FailureBuilder
import org.jvnet.hudson.test.HudsonTestCase
import hudson.model.AbstractBuild
import hudson.model.ParametersAction
import com.cloudbees.plugins.flow.BuildFlow
import jenkins.model.Jenkins
import static hudson.model.Result.SUCCESS
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import static hudson.model.Result.FAILURE
import java.util.logging.Logger
import java.util.logging.Level
import java.util.logging.Handler
import java.util.logging.ConsoleHandler
import com.cloudbees.plugins.flow.JobInvocation
import com.cloudbees.plugins.flow.FlowDelegate
import hudson.Launcher
import hudson.model.BuildListener
import hudson.tasks.Builder
import com.cloudbees.plugin.flow.ConfigurableFailureBuilder
import hudson.model.Job

import static hudson.model.Result.UNSTABLE

abstract class DSLTestCase extends HudsonTestCase {

    def createJob = {String name ->
        return createFreeStyleProject(name);
    }

     def createJobs = { names ->
         def jobs = []
         names.each {
             jobs.add(createJob(it))
         }
         return jobs
    }

    def createFailJob = {String name, int failures = Integer.MAX_VALUE ->
        def job = createJob(name)
        job.getBuildersList().add(new ConfigurableFailureBuilder(failures));
        return job
    }

    def createUnstableJob = {String name ->
        def job = createJob(name)
        job.getBuildersList().add(new UnstableBuilder());
        return job
    }


    def run = { script ->
        BuildFlow flow = new BuildFlow(Jenkins.instance, getName())
        flow.dsl = script
        return flow.scheduleBuild2(0).get()
    }

    def runWithCause = { script, cause ->
        BuildFlow flow = new BuildFlow(Jenkins.instance, getName())
        flow.dsl = script
        return flow.scheduleBuild2(0, cause).get()
    }

    def assertSuccess = { job ->
        assertNotNull("job ${job.name} didn't run", job.builds.lastBuild)
        assert SUCCESS == job.builds.lastBuild.result
        return job.builds.lastBuild
    }

    def assertDidNotRun = { job ->
        assert 0 == job.builds.size()
    }

    def assertAllSuccess = { jobs ->
        jobs.each {
            assertNotNull("job ${it.name} didn't run", it.builds.lastBuild)
            assert SUCCESS == it.builds.lastBuild.result
        }
    }

    def assertFailure = { job ->
        assert FAILURE == job.builds.lastBuild.result
    }

    def assertUnstable = { job ->
        assert UNSTABLE == job.builds.lastBuild.result
    }

    def assertException(Class<? extends Exception> exClass, Closure closure) {
        def thrown = false
        try {
            closure()
        } catch (Exception e) {
            if (exClass.isAssignableFrom(e.getClass())) {
                thrown = true
            }
        }
        assert thrown
    }

    void assertHasParameter(Job job, String name, String value) {
        assertHasParameter(job.builds.lastBuild, name, value)
    }

    void assertHasParameter(AbstractBuild build, String name, String value) {
        boolean found = false
        build.actions.each {action ->
            if (action instanceof ParametersAction)
                if (action.getParameter(name)?.value == value) {
                    found = true
                    return
                }
        }
        assertTrue("build don't have expected parameter set " + name + "=" + value, found)
    }

    void assertRan(Job job, int times, Result result) {
        assert job.builds.size() == times
        job.builds.each { build ->
            assert build.result == result
        }
    }
}
