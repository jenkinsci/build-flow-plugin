package dsl

import hudson.model.Cause
import hudson.model.Result

class CauseTest extends DSLTestCase {

    def successBuild =  """flow {
        assert cause != null
        assert cause.upstreamProject == "root"
        assert cause.upstreamBuild == 1
    }"""

    public void testCause() {
        def root = createFreeStyleProject("root").createExecutable()
        def cause = new Cause.UpstreamCause(root)
        def ret = runWithCause(successBuild, cause)
        assert Result.SUCCESS == ret
    }

    def successBuild2 =  """flow {
        assert cause == null
    }"""

    public void testWithoutCause() {
        def ret = run(successBuild2)
        assert Result.SUCCESS == ret
    }
}
