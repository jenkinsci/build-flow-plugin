package dsl

import org.junit.Test
import com.cloudbees.plugins.flow.dsl.FlowDSL
import com.cloudbees.plugins.flow.dsl.JobCause

public class DSLTest {

	def script = """
	flow {
	    println "\\nTriggered by " + cause.build + "\\n"

	    a = build("Job1")
	    b = build("Job2", param1: "troulala", param2: "machin")
	    c = build("Job3")

	    println "\\n" + a.result() + " " + b.result() + " " + c.result() + "\\n"

	    par = parallel {
	        build("jobp1")
	        build("jobp2")
	    }

	    println ""
	    par.values().each { job -> println job.result() }
	    println ""

	    guard {
	        build("jobg1")
	        build("willFail")
	    } rescue {
	        build("Cleaning")
	    }
    }
	"""

    @Test
    void testParseDSL() {
        new FlowDSL().executeFlowScript(script, new JobCause())
    }
}
