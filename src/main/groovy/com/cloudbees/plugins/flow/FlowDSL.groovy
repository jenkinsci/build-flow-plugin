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

import com.cloudbees.plugins.flow.JobNotFoundException
import java.util.concurrent.Future
import java.util.logging.Logger
import jenkins.model.Jenkins
import hudson.model.*
import static hudson.model.Result.SUCCESS
import com.cloudbees.plugins.flow.FlowRun
import com.cloudbees.plugins.flow.FlowCause
import com.cloudbees.plugins.flow.FlowExecutionFailureException

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
            flow.cleanAfterRun()
        }
    }

    // TODO define a parseFlowScript to validate flow DSL and maintain jobs dependencygraph
}

public class FlowDelegate {

    private static final Logger LOGGER = Logger.getLogger(FlowDelegate.class.getName());

    private ThreadLocal<Boolean> retryContext = new ThreadLocal<Boolean>()

    def List<Cause> causes
    def FlowRun flowRun

    public FlowDelegate(FlowRun flowRun) {
        this.flowRun = flowRun
        causes = flowRun.causes
        retryContext.set(false)
    }

    def fail() {
        // Stop the flow execution
        throw new FlowExecutionFailureException()
    }

    def build(String jobName) {
        build([:], jobName)
    }

    def build(Map args, String jobName) {
        // ask for job with name ${name}
        JobInvocation job = new JobInvocation(jobName, args, new FlowCause(flowRun))
        job.runAndWait()
        flowRun.addBuild(job.build)
        if (job.result.isWorseThan(SUCCESS)) {
            fail();
        }
        return job;
    }

    def guard(guardedClosure) {
        def deleg = this;
        [ rescue : { rescueClosure ->
            rescueClosure.delegate = deleg
            rescueClosure.resolveStrategy = Closure.DELEGATE_FIRST

            LOGGER.fine("Guarded {")
            try {
                guardedClosure()
            } finally {

                LOGGER.fine("} Rescuing {")
                rescueClosure()
                LOGGER.fine("}")
            }
        } ]
    }


    def retry(retryClosure) {
        return retry(SUCCESS, retryClosure)
    }
    def retry(Result result, retryClosure) {
        retryContext.set(true)
        return {
            if (retryContext.get()) {
                retryContext.set(false)
                retryClosure()
                if (flowRun.localResult.isWorseThan(SUCCESS)) {
                    retryContext.set(true)
                }
            }
        }
    }

    private def cleanAfterRun() {
        retryContext.remove()
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Property ${name} doesn't exist.");
    }

    def methodMissing(String name, Object args) {
        throw new MissingMethodException("Method ${name} doesn't exist.");
    }
}

public class JobInvocation {

    private static final Logger LOGGER = Logger.getLogger(JobInvocation.class.getName());

    def String name
    def Map args
    def Cause cause
    def AbstractProject<?, ? extends AbstractBuild<?, ?>> project
    def AbstractBuild build
    def Result result = SUCCESS
    def Future<? extends AbstractBuild<?, ?>> future

    // TODO : add helpers for manipulation inside DSL

    public JobInvocation(String name, Map args, Cause cause) {
        this.name = name
        this.args = args
        this.cause = cause
        Item item = Jenkins.getInstance().getItemByFullName(name);
        if (item instanceof AbstractProject) {
            project = (AbstractProject<?, ? extends AbstractBuild<?,?>>) item;
        } else {
            throw new JobNotFoundException("Item '${name}' not found (or isn't a job).")
        }
    }

    def runAndWait() {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause, getActions());
        LOGGER.fine("Jenkins is running job : ${name} with args : ${args} and blocking")
        build = future.get();
        result = build.getResult();
        return this;
    }

    def runAndContinue() {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause, getActions());
        LOGGER.fine("Jenkins is running job : ${name} with args : ${args} and continuing")
        return this;
    }

    def result() {
        return result;
    }

    def build() {
        if (build == null) {
            build = future.get()
        }
        return build
    }

    def future() {
        return future
    }

    def String toString() {
        return "Job : ${name} with ${args}"
    }

    def getActions() {
        List<Action> actions = new ArrayList<Action>();
        for (Object param: args) {
            String paramName = param.key
            Object paramValue = param.value
            if (paramValue instanceof Closure) {
                paramValue = getClosureValue(paramValue)
            }
            if (paramValue instanceof Boolean) {
                actions.add(new ParametersAction(new BooleanParameterValue(paramName, (Boolean) paramValue)))
            }
            else {
                //TODO For now we will only support String and boolean parameters
                actions.add(new ParametersAction(new StringParameterValue(paramName, paramValue.toString())))
            }
        }
        return actions
    }

    def getClosureValue(closure) {
        return closure()
    }
}
