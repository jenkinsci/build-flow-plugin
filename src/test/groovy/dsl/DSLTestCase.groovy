package dsl

import org.jvnet.hudson.test.HudsonTestCase
import hudson.model.FreeStyleProject
import org.jvnet.hudson.test.FailureBuilder
import com.cloudbees.plugins.flow.dsl.FlowDSL
import org.junit.Assert
import hudson.model.Result

abstract class DSLTestCase extends HudsonTestCase {

    def createJob = {String name ->
        return createFreeStyleProject(name);
    }

     def createJobs = { names ->
         def jobs = []
         names.each {
             jobs.add(createJob(it))
         }
         return jobs
    }

    def createFailJob = {String name ->
        def job = createJob(name)
        job.getBuildersList().add(new FailureBuilder())
        return job
    }

    def run = { script ->
        return new FlowDSL().executeFlowScript(script, null)
    }

    def runWithCause = { script, cause ->
        return new FlowDSL().executeFlowScript(script, cause)
    }

    def assertSuccess = { job ->
        assert Result.SUCCESS == job.getBuilds().getFirstBuild().getResult()
    }

    def assertAllSuccess = { jobs ->
        jobs.each {
            assert Result.SUCCESS == it.getBuilds().getFirstBuild().getResult()
        }
    }

    def assertFailure = { job ->
        assert Result.FAILURE == job.getBuilds().getFirstBuild().getResult()
    }
}
