package dsl

import hudson.model.Result
import hudson.model.AbstractBuild
import com.cloudbees.plugins.flow.JobNotFoundException

class BuildTest extends DSLTestCase {

    def successBuild =  """flow {
        build("job1")
    }"""

    def successBuildParam =  """flow {
        build("job1", p1: "p1", p2: "p2")
    }"""

    public void testBuildWithoutJob() {
        assertException(JobNotFoundException.class) {
            run(successBuild)
        }
    }

    public void testBuildWithoutReturn() {
        def job1 = createJob("job1")
        run(successBuild)
        assertSuccess(job1)
    }

    public void testBuildWithParams() {
        def job1 = createJob("job1")
        run(successBuildParam)
        assertSuccess(job1)
        assert job1.getBuilds().lastBuild.getActions().size() == 2
    }

    def failBuild = """flow {
        build("willFail")
    }"""

    public void testBuildWithoutReturnFailed() {
        def willFail = createFailJob("willFail");
        run(failBuild)
        assertFailure(willFail)
    }

    def returnBuild =  """flow {
        a = build("job1")
        assert a != null
        assert a.result() == hudson.model.Result.SUCCESS
        assert a.name == "job1"
    }"""

    public void testBuildWithReturn() {
        def job1 = createJob("job1")
        def ret = run(returnBuild)
        assertSuccess(job1)
        assert Result.SUCCESS == ret
    }
}
