/*
 * The MIT License
 *
 * Copyright (c) 2016, Tom Fenelly, Craig Rodrigues
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

import com.cloudbees.plugin.flow.ConfigurableFailureBuilder

import hudson.model.FreeStyleBuild
import hudson.model.FreeStyleProject
import hudson.model.ParametersAction
import hudson.model.ParametersDefinitionProperty
import hudson.model.Job
import hudson.model.Result
import hudson.model.Run

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

import java.io.IOException
import java.util.List

public class PipelineTest extends DSLTestCase {

    @Test
    public void test_SuccessfulPipeline() throws Exception {
        // From a pipeline, build a job that succeeds
        def proj1 = jenkinsRule.createFreeStyleProject("proj1")
        def build_flow1 = createFlow("build_flow1",
                                     """build 'proj1'""")
        def pipeline_1 = createPipeline("pipeline_proj1",
                                        """build job: 'build_flow1'""")
        def pipeline_1_result = pipeline_1.scheduleBuild2(0).get()
        jenkinsRule.assertBuildStatusSuccess(pipeline_1_result)
        jenkinsRule.assertBuildStatusSuccess(proj1.getLastBuild())
    }

    @Test
    public void test_FailedPipeline() throws Exception {
        // From a pipeline, build a job that fails
        def proj2 = jenkinsRule.createFreeStyleProject("proj2")
        proj2.getBuildersList().add(new ConfigurableFailureBuilder(1))
        def build_flow2 = createFlow("build_flow2",
                                     """build 'proj2'""")
        def pipeline_2 = createPipeline("pipeline_proj2",
                                        """build job: 'build_flow2'""")
        def pipeline_2_result = pipeline_2.scheduleBuild2(0).get()
        jenkinsRule.assertBuildStatus(Result.FAILURE, pipeline_2_result)
        jenkinsRule.assertBuildStatus(Result.FAILURE, proj2.getLastBuild())
    }

    private def createPipeline(String name, String script) throws IOException {
        def job = jenkinsRule.jenkins.createProject(WorkflowJob.class, name)
        job.setDefinition(new CpsFlowDefinition("node {" + script + "}", true))
        return job
    }

    private BuildFlow createFlow(String name, String dsl) {
        def job = jenkinsRule.jenkins.createProject(BuildFlow, name)
        job.dsl = dsl
        return job
    }
}
