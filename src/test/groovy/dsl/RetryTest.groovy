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
import static hudson.model.Result.FAILURE
import hudson.model.Job

class RetryTest extends DSLTestCase {

    def successBuild =  """
        3.times retry {
            build("job1")
        }
    """

    public void testNoRetry() {
        Job job1 = createJob("job1")
        def ret = run(successBuild)
        assertSuccess(job1)
        assert job1.builds.size() == 1
        assert SUCCESS == ret.result
    }

    def retryBuild =  """
        3.times retry {
            build("willFail")
        }
    """

    public void testRetry() {
        def job1 = createFailJob("willFail")
        def ret = run(retryBuild)
        
        assertRan(job1, 3, FAILURE)
        assert SUCCESS == ret.result // TODO : should return failure
    }
    
    def retryGuardBuild =  """
        3.times retry {
            guard {
                build("willFail")
            } rescue {
                build("rescue")
            }
        }
    """

    public void testRetryGuard() {
        def fail = createFailJob("willFail")
        def rescue = createJob("rescue")
        def ret = run(retryGuardBuild)

        assertRan(fail, 3, FAILURE)
        assertRan(rescue, 3, SUCCESS)
        assert SUCCESS == ret.result // TODO : should return failure
    }

    def retryGuardParBuild =  """
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
    """

    public void testRetryGuardPar() {
        def fail = createFailJob("willFail")
        def jobs = createJobs(["job1", "job2"])
        def ret = run(retryGuardBuild)
        assert SUCCESS == ret.result // TODO : should return failure
    }

    private void assertRan(Job job, int times, Result result) {
        assert job.builds.size() == times
        job.builds.each { build ->
            assert build.result == result
        }
    }


}
