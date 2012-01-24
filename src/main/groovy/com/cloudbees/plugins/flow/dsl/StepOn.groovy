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

import hudson.model.Result;

public class StepOn {

    Result result;
    Step parentStep;
    String nextStepName;

    public StepOn(Step parentStep, Result result) {
        this.parentStep = parentStep;
        this.result = result;
    }
    
    public Step trigger(String stepName) {
    	this.nextStepName = stepName;
        return this.parentStep;
    }

}