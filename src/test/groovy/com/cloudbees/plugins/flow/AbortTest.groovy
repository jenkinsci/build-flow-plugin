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

/**
 * Tests that when a flow is aborted is it reported correctly.
 * @author James Nord
 */
class AbortTest extends DSLTestCase {


	/**
	 * Tests that when a Flow is aborted it correctly aborts jobs that it started.
	 */
	public void testThatAbortAbortsStartedJobs() {
		File f1 = new File("target/${getName()}_job1.lock")
		f1.createNewFile()

		Job job1 = createBlockingJob("job1", f1)
		Job job2 = createFreeStyleProject("job2")

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

		assertBuildStatus(Result.ABORTED, flow)
		assertBuildStatus(Result.ABORTED, job1.lastBuild)
		assertDidNotRun(job2)
	}
}
