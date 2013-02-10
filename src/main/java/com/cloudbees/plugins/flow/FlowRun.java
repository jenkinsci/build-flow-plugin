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

import hudson.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;

/**
 * Maintain the state of execution of a build flow as a chain of triggered jobs
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class FlowRun extends Build<BuildFlow, FlowRun> {

    private static final Logger LOGGER = Logger.getLogger(FlowRun.class.getName());
    
    private String dsl;

    private JobInvocation.Start startJob = new JobInvocation.Start(this);

    private DirectedGraph<JobInvocation, String> jobsGraph = new SimpleDirectedGraph<JobInvocation, String>(String.class);

    private transient ThreadLocal<FlowState> state = new ThreadLocal<FlowState>();
    
    private transient AtomicInteger buildIndex = new AtomicInteger(1);

    public FlowRun(BuildFlow job, File buildDir) throws IOException {
        super(job, buildDir);
        setup(job);
    }

    public FlowRun(BuildFlow job) throws IOException {
        super(job);
        setup(job);
    }

    private void setup(BuildFlow job) {
        this.dsl = job.getDsl();
        jobsGraph.addVertex(startJob);
        state.set(new FlowState(SUCCESS, startJob));
    }

    /* package */ Run  run(JobInvocation job, List<Action> actions) throws ExecutionException, InterruptedException {
        job.setBuildIndex(buildIndex.getAndIncrement());
        job.run(new FlowCause(this), actions);
        addBuild(job);
        getState().setResult(job.getResult());
        return job.getBuild();
    }

    /* package */ FlowState getState() {
        return state.get();
    }

    /* package */ void setState(FlowState s) {
        state.set(s);
    }

    public DirectedGraph<JobInvocation, String> getJobsGraph() {
        return jobsGraph;
    }

    public JobInvocation getStartJob() {
        return startJob;
    }

    public BuildFlow getBuildFlow() {
        return project;
    }

    public void doGetDot(StaplerRequest req, StaplerResponse rsp) throws IOException {
        new DOTExporter().export(rsp.getWriter(), jobsGraph);
    }

    public synchronized void addBuild(JobInvocation job) throws ExecutionException, InterruptedException {
        jobsGraph.addVertex(job);
        for (JobInvocation up : state.get().getLastCompleted()) {
            String edge = up.getId() + " => " + job.getId();
            LOGGER.fine("added build to execution graph " + edge);
            jobsGraph.addEdge(up, job, edge);
        }
        state.get().setLastCompleted(job);
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
            if(!preBuild(listener, project.getPublishersList()))
                return FAILURE;

            try {
                setResult(SUCCESS);
                new FlowDSL().executeFlowScript(FlowRun.this, dsl, listener);
            } finally {
                boolean failed=false;
                for( int i=buildEnvironments.size()-1; i>=0; i-- ) {
                    if (!buildEnvironments.get(i).tearDown(FlowRun.this,listener)) {
                        failed=true;
                    }
                }
                if (failed) return Result.FAILURE;
            }
            return getState().getResult();
        }

        @Override
        public void post2(BuildListener listener) throws IOException, InterruptedException {
            if(!performAllBuildSteps(listener, project.getPublishersList(), true))
                setResult(FAILURE);
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {
            performAllBuildSteps(listener, project.getPublishersList(), false);
            super.cleanUp(listener);
        }
    }

}
