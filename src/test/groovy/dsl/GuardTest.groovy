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

     def successBuildPar =  """flow {
        def g, r
        guard {
            build("job1")
            par = parallel {
                a = build("job2")
                assert !a.future.done
                b = build("job3")
                assert !b.future.done
            }
            par.values().each {
                assert it.future.done
            }
            g = true
        } rescue {
            build("clean")
            r = true
        }
        assert g
        assert r
    }"""

    public void testGuardPassPar() {
        def jobs = createJobs(["job1", "job2", "job3", "clean"])
        def ret = run(successBuildPar)
        assertAllSuccess(jobs)
        assert Result.SUCCESS == ret
    }

    def successBuildParRetry =  """flow {
        def a = 0
        def g, r
        guard {
            build("job1")
            par = parallel {
                job1 = build("job2")
                assert !job1.future.done
                job2 = build("job3")
                assert !job2.future.done
            }
            par.values().each {
                assert it.future.done
            }
            2.times retry {
                build("job3")
                a++
            }
            g = true
        } rescue {
            build("clean")
            r = true
        }
        assert a == 1
    }"""

    public void testGuardPassParRetry() {
        def jobs = createJobs(["job1", "job2", "job3", "clean"])
        def ret = run(successBuildParRetry)
        assertAllSuccess(jobs)
        assert Result.SUCCESS == ret
    }
}
