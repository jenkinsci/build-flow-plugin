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

package com.cloudbees.plugins.flow.dsl;

public class FlowDSL {

    ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false)
        cl(emc)
        emc.initialize()
        return emc
    }

    String executeFlowScript(String dsl, JobCause cause) {
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
        def ret = FlowDSLSyntax.DONE_KEYWORD
        if (flow.failed())  {
            ret = FlowDSLSyntax.FAILED_KEYWORD
        }
        flow.cleanAfterRun()
        return ret
    }
}

public class FlowDelegate {

    private ThreadLocal<Boolean> parallel = new ThreadLocal<Boolean>()
    private ThreadLocal<List<JobInvocation>> parallelJobs = new ThreadLocal<List<JobInvocation>>()
    private ThreadLocal<List<String>> failuresContext = new ThreadLocal<List<String>>()
    def cause

    public FlowDelegate(JobCause c) {
        cause = c
        parallel.set(false)
        parallelJobs.set(new ArrayList<String>())
        failuresContext.set(new ArrayList<String>())
    }

    def failed() {
        return !failuresContext.get().isEmpty()
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
        // TODO : handle paralle inside parallel
        if (failuresContext.get().isEmpty()) {
            Map<String, String> results = new HashMap<String, String>()
            List<JobInvocation> oldJobs = new ArrayList<JobInvocation>()
            if (parallel.get()) {
                oldJobs = parallelJobs.get()
                parallelJobs.set(new ArrayList<JobInvocation>())
            }
            parallel.set(true);
            closure()
            // TODO : run all job from TL list in parallel then return results
            println "Parallel execution {"
            for (JobInvocation job : parallelJobs.get()) {
                results.put(job.name, job.runAndWait())    // TODO : change it
            }
            println "}"
            // TODO : run all job from TL list in parallel then return results
            parallelJobs.get().clear()
            if (!oldJobs.isEmpty()) {
                parallelJobs.set(oldJobs)
            }
            parallel.set(false);
            results.values().each {
                if (it.result() != FlowDSLSyntax.DONE_KEYWORD) {
                    failuresContext.get().add(it.result())
                }
            }
            return results
        }
    }

    def guard(guardedClosure) {
        if (failuresContext.get().isEmpty()) {
            return new GuardedJob(guardedClosure, failuresContext)
        }
    }

    private def executeJenkinsJobWithName(String name) {
        if (failuresContext.get().isEmpty()) {
            return executeJenkinsJobWithNameAndArgs(name, [:])
        }
    }

    private def executeJenkinsJobWithNameAndArgs(String name, Map args) {
        if (failuresContext.get().isEmpty()) {
            // ask for job with name ${name}
            JobInvocation job = findJob(name, args)
            if (parallel.get()) {
                // if parallel enabled, push the job in a threadlocal list and let other run it for you
                parallelJobs.get().add(job)
            } else {
                job.runAndWait()
            }
            if (job.result() != FlowDSLSyntax.DONE_KEYWORD) {
                failuresContext.get().add(job.result())
            }
            return job;
        }
    }

    // TODO : plug with actual job search
    def findJob(String name, Map args) {
        return new JobInvocation(name: name, args: args)
    }

    def cleanAfterRun() {
        parallel.remove()
        parallelJobs.remove()
        failuresContext.remove()
    }
}

public class GuardedJob {
    def guardedClosure
    def ThreadLocal<List<String>> failureContext
    public GuardedJob(Closure c, ThreadLocal<List<String>> f) {
        guardedClosure = c
        failureContext = f
    }
    def rescue(rescueClosure) {
        List<String> oldContext = failureContext.get()
        failureContext.set(new ArrayList<String>())
        try {
            println "Guarded {"
            guardedClosure()
            print "}"
            if (!failureContext.get().isEmpty()) {
                throw new RuntimeException("Failure appened during guarded exec")
            }
        } catch (Throwable t) {
            println " Rescuing {"
            rescueClosure()
            println "}"
        }
        println ""
        failureContext.set(oldContext)
    }
}

// TODO : plug with actual Job invocation
public class JobInvocation {

    String name
    Map args

    def runAndWait() {
        println "Jenkins is running job : ${name} with args : ${args}"
        return this;
    }

    def result() {
        if (name == "willFail") {
            return FlowDSLSyntax.FAILED_KEYWORD
        }
        return FlowDSLSyntax.DONE_KEYWORD
    }

    def String toString() {
        return "Job : ${name} with ${args}"
    }
}

// TODO : use actual cause
public class JobCause {
    def from = "root"
    def build = new JobInvocation(name: "root", args: [:])
}

public class FlowDSLSyntax  {
    public final static String BUILD_KEYWORD = "build"
    public final static String CAUSE_KEYWORD = "cause"
    public final static String PARALLEL_KEYWORD = "parallel"
    public final static String GUARD_KEYWORD = "guard"
    public final static String RESCUE_KEYWORD = "rescue"
    public final static String CLEAN_KEYWORD = "clean"
    public final static String DONE_KEYWORD = "DONE"
    public final static String FAILED_KEYWORD = "FAILED"
    public final static String WARNING_KEYWORD = "WARNING"
}