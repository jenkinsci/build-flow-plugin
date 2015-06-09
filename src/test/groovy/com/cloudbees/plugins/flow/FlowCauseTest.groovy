package com.cloudbees.plugins.flow

import com.gargoylesoftware.htmlunit.html.HtmlPage
import hudson.model.Cause
import hudson.model.CauseAction
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

import static org.junit.Assert.assertTrue

class FlowCauseTest {

    @Rule
    public JenkinsRule j = new JenkinsRule()

    protected BuildFlow createFlow(String name, String dsl) {
        def job = j.jenkins.createProject(BuildFlow, name)
        job.dsl = dsl
        job.save()
        return job
    }

    /**
     * Given a pipeline A --> FLOW --> B, then the build for a B build must display Flow <-- A.
     */
    @Test
    void 'Flow cause not blocking the upstream causes'() {
        // Jobs
        def a = createFlow('A', 'build("B")')
        def b = createFlow('B', 'build("C")')
        def c = createFlow('C', '')

        // Triggers A
        j.assertBuildStatusSuccess(a.scheduleBuild2(0, new Cause.UserIdCause()))

        // Checks the builds
        j.assertBuildStatusSuccess(b.builds.lastBuild)

        // Last build in the sequence
        def build = c.builds.lastBuild
        j.assertBuildStatusSuccess(build)

        // Checks the cause tree
        def causeAction = build.actions.find { it instanceof CauseAction } as CauseAction
        assert causeAction != null: "A cause action must be associated with the build"
        assert causeAction.causes.size() == 1
        def cause = causeAction.causes.first() as Cause.UpstreamCause
        assert cause.shortDescription == 'Started by build flow B#1'
        def upstreamCause = cause.upstreamCauses.first() as Cause.UpstreamCause
        assert upstreamCause.shortDescription == 'Started by upstream project "A" build number 1'

        // Goes to the build status page of C
        HtmlPage page = j.createWebClient().goTo(c.builds.lastBuild.url)
        assert page.titleText == 'C #1 [Jenkins]'

        // Just checking the different causes are in the page (a bit primitive)
        def bPosition = page.body.textContent.indexOf('Started by build flow  B')
        assertTrue(bPosition > 0)
        assertTrue(page.body.textContent.indexOf('Started by upstream project A build number 1') > bPosition)

    }

}
