package com.cloudbees.plugins.flow.dsl;

import hudson.model.Result;

public class StepOn {

    Result result;
    Step parentStep;
    String nextStepName;

    public StepOn(Step parentStep, Result result) {
        this.parentStep = parentStep;
        this.result = result;
    }
    
    public Step trigger(String stepName) {
    	this.nextStepName = stepName;
        return this.parentStep;
    }

}