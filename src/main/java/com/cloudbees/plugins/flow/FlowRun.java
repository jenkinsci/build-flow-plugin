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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
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

    private JobInvocation.Start startJob = new JobInvocation.Start(this);

    private DirectedGraph<JobInvocation, JobEdge> jobsGraph = new SimpleDirectedGraph<JobInvocation, JobEdge>(JobEdge.class);

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
        startJob.buildStarted(this);
        jobsGraph.addVertex(startJob);
        state.set(new FlowState(SUCCESS, startJob));
    }

    /* package */ Run  run(JobInvocation job, List<Action> actions) throws ExecutionException, InterruptedException {
        addBuild(job);
        job.run(new FlowCause(this, job), actions);
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

    /**
     * Assigns a unique row and column to each build in the graph
     */
    private void setupDisplayGrid() {
        List<List<JobInvocation>> allPaths = findAllPaths(startJob);
        // make the longer paths bubble up to the top
        Collections.sort(allPaths, new Comparator<List<JobInvocation>>() {
            public int compare(List<JobInvocation> job1, List<JobInvocation> job2) {
                return job2.size() - job1.size();
            }
        });
        // set the build row and column of each build
        // loop backwards through the rows so that the lowest path a job is on
        // will be assigned
        for (int row = allPaths.size() - 1; row >= 0; row--) {
            List<JobInvocation> path = allPaths.get(row);
            for (int column = 0; column < path.size(); column++) {
                JobInvocation job = path.get(column);
                job.setDisplayColumn(Math.max(job.getDisplayColumn(), column));
                job.setDisplayRow(row);
            }
        }
    }

    /**
     * Finds all paths that start at the given vertex
     * @param start the origin
     * @return a list of paths
     */
    private List<List<JobInvocation>> findAllPaths(JobInvocation start) {
        List<List<JobInvocation>> allPaths = new LinkedList<List<JobInvocation>>();
        if (jobsGraph.outDegreeOf(start) == 0) {
            // base case
            List<JobInvocation> singlePath = new LinkedList<JobInvocation>();
            singlePath.add(start);
            allPaths.add(singlePath);
        } else {
            for (JobEdge edge : jobsGraph.outgoingEdgesOf(start)) {
                List<List<JobInvocation>> allPathsFromTarget = findAllPaths(edge.getTarget());
                for (List<JobInvocation> path : allPathsFromTarget) {
                    path.add(0, start);
                }
                allPaths.addAll(allPathsFromTarget);
            }
        }
        return allPaths;
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
        job.setBuildIndex(buildIndex.getAndIncrement());
        jobsGraph.addVertex(job);
        for (JobInvocation up : state.get().getLastCompleted()) {
            String edge = up.getId() + " => " + job.getId();
            LOGGER.fine("added build to execution graph " + edge);
            jobsGraph.addEdge(up, job, new JobEdge(up, job));
        }
        state.get().setLastCompleted(job);
        setupDisplayGrid();
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
            FlowRun.this.getStartJob().buildCompleted();
            super.cleanUp(listener);
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
