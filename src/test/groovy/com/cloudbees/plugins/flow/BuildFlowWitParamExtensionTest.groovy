package com.cloudbees.plugins.flow

import jenkins.model.Jenkins;

import com.cloudbees.plugin.flow.TestDSLExtension
import hudson.model.Result
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;

class BuildFlowWitParamExtensionTest extends DSLTestCase {

    public void testUseParams() {
        def flow = run("""
            def p = withParam.subversionRevision
            def p2 = withParam.gitRevision()
            def p3 = withParam.predefined("a=b")

            println p.class
            println p2.class
        """)
        System.out.println flow.log
        assert Result.SUCCESS == flow.result
    }

}
