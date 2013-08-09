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

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Maintain the state of execution of a build flow as a chain of triggered jobs
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class FlowRun extends Build<BuildFlow, FlowRun> {

    private static final Logger LOGGER = Logger.getLogger(FlowRun.class.getName());
    
    private String dsl;

    private JobInvocation.Start startJob;

    private DirectedGraph<JobInvocation, JobEdge> jobsGraph;

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
        if (jobsGraph == null) {
            jobsGraph = new SimpleDirectedGraph<JobInvocation, JobEdge>(JobEdge.class);
        }
        if (startJob == null) {
            startJob = new JobInvocation.Start(this);
        }
        this.dsl = job.getDsl();
        startJob.buildStarted(this);
        jobsGraph.addVertex(startJob);
        state.set(new FlowState(SUCCESS, startJob));
    }

    /* package */ void schedule(JobInvocation job, List<Action> actions) throws ExecutionException, InterruptedException {
        addBuild(job);
        job.run(new FlowCause(this, job), actions);
    }

    /* package */ Run waitForCompletion(JobInvocation job) throws ExecutionException, InterruptedException {
        job.waitForCompletion();
        getState().setResult(job.getResult());
        return job.getBuild();
    }

    /* package */ FlowState getState() {
        return state.get();
    }

    /* package */ void setState(FlowState s) {
        state.set(s);
    }

    public DirectedGraph<JobInvocation, JobEdge> getJobsGraph() {
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
            jobsGraph.addEdge(up, job, new JobEdge(up, job));
        }
        state.get().setLastCompleted(job);
    }

    @Override
    public void run() {
        execute(new RunnerImpl(dsl));
    }
    
    protected class RunnerImpl extends RunExecution {

        private final String dsl;

        public RunnerImpl(String dsl) {
            this.dsl = dsl;
        }

        @Override
        public Result run(BuildListener listener) throws Exception, RunnerAbortedException {
            setResult(SUCCESS);
            new FlowDSL().executeFlowScript(FlowRun.this, dsl, listener);
            return getState().getResult();
        }

        @Override
        public void post(BuildListener listener) throws Exception {
            FlowRun.this.startJob.buildCompleted();
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {

        }
    }

    public static class JobEdge {

        private JobInvocation source;
        private JobInvocation target;

        public JobEdge(JobInvocation source, JobInvocation target) {
            this.source = source;
            this.target = target;
        }

        public JobInvocation getSource() {
            return source;
        }

        public JobInvocation getTarget() {
            return target;
        }

    }

}
