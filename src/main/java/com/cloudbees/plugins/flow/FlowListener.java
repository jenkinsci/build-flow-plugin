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

import java.util.List;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.Cause;
import hudson.model.listeners.RunListener;

@Extension
public class FlowListener extends RunListener<Run<?, ?>> {

    @Override
    public synchronized void onStarted(Run<?, ?> startedBuild,
            TaskListener listener) {
        List<Cause> causes = startedBuild.getCauses();
        for (Cause cause : causes) {
            if (cause instanceof FlowCause) {
                ((FlowCause) cause).getAssociatedJob().buildStarted(
                        startedBuild);
            }
        }
    }

    @Override
    public synchronized void onCompleted(Run<?, ?> finishedBuild,
            TaskListener listener) {
        List<Cause> causes = finishedBuild.getCauses();
        for (Cause cause : causes) {
            if (cause instanceof FlowCause) {
                ((FlowCause) cause).getAssociatedJob().buildCompleted();
            }
        }
    }

    @Override
    public synchronized void onFinalized(Run<?, ?> finalizedBuild) {
        List<Cause> causes = finalizedBuild.getCauses();
        for (Cause cause : causes) {
            if (cause instanceof FlowCause) {
                ((FlowCause) cause).getAssociatedJob().buildFinalized();
            }
        }
    }
}
