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
import org.mockito.Mockito;

import hudson.model.Result;
import hudson.model.AbstractBuild;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class DSLTest {
	
	def script = """\
	flow {
		step1 {
			trigger "jobA" then {
				jobANum = buildNumber()
				myVariable = "toto"
			}
			trigger "jobC"
		} on SUCCESS trigger step2 on FAILURE trigger step3
		  
		step2 {
			trigger "jobB" then {
				jobBResult = buildResult()
			}
		} on {jobBResult == SUCCESS} trigger step3
		
		step3 {
			trigger "jobC" with params:[MY_BOOL_PARAM: true, MY_STR_PARAM: {myVariable + "2"}] and {
				copyArtifact "*.jar" from "jobA" number jobANum to "lib/jars"
			}
		}
	}
	"""
    
    @Test
    void testParseDSL() {        
        Flow f = FlowDSL.readFlow(script);
        assertThat(f.entryStepName, is("step1"));
        assertThat(f.steps.size(), is(3));
        assertThat(f.getStep("step1").jobs[0].name, is("jobA"))
        assertThat(f.getStep("step1").jobs[0].then, is(notNullValue()))
        assertThat(f.getStep("step1").stepOns[0].result, is(Result.SUCCESS))
        assertThat(f.getStep("step1").stepOns[0].nextStepName, is("step2"))
        assertThat(f.getStep("step1").stepOns[1].result, is(Result.FAILURE))
        assertThat(f.getStep("step1").stepOns[1].nextStepName, is("step3"))
        assertThat(f.getStep("step1").getTriggerOn(Result.SUCCESS).name, is("step2"))
        assertThat(f.getStep("step1").getTriggerOn(Result.FAILURE).name, is("step3"))
        assertThat(f.getStep("step1").getTriggerOn(Result.UNSTABLE).name, is("step3"))
        assertThat(f.getStep("step3").jobs[0].name, is("jobC"))
		assertThat(f.getStep("step3").jobs[0].params["MY_BOOL_PARAM"], is(true))
		assertThat(f.getStep("step3").jobs[0].params["MY_STR_PARAM"], is(Closure.class))
		assertThat(f.getStep("step3").jobs[0].and, is(notNullValue()))
    }
	
	@Test
	void testEvalClosures() {
		AbstractBuild build = Mockito.mock(AbstractBuild.class);
		Mockito.when(build.getNumber()).thenReturn(10);
		Flow f = FlowDSL.readFlow(script);
		FlowDSL.evalJobThen(f.getStep("step1").jobs[0].then, f, build);
		assertThat(f.storage["jobANum"], is(10));
		Mockito.when(build.getResult()).thenReturn(Result.SUCCESS);
		FlowDSL.evalJobThen(f.getStep("step2").jobs[0].then, f, build);
		assertThat(f.getStep("step2").getTriggerOn(Result.SUCCESS).name, is("step3"))
		Mockito.when(build.getResult()).thenReturn(Result.FAILURE);
		FlowDSL.evalJobThen(f.getStep("step2").jobs[0].then, f, build);
		assertThat(f.getStep("step2").getTriggerOn(Result.FAILURE), is(nullValue()))
		
		assertThat(FlowDSL.evalParam(f.getStep("step3").jobs[0].params["MY_STR_PARAM"], f), is("toto2"));
	}
}
