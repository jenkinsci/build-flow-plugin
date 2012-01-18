package com.cloudbees.plugins.flow.dsl;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class DSLTest {
    
    @Test
    void testScriptEval() {
        FlowDSL dsl = new FlowDSL();
        def script = """\
        flow {
            step("step1") {
                job( "jobA" )
            }.onSuccess("step2")
             .onError("step3")
            step("step2") {
                job( "jobB" )
            }.onSuccess("step3")
            step("step3") {
                job( "jobC" )
            }
        }
        """
        Flow f = dsl.evalScript(script);
        assertThat(f.entryStepName, is("step1"));
        assertThat(f.steps.size(), is(3));
        assertThat(((JobStep)f.getStep("step1")).job.name, is("jobA"))
        assertThat(((JobStep)f.getStep("step1").onSuccess).job.name, is("jobB"))
        assertThat(((JobStep)f.getStep("step1").onError).job.name, is("jobC"))
    }
}
