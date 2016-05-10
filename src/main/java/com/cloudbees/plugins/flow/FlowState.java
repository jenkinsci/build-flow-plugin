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

package com.cloudbees.plugins.flow;

import hudson.model.Result;

import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FlowState {

    private Result result;

    private Set<JobInvocation> lastCompleted;

    public FlowState(Result result, Set<JobInvocation> previous) {
        assert result != null;
        this.result = result;
        setLastCompleted(previous);
    }

    public FlowState(Result result, JobInvocation previous) {
        assert result != null;
        this.result = result;
        setLastCompleted(previous);
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        assert result != null;
        this.result = result;
    }

    public Set<JobInvocation> getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(JobInvocation lastCompleted) {
        this.lastCompleted = Collections.singleton(lastCompleted);
    }

    public void setLastCompleted(Set<JobInvocation> lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    public JobInvocation getLastBuild() {
        return this.lastCompleted.iterator().next();
    }

}
