package com.cloudbees.plugins.flow.dsl;

public class Job {

    String name;

    public Job(String name) {
        this.name=name;
    }
    
    public String getName() {
        return this.name;
    }

}