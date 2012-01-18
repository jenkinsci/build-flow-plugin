/*
 Copyright (C) 2011 CloudBees Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Affero General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, see <http://www.gnu.org/licenses/>.
*/
package com.cloudbees.plugins.flow;

import hudson.model.Cause.UpstreamCause;

import hudson.model.Action;

import com.cloudbees.plugins.flow.dsl.Flow;
import com.cloudbees.plugins.flow.dsl.JobStep;
import com.cloudbees.plugins.flow.dsl.Step;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Executor;
import java.io.File;
import java.io.IOException;
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
    
    private Flow flow;

    public FlowRun(BuildFlow job) throws IOException {
        super(job);
        this.flow = job.getFlow();
    }

    public FlowRun(BuildFlow project, File buildDir) throws IOException {
        super(project, buildDir);
        this.flow = project.getFlow();
    }
    
    public Flow getFlow() {
        return flow;
    }

    public BuildFlow getBuildFlow() {
        return project;
    }

    public Future<? extends AbstractBuild<?,?>> startStep(Step s) {
        if (s instanceof JobStep) {
            String jobName = ((JobStep) s).getJob().getName();
            Item i = Jenkins.getInstance().getItem(jobName);
            if (i instanceof AbstractProject) {
                AbstractProject<?, ? extends AbstractBuild<?,?>> p = (AbstractProject<?, ? extends AbstractBuild<?,?>>) i;
                Cause cause = new UpstreamCause(this);
                Action action = new BuildFlowAction(this);
                return p.scheduleBuild2(p.getQuietPeriod(), cause, action);
            }
            else {
                throw new RuntimeException(jobName + " is not a job");
            }
        }
        else {
            throw new RuntimeException("Only job steps are supported");
        }
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
            boolean flowCompleted = false;
            try {
                Step currentStep = getFlow().getEntryStep();
                while (currentStep != null) {
                    Future<? extends AbstractBuild<?,?>> f = startStep(currentStep);
                    Result intermediateResult = f.get().getResult();
                    if (intermediateResult == Result.SUCCESS && currentStep.getOnSuccess() != null) {
                        currentStep = currentStep.getOnSuccess();
                    }
                    else if (intermediateResult != Result.SUCCESS && currentStep.getOnError() != null) {
                        currentStep = currentStep.getOnError();
                    }
                    else {
                        currentStep = null;
                        r = intermediateResult;
                    }
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
