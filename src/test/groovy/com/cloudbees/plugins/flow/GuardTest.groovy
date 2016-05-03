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
import hudson.model.Job
import static hudson.model.Result.FAILURE
import org.junit.Test

class GuardTest extends DSLTestCase {

    @Test
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

    /*@Test
    public void testGuardWithFail() {
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

    @Test
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
    @Test
    public void testGuardPassParRetry() {
        def jobs = createJobs(["job1", "job2", "job3", "clean"])
        def ret = run(successBuildParRetry)
        assertAllSuccess(jobs)
        assert SUCCESS == ret.result
    }*/
}
