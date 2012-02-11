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

import com.cloudbees.plugins.flow.dsl.FlowDSL;
import hudson.model.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * Maintain the state of execution of a build flow as a chain of triggered jobs
 *
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class FlowRun extends AbstractBuild<BuildFlow, FlowRun>{

    private static final Logger LOGGER = Logger.getLogger(FlowRun.class.getName());
    
	private String dsl;

    public FlowRun(BuildFlow job) throws IOException {
        super(job);
        this.dsl = job.getDsl();
    }

    public FlowRun(BuildFlow project, File buildDir) throws IOException {
        super(project, buildDir);
        this.dsl = project.getDsl();
    }

    public BuildFlow getBuildFlow() {
        return project;
    }

    @Override
    public void run() {
        run(createRunner());        
    }
    
    protected Runner createRunner() {
        return new RunnerImpl(this, dsl);
    }
    
    protected class RunnerImpl extends AbstractRunner {

        private final Cause cause;
        private final String dsl;

        public RunnerImpl(AbstractBuild<?, ?> build, String dsl) {
            this.cause = new Cause.UpstreamCause(build);
            this.dsl = dsl;
        }

        protected Result doRun(BuildListener listener) throws Exception {
            Result r = null;
            try {
                r = new FlowDSL().executeFlowScript("", cause);
            } catch (Exception e) {
                r = Executor.currentExecutor().abortResult();
                throw e;
            } finally {
                // TODO : do we need this ?
                if (r != null) setResult(r);
                boolean failed=false;
                for( int i=buildEnvironments.size()-1; i>=0; i-- ) {
                    if (!buildEnvironments.get(i).tearDown(FlowRun.this,listener)) {
                        failed=true;
                    }
                }
                if (failed) return Result.FAILURE;
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
}
