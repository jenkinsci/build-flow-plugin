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

import hudson.model.Cause
import hudson.model.FreeStyleBuild
import hudson.model.Job

import static hudson.model.Result.SUCCESS
import static hudson.model.Result.FAILURE

public class CombinationTest extends DSLTestCase {

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
