package com.cloudbees.plugins.flow.dsl;

import com.cloudbees.plugins.flow.BuildFlow;
import com.cloudbees.plugins.flow.FlowRun;
import dsl.DSLTest;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Ant;
import java.io.IOException;
import org.jvnet.hudson.test.HudsonTestCase;

public class DSLTestCase extends HudsonTestCase {

    public BuildFlow createBuildFlow(String name) throws IOException {
        return (BuildFlow) hudson.createProject(BuildFlow.DESCRIPTOR, name);
    }

    public void testRunDSLBasic() throws Exception {
        FreeStyleProject root = createFreeStyleProject("root");
        FreeStyleBuild rootBuild = root.createExecutable();
        FreeStyleProject job1 = createFreeStyleProject("Job1");
        FreeStyleProject job2 = createFreeStyleProject("Job2");
        FreeStyleProject job3 = createFreeStyleProject("Job3");
        FreeStyleProject jobp1 = createFreeStyleProject("jobp1");
        FreeStyleProject jobp2 = createFreeStyleProject("jobp2");

        Result flowResult = new DSLTest().testParseDSL(new Cause.UpstreamCause(rootBuild));

        assertEquals(Result.SUCCESS, job1.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, job2.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, job3.getBuilds().getFirstBuild().getResult());

        assertEquals(Result.SUCCESS, jobp1.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, jobp2.getBuilds().getFirstBuild().getResult());
        assertEquals(Result.SUCCESS, flowResult);
    }

    public void testBasic() throws Exception {
        BuildFlow flow = createBuildFlow("flow1");
        assertNotNull(flow);
    }
}
