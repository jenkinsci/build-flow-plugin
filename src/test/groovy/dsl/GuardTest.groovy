package dsl

import hudson.model.Result

class GuardTest extends DSLTestCase {

    def successBuild =  """flow {
        guard {
            build("job1")
            build("job2")
            build("job3")
        } rescue {
            build("clean")
        }
    }"""

    public void testGuardPass() {
        def jobs = createJobs(["job1", "job2", "job3", "clean"])
        def ret = run(successBuild)
        assertAllSuccess(jobs)
        assert Result.SUCCESS == ret
    }

    public void testGuardWithFail() {
        def jobs = createJobs(["job1", "job3", "clean"])
        def failure = createFailJob("job2")
        def ret = run(successBuild)
        assertFailure(failure)
        assertSuccess(jobs.get(0))
        jobs.get(1).getBuilds().isEmpty()
        assertSuccess(jobs.get(2))
        assert Result.SUCCESS == ret
    }
}
