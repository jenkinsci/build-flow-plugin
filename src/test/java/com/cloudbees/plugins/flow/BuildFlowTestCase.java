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

package com.cloudbees.plugins.flow;

import hudson.model.Hudson;

import java.io.IOException;

import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildFlowTestCase extends HudsonTestCase {

    public BuildFlow createBuildFlow(String name) throws IOException {
        return (BuildFlow) hudson.createProject(BuildFlow.DESCRIPTOR, name);
    }
    
    public void testBasic() throws Exception {
        BuildFlow flow = createBuildFlow("flow1");
        assertNotNull(flow);
    }

}
