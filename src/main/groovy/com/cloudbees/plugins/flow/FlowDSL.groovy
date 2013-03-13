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

import hudson.util.spring.ClosureScript
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import java.util.logging.Logger
import jenkins.model.Jenkins
import hudson.model.*
import static hudson.model.Result.SUCCESS
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

import java.util.concurrent.TimeUnit
import hudson.slaves.NodeProperty
import hudson.slaves.EnvironmentVariablesNodeProperty

import hudson.console.HyperlinkNote
import java.util.concurrent.Future
import java.util.concurrent.Callable

import static hudson.model.Result.FAILURE
import java.util.concurrent.ExecutionException
import java.util.concurrent.CopyOnWriteArrayList
import hudson.security.ACL
import org.acegisecurity.context.SecurityContextHolder

public class FlowDSL {

    def void executeFlowScript(FlowRun flowRun, String dsl, BuildListener listener) {
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

        // TODO : add restrictions for System.exit, etc ...
        FlowDelegate flow = new FlowDelegate(flowRun, listener, upstream, envMap)


        // parse the script in such a way that it delegates to the flow object as default
        def cc = new CompilerConfiguration();
        cc.scriptBaseClass = ClosureScript.class.name;
        def ic = new ImportCustomizer()
        ic.addStaticStars(Result.class.name)
        cc.addCompilationCustomizers(ic)

        ClosureScript dslScript = (ClosureScript)new GroovyShell(Jenkins.instance.pluginManager.uberClassLoader,new Binding(),cc).parse(dsl)
        dslScript.setDelegate(flow);

        try {
            dslScript.run()
        } catch(JobExecutionFailureException e) {
            listener.println("flow failed to complete : " + flowRun.state.result)
        } catch (Exception e) {
            listener.error("Failed to run DSL Script")
            e.printStackTrace(listener.getLogger())
            throw e;
        }

    }

    // TODO define a parseFlowScript to validate flow DSL and maintain jobs dependencygraph
}

@SuppressWarnings("GroovyUnusedDeclaration")
public class FlowDelegate {

    private static final Logger LOGGER = Logger.getLogger(FlowDelegate.class.getName());
    def List<Cause> causes
    def FlowRun flowRun
    BuildListener listener
    int indent = 0
    private AbstractBuild upstream;
    private Map env;

    public FlowDelegate(FlowRun flowRun, BuildListener listener, upstream, env) {
        this.flowRun = flowRun
        this.listener = listener
        causes = flowRun.causes
        this.upstream = upstream
        this.env = env
    }

    def getOut() {
        return listener.logger
    }

    // TODO Assuring proper indent should be done in the listener?
    def synchronized println_with_indent(Closure f) {
        for (int i = 0; i < indent; ++i) {
            out.print("    ")
        }
        f()
        out.println()
    }

    def println(String s) {
        println_with_indent { out.println(s) }
    }

    def fail() {
        // Stop the flow execution
        throw new JobExecutionFailureException()
    }

    def build(String jobName) {
        build([:], jobName)
    }

    def getBuild() {
        return flowRun;
    }

    def getParams() {
        return flowRun.buildVariables;
    }

    /**
     * Upstream build that triggered this flow execution, if any.
     */
    AbstractBuild getUpstream() {
        return upstream;
    }

    /**
     * Environment variables that the build gets from its context.
     */
    Map<String,String> getEnv() {
        return env
    }

    /**
     * Check flow status and stop if unexpected failure is detected
     */
    private void statusCheck() {
        if (flowRun.state.result.isWorseThan(SUCCESS)) {
            fail()
        }
    }

    def build(Map args, String jobName) {
        statusCheck()
        // ask for job with name ${name}
        JobInvocation job = new JobInvocation(flowRun, jobName)
        Job p = job.getProject()
        println("Trigger job " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()))


        Run r = flowRun.run(job, getActions(p, args));
        if (null == r) {
            println("Failed to start ${jobName}.")
            fail();
        }

        println(HyperlinkNote.encodeTo('/'+ r.getUrl(), r.getFullDisplayName())
                + " completed ${r.result.isWorseThan(SUCCESS) ? " : " + r.result : ""}")
        return job;
    }

    def getActions(Job job, Map args) {

        List<Action> actions = new ArrayList<Action>();
        List<ParameterValue> params = [];
        Set<String> addedParams = new HashSet<String>();
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
            addedParams.add(paramName);
            //TODO For now we only support String and boolean parameters
        }


        /* Add default values from defined params in the target job */
        List<Action> originalActions = job.getActions();
        

        List<ParameterDefinition> jobParams = null;
        for(Action action:originalActions) {
            if (action instanceof ParametersDefinitionProperty) {
                ParametersDefinitionProperty parametersAction = (ParametersDefinitionProperty) action;
                jobParams = parametersAction.getParameterDefinitions();
            }
        }

        if (jobParams != null) {

            for (ParameterDefinition originalParam: jobParams) {

                String paramName = originalParam.getName()
                ParameterValue originalParamValue = originalParam.getDefaultParameterValue();

                if (addedParams.contains(paramName)){
                    //Already filled parameter
                    continue;
                }

                params.add(originalParamValue)
            }
        }

        //Additionnal parameters not available in the target job
        actions.add(new ParametersAction(params));
        return actions
    }

    def getClosureValue(closure) {
        return closure()
    }

    def guard(guardedClosure) {
        statusCheck()
        def deleg = this;
        [ rescue : { rescueClosure ->
            rescueClosure.delegate = deleg
            rescueClosure.resolveStrategy = Closure.DELEGATE_FIRST

            try {
                println("guard {")
                ++indent
                guardedClosure()
            } finally {
                --indent
                // Force result to SUCCESS so that rescue closure will execute
                Result r = flowRun.state.result
                flowRun.state.result = SUCCESS
                println("} rescue {")
                ++indent
                try {
                    rescueClosure()
                } finally {
                    --indent
                    println("}")
                }
                // restore result, as the worst from guarded and rescue closures
                flowRun.state.result = r.combine(flowRun.state.result)
            }
        } ]
    }

    def ignore(Result result, closure) {
        statusCheck()
        Result r = flowRun.state.result
        try {
            println("ignore("+result+") {")
            ++indent
            closure()
        } finally {

            final boolean ignore = flowRun.state.result.isBetterOrEqualTo(result)
            if (ignore) {
                // restore result
                println("// ${flowRun.state.result} ignored")
                flowRun.state.result = r
            }
            --indent
            println("}")

            if (ignore) return   // hides JobExecutionFailureException that may have been thrown running the closure
        }
    }

    def retry(int attempts, retryClosure) {
        statusCheck()
        Result origin = flowRun.state.result
        int i
        while( attempts-- > 0) {
            // Restore the pre-retry result state to ignore failures
            flowRun.state.result = origin
            println("retry (attempt $i++} {")
            ++indent

            retryClosure()

            --indent

            if (flowRun.state.result.isBetterOrEqualTo(SUCCESS)) {
                println("}")
                return;
            }

            println("} // failed")
        }
    }

    // allows syntax like : parallel(["Kohsuke","Nicolas"].collect { name -> return { build("job1", param1:name) } })
    def List<FlowState> parallel(Collection<? extends Closure> closures) {
        parallel(closures as Closure[])
    }

    // allows collecting job status by name rather than by index
    // inspired by https://github.com/caolan/async#parallel
    def Map<?, FlowState> parallel(Map<?, ? extends Closure> args) {
        def keys     = new ArrayList<?>()
        def closures = new ArrayList<? extends Closure>()
        args.entrySet().each { e ->
          keys.add(e.key)
          closures.add(e.value)
        }
        def results = new LinkedHashMap<?, FlowState>()
        def flowStates = parallel(closures) // as List<FlowState>
        flowStates.eachWithIndex { v, i -> results[keys[i]] = v }
        results
    }

    def List<FlowState> parallel(Closure ... closures) {
        statusCheck()
        ExecutorService pool = Executors.newCachedThreadPool()
        Set<Run> upstream = flowRun.state.lastCompleted
        Set<Run> lastCompleted = Collections.synchronizedSet(new HashSet<Run>())
        def results = new CopyOnWriteArrayList<FlowState>()
        def tasks = new ArrayList<Future<FlowState>>()

        println("parallel {")
        ++indent

        def current_state = flowRun.state
        try {

            closures.each {closure ->
                Closure<FlowState> track_closure = {
                    def ctx = ACL.impersonate(ACL.SYSTEM)
                    try {
                        flowRun.state = new FlowState(SUCCESS, upstream)
                        closure()
                        lastCompleted.addAll(flowRun.state.lastCompleted)
                        return flowRun.state
                    } finally {
                        SecurityContextHolder.setContext(ctx)
                    }
                }

                tasks.add(pool.submit(track_closure as Callable))
            }

            tasks.each {task ->
                try {
                    def final_state = task.get()
                    Result result = final_state.result
                    results.add(final_state)
                    current_state.result = current_state.result.combine(result)
                } catch(ExecutionException e)
                {
                    // TODO perhaps rethrow?
                    current_state.result = FAILURE
                }
            }

            pool.shutdown()
            pool.awaitTermination(1, TimeUnit.DAYS)
            current_state.lastCompleted =lastCompleted
        } finally {
            flowRun.state = current_state
            --indent
            println("}")
        }
        return results
    }
    

    def propertyMissing(String name) {
        throw new MissingPropertyException("Property ${name} doesn't exist.");
    }

    def methodMissing(String name, Object args) {
        throw new MissingMethodException(name, this.class, args);
    }
}
