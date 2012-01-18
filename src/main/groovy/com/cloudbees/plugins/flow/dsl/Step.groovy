package com.cloudbees.plugins.flow.dsl;

import java.util.Map;

public class Step {

    String name;
    private String onSuccessStepName;
    private String onErrorStepName;
    Flow parentFlow;
    Step onSuccess;
    Step onError;


    public Step(String name, Flow parentFlow) {
        this.name = name;
        this.parentFlow = parentFlow;
    }
    
    Step onSuccess(String stepName) {
        onSuccessStepName = stepName;
        return this;
    }

    Step onError(String stepName) {
        onErrorStepName = stepName;
        return this;
    }
    
    Step getOnSuccess() {
        return parentFlow.steps.get(onSuccessStepName);
    }
    
    Step getOnError() {
        return parentFlow.steps.get(onErrorStepName);
    }

}