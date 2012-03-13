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

import static hudson.model.Result.SUCCESS
import hudson.model.Job
import static hudson.model.Result.FAILURE

class GuardTest extends DSLTestCase {

    public void testGuardPass() {
        def jobs = createJobs(["job1", "job2", "job3", "clean"])
        def ret = run("""
            guard {
                build("job1")
                build("job2")
                build("job3")
            } rescue {
                build("clean")
            }
        """)
        assertAllSuccess(jobs)
        assert SUCCESS == ret.result
    }

    /*public void testGuardWithFail() {
        Job job1 = createJob("job1");
        def failure = createFailJob("fails")
        Job job3 = createJob("job3");
        Job clean = createJob("clean");

        def ret = run("""
            guard {
                build("job1")
                build("fails")
                build("job3")
            } rescue {
                build("clean")
            }
        """)
        assertSuccess(job1)
        assertFailure(failure)
        assertDidNotRun(job3)
        assertSuccess(clean)
        assert FAILURE == ret.result
    }

     def successBuildPar =  """
        def g, r
        guard {
            build("job1")
            par = parallel {
                a = build("job2")
                assert !a.future.done
                b = build("job3")
                assert !b.future.done
            }
            par.values().each {
                assert it.future.done
            }
            g = true
        } rescue {
            build("clean")
            r = true
        }
        assert g
        assert r
    """

    public void testGuardPassPar() {
        def jobs = createJobs(["job1", "job2", "job3", "clean"])
        def ret = run(successBuildPar)
        assertAllSuccess(jobs)
        assert SUCCESS == ret.result
    }

    def successBuildParRetry =  """
        def a = 0
        def g, r
        guard {
            build("job1")
            par = parallel {
                job1 = build("job2")
                assert !job1.future.done
                job2 = build("job3")
                assert !job2.future.done
            }
            par.values().each {
                assert it.future.done
            }
            2.times retry {
                build("job3")
                a++
            }
            g = true
        } rescue {
            build("clean")
            r = true
        }
        assert a == 1
    """

    public void testGuardPassParRetry() {
        def jobs = createJobs(["job1", "job2", "job3", "clean"])
        def ret = run(successBuildParRetry)
        assertAllSuccess(jobs)
        assert SUCCESS == ret.result
    }*/
}
