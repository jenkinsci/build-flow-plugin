package com.cloudbees.plugins.flow;

import hudson.ExtensionPoint;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class DownStreamRunDeclarer implements ExtensionPoint {


    public abstract List<Run> getDownStream(Run r) throws ExecutionException, InterruptedException;

    public static List<DownStreamRunDeclarer> all() {
        return Jenkins.getInstance().getExtensionList(DownStreamRunDeclarer.class);
    }

}
