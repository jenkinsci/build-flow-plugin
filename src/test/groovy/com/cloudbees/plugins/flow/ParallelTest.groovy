/*
 * Copyright (C) 2011 CloudBees Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.cloudbees.plugins.flow

import static hudson.model.Result.SUCCESS

class ParallelTest extends DSLTestCase {

    def parBuild = """
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
    """

    public void testParallelWithoutReturn() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def ret = run(parBuild)
        assertAllSuccess(jobs)
        assert SUCCESS == ret.result
    }

    def parBuildRet = """
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
    """

    public void testParallelWithReturn() {
        def jobs = createJobs(["job1", "job2", "job3"])
        def ret = run(parBuild)
        assertAllSuccess(jobs)
        assert SUCCESS == ret.result
    }

    def parParBuild = """
        parallel {
            build("job1")
            parallel {
                build("job1")
            }
        }
    """

    public void testParallelParallel() {
        def job1 = createJob("job1")
        assertException(RuntimeException.class) {
            run(parParBuild)
        }
    }

    def parGuardBuild = """
        parallel {
            build("job1")
            guard {
                build("job1")
            } rescue {
                build("job1")
            }
        }
    """

    public void testParallelGuard() {
        def job1 = createJob("job1")
        assertException(RuntimeException.class) {
            run(parGuardBuild)
        }
    }

    def parRetryBuild = """
        parallel {
            build("job1")
            2.times retry {
                build("job1")
            }
        }
    """

    public void testParallelRetry() {
        def job1 = createJob("job1")
        assertException(RuntimeException.class) {
            run(parRetryBuild)
        }
    }
}
