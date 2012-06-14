/*
 * Copyright (C) 2012 Tuenti S.L
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
import hudson.model.Result
import static hudson.model.Result.FAILURE

class LockTest extends DSLTestCase {
    
    public void testlockHappyPath() {
       def job1 = createJob("job1")
        def flow = run("""
            locksection("lock1") {
              build("job1")
            }
        """)
        println flow.builds.edgeSet()
        assertRan(job1, 1, SUCCESS)
    }
    
    public void testlockFailingJob() {
       def job1 = createFailJob("job1")
        def flow = run("""
            locksection("lock1") {
              build("job1")
            }
        """)
        println flow.builds.edgeSet()
        assertRan(job1, 1, FAILURE)
    }
}
