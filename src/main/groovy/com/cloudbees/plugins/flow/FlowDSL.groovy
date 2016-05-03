/*
 * The MIT License
 *
 * Copyright (c) 2013, CloudBees, Inc., Nicolas De Loof.
 *                     Cisco Systems, Inc., a California corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cloudbees.plugins.flow

import hudson.AbortException
import hudson.console.ModelHyperlinkNote
import hudson.model.*
import hudson.security.ACL
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.NodeProperty
import hudson.util.spring.ClosureScript
import jenkins.model.Jenkins

import org.acegisecurity.context.SecurityContextHolder
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import java.util.concurrent.*
import java.util.logging.Logger

import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS
import hudson.model.Result

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
        }
        catch (AbortException e) {
            // aborted should not cause any logging.
            killRunningJobs(flowRun, listener)
        }
        catch (InterruptedException e) {
            // aborted should not cause any logging.
            killRunningJobs(flowRun, listener)
        } catch (Exception e) {
            listener.error("Failed to run DSL Script")
            e.printStackTrace(listener.getLogger())
            throw e;
        }
    }

    private void killRunningJobs(FlowRun flowRun, BuildListener listener) {
        flowRun.state.result = Executor.currentExecutor().abortResult();
        Executor.currentExecutor().recordCauseOfInterruption(flowRun, listener);

        def graph = flowRun.jobsGraph
        graph.vertexSet().each() { ji ->
            if (flowRun.project != ji.project) {
                // Our project is the fist JobInvocation and we would just be aborting ourselves again.
                println("aborting ${ji.name}")
                ji.abort()
            }
        }
        // wait until all the downstream builds have aborted.
        // we do this in a separate block as aborting a job may take some time to complete.
        graph.vertexSet().each() { ji ->
            if (ji.started && ! ji.completed) {
                if (flowRun.project != ji.project) {
                    // Our project is the fist JobInvocation and we can't schedule ourself to run
                    // so we would have started but have no build.
                    // so don't wait for our self which will cause an exception.
                    println("Waiting for ${ji.name} to finish...")

                    ji.waitForCompletion()
                }
            }
        }
        listener.getLogger().println(hudson.model.Messages.Run_BuildAborted());
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
        println_with_indent { out.print(s) }
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
        if (flowRun.state.result.isWorseThan(Result.fromString(flowRun.getAbortWhenWorseThan()))) {
            println("Abort execution, because one of the last builds is worse than " + flowRun.getAbortWhenWorseThan())
            fail()
        }
    }

    def build(Map args, String jobName) {
        statusCheck()
        // ask for job with name ${name}
        JobInvocation job = new JobInvocation(flowRun, jobName)
        Job p = job.getProject()
        println("Schedule job " + ModelHyperlinkNote.encodeTo(p))

        flowRun.schedule(job, getActions(p,args));
        Run r = job.waitForStart()
        println("Build " + ModelHyperlinkNote.encodeTo('/'+ r.getUrl(), r.getFullDisplayName()) + " started")

        if (null == r) {
            println("Failed to start ${jobName}.")
            fail();
        }

        flowRun.waitForCompletion(job);
        // [JENKINS-22960] wait for build to be finalized.
        flowRun.waitForFinalization(job);
        println(ModelHyperlinkNote.encodeTo('/'+ r.getUrl(), r.getFullDisplayName())
                + " completed ${r.result.isWorseThan(SUCCESS) ? " : " + r.result : ""}")
        return job;
    }

    def getActions(Job job, Map args) {

        List<Action> originalActions = job.getActions();

        List<ParameterDefinition> jobParams = null;
        for(Action action:originalActions) {
            if (action instanceof ParametersDefinitionProperty) {
                ParametersDefinitionProperty parametersAction = (ParametersDefinitionProperty) action;
                jobParams = parametersAction.getParameterDefinitions();
            }
        }
        
        List<Action> actions = new ArrayList<Action>();
        List<ParameterValue> params = [];
        Set<String> addedParams = new HashSet<String>();
        for (Map.Entry param: args) {
            String paramName = param.key
            Object paramValue = param.value
            if (paramValue instanceof Closure) {
                paramValue = getClosureValue(paramValue)
            }
            //Use pre-defined parameter type if it exists and it's simple
            if (jobParams != null) {
                for (ParameterDefinition originalParam: jobParams) {
                    if (originalParam instanceof SimpleParameterDefinition) {
                        if (paramName.equals(originalParam.name)) {
                            try {
                                params.add(originalParam.createValue(paramValue));
                                addedParams.add(paramName);
                            } catch (Exception e) {
                                //This usually means that createValue(String) is
                                //unimplemented and we can't use the definition.
                            }
                        }
                    }
                }
            }
            if (addedParams.contains(paramName)) {
                //Added the parameter already, so go on to the next one
                continue;
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
        def closureException = null
        try {
            println("ignore("+result+") {")
            ++indent
            closure()
        }
        catch ( Exception ex ) {
            closureException = ex
        }
        finally {
            // rethrow if there was a non-JobExecutionFailureException Exception
            if ( closureException != null && !(closureException instanceof JobExecutionFailureException) ) {
                throw closureException
            }

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

    def retry(int attempts, worstAllowed='SUCCESS', retryClosure) {
        statusCheck()
        Result origin = flowRun.state.result
        int i = 0;
        Result worstAllowedResult = Result.fromString(worstAllowed)
        while( attempts-- > 0) {
            // Restore the pre-retry result state to ignore failures
            flowRun.state.result = origin
            i++;
            println("retry (attempt $i) {")
            ++indent

            retryClosure()

            --indent

            if (flowRun.state.result.isBetterOrEqualTo(worstAllowedResult)) {
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
        // TODO use NamingThreadFactory since Jenkins 1.541
        ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                def thread = Executors.defaultThreadFactory().newThread(r);
                thread.name = "BuildFlow parallel statement thread for " + flowRun.parent.fullName;
                return thread;
            }
        });
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
                    listener.error("Failed to run DSL Script")
                    e.printStackTrace(listener.getLogger())
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

    /**
     * Access the build flow DSL extensions that come from other plugins.
     *
     * <p>
     * For example,
     *
     * <pre>
     * build("job1")
     * x = extension.'foobar' // foobar is the name of the plugin
     * x.someMethod()
     * </pre>
     */
    def getExtension() {
        return new DynamicExtensionLoader(this);
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Property ${name} doesn't exist.");
    }

    def methodMissing(String name, Object args) {
        throw new MissingMethodException(name, this.class, args);
    }
}

class DynamicExtensionLoader {
    FlowDelegate outer;

    DynamicExtensionLoader(FlowDelegate outer) {
        this.outer = outer
    }

    def propertyMissing(name) {
        def v = BuildFlowDSLExtension.all().findResult {
            BuildFlowDSLExtension ext -> ext.createExtension(name, outer)
        }
        if (v==null)
            throw new UnsupportedOperationException("No such extension available: "+name)
        return v;
    }
}
