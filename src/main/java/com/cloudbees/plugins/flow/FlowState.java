package com.cloudbees.plugins.flow;

import hudson.model.Result;

import java.util.Collections;
import java.util.Set;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FlowState {

    private Result result;

    private Set<JobInvocation> lastCompleted;

    public FlowState(Result result, Set<JobInvocation> previous) {
        assert result != null;
        this.result = result;
        setLastCompleted(previous);
    }

    public FlowState(Result result, JobInvocation previous) {
        assert result != null;
        this.result = result;
        setLastCompleted(previous);
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        assert result != null;
        this.result = result;
    }

    public Set<JobInvocation> getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(JobInvocation lastCompleted) {
        this.lastCompleted = Collections.singleton(lastCompleted);
    }

    public void setLastCompleted(Set<JobInvocation> lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    public JobInvocation getLastBuild() {
        return this.lastCompleted.iterator().next();
    }

}
