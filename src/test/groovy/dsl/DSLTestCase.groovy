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

import com.cloudbees.plugins.flow.dsl.FlowDSL
import hudson.model.Result
import org.jvnet.hudson.test.FailureBuilder
import org.jvnet.hudson.test.HudsonTestCase
import hudson.model.AbstractBuild
import hudson.model.ParametersAction

abstract class DSLTestCase extends HudsonTestCase {

    def createJob = {String name ->
        return createFreeStyleProject(name);
    }

     def createJobs = { names ->
         def jobs = []
         names.each {
             jobs.add(createJob(it))
         }
         return jobs
    }

    def createFailJob = {String name ->
        def job = createJob(name)
        job.getBuildersList().add(new FailureBuilder())
        return job
    }

    def run = { script ->
        return new FlowDSL().executeFlowScript(script, null)
    }

    def runWithCause = { script, cause ->
        return new FlowDSL().executeFlowScript(script, cause)
    }

    def assertSuccess = { job ->
        assert Result.SUCCESS == job.builds.lastBuild.result
        return job.builds.lastBuild
    }

    def assertAllSuccess = { jobs ->
        jobs.each {
            assert Result.SUCCESS == it.builds.lastBuild.result
        }
    }

    def assertFailure = { job ->
        assert Result.FAILURE == job.builds.lastBuild.result
    }

    def assertException(Class<? extends Exception> exClass, Closure closure) {
        def thrown = false
        try {
            closure()
        } catch (Exception e) {
            if (exClass.isAssignableFrom(e.getClass())) {
                thrown = true
            }
        }
        assert thrown
    }

    void assertHasParameter(AbstractBuild build, String name, String value) {
        boolean found = false
        build.actions.each {action ->
            if (action?.getParameter(name)?.value == value) {
                found = true
                return
            }
        }
        assertTrue("build don't have expected parameter set " + name + "=" + value, found)
    }    
}
