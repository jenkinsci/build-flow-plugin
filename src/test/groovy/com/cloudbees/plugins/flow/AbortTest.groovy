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

import hudson.model.Job
import hudson.model.Result
import jenkins.model.Jenkins
import hudson.model.Cause.UserIdCause;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import org.junit.Test

/**
 * Tests that when a flow is aborted is it reported correctly.
 * @author James Nord
 */
class AbortTest extends DSLTestCase {


	/**
	 * Tests that when a Flow is aborted it correctly aborts jobs that it started.
	 */
	@Test
	public void testThatAbortAbortsStartedJobs() {
		File f1 = new File("target", "${name.getMethodName()}_job1.lock")
		f1.mkdirs()
		f1.createNewFile()

		def job1 = createBlockingJob("job1", f1)
		def job2 = jenkinsRule.createFreeStyleProject("job2")

		def future = schedule("""
		                     build("job1")
		                     build("job2")
		                     """)

		def flow = future.waitForStart()
		// wait for job1 to start
		while (!job1.building) {
			Thread.sleep(10L)
		}
		println("job has started")

		// abort the flow
		println("aborting...")
		flow.oneOffExecutor.interrupt(Result.ABORTED)
		println("aborting request sent...")
		// wait for the flow to finish executing.
		future.get();

		jenkinsRule.assertBuildStatus(Result.ABORTED, flow)
		jenkinsRule.assertBuildStatus(Result.ABORTED, job1.lastBuild)
		assertDidNotRun(job2)
	}

	/**
	 * Tests that when a Flow is aborted it correctly aborts jobs that it started
	 * in a parallel block.
	 */
	@Test
	public void testThatAbortAbortsStartedJobs_ParallelBlock() {
		File f1 = new File("target", "${name.getMethodName()}_job1.lock")
		File f2 = new File("target", "${name.getMethodName()}_job2.lock")
		f1.mkdirs()
		f1.createNewFile()
		f2.mkdirs()
		f2.createNewFile()

		Job job1 = createBlockingJob("job1", f1)
		Job job2 = createBlockingJob("job2", f2)

		def future = schedule("""
			parallel(
				{ build("job1") },
				{ build("job2") },
			)
			build.state.result = FAILURE
			fail()
		""")

		def flow = future.waitForStart()
		// wait for job1 to start
		while (!job1.building) {
			Thread.sleep(10L)
		}
		println("job has started")

		// abort the flow
		println("aborting...")
		flow.oneOffExecutor.interrupt(Result.ABORTED)
		println("aborting request sent...")
		// wait for the flow to finish executing.
		future.get();

		jenkinsRule.assertBuildStatus(Result.ABORTED, job1.lastBuild)
		jenkinsRule.assertBuildStatus(Result.ABORTED, job2.lastBuild)
		jenkinsRule.assertBuildStatus(Result.ABORTED, flow)
	}

	/**
	 * Ensures an ignored parallel block does not supress the aborted job status.
	 */
	@Test
	public void testThatAbortAbortsStartedJobs_IgnoredParallel() {
		File f1 = new File("target", "${name.getMethodName()}_job1.lock")
		File f2 = new File("target", "${name.getMethodName()}_job2.lock")
		f1.mkdirs()
		f1.createNewFile()
		f2.mkdirs()
		f2.createNewFile()

		Job job1 = createBlockingJob("job1", f1)
		Job job2 = createBlockingJob("job2", f2)

		def future = schedule("""
			ignore(ABORTED) {
				parallel(
					{ build("job1") },
					{ build("job2") },
				)
			}
			build.state.result = FAILURE
			fail()
		""")

		def flow = future.waitForStart()
		// wait for job1 to start
		while (!job1.building) {
			Thread.sleep(10L)
		}
		println("job has started")

		// abort the flow
		println("aborting...")
		flow.oneOffExecutor.interrupt(Result.ABORTED)
		println("aborting request sent...")
		// wait for the flow to finish executing.
		future.get();

		jenkinsRule.assertBuildStatus(Result.ABORTED, job1.lastBuild)
		jenkinsRule.assertBuildStatus(Result.ABORTED, job2.lastBuild)
		jenkinsRule.assertBuildStatus(Result.ABORTED, flow)
	}

	/**
	 * Tests that when a Flow is aborted it correctly aborts jobs that it queued.
	 */
	@Test
	public void testThatAbortAbortsQueuedJobs() {
		File f1 = new File("target", "${name.getMethodName()}_job1.lock")
		f1.createNewFile()

		Job job1 = createBlockingJob("job1", f1)

		BuildFlow flow = new BuildFlow(Jenkins.instance, name.getMethodName())
		flow.concurrentBuild = true;
		flow.onCreatedFromScratch()
		flow.dsl = """  build("job1", param1: build.number)  """

		// Start an initially blocked job to to create a queue.
		def sfr1 = job1.scheduleBuild2(0,new UserIdCause(), new ParametersAction(new StringParameterValue("param1", "first")));
		def fr1 = sfr1.waitForStart()

		println("Starting build flows...")
		// Start three concurrent flows queueing up a job1,
		// wait for the job1 to queue up before starting the next
		// flow to ensure a consistent queue.
		def queue = Jenkins.instance.queue;
		def flows = []
		for (i in 1..3) {
			def scheduled = flow.scheduleBuild2(0)
			def future = scheduled.waitForStart()
			while (queue.getItems(job1).size() < i ) {
				Thread.sleep(10L);
			}
			flows.add([flow:scheduled, future: future])
		}

		// abort the second flow
		println("aborting flow #2...")
		flows[1].future.oneOffExecutor.interrupt(Result.ABORTED)
		println("aborting request sent...")
		// wait for the flow to finish executing.
		jenkinsRule.assertBuildStatus(Result.ABORTED, flows[1].flow.get())

		// Release the blocked job
		System.out.println("releasing jobs")
		f1.delete()

		jenkinsRule.waitUntilNoActivityUpTo(25000)

		assertRan(job1, 3, Result.SUCCESS)

		// Needs to assert that only the aborted build, (param1:2), is missing
		// since no records exist when queued builds are cancelled.
		assertHasParameter(job1.builds[2], 'param1', 'first')
		assertHasParameter(job1.builds[1], 'param1', '1')
		assertHasParameter(job1.builds[0], 'param1', '3')

		// Assert that non aborted flows succeeded.
		jenkinsRule.assertBuildStatusSuccess(flows[0].flow.get())
		jenkinsRule.assertBuildStatusSuccess(flows[2].flow.get())
	}
}
