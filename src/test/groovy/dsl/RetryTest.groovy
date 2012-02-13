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
}
