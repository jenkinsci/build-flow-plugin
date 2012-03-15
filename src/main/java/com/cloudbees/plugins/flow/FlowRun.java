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

    private transient ThreadLocal<Run<?,?>> last = new ThreadLocal<Run<?,?>>();

    public FlowRun(BuildFlow job) throws IOException {
        super(job);
        this.dsl = job.getDsl();
        this.builds.addVertex(this); // Initial vertex for the build DAG
        last.set(this);
    }

    public FlowRun(BuildFlow project, File buildDir) throws IOException {
        super(project, buildDir);
        this.dsl = project.getDsl();
        this.builds.addVertex(this); // Initial vertex for the build DAG
        last.set(this);
    }

    public DirectedGraph<Run<?, ?>, String> getBuilds() {
        return builds;
    }

    public BuildFlow getBuildFlow() {
        return project;
    }

    /* package */ Run<?,?> getLocalBuild() {
        return last.get();
    }

    /* package */ void setLocalBuild(Run<?,?> run) {
        last.set(run);
    }

    public void addBuild(AbstractBuild<?,?> build) throws DirectedAcyclicGraph.CycleFoundException {
        Run<?, ?> from = last.get();
        builds.addVertex(build);
        String edge = from.toString() + " => " + build.toString();
        LOGGER.fine("added build to execution graph " + edge);
        builds.addEdge(from, build, edge);
        last.set(build);
    }

    /**
     * {Run#setResult} only let result get worst. For the build-flow we support result reset to manage
     * retry or try+finally logic
     */
    @Override
    public void setResult(Result result) {
        // result only can change when building
        assert isBuilding();

        this.result = result;
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
