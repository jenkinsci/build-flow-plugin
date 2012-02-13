package dsl

import org.junit.Test
import com.cloudbees.plugins.flow.dsl.FlowDSL
import hudson.model.Cause
import hudson.model.Result
import org.jvnet.hudson.test.HudsonTestCase
import hudson.model.FreeStyleProject
import hudson.model.FreeStyleBuild

public class ComplexTest extends DSLTestCase {

    def script = """
	flow {

	    assert cause != null
	    assert cause.upstreamProject == "root"
	    assert cause.upstreamBuild == 1

	    println "\\nTriggered by '" + cause.upstreamProject + "' with build number '" + cause.upstreamBuild + "'\\n"

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
    }
	"""


    public void testComplexDSL() {
        FreeStyleProject root = createFreeStyleProject("root");
        FreeStyleBuild rootBuild = root.createExecutable();
        FreeStyleProject job1 = createFreeStyleProject("Job1");
        FreeStyleProject job2 = createFreeStyleProject("Job2");
        FreeStyleProject job3 = createFreeStyleProject("Job3");
        FreeStyleProject jobp1 = createFreeStyleProject("jobp1");
        FreeStyleProject jobp2 = createFreeStyleProject("jobp2");
        FreeStyleProject jobp3 = createFreeStyleProject("jobp3");
        FreeStyleProject jobp4 = createFreeStyleProject("jobp4");
        FreeStyleProject jobp5 = createFreeStyleProject("jobp5");
        FreeStyleProject jobp6 = createFreeStyleProject("jobp6");
        FreeStyleProject jobp7 = createFreeStyleProject("jobp7");

        Result flowResult = runWithCause(script, new Cause.UpstreamCause(rootBuild));

        assertEquals(Result.SUCCESS, job1.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, job2.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, job3.getBuilds().getFirstBuild().getResult());

        assertEquals(Result.SUCCESS, jobp1.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, jobp2.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, jobp3.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, jobp4.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, jobp5.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, jobp6.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, jobp7.getBuilds().getFirstBuild().getResult());

        assertEquals(Result.SUCCESS, flowResult);
    }
}
