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

import java.util.logging.Logger
import jenkins.model.Jenkins
import hudson.model.*
import static hudson.model.Result.SUCCESS

public class FlowDSL {

    private ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false)
        cl(emc)
        emc.initialize()
        return emc
    }

    def void executeFlowScript(FlowRun flowRun, String dsl) {
        // TODO : add restrictions for System.exit, etc ...
        FlowDelegate flow = new FlowDelegate(flowRun)

        // Retrieve the upstream build if the flow was triggered by another job
        AbstractBuild upstream = null;
        flowRun.causes.each{ cause -> 
            if (cause instanceof Cause.UpstreamCause) {
                Job job = Jenkins.instance.getItemByFullName(cause.upstreamProject)
                upstream = job?.getBuildByNumber(cause.upstreamBuild)
                // TODO handle matrix jobs ?
            }
        }
        
        def binding = new Binding([
                upstream: upstream,
                SUCCESS: SUCCESS,
                UNSTABLE: Result.UNSTABLE,
                FAILURE: Result.FAILURE,
                ABORTED: Result.ABORTED,
                NOT_BUILT: Result.NOT_BUILT
        ])

        Script dslScript = new GroovyShell(binding).parse("flow { " + dsl + "}")
        dslScript.metaClass = createEMC(dslScript.class, {
            ExpandoMetaClass emc ->
            emc.flow = {
                Closure cl ->
                cl.delegate = flow
                cl.resolveStrategy = Closure.DELEGATE_FIRST
                cl()
            }
        })
        try {
            dslScript.run()
        } finally {
            //
        }
    }

    // TODO define a parseFlowScript to validate flow DSL and maintain jobs dependencygraph
}

public class FlowDelegate {

    private static final Logger LOGGER = Logger.getLogger(FlowDelegate.class.getName());

    def List<Cause> causes
    def FlowRun flowRun

    public FlowDelegate(FlowRun flowRun) {
        this.flowRun = flowRun
        causes = flowRun.causes
    }

    def fail() {
        // Stop the flow execution
        throw new JobExecutionFailureException()
    }

    def build(String jobName) {
        build([:], jobName)
    }

    def build(Map args, String jobName) {
        if (flowRun.result.isWorseThan(SUCCESS)) {
            fail()
        }
        // ask for job with name ${name}
        JobInvocation job = new JobInvocation(jobName)
        job.run(new FlowCause(flowRun), getActions(args))
        flowRun.addBuild(job)
        flowRun.setResult(job.result)
        return job;
    }

    def getActions(Map args) {
        List<Action> actions = new ArrayList<Action>();
        for (Map.Entry param: args) {
            String paramName = param.key
            Object paramValue = param.value
            if (paramValue instanceof Closure) {
                paramValue = getClosureValue(paramValue)
            }
            if (paramValue instanceof Boolean) {
                actions.add(new ParametersAction(new BooleanParameterValue(paramName, (Boolean) paramValue)))
            }
            else {
                actions.add(new ParametersAction(new StringParameterValue(paramName, paramValue.toString())))
            }
            //TODO For now we only support String and boolean parameters
        }
        return actions
    }

    def getClosureValue(closure) {
        return closure()
    }

    def guard(guardedClosure) {
        def deleg = this;
        [ rescue : { rescueClosure ->
            rescueClosure.delegate = deleg
            rescueClosure.resolveStrategy = Closure.DELEGATE_FIRST

            try {
                guardedClosure()
            } finally {
                // Force result to SUCCESS so that rescue closure will execute
                Result r = flowRun.result
                flowRun.result = SUCCESS
                rescueClosure()
                // restore result, as the worst from guarded and rescue closures
                flowRun.result = r.combine(flowRun.result)
            }
        } ]
    }

    def retry(int attempts, retryClosure) {
        Result origin = flowRun.result
        while( attempts-- > 0) {
            // Restore the pre-retry result state to ignore failures
            flowRun.result = origin
            retryClosure()
            if (flowRun.result.isBetterOrEqualTo(SUCCESS)) {
                return;
            }
        }
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Property ${name} doesn't exist.");
    }

    def methodMissing(String name, Object args) {
        throw new MissingMethodException("Method ${name} doesn't exist.");
    }
}
