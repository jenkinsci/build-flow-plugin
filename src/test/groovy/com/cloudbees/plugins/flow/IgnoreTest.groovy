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

    public void testIgnoreJobFailureButRunUnstable() {
        Job willUnstable = createUnstableJob("willUnstable")
        Job willFail = createFailJob("willFail");
        Job wontRun= createJob("wontRun");
        def flow = runWithAbortWhenWorseThan("""
            ignore(FAILURE) {
                build("willUnstable")
                build("willFail")
                build("wontRun")
            }
        """, UNSTABLE)
        assertRan(willUnstable)
        assertFailure(willFail)
        assertDidNotRun(wontRun)
        assert SUCCESS == flow.result
    }

    public void testIgnoreJobFailureButRunFailure() {
        Job willUnstable = createUnstableJob("willUnstable")
        Job willFail = createFailJob("willFail");
        Job willRun= createJob("willRun");
        def flow = runWithAbortWhenWorseThan("""
            ignore(FAILURE) {
                build("willUnstable")
                build("willFail")
                build("willRun")
            }
        """, FAILURE)
        assertRan(willUnstable)
        assertFailure(willFail)
        assertRan(willRun)
        assert SUCCESS == flow.result
    }

    public void testIgnoreJobUnstableButRunUnstable() {
        Job willUnstable = createUnstableJob("willUnstable")
        Job willRun= createJob("willRun");
        def flow = runWithAbortWhenWorseThan("""
            ignore(UNSTABLE) {
                build("willUnstable")
                build("willRun")
            }
        """, UNSTABLE)
        assertRan(willUnstable)
        assertRan(willRun)
        assert SUCCESS == flow.result
    }

    public void testIgnoreJobUnstableButRunFailure() {
        Job willUnstable = createUnstableJob("willUnstable")
        Job willRun= createJob("willRun");
        def flow = runWithAbortWhenWorseThan("""
            ignore(UNSTABLE) {
                build("willUnstable")
                build("willRun")
            }
        """, FAILURE)
        assertRan(willUnstable)
        assertRan(willRun)
        assert SUCCESS == flow.result
    }

}
