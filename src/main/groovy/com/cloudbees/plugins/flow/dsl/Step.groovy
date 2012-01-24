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



package com.cloudbees.plugins.flow.dsl;

import java.util.Map;
import hudson.model.Result;

public class Step {

    String name;
    Flow parentFlow;
    List<Job> jobs = new ArrayList<Job>();
    List<StepOn> stepOns = new ArrayList<StepOn>();

    public Step(String name, Flow parentFlow) {
        this.name = name;
        this.parentFlow = parentFlow;
    }
    
    public List<Job> getJobs() {
        return this.jobs;
    }
    
    public void addJob(String jobName) {
        this.jobs.add(new Job(jobName));
    }

    public StepOn on(Result result) {
        def stepOn = new StepOn(this, result);
        this.stepOns.add(stepOn);
        return stepOn;
    }
    
    public Step getTriggerOn(Result result) {
        for (stepOn in stepOns) {
           if (result.isBetterOrEqualTo(stepOn.result)) {
              return parentFlow.getStep(stepOn.nextStepName);
           }
        }
        return null;
    }
    
}
