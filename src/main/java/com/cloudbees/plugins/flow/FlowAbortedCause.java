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
package com.cloudbees.plugins.flow;


import jenkins.model.CauseOfInterruption;
import jenkins.model.Jenkins;

public class FlowAbortedCause extends CauseOfInterruption {

	/** The BuildFlow project that caused the abort */
	private final String project;
	/** The build number of the project that caused the abort. */
	private final int build;

	public FlowAbortedCause(FlowRun flowRun) {
		this.project = flowRun.getParent().getFullName();
		this.build = flowRun.getNumber();
	}

	public String getAbortedBuildFlow() {
		return project;
	}

	public int getAbortedBuildNumber() {
		return build;
	}

	@Override
	public String getShortDescription() {
		return "Job aborted as " + project + "#" + build +" was aborted.";
	}
}
