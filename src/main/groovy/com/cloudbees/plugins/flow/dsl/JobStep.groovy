package com.cloudbees.plugins.flow.dsl;

public class JobStep extends Step {

  Job job;

  public JobStep(String name, Job job, Flow parentFlow) {
    super(name, parentFlow);
    this.job = job;
  }
  
  public Job getJob() {
      return this.job;
  }

}