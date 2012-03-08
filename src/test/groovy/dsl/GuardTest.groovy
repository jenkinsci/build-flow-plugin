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

package dsl

import hudson.model.Result
import static hudson.model.Result.SUCCESS

class GuardTest extends DSLTestCase {

    def successBuild =  """
        guard {
            build("job1")
            build("job2")
            build("job3")
        } rescue {
            build("clean")
        }
    """

    public void testGuardPass() {
        def jobs = createJobs(["job1", "job2", "job3", "clean"])
        def ret = run(successBuild)
        assertAllSuccess(jobs)
        assert SUCCESS == ret.result
    }

    public void testGuardWithFail() {
        def jobs = createJobs(["job1", "job3", "clean"])
        def failure = createFailJob("job2")
        def ret = run(successBuild)
        assertFailure(failure)
        assertSuccess(jobs.get(0))
        jobs.get(1).getBuilds().isEmpty()
        assertSuccess(jobs.get(2))
        assert SUCCESS == ret.result
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
    }
}
