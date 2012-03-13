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

import static hudson.model.Result.SUCCESS
import static hudson.model.Result.FAILURE
import hudson.model.Job

class BuildTest extends DSLTestCase {

    public void testUnknownJob() {
        def run = run("""
            build("unknown")
        """)
        assert FAILURE == run.result
    }

    public void testSingleBuild() {
        Job job1 = createJob("job1")
        def run = run("""
            build("job1")
        """)
        assert SUCCESS == run.result
        assertSuccess(job1)
    }

    public void testSequentialBuilds() {
        Job job1 = createJob("job1")
        Job job2 = createJob("job2")
        Job job3 = createJob("job3")
        def run = run("""
            build("job1")
            build("job2")
            build("job3")
        """)
        assert SUCCESS == run.result
        assertSuccess(job1)
        assertSuccess(job2)
        assertSuccess(job3)
    }

    public void testBuildWithParams() {
        Job job1 = createJob("job1")
        run("""
            build("job1", param1: "one", param2: "two")
        """)
        def build = assertSuccess(job1)
        assertHasParameter(build, "param1", "one")
        assertHasParameter(build, "param2", "two")
    }

    def failBuild = """
        build("willFail")
    """

    public void testBuildWithoutReturnFailed() {
        Job willFail = createFailJob("willFail");
        run(failBuild)
        assertFailure(willFail)
    }

    def returnBuild =  """
        a = build("job1")
        assert a != null
        assert a.result == SUCCESS
        assert a.name == "job1"
    """

    public void testBuildWithReturn() {
        Job job1 = createJob("job1")
        def run = run(returnBuild)
        assertSuccess(job1)
        assert SUCCESS == run.result
    }
}
