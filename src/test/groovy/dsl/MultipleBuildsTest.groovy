package dsl

import hudson.model.Result

class MultipleBuildsTest extends DSLTestCase {

    def successBuild2 =  """flow {
        build("job1")
        build("job2")
        build("job3")
    }"""

    public void testBuildsWithoutReturn() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def ret = run(successBuild2)
        assertAllSuccess(jobs)
        assert Result.SUCCESS == ret
    }

    def failBuild2 = """flow {
        build("job1")
        build("job2")
        build("job3")
        build("willFail")
    }"""

    public void testBuildsWithoutReturnFailed() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def willFail = createFailJob("willFail");
        def ret = run(failBuild2)
        assertAllSuccess(jobs)
        assertFailure(willFail)
        assert Result.FAILURE == ret
    }

    def failBuild3 = """flow {
        build("job1")
        build("willFail")
        build("job2")
        build("job3")
    }"""

    public void testBuildsWithoutReturnFailedAndDontContinue() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def willFail = createFailJob("willFail");
        def ret = run(failBuild3)
        assertSuccess(jobs.get(0))
        jobs.get(1).getBuilds().isEmpty()
        jobs.get(2).getBuilds().isEmpty()
        assertFailure(willFail)
        assert Result.FAILURE == ret
    }
}
