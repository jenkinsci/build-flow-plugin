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
@Extension
public class FlowDownStreamRunDeclarer extends DownStreamRunDeclarer {

    @Override
    public List<Run> getDownStream(Run r) throws ExecutionException, InterruptedException {

        if (r instanceof FlowRun) {
            FlowRun f = (FlowRun) r;
            return getOutgoingEdgeRuns(f, f.getStartJob());
        }

        FlowCause flow = (FlowCause) r.getCause(FlowCause.class);
        if (flow != null) {
            FlowRun f = flow.getFlowRun();
            return getOutgoingEdgeRuns(f, flow.getAssociatedJob());
        }

        return Collections.emptyList();
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
