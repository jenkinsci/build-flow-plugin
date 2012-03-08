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

class MultipleBuildsTest extends DSLTestCase {

    def successFlow =  """
        flow {
            build("job1")
            build("job2")
            build("job3")
        }
    """

    public void testBuildsWithoutReturn() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def ret = run(successFlow)
        assertAllSuccess(jobs)
        assert Result.SUCCESS == ret
    }

    def failingFlow = """
        flow {
            build("job1")
            build("job2")
            build("job3")
            build("willFail")
        }
    """

    public void testBuildsWithoutReturnFailed() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def willFail = createFailJob("willFail");
        def ret = run(failingFlow)
        assertAllSuccess(jobs)
        assertFailure(willFail)
        assert Result.FAILURE == ret
    }

    def interruptedByFailureFlow = """
        flow {
            build("job1")
            build("willFail")
            build("job2")
            build("job3")
        }
    """

    public void testBuildsWithoutReturnFailedAndDontContinue() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def willFail = createFailJob("willFail");
        def ret = run(interruptedByFailureFlow)
        assertSuccess(jobs.get(0))
        jobs.get(1).getBuilds().isEmpty()
        jobs.get(2).getBuilds().isEmpty()
        assertFailure(willFail)
        assert Result.FAILURE == ret
    }
}
