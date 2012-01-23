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
        stepOns.each() { stepOn ->
           if (result.isBetterOrEqualTo(stepOn.result)) {
              return parentFlow.getStep(stepOn.nextStepName);
           }
        }
        return null;
    }
    
}