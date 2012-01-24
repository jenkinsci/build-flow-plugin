/*
 * Copyright (C) 2011 CloudBees Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */



package com.cloudbees.plugins.flow.dsl;

import org.junit.Test;
import hudson.model.Result;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class DSLTest {
    
    @Test
    void testScriptEval() {
        FlowDSL dsl = new FlowDSL();
        def script = """\
        flow {
            step1 {
                job "jobA"
                job "jobC"
            } on SUCCESS trigger step2 on FAILURE trigger step3
              
            step2 {
                job "jobB"
            } on SUCCESS trigger step3
            
            step3 {
                job "jobC"
            }
        }
        """
        Flow f = dsl.evalScript(script);
        assertThat(f.entryStepName, is("step1"));
        assertThat(f.steps.size(), is(3));
        assertThat(f.getStep("step1").jobs[0].name, is("jobA"))
        assertThat(f.getStep("step1").stepOns[0].result, is(Result.SUCCESS))
        assertThat(f.getStep("step1").stepOns[0].nextStepName, is("step2"))
        assertThat(f.getStep("step1").stepOns[1].result, is(Result.FAILURE))
        assertThat(f.getStep("step1").stepOns[1].nextStepName, is("step3"))
    }
}
