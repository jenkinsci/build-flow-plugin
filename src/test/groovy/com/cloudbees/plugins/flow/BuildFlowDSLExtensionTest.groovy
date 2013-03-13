package com.cloudbees.plugins.flow

import com.cloudbees.plugin.flow.TestDSLExtension
import hudson.model.Result

class BuildFlowDSLExtensionTest extends DSLTestCase {

    public void testUseExtension() {
        BuildFlowDSLExtension.all().add(new TestDSLExtension())
        def flow = run("""
            x = extension.test123
            println "name="+x.name
            println "dsl="+x.dsl.class.name
        """)
        System.out.println flow.log
        assert Result.SUCCESS == flow.result
        assert flow.log.contains("name=test123")
        assert flow.log.contains("dsl="+FlowDelegate.class.name)
    }

}
