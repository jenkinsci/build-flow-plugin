package dsl

import org.junit.Test
import com.cloudbees.plugins.flow.dsl.FlowDSL
import hudson.model.Cause
import hudson.model.Result

public class DSLTest {

	def script = """
	flow {

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

        3.times retry {
            build("job1")
	    }
    }
	"""

    def String getScript() {
        return script;
    }

    @Test
    public void testParseDSL() {
        // TODO : need to mock it for quick dev
    }

    public Result testParseDSL(Cause cause) {
        return new FlowDSL().executeFlowScript(script, cause)
    }
}
