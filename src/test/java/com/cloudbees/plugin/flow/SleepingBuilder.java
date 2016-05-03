/*
 * The MIT License
 *
 * Copyright (c) 2013-2015, Cisco Systems, Inc., a California corporation
 *                          SAP SE
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
package com.cloudbees.plugin.flow;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import static hudson.model.Result.*;

/**
 * A Builder that will sleep for the specified number of seconds.
 *
 * @author: James Nord
 */
public class SleepingBuilder extends Builder {

    public final int sleepSeconds;

    SleepingBuilder(int sleepSeconds) {
        this.sleepSeconds = sleepSeconds;
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        System.out.println("Sleeping Builder in build " + build.getFullDisplayName() + "starting");
        try {
            System.out.println("Sleeping Builder in build " + build.getFullDisplayName() + "waiting");
            Thread.sleep(sleepSeconds * 1000);
        } catch (InterruptedException ex) {
            build.setResult(ABORTED);
        }
        System.out.println("Sleeping Builder in build " + build.getFullDisplayName() + " completing " + build.getResult());
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public String getDisplayName() {
            return "Sleeping builder";
        }

        public SleepingBuilder newInstance(StaplerRequest req, JSONObject data) {
            return new SleepingBuilder(10);
        }
    }
}
