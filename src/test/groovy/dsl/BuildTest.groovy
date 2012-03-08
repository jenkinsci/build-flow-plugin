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

import com.cloudbees.plugins.flow.JobNotFoundException
import hudson.model.Result
import hudson.model.ParametersAction
import hudson.model.AbstractBuild
import hudson.model.Action
import static hudson.model.Result.SUCCESS
import static hudson.model.Result.FAILURE

class BuildTest extends DSLTestCase {

    def successBuild =  """    build("job1") """

    public void testUnknownJob() {
        def run = run(successBuild)
        assert FAILURE == run.result
    }

    public void testBuildWithoutReturn() {
        def job1 = createJob("job1")
        def run = run(successBuild)
        assert SUCCESS == run.result
        assertSuccess(job1)
    }

    def successBuildParam =  """    build("job1", param1: "one", param2: "two") """

    public void testBuildWithParams() {
        def job1 = createJob("job1")
        run(successBuildParam)

        def build = assertSuccess(job1)
        assertHasParameter(build, "param1", "one")
        assertHasParameter(build, "param2", "two")
    }

    def failBuild = """    build("willFail") """

    public void testBuildWithoutReturnFailed() {
        def willFail = createFailJob("willFail");
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
        def job1 = createJob("job1")
        def run = run(returnBuild)
        assertSuccess(job1)
        assert SUCCESS == run.result
    }
}
