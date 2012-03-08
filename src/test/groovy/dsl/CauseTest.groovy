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

package dsl

import hudson.model.Cause
import hudson.model.Result

class CauseTest extends DSLTestCase {

    def successBuild =  """
        flow {
            assert cause != null
            assert cause.upstreamProject == "root"
            assert cause.upstreamBuild == 1
        }
    """

    public void testCause() {
        def root = createFreeStyleProject("root").createExecutable()
        def cause = new Cause.UpstreamCause(root)
        def ret = runWithCause(successBuild, cause)
        assert Result.SUCCESS == ret
    }

    def successBuild2 =  """
        flow {
            assert cause == null
        }
    """

    public void testWithoutCause() {
        def ret = run(successBuild2)
        assert Result.SUCCESS == ret
    }
}
