package dsl

import hudson.model.Cause
import hudson.model.Result

class RetryTest extends DSLTestCase {

    def successBuild =  """flow {
        def a = 0
        3.times retry {
            build("job1")
            a++
        }
        println "closure run " + a
        assert a == 1
    }"""

    public void testNoRetry() {
        def job1 = createJob("job1")
        def ret = run(successBuild)
        assertSuccess(job1)
        assert Result.SUCCESS == ret
    }

    def retryBuild =  """flow {
        def a = 0
        3.times retry {
            build("willFail")
            a++
        }
        println "closure run " + a
        assert a == 3
    }"""

    public void testRetry() {
        def job1 = createFailJob("willFail")
        def ret = run(retryBuild)
        assertFailure(job1)
        assert Result.SUCCESS == ret // TODO : should return failure
    }

    def retryGuardBuild =  """flow {
        def a = 0, b = 0, c = 0
        3.times retry {
            guard {
                build("job1")
                b++
            } rescue {
                build("willFail")
                c++
            }
            a++
        }
        assert a == 3
        assert b == 3
        assert c == 3
    }"""

    public void testRetryGuard() {
        def fail = createFailJob("willFail")
        def jobs = createJobs(["job1", "job2"])
        def ret = run(retryGuardBuild)
        assert Result.SUCCESS == ret // TODO : should return failure
    }

    def retryGuardParBuild =  """flow {
        def a = 0, b = 0, c = 0
        3.times retry {
            guard {
                build("job1")
                b++
            } rescue {
                build("willFail")
                c++
            }
            par = parallel {
                j1 = build("job1")
                assert !j1.future.done
                j2 = bbuild("job2")
                assert !j2.future.done
            }
            a++
            par.values().each {
                assert it.future.done
            }
        }
        assert a == 3
        assert b == 3
        assert c == 3
    }"""

    public void testRetryGuardPar() {
        def fail = createFailJob("willFail")
        def jobs = createJobs(["job1", "job2"])
        def ret = run(retryGuardBuild)
        assert Result.SUCCESS == ret // TODO : should return failure
    }
}
