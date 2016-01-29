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

import hudson.model.Cause

import static hudson.model.Result.SUCCESS
import hudson.model.Job
import org.junit.Test

class UpstreamTest extends DSLTestCase {

    @Test
    public void testUpstream() {

        Job job1 = createJob("job1")
        def root = jenkinsRule.createFreeStyleProject("root").createExecutable()
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
