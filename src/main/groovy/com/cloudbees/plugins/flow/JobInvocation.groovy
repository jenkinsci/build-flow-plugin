package com.cloudbees.plugins.flow;

import hudson.model.*;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import java.text.DateFormat;
import java.util.UUID;
import java.awt.Color;

/**
 * @author: <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JobInvocation {

    private static final Logger LOGGER = Logger.getLogger(JobInvocation.class.getName());

    private final String name;
    private int buildNumber;

    // The FlowRun build that initiated this job
    private transient final FlowRun run;

    private transient AbstractProject<?, ? extends AbstractBuild<?, ?>> project;

    private transient AbstractBuild build;

    private transient Future<? extends AbstractBuild<?, ?>> future;

    // A unique number that identifies when in the FlowRun this job was started
    private int buildIndex;

    private int displayColumn;

    private int displayRow;

    // Whether the build has started. If true, this.build should be set.
    private boolean started;
    // Whether the build has completed
    private boolean completed;

    public JobInvocation(FlowRun run, AbstractProject project) {
        this.run = run;
        this.name = project.getFullName();
        this.project = project;
    }

    public JobInvocation(FlowRun run, String name) {
        this(run, getProjectByName(run, name));
    }

    private static AbstractProject getProjectByName(FlowRun run, String name) {
        final ItemGroup context = run.getProject().getParent()
        AbstractProject item = Jenkins.getInstance().getItem(name, (ItemGroup) context, AbstractProject.class);
        if (item == null) {
            throw new JobNotFoundException("Item " + name + "not found (or isn't a job).");
        }
        return item;
    }

    /* package */ JobInvocation run(Cause cause, List<Action> actions) {
        future = project.scheduleBuild2(project.getQuietPeriod(), cause, actions);
        if (future == null) {
            throw new CouldNotScheduleJobException("Could not schedule job "
                    + project.getName() +", ensure its not already enqueued with same parameters", e);
        }
        return this;
    }

    /**
     * Delegate method calls we don't implement to the actual {@link Run} so that DSL feels like a Run object has been
     * returned, but we can lazy-resolve the actual Run object and add some helper methods
     */
    def invokeMethod(String name, args) {
        // Calling $name with $args on actual Run object
        getBuild()."$name"(*args)
    }

    def propertyMissing(String name) {
        // Retrieve property $name from actual Run object
        return getBuild()."$name"
    }

    public Result getResult() throws ExecutionException, InterruptedException {
        waitForCompletion();
        return build.getResult();
    }

    public String getResultString() throws ExecutionException, InterruptedException {
        return getResult().toString().toLowerCase();
    }

    public String getColorForHtml() {
        BallColor color = BallColor.NOTBUILT;
        if (build != null) {
            color = build.getIconColor();
        }
        return color.getHtmlBaseColor();
    }

    /* package */ void setBuildIndex(int buildIndex) {
        this.buildIndex = buildIndex;
    }

    public int getDisplayColumn() {
        return displayColumn;
    }

    /* package */ void setDisplayColumn(int displayColumn) {
        this.displayColumn = displayColumn;
    }

    public int getDisplayRow() {
        return displayRow;
    }

    /* package */ void setDisplayRow(int displayRow) {
        this.displayRow = displayRow;
    }

    /* package */ AbstractBuild getFlowRun() {
        return run;
    }

    /* package */ void buildStarted(AbstractBuild build) {
        this.started = true;
        this.build = build;
        this.buildNumber = build.getNumber();
    }

    /* package */ void buildCompleted() {
        this.completed = true;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return "build-" + buildIndex;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getBuildUrl() {
        return this.build != null ? this.build.getAbsoluteUrl() : null;
    }

    public String getStartTime() {
        String formattedStartTime = "";
        if (build.getTime() != null) {
            formattedStartTime = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT)
                .format(build.getTime());
        }
        return formattedStartTime;
    }

    public Run getBuild() throws ExecutionException, InterruptedException {
        if (build == null) {
            if (future != null) {
                // waiting for build to run
                build = future.get();
                buildNumber = build.getNumber();
            } else if (buildNumber > 0) {
                // loaded from persistent store
                build = getProject().getBuildByNumber(buildNumber);
            }
        }
        return build;
    }

    public AbstractProject<?, ? extends AbstractBuild<?, ?>> getProject() {
        if (project == null)
            project = Jenkins.getInstance().getItemByFullName(name, AbstractProject.class);
        return project;
    }

    public String toString() {
        return name + (build != null ? " #" + build.number : "");
    }

    public void waitForCompletion() throws ExecutionException, InterruptedException {
        if (!completed) {
            if (future != null) {
                future.get();
            } else {
                throw new RuntimeException("Can't wait for completion.");
            }
        }
    }

    /**
     * Initial vertex for the build DAG. To be used by FlowRun constructor to initiate the DAG
     */
    static class Start extends JobInvocation {

        public Start(FlowRun run) {
            super(run, run.getProject());
        }
    }
}
