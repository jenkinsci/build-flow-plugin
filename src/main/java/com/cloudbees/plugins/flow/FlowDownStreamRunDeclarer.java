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

import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.buildgraphview.DownStreamRunDeclarer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(optional = true)
public class FlowDownStreamRunDeclarer extends DownStreamRunDeclarer {

    @Override
    public List<Run> getDownStream(Run r) throws ExecutionException, InterruptedException {

        if (r instanceof FlowRun) {
            FlowRun f = (FlowRun) r;
            return getOutgoingEdgeRuns(f, f.getStartJob());
        }

        List<Run> runs = Collections.emptyList();
        FlowCause cause = (FlowCause) r.getCause(FlowCause.class);
        FlowRun f;
        while (runs.isEmpty() && cause != null) {
            f = cause.getFlowRun();
            runs = getOutgoingEdgeRuns(f, cause.getAssociatedJob());
            cause = (FlowCause) cause.getFlowRun().getCause(FlowCause.class);
        }

        return runs;
    }

    private List<Run> getOutgoingEdgeRuns(FlowRun f, JobInvocation start) throws ExecutionException, InterruptedException {
        Set<FlowRun.JobEdge> edges = f.getJobsGraph().outgoingEdgesOf(start);
        List<Run> runs = new ArrayList<Run>(edges.size());
        for (FlowRun.JobEdge edge : edges) {
            runs.add(edge.getTarget().getBuild());
        }
        return runs;
    }
}
