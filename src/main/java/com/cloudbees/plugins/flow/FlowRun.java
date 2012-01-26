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

import groovy.lang.Closure;
import hudson.model.BooleanParameterValue;
import hudson.model.Cause.UpstreamCause;

import hudson.model.Action;

import com.cloudbees.plugins.flow.dsl.Flow;
import com.cloudbees.plugins.flow.dsl.FlowDSL;
import com.cloudbees.plugins.flow.dsl.Step;
import com.cloudbees.plugins.flow.dsl.Job;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Executor;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

import static hudson.model.Result.FAILURE;

/**
 * Maintain the state of execution of a build flow as a chain of triggered jobs
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class FlowRun extends AbstractBuild<BuildFlow, FlowRun>{
    
	private String dsl;
    private transient Flow flow;

    public FlowRun(BuildFlow job) throws IOException {
        super(job);
        this.dsl = job.getDsl();
        this.flow = job.getFlow();
    }

    public FlowRun(BuildFlow project, File buildDir) throws IOException {
        super(project, buildDir);
        this.flow = project.getFlow();
    }
    
    public Flow getFlow() {
    	if (flow == null) {
    		this.flow = FlowDSL.readFlow(dsl);
    	}
        return flow;
    }

    public BuildFlow getBuildFlow() {
        return project;
    }

    public Map<Job, Future<? extends AbstractBuild<?,?>>> startStep(Step s) {
        Cause cause = new UpstreamCause(this);
        Action flowAction = new BuildFlowAction(this);
        List<Action> actions = new ArrayList<Action>();
        actions.add(flowAction);
        Map<Job, Future<? extends AbstractBuild<?,?>>> futures = new HashMap<Job, Future<? extends AbstractBuild<?,?>>>();
        for (Job j : s.getJobs()) {
        	String jobName = j.getName();
            Item i = Jenkins.getInstance().getItem(jobName);
            if (i instanceof AbstractProject) {
                AbstractProject<?, ? extends AbstractBuild<?,?>> p = (AbstractProject<?, ? extends AbstractBuild<?,?>>) i;
                if (j.getAnd() != null) {
                	FlowDSL.evalJobAnd(j.getAnd(), flow);
                }
                for (Object param: j.getParams().keySet()) {
                	String paramName = param.toString();
                	Object paramValue = j.getParams().get(param);
                	if (paramValue instanceof Closure) {
                		paramValue = FlowDSL.evalParam((Closure<?>) paramValue, s.getParentFlow());
                	}
                	if (paramValue instanceof Boolean) {
                		actions.add(new ParametersAction(new BooleanParameterValue(paramName, (Boolean) paramValue)));
                	}
                	else {
                		//TODO For now we will only support String and boolean parameters
                		actions.add(new ParametersAction(new StringParameterValue(paramName, paramValue.toString())));
                	}
                }                
                futures.put(j, p.scheduleBuild2(p.getQuietPeriod(), cause, actions));
            }
            else {
                throw new RuntimeException(jobName + " is not a job");
            }
        }
        return futures;
    }

    @Override
    public void run() {
        run(createRunner());        
    }
    
    protected Runner createRunner() {
        return new RunnerImpl();
    }
    
    protected class RunnerImpl extends AbstractRunner {
        protected Result doRun(BuildListener listener) throws Exception {

            Result r = null;
            try {
                Step currentStep = getFlow().getEntryStep();
                while (currentStep != null) {
                	Map<Job, Future<? extends AbstractBuild<?,?>>> futures = startStep(currentStep);
                    Result stepResult = Result.SUCCESS;
                    for (Job j : futures.keySet()) {
                    	Future<? extends AbstractBuild<?,?>> f = futures.get(j);
                    	AbstractBuild<?, ?> build = f.get();
                    	if (j.getThen() != null) {
                    		FlowDSL.evalJobThen(j.getThen(), getFlow(), build);
                    	}
                    	stepResult = stepResult.combine(build.getResult());
                    }
                    
                    Step nextStep = currentStep.getTriggerOn(stepResult);
                    if (nextStep == null) {
                        r = stepResult;
                    }
                   	currentStep = nextStep;
                }
            } catch (InterruptedException e) {
                r = Executor.currentExecutor().abortResult();
                throw e;
            } finally {
                if (r != null) setResult(r);
                // tear down in reverse order
                boolean failed=false;
                for( int i=buildEnvironments.size()-1; i>=0; i-- ) {
                    if (!buildEnvironments.get(i).tearDown(FlowRun.this,listener)) {
                        failed=true;
                    }                    
                }
                // WARNING The return in the finally clause will trump any return before
                if (failed) return FAILURE;
            }

            return r;
        }

        @Override
        public void post2(BuildListener listener) throws IOException, InterruptedException {
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {
            super.cleanUp(listener);
        }

    }
    
    private static final Logger LOGGER = Logger.getLogger(FlowRun.class.getName());
}
