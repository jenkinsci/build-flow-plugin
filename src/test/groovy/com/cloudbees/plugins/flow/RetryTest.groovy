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

import hudson.model.Result
import static hudson.model.Result.SUCCESS
import static hudson.model.Result.UNSTABLE
import static hudson.model.Result.FAILURE
import hudson.model.Job
import org.junit.Test

class RetryTest extends DSLTestCase {

    @Test
    public void testNoRetry() {
        Job job1 = createJob("job1")
        def flow = run("""
            retry(3) {
                build("job1")
            }
        """)
        assertRan(job1, 1, SUCCESS)
        assert SUCCESS == flow.result
    }

    @Test
    public void testRetry() {
        def job1 = createFailJob("willFail")
        def flow = run("""
            retry(3) {
                build("willFail")
            }
        """)
        assertRan(job1, 3, FAILURE)
        assert FAILURE == flow.result
    }

    @Test
    public void testRetryThenSuccess() {
        testRetryThenSuccess("""
            retry(3) {
                build("willFail2times")
            }
        """)
    }

    @Test
    public void testNoQuotesNotation() {
        testRetryThenSuccess("""
            retry 3, {
                build("willFail2times")
            }
        """)
    }

/*
    @Test
    public void testNTimesNotation() {
        testRetryThenSuccess("""
            3.times retry {
                build("willFail2times")
            }
        """)
    }
*/

    public void testRetryThenSuccess(String script) {
        def job1 = createFailJob("willFail2times", 2)
        def flow = run(script)
        assert job1.builds.size() == 3
        assert job1.builds[2].result == FAILURE
        assert job1.builds[1].result == FAILURE
        assert job1.builds[0].result == SUCCESS

        assert SUCCESS == flow.result
        println flow.jobsGraph.edgeSet()
    }
    
    @Test
    public void testRetryGuard() {
        def fail = createFailJob("willFail")
        def rescue = createJob("rescue")
        def flow = run("""
            retry(3) {
                guard {
                    build("willFail")
                } rescue {
                    build("rescue")
                }
            }
        """)

        assertRan(fail, 3, FAILURE)
        assertRan(rescue, 3, SUCCESS)
        assert FAILURE == flow.result
        println flow.jobsGraph.edgeSet()
    }

    @Test
    public void testNoRetryUnstable() {
        Job job1 = createUnstableJob("job1")
        def flow = run("""
            retry(3, 'UNSTABLE') {
                build("job1")
            }
        """)
        assertRan(job1, 1, UNSTABLE)
        assert UNSTABLE == flow.result
    }



}
