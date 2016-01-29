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

import hudson.model.Cause
import hudson.model.FreeStyleBuild
import hudson.model.Job

import static hudson.model.Result.SUCCESS
import static hudson.model.Result.FAILURE
import org.junit.Test

public class CombinationTest extends DSLTestCase {

    @Test
    public void testRetryWithGuard() {
        Job job1 = createJob("job1")
        Job willFail = createFailJob("willFail")
        def flow = run("""
            retry(3) {
                guard {
                    build("job1")
                } rescue {
                    build("willFail")
                }
            }
        """)
        assertRan(job1, 3, SUCCESS)
        assertRan(willFail, 3, FAILURE)
        assert FAILURE == flow.result
    }

    @Test
    public void testGuardWithParallel() {
        def jobs = createJobs(["job1", "job2", "rescue"])
        Job willFail = createFailJob("willFail")
        def flow = run("""
            guard {
                parallel (
                    { build("job1") },
                    { build("job2") },
                    { build("willFail") }
                )
            } rescue {
                build("rescue")
            }
        """)
        assertAllSuccess(jobs)
        assertRan(willFail, 1, FAILURE)
        assert FAILURE == flow.result
    }

    @Test
    public void testParallelSubFlows() {
        def jobs = createJobs(["job1", "job2", "job3"])
        Job willFail = createFailJob("willFail")
        Job job5 = createJob("job5")

        def flow = run("""
            parallel (
                {
                    guard {
                        build("job1")
                    } rescue {
                        build("job2")
                    }
                },
                {
                    build("job3")
                    retry(3) {
                        build("willFail")
                    }
                }
            )
            build("job5")
        """)
        assertAllSuccess(jobs)
        assertRan(willFail, 3, FAILURE)
        assertDidNotRun(job5)
        assert FAILURE == flow.result
        println flow.jobsGraph.edgeSet()
    }
}
