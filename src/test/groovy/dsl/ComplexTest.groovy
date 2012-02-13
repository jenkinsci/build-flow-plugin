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

        3.times retry {
            build("job1")
	    }

	    println "\\nTriggered by '" + cause.upstreamProject + "' with build number '" + cause.upstreamBuild + "'\\n"

	    a = build("Job1")
	    b = build("Job2", param1: "troulala", param2: "machin")
	    c = build("Job3")

	    println "\\n" + a.result() + " " + b.result() + " " + c.result() + "\\n"

	    par = parallel {
	        build("jobp1")
	        build("jobp2")
	        build("jobp3")
	        build("jobp4")
	        build("jobp5")
	        build("jobp6")
	        build("jobp7")
	    }

	    println ""
	    par.values().each { job -> println job.name + " => " + job.result() }
	    println ""

        build("job3")

	    guard {
	        build("job1")
	        build("job2")
	    } rescue {
	        build("job3")
	    }
    }
	"""

    def String getScript() {
        return script;
    }


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
