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

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

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
        return new RunnerImpl();
    }
    
    protected class RunnerImpl extends AbstractRunner {

        protected Result doRun(BuildListener listener) throws Exception {
            Result r = null;
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
