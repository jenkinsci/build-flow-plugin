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

import hudson.model.Cause

import static hudson.model.Result.SUCCESS
import hudson.model.Job

class UpstreamTest extends DSLTestCase {

    public void testUpstream() {

        Job job1 = createJob("job1")
        def root = createFreeStyleProject("root").createExecutable()
        def cause = new Cause.UpstreamCause(root)
        def run = runWithCause("""
            build("job1",
                  param1: upstream.parent.name,
                  param2: upstream.number)
        """, cause)
        def build = assertSuccess(job1)
        assertHasParameter(build, "param1", "root")
        assertHasParameter(build, "param2", "1")

        assert SUCCESS == run.result
    }

}
