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

import hudson.model.Result
import static hudson.model.Result.SUCCESS
import static hudson.model.Result.FAILURE
import hudson.model.Job

class RetryTest extends DSLTestCase {

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

    public void testRetryThenSuccess() {
        testRetryThenSuccess("""
            retry(3) {
                build("willFail2times")
            }
        """)
    }

    public void testNoQuotesNotation() {
        testRetryThenSuccess("""
            retry 3, {
                build("willFail2times")
            }
        """)
    }

/*
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



}
