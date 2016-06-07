/*
 * The MIT License
 *
 * Copyright (c) 2013, Cisco Systems, Inc., a California corporation
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

import java.io.File;

import static hudson.model.Result.ABORTED;
import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;

/**
 * A Builder that will block until a file exists.
 *
 * @author James Nord
 */
public class BlockingBuilder extends Builder {

    public final static File DEFAULT_FILE = new File("target", "build_block.txt");

    public final File file;

    BlockingBuilder(File file) {
        this.file = file;
    }


    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        System.out.println("Blocking Builder in build " + build.getFullDisplayName() + "starting");
        try {
            System.out.println("Blocking Builder in build " + build.getFullDisplayName() + "waiting");
            while (file.exists()) {
                Thread.sleep(10L);
            }
            build.setResult(SUCCESS);
        } catch (InterruptedException ex) {
            build.setResult(ABORTED);
        }
        System.out.println("Blocking Builder in build " + build.getFullDisplayName() + " completing " + build.getResult());
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public String getDisplayName() {
            return "Blocking builder";
        }

        public BlockingBuilder newInstance(StaplerRequest req, JSONObject data) {
            return new BlockingBuilder(DEFAULT_FILE);
        }
    }
}
