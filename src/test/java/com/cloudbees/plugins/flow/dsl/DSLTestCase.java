package com.cloudbees.plugins.flow.dsl;

import com.cloudbees.plugins.flow.BuildFlow;
import java.io.IOException;
import org.jvnet.hudson.test.HudsonTestCase;

public class DSLTestCase extends HudsonTestCase {

    public BuildFlow createBuildFlow(String name) throws IOException {
        return (BuildFlow) hudson.createProject(BuildFlow.DESCRIPTOR, name);
    }

    public void testBasic() throws Exception {
        BuildFlow flow = createBuildFlow("flow1");
        assertNotNull(flow);
    }
}
