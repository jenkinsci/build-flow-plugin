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

import hudson.model.FreeStyleProject
import hudson.model.Job
import hudson.model.ParametersDefinitionProperty
import hudson.model.StringParameterDefinition

import static hudson.model.Result.*

class IgnoreTest extends DSLTestCase {

    public void testIgnoreJobFailure() {
        Job willFail = createFailJob("willFail");
        Job wontRun= createJob("wontRun");
        def flow = run("""
            ignore(FAILURE) {
                build("willFail")
                build("wontRun")
            }
        """)
        assertFailure(willFail)
        assertDidNotRun(wontRun)
        assert SUCCESS == flow.result
    }

    public void testIgnoreJobUnstable() {
        Job unstable = createUnstableJob("unstable");
        Job wontRun= createJob("wontRun");
        def flow = run("""
            ignore(UNSTABLE) {
                build("unstable")
                build("wontRun")
            }
        """)
        assertUnstable(unstable)
        assertDidNotRun(wontRun)
        assert SUCCESS == flow.result
    }

    public void testIgnoreJobUnstableButFailureFailure() {
        Job willFail = createFailJob("willFail");
        Job wontRun= createJob("wontRun");
        def flow = run("""
            ignore(UNSTABLE) {
                build("willFail")
                build("wontRun")
            }
        """)
        assertFailure(willFail)
        assertDidNotRun(wontRun)
        assert FAILURE == flow.result
    }

}
