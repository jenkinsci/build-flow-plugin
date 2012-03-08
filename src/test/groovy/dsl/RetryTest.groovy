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

class RetryTest extends DSLTestCase {

    def successBuild =  """
        flow {
            def a = 0
            3.times retry {
                build("job1")
                a++
            }
            println "closure run " + a
            assert a == 1
        }
    """

    public void testNoRetry() {
        def job1 = createJob("job1")
        def ret = run(successBuild)
        assertSuccess(job1)
        assert Result.SUCCESS == ret
    }

    def retryBuild =  """
        flow {
            def a = 0
            3.times retry {
                build("willFail")
                a++
            }
            println "closure run " + a
            assert a == 3
        }
    """

    public void testRetry() {
        def job1 = createFailJob("willFail")
        def ret = run(retryBuild)
        assertFailure(job1)
        assert Result.SUCCESS == ret // TODO : should return failure
    }

    def retryGuardBuild =  """
        flow {
            def a = 0, b = 0, c = 0
            3.times retry {
                guard {
                    build("job1")
                    b++
                } rescue {
                    build("willFail")
                    c++
                }
                a++
            }
            assert a == 3
            assert b == 3
            assert c == 3
        }
    """

    public void testRetryGuard() {
        def fail = createFailJob("willFail")
        def jobs = createJobs(["job1", "job2"])
        def ret = run(retryGuardBuild)
        assert Result.SUCCESS == ret // TODO : should return failure
    }

    def retryGuardParBuild =  """
        flow {
            def a = 0, b = 0, c = 0
            3.times retry {
                guard {
                    build("job1")
                    b++
                } rescue {
                    build("willFail")
                    c++
                }
                par = parallel {
                    j1 = build("job1")
                    assert !j1.future.done
                    j2 = bbuild("job2")
                    assert !j2.future.done
                }
                a++
                par.values().each {
                    assert it.future.done
                }
            }
            assert a == 3
            assert b == 3
            assert c == 3
        }
    """

    public void testRetryGuardPar() {
        def fail = createFailJob("willFail")
        def jobs = createJobs(["job1", "job2"])
        def ret = run(retryGuardBuild)
        assert Result.SUCCESS == ret // TODO : should return failure
    }
}
