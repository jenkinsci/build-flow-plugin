package com.cloudbees.plugins.flow.dsl;

import java.util.List;
import java.util.Map;

public class Flow {
    
    Map<String, Step> steps = new HashMap<String, Step>();
    String entryStepName;
    
    public Step getStep(String name) {
        return steps.get(name);
    }
    
    public Step getEntryStep() {
        return steps.get(entryStepName);
    }
}