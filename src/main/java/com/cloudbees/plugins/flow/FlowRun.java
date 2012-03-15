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

package com.cloudbees.plugins.flow;

import edu.washington.cac.nms.alertpub.DirectedAcyclicGraph;
import hudson.model.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;

import static hudson.model.Result.SUCCESS;

/**
 * Maintain the state of execution of a build flow as a chain of triggered jobs
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class FlowRun extends AbstractBuild<BuildFlow, FlowRun>{

    private static final Logger LOGGER = Logger.getLogger(FlowRun.class.getName());
    
	private String dsl;

    private DirectedGraph<Run<?,?>, String> builds = new SimpleDirectedGraph<Run<?,?>, String>(String.class);

    private transient ThreadLocal<Run<?,?>> local = new ThreadLocal<Run<?,?>>();

    public FlowRun(BuildFlow job) throws IOException {
        super(job);
        this.dsl = job.getDsl();
        this.builds.addVertex(this); // Initial vertex for the build DAG
        local.set(this);
    }

    public FlowRun(BuildFlow project, File buildDir) throws IOException {
        super(project, buildDir);
        this.dsl = project.getDsl();
        this.builds.addVertex(this); // Initial vertex for the build DAG
        local.set(this);
    }

    public DirectedGraph<Run<?, ?>, String> getBuilds() {
        return builds;
    }

    public BuildFlow getBuildFlow() {
        return project;
    }

    /* package */ Run<?,?> getLocalBuild() {
        return local.get();
    }

    /* package */ void setLocalBuild(Run<?,?> run) {
        local.set(run);
    }

    public void addBuild(AbstractBuild<?,?> build) throws DirectedAcyclicGraph.CycleFoundException {
        Run<?, ?> from = local.get();
        builds.addVertex(build);
        String edge = from.toString() + " => " + build.toString();
        LOGGER.fine("added build to execution graph " + edge);
        builds.addEdge(from, build, edge);
        local.set(build);
    }

    /**
     * Compute the result of the flow execution by combining all results for triggered jobs
     */
    private void computeResult() {
        // Result r = combineWithOutgoing(SUCCESS, this);
        setResult(getLocalResult());
    }

    /**
     * Compute the result for the flow from the local execution point.
     * Designed for use by the DSL
     */
    /* package */ Result getLocalResult() {
        return combineWithIncoming(SUCCESS, local.get());
    }
    
    private Result combineWithOutgoing(Result r, Run build) {
        r = r.combine(build.getResult());
        for (String edge : builds.outgoingEdgesOf(build)) {
            build = builds.getEdgeTarget(edge);
            r = combineWithOutgoing(r, build);
        }
        return r;
    }

    private Result combineWithIncoming(Result r, Run build) {
        r = r.combine(build.getResult());
        for (String edge : builds.incomingEdgesOf(build)) {
            build = builds.getEdgeSource(edge);
            r = combineWithIncoming(r, build);
        }
        return r;
    }


    @Override
    public void run() {
        run(createRunner());        
    }
    
    protected Runner createRunner() {
        return new RunnerImpl(dsl);
    }
    
    protected class RunnerImpl extends AbstractRunner {

        private final String dsl;

        public RunnerImpl(String dsl) {
            this.dsl = dsl;
        }

        protected Result doRun(BuildListener listener) throws Exception {
            try {
                setResult(SUCCESS);
                new FlowDSL().executeFlowScript(FlowRun.this, dsl);
            } finally {
                computeResult();
                boolean failed=false;
                for( int i=buildEnvironments.size()-1; i>=0; i-- ) {
                    if (!buildEnvironments.get(i).tearDown(FlowRun.this,listener)) {
                        failed=true;
                    }
                }
                if (failed) return Result.FAILURE;
            }
            return getResult();
        }

        @Override
        public void post2(BuildListener listener) throws IOException, InterruptedException {
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {
            super.cleanUp(listener);
        }

    }

}
