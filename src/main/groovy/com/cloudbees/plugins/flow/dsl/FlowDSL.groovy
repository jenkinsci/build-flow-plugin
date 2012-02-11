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

package com.cloudbees.plugins.flow.dsl

import jenkins.model.Jenkins
import java.util.logging.Logger
import hudson.model.Item
import hudson.model.AbstractProject
import hudson.model.AbstractBuild
import java.util.concurrent.Future
import hudson.model.Cause
import hudson.model.Result;

public class FlowDSL {

    private ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false)
        cl(emc)
        emc.initialize()
        return emc
    }

    def Result executeFlowScript(String dsl, Cause cause) {
        // TODO : add all restrictions, etc ...
        FlowDelegate flow = new FlowDelegate(cause)
        Script dslScript = new GroovyShell().parse(dsl)
        dslScript.metaClass = createEMC(dslScript.class, {
            ExpandoMetaClass emc ->
            emc.flow = {
                Closure cl ->
                cl.delegate = flow
                cl.resolveStrategy = Closure.DELEGATE_FIRST
                cl()
            }
        })
        dslScript.run()
        def ret = flow.failed()
        flow.cleanAfterRun()
        return ret
    }
}

public class FlowDelegate {

    private static final Logger LOGGER = Logger.getLogger(FlowDelegate.class.getName());

    private ThreadLocal<Boolean> parallel = new ThreadLocal<Boolean>()
    private ThreadLocal<List<JobInvocation>> parallelJobs = new ThreadLocal<List<JobInvocation>>()
    private ThreadLocal<List<String>> failuresContext = new ThreadLocal<List<String>>()
    def Cause cause

    public FlowDelegate(Cause c) {
        cause = c
        parallel.set(false)
        parallelJobs.set(new ArrayList<String>())
        failuresContext.set(new ArrayList<String>())
    }

    def failed() {
        // TODO : return the right Result based on failures priority
        if (failuresContext.get().isEmpty()) {
            return Result.SUCCESS
        }
        return Result.FAILURE
    }

    def build(String jobName) {
        if (failuresContext.get().isEmpty()) {
            executeJenkinsJobWithName(jobName);
        }
    }

    def build(Map args, String jobName) {
        if (failuresContext.get().isEmpty()) {
            executeJenkinsJobWithNameAndArgs(jobName, args);
        }
    }

    def parallel(closure) {
        // TODO : handle parallel inside parallel
        if (failuresContext.get().isEmpty()) {
            Map<String, String> results = new HashMap<String, String>()
            List<JobInvocation> oldJobs = new ArrayList<JobInvocation>()
            if (parallel.get()) {
                oldJobs = parallelJobs.get()
                parallelJobs.set(new ArrayList<JobInvocation>())
            }
            parallel.set(true);
            closure()
            println "Parallel execution {"
            for (JobInvocation job : parallelJobs.get()) {
                results.put(job.name, job.runAndContinue())
            }
            println "}"
            println "Waiting for jobs : ${parallelJobs.get()}"
            parallelJobs.get().clear()
            if (!oldJobs.isEmpty()) {
                parallelJobs.set(oldJobs)
            }
            parallel.set(false);
            results.values().each {
                // TODO : enhance it
                AbstractBuild<?, ?> build = it.future().get()
                if (build.getResult() != Result.SUCCESS) {
                    failuresContext.get().add(it.result())
                }
            }
            return results
        }
    }

    def guard(guardedClosure) {
        def deleg = this;
        [ rescue : { rescueClosure ->
            rescueClosure.delegate = deleg
            rescueClosure.resolveStrategy = Closure.DELEGATE_FIRST
            if (failuresContext.get().isEmpty()) {
                List<String> oldContext = failuresContext.get()
                failuresContext.set(new ArrayList<String>())
                println "Guarded {"
                guardedClosure()
                print "}"
                if (failuresContext.get().isEmpty()) { // TODO : check if we have to do try/catch or try/finally
                    println " Rescuing {"
                    rescueClosure()
                    println "}"
                }
                println ""
                failuresContext.set(oldContext)
            }
        } ]
    }

    private def executeJenkinsJobWithName(String name) {
        if (failuresContext.get().isEmpty()) {
            return executeJenkinsJobWithNameAndArgs(name, [:])
        }
    }

    private def executeJenkinsJobWithNameAndArgs(String name, Map args) {
        if (failuresContext.get().isEmpty()) {
            // ask for job with name ${name}
            JobInvocation job = findJob(name, args, cause)
            if (parallel.get()) {
                // if parallel enabled, push the job in a threadlocal list and let other run it for you
                parallelJobs.get().add(job)
            } else {
                job.runAndWait()
            }
            if (job.result() != Result.SUCCESS) {
                failuresContext.get().add(job.result())
            }
            return job;
        }
    }

    def findJob(String name, Map args, Cause cause) {
        return new JobInvocation(name, args, cause)
    }

    def cleanAfterRun() {
        parallel.remove()
        parallelJobs.remove()
        failuresContext.remove()
    }
}

public class JobInvocation {

    def String name
    def Map args
    def Cause cause
    def AbstractProject<?, ? extends AbstractBuild<?, ?>> project
    def AbstractBuild build
    def Result result = Result.SUCCESS
    def Future<? extends AbstractBuild<?, ?>> future

    public JobInvocation(String name, Map args, Cause cause) {
        this.name = name
        this.args = args
        this.cause = cause
        Item item = Jenkins.getInstance().getItem(name);
        if (item instanceof AbstractProject) {
            project = (AbstractProject<?, ? extends AbstractBuild<?,?>>) item;
        } else {
            throw new RuntimeException("Item '${name}' isn't a job.")
        }
    }

    def runAndWait() {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause);
        println "Jenkins is running job : ${name} with args : ${args} and blocking"
        build = future.get();
        result = build.getResult();
        return this;
    }

    def runAndContinue() {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause);
        println "Jenkins is running job : ${name} with args : ${args} and continuing"
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
}