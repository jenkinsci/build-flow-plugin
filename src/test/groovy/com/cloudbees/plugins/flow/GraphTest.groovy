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
import hudson.model.Job

class GraphTest extends DSLTestCase {

    public void testGraph() {
        Job job1 = createJob("job1")
        def run = run("""
            build("job1")
        """)
        assertSuccess(job1)

        assert SUCCESS == run.result
        
        assertNotNull run.jobsGraph
        println run.jobsGraph.vertexSet()
    }

    public void testSetRowsAndColumnsForBuild() {
        def job1 = createJob("job1")
        def job2 = createJob("job2")
        def job3 = createJob("job3")
        def job4 = createJob("job4")

        def flow = run("""
            parallel (
                {
                    build("job1")
                    build("job2")
                },
                {
                    build("job3")
                }
            )
            build("job4")
        """)

        Set<JobInvocation> jobs = flow.getJobsGraph().vertexSet();
        for (JobInvocation job : jobs) {
            switch (job.getProject()) {
                case job1:
                    println("assert for job1");
                    assert 1 == job.displayColumn
                    assert 0 == job.displayRow
                    break;
                case job2:
                    println("assert for job2");
                    assert 2 == job.displayColumn
                    assert 0 == job.displayRow
                    break;
                case job3:
                    println("assert for job3");
                    assert 1 == job.displayColumn
                    assert 1 == job.displayRow
                    break;
                case job4:
                    println("assert for job4");
                    assert 3 == job.displayColumn
                    assert 0 == job.displayRow
                    break;
            }
        }
    }

}
