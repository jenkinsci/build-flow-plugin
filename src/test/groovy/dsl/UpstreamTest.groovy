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
import static hudson.model.Result.SUCCESS

class UpstreamTest extends DSLTestCase {

    def successBuild =  """
        flow {
            assert upstream != null
            assert upstream.parent.name == "root"
            assert upstream.number == 1
        }
    """

    public void testUpstream() {
        def root = createFreeStyleProject("root").createExecutable()
        def cause = new Cause.UpstreamCause(root)
        def ret = runWithCause(successBuild, cause)
        assert SUCCESS == ret.result
    }

}
