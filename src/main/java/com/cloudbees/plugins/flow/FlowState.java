package com.cloudbees.plugins.flow;

import hudson.model.Result;
import hudson.model.Run;

import java.util.Collections;
import java.util.Set;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FlowState {

    private Result result;

    private Set<Run> lastCompleted;

    public FlowState(Result result, Set<Run> previous) {
        assert result != null;
        this.result = result;
        setLastCompleted(previous);
    }

    public FlowState(Result result, Run previous) {
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

    public Set<Run> getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(Run lastCompleted) {
        this.lastCompleted = Collections.singleton(lastCompleted);
    }

    public void setLastCompleted(Set<Run> lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    public Run getLastBuild() {
        return this.lastCompleted.iterator().next();
    }

}
