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

import static hudson.model.Result.SUCCESS
import hudson.model.Result
import static hudson.model.Result.FAILURE
import org.junit.Test

class ParallelTest extends DSLTestCase {

    @Test
    public void testParallel() {
        def jobs = createJobs(["job1", "job2", "job3", "job4"])
        def flow = run("""
            parallel(
                { build("job1") },
                { build("job2") },
                { build("job3") }
            )
            build("job4")
        """)
        assertAllSuccess(jobs)
        assert SUCCESS == flow.result
        println flow.jobsGraph.edgeSet()
    }

    @Test
    public void testFailOnParallelFailed() {
        createJobs(["job1", "job2"])
        createFailJob("willFail")
        def job4 = createJob("job4")
        def flow = run("""
            parallel (
                { build("job1") },
                { build("job2") },
                { build("willfail") }
            )
            build("job4")
        """)
        assertDidNotRun(job4)
        assert FAILURE == flow.result
        println flow.jobsGraph.edgeSet()
    }


    @Test
    public void testFailOnJobSequenceFailed() {
        def jobs = createJobs(["job1", "job2", "job3"])
        createFailJob("willFail")
        def job4 = createJob("job4")
        def flow = run("""
            parallel (
                {
                    build("job1")
                    build("job2")
                },
                {
                    build("job3")
                    build("willfail")
                }
            )
            build("job4")
        """)
        assertDidNotRun(job4)
        assertAllSuccess(jobs)
        assert FAILURE == flow.result
        println flow.jobsGraph.edgeSet()
    }

    @Test
    public void testGetParallelResults() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def job4 = createJob("job4")
        def flow = run("""
            join = parallel (
                { build("job1") },
                { build("job2") },
                { build("job3") }
            )
            build("job4",
                   r1: join[0].result.name,
                   r2: join[1].lastBuild.parent.name)
        """)
        assertAllSuccess(jobs)
        assertSuccess(job4)
        assertHasParameter(job4, "r1", "SUCCESS")
        assertHasParameter(job4, "r2", "job2")
        assert SUCCESS == flow.result
        println flow.jobsGraph.edgeSet()
    }

    @Test
    public void testParallelMap() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def job4 = createJob("job4")
        def flow = run("""
            join = parallel ([
                first:  { build("job1") },
                second: { build("job2") },
                third:  { build("job3") }
            ])
            build("job4",
                   r1: join.first.result.name,
                   r2: join.second.lastBuild.parent.name)
        """)
        assertAllSuccess(jobs)
        assertSuccess(job4)
        assertHasParameter(job4, "r1", "SUCCESS")
        assertHasParameter(job4, "r2", "job2")
        assert SUCCESS == flow.result
        println flow.jobsGraph.edgeSet()
    }

}
