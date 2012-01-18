package com.cloudbees.plugins.flow.dsl;

import java.util.List;
import java.util.Map;

public class Flow {
    
    Map<String, Step> steps = new HashMap<String, Step>();
    List<String> entryStepNames = new ArrayList<String>();
    
    public Step getStep(String name) {
        return steps.get(name);
    }
    
    public List<Step> getEntrySteps() {
        List<Step> result = new ArrayList<Step>();
        for (String entryStepName : entryStepNames) {
            result.add(steps.get(entryStepName));
        }        
        return result;
    }
}