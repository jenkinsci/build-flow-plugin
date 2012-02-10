package dsl

import org.junit.Test
import com.cloudbees.plugins.flow.dsl.FlowDSL

public class DSLTest {

	def script = """
	flow {
	    a = build("Job1")
	    b = build("Job2", param1: "troulala", param2: "machin")
	    c = build("Job3")
	    println a + " " + b + " " + c
    }
	"""

    @Test
    void testParseDSL() {
        new FlowDSL().executeFlowScript(script)
    }
}