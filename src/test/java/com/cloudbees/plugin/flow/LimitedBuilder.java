/*
 * The MIT License
 *
 * Copyright (c) 2015, SAP SE
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
import static hudson.model.Result.*;
import hudson.tasks.Builder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 */
public class LimitedBuilder extends Builder {

    private static final Map<String,Semaphore> globalMap =  new HashMap<String, Semaphore>();
    private final Semaphore sem;

    public LimitedBuilder( String limitKey ) {
        sem = getSemaphore(limitKey);
        if ( sem == null ) {
            throw new IllegalArgumentException("Key not initialized: " + limitKey);
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        System.out.println("Limited Builder in build " + build.getFullDisplayName() + "starting");
        if ( !sem.tryAcquire() ) {
            build.setResult(FAILURE);
            System.out.println("Limited Builder in build " + build.getFullDisplayName() + " over permits limit!");
            return true;
        }
        try {
            System.out.println("Limited Builder in build " + build.getFullDisplayName() + "waiting");
            Thread.sleep(5 * 1000);
            build.setResult(SUCCESS);
        } catch (InterruptedException ex) {
            build.setResult(ABORTED);
        }
        sem.release();
        System.out.println("Limited Builder in build " + build.getFullDisplayName() + " completing " + build.getResult());
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public String getDisplayName() {
            return "Limited builder";
        }

        public SleepingBuilder newInstance(StaplerRequest req, JSONObject data) {
            throw new UnsupportedOperationException();
        }
    }

    public static synchronized void initialize( String name, int permits ) {
        if ( globalMap.containsKey(name) ) throw new IllegalArgumentException("Key reuse is not permitted!");
        Semaphore s = new Semaphore(permits);
        globalMap.put(name, s);
    }

    private static synchronized Semaphore getSemaphore( String name ) {
        return globalMap.get(name);
    }
}
