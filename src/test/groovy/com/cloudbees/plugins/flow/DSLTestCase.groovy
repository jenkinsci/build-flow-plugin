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

import com.cloudbees.plugin.flow.UnstableBuilder
import com.cloudbees.plugins.flow.FlowDSL
import hudson.model.Result
import org.jvnet.hudson.test.FailureBuilder
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
import com.cloudbees.plugin.flow.BlockingBuilder
import hudson.model.Job
import org.junit.Assert
import org.junit.Rule
import org.junit.rules.TestName
import org.jvnet.hudson.test.JenkinsRule


import static hudson.model.Result.UNSTABLE

abstract class DSLTestCase {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule()

    @Rule
    public TestName name = new TestName()

    def createJob = {String name ->
        return jenkinsRule.createFreeStyleProject(name);
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
        job.onCreatedFromScratch() // need this to updateTransientActions
        return job
    }

    def createBlockingJob = {String name, File file = BlockingBuilder.DEFAULT_FILE ->
        def job = createJob(name)
        job.getBuildersList().add(new BlockingBuilder(file));
        job.onCreatedFromScratch() // need this to updateTransientActions
        return job
    }

    def run = { script ->
        BuildFlow flow = new BuildFlow(Jenkins.instance, name.getMethodName())
        flow.dsl = script
        flow.onCreatedFromScratch() // need this to updateTransientActions
        return flow.scheduleBuild2(0).get()
    }

    def runWithWorkspace = { script ->
        BuildFlow flow = new BuildFlow(Jenkins.instance, name.getMethodName())
        flow.dsl = script
        flow.buildNeedsWorkspace = true
        flow.onCreatedFromScratch() // need this to updateTransientActions
        return flow.scheduleBuild2(0).get()
    }

    def schedule = { script ->
        BuildFlow flow = new BuildFlow(Jenkins.instance, name.getMethodName())
        flow.dsl = script
        flow.onCreatedFromScratch() // need this to updateTransientActions
        return flow.scheduleBuild2(0)
    }

    def runWithCause = { script, cause ->
        BuildFlow flow = new BuildFlow(Jenkins.instance, name.getMethodName())
        flow.dsl = script
        flow.onCreatedFromScratch() // need this to updateTransientActions
        return flow.scheduleBuild2(0, cause).get()
    }

    def assertSuccess = { job ->
        Assert.assertNotNull("job ${job.name} didn't run", job.builds.lastBuild)
        assert SUCCESS == job.builds.lastBuild.result
        return job.builds.lastBuild
    }

    def assertDidNotRun = { job ->
        assert 0 == job.builds.size()
    }

    def assertAllSuccess = { jobs ->
        jobs.each {
            Assert.assertNotNull("job ${it.name} didn't run", it.builds.lastBuild)
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
        Assert.assertTrue("build don't have expected parameter set " + name + "=" + value, found)
    }

    void assertRan(Job job, int times, Result result) {
        assert job.builds.size() == times
        job.builds.each { build ->
            assert build.result == result
        }
    }
}
