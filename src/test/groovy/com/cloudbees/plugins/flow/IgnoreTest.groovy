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
        def flow = run("""
            ignore(FAILURE) {
                build("willFail")
            }
        """)
        assertFailure(willFail)
        assert SUCCESS == flow.result
    }

    public void testIgnoreJobUnstable() {
        Job unstable = createUnstableJob("unstable");
        def flow = run("""
            ignore(UNSTABLE) {
                build("unstable")
            }
        """)
        assertUnstable(unstable)
        assert SUCCESS == flow.result
    }

    public void testIgnoreJobUnstableButFailureFailure() {
        Job willFail = createFailJob("willFail");
        def flow = run("""
            ignore(UNSTABLE) {
                build("willFail")
            }
        """)
        assertFailure(willFail)
        assert FAILURE == flow.result
    }

}
