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

import java.util.List;
import java.util.logging.Logger
import jenkins.model.Jenkins
import hudson.model.*
import static hudson.model.Result.SUCCESS
import static hudson.model.Result.UNSTABLE
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import hudson.slaves.NodeProperty
import hudson.slaves.EnvironmentVariablesNodeProperty
import java.util.concurrent.CopyOnWriteArrayList
import hudson.console.HyperlinkNote
import org.tuenti.lock.FlowLock

public class FlowDSL {

    private ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false)
        cl(emc)
        emc.initialize()
        return emc
    }

    def void executeFlowScript(FlowRun flowRun, String dsl, BuildListener listener) {
        // TODO : add restrictions for System.exit, etc ...
        FlowDelegate flow = new FlowDelegate(flowRun, listener)

        // Retrieve the upstream build if the flow was triggered by another job
        AbstractBuild upstream = null;
        flowRun.causes.each{ cause -> 
            if (cause instanceof Cause.UpstreamCause) {
                Job job = Jenkins.instance.getItemByFullName(cause.upstreamProject)
                upstream = job?.getBuildByNumber(cause.upstreamBuild)
                // TODO handle matrix jobs ?
            }
        }

        def envMap = [:]
        def getEnvVars = { NodeProperty nodeProperty ->
            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                envMap.putAll( nodeProperty.envVars );
            }
        }
        Jenkins.instance.globalNodeProperties.each(getEnvVars)
        flowRun.builtOn.nodeProperties.each(getEnvVars)

        def binding = new Binding([
                build: flowRun,
                out: listener.logger,
                env: envMap,
                upstream: upstream,
                params: flowRun.getBuildVariables(),
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
    BuildListener listener

    public FlowDelegate(FlowRun flowRun, BuildListener listener) {
        this.flowRun = flowRun
        this.listener = listener
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
        if (flowRun.state.result.isWorseThan(SUCCESS)) {
            fail()
        }
        // ask for job with name ${name}
        JobInvocation job = new JobInvocation(flowRun, jobName)
        def p = job.getProject()
        listener.logger.println("Trigger job " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()))
        Run r = flowRun.schedule(job, getActions(args));
        listener.logger.println(HyperlinkNote.encodeTo('/'+ r.getUrl(), r.getFullDisplayName())+" completed")
        return job;
    }

    def getActions(Map args) {
        List<Action> actions = new ArrayList<Action>();
        List<ParameterValue> params = [];
        for (Map.Entry param: args) {
            String paramName = param.key
            Object paramValue = param.value
            if (paramValue instanceof Closure) {
                paramValue = getClosureValue(paramValue)
            }
            if (paramValue instanceof Boolean) {
                params.add(new BooleanParameterValue(paramName, (Boolean) paramValue))
            }
            else {
                params.add(new StringParameterValue(paramName, paramValue.toString()))
            }
            //TODO For now we only support String and boolean parameters
        }
        actions.add(new ParametersAction(params));
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
                listener.logger.println("guard {")
                guardedClosure()
            } finally {
                // Force result to SUCCESS so that rescue closure will execute
                Result r = flowRun.state.result
                flowRun.state.result = SUCCESS
                listener.logger.println("} rescue {")
                rescueClosure()
                listener.logger.println("}")
                // restore result, as the worst from guarded and rescue closures
                flowRun.state.result = r.combine(flowRun.state.result)
            }
        } ]
    }

    def retry(int attempts, retryClosure) {
        Result origin = flowRun.state.result
        int i
        while( attempts-- > 0) {
            // Restore the pre-retry result state to ignore failures
            flowRun.state.result = origin
            listener.logger.println("retry (attempt $i++} {")
            retryClosure()
            if (flowRun.state.result.isBetterOrEqualTo(SUCCESS)) {
                listener.logger.println("}")
                return;
            }
            listener.logger.println("} // failed")
        }
    }

    def parallel(Closure ... closures) {
        ExecutorService pool = Executors.newCachedThreadPool()
        Set<Run> upstream = flowRun.state.lastCompleted
        Set<Run> lastCompleted = Collections.synchronizedSet(new HashSet<Run>())
        def results = new CopyOnWriteArrayList();

        listener.logger.println("parallel {")
        closures.eachWithIndex { closure, idx ->
            pool.submit( {
                flowRun.state = new FlowState(SUCCESS, upstream)
                closure()
                lastCompleted.addAll(flowRun.state.lastCompleted)
                results[idx] = flowRun.state
            } )
        }
        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.DAYS)
        flowRun.state.lastCompleted = lastCompleted
        results.each {
            flowRun.state.result = flowRun.state.result.combine(it.result)
        }
        listener.logger.println("}")
        return results
    }
    
    def locksection (String name, lockClosure) {
        FlowLock lock = null;
        if (shouldNotContinue()) {
            listener.logger.println("Current status is ${currentStatus}, skipping lock section")
            return
        } 
        try {
            lock = new FlowLock(name, flowRun, listener);
            lock.getLock();
            lockClosure();
        } finally {
            lock.releaseLock();
        }
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Property ${name} doesn't exist.");
    }

    def methodMissing(String name, Object args) {
        throw new MissingMethodException(name, this.class, args);
    }
    
    def shouldNotContinue() {
        return flowRun.state.result.isWorseThan(UNSTABLE)
    }
}
