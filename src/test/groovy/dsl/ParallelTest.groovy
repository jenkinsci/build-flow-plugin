package dsl

import hudson.model.Result

class ParallelTest extends DSLTestCase {

    def parBuild = """flow {
        def a, b, c
        parallel {
            a = build("job1")
            assert !a.future().isDone()
            b = build("job2")
            assert !b.future().isDone()
            c = build("job3")
            assert !c.future().isDone()
        }
        assert a.future().isDone()
        assert b.future().isDone()
        assert c.future().isDone()
    }"""

    public void testParallelWithoutReturn() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def ret = run(parBuild)
        assertAllSuccess(jobs)
        assert Result.SUCCESS == ret
    }

    def parBuildRet = """flow {
        def a, b, c
        p = parallel {
            a = build("job1")
            assert !a.future.done
            b = build("job2")
            assert !b.future.done
            c = build("job3")
            assert !c.future.done
        }
        assert a.future().isDone()
        assert b.future().isDone()
        assert c.future().isDone()
        assert p.size() == 3
        p.values().each {
            assert it.result ==  hudson.model.Result.SUCCESS
        }
    }"""

    public void testParallelWithReturn() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def ret = run(parBuild)
        assertAllSuccess(jobs)
        assert Result.SUCCESS == ret
    }

    def parParBuild = """flow {
        parallel {
            build("job1")
            parallel {
                build("job1")
            }
        }
    }"""

    public void testParallelParallel() {
        def job1 = createJob("job1")
        assertException(RuntimeException.class) {
            run(parParBuild)
        }
    }

    def parGuardBuild = """flow {
        parallel {
            build("job1")
            guard {
                build("job1")
            } rescue {
                build("job1")
            }
        }
    }"""

    public void testParallelGuard() {
        def job1 = createJob("job1")
        assertException(RuntimeException.class) {
            run(parGuardBuild)
        }
    }

    def parRetryBuild = """flow {
        parallel {
            build("job1")
            2.times retry {
                build("job1")
            }
        }
    }"""

    public void testParallelRetry() {
        def job1 = createJob("job1")
        assertException(RuntimeException.class) {
            run(parRetryBuild)
        }
    }
}
