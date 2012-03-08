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

package dsl

import hudson.model.Cause
import hudson.model.FreeStyleBuild
import hudson.model.Job
import hudson.model.Result
import com.cloudbees.plugins.flow.FlowRun
import static hudson.model.Result.SUCCESS

public class ComplexTest extends DSLTestCase {

    def script = """
	    assert upstream != null

	    println "\\nTriggered by '" + upstream.parent + "' with build number '" + upstream.buildNumber + "'\\n"

	    def a = 0
        3.times retry {
            build("job1")
	        a++
	    }
	    assert a == 1

	    a = build("Job1")
	    b = build("Job2", param1: "troulala", param2: "machin")
	    c = build("Job3")

	    assert a.future.done
	    assert b.future.done
	    assert c.future.done

	    println "\\n" + a.result() + " " + b.result() + " " + c.result() + "\\n"

	    par = parallel {
	        a = build("jobp1")
            assert !a.future.done
	        b = build("jobp2")
            assert !b.future.done
	        c = build("jobp3")
            assert !c.future.done
	        d = build("jobp4")
            assert !d.future.done
	        e = build("jobp5")
            assert !e.future.done
	        f = build("jobp6")
            assert !f.future.done
	        g = build("jobp7")
            assert !g.future.done
	    }

	    println ""
	    par.values().each { job ->
	        println job.name + " => " + job.result()
	        assert job.future.done
	    }
	    println ""

        assert build("job3").future.done

	    guard {
	        assert build("job1").future.done
	        assert build("job2").future.done
	    } rescue {
	        r = build("job3")
	        assert r.future.done
	    }
	"""


    public void testComplexDSL() {
        Job root = createJob("root");
        FreeStyleBuild rootBuild = root.createExecutable();
        Job job1 = createJob("Job1");
        Job job2 = createJob("Job2");
        Job job3 = createJob("Job3");
        Job jobp1 = createJob("jobp1");
        Job jobp2 = createJob("jobp2");
        Job jobp3 = createJob("jobp3");
        Job jobp4 = createJob("jobp4");
        Job jobp5 = createJob("jobp5");
        Job jobp6 = createJob("jobp6");
        Job jobp7 = createJob("jobp7");

        FlowRun flowResult = runWithCause(script, new Cause.UpstreamCause(rootBuild));
        assertAllSuccess(job1, job2, job3, jobp1, jobp2, jobp3, jobp4, jobp5, jobp6, jobp7);
        assertEquals(SUCCESS, flowResult.result);
    }
}
