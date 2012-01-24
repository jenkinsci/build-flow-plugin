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

public class FlowDSL {
    
    private FlowDSL() {
        
    }

    static ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false);
        cl(emc)
        emc.initialize()
        return emc
    }

    public static Flow evalScript(String script) {

        Binding binding = new Binding();
        ClassLoader parent = FlowDSL.class.getClassLoader();
        Script dslScript = new GroovyShell(parent, binding).parse(script);
        dslScript.metaClass = createEMC(dslScript.class, { ExpandoMetaClass emc ->

            emc.flow = { Closure cl ->
                Flow f = new Flow();
                FlowDelegate fd = new FlowDelegate(f);
                cl.delegate = fd;
                cl.resolveStrategy = Closure.DELEGATE_ONLY;
                cl();
                return f;
            }
        })
        return dslScript.run();
    }
}


public class FlowDelegate implements Serializable {

	def SUCCESS = Result.SUCCESS;
	def UNSTABLE = Result.UNSTABLE;
	def FAILURE = Result.FAILURE;

    Flow flow;

    public FlowDelegate(Flow flow) {
        this.flow = flow;
    }
    
    def invokeMethod(String name, args) {
       if (name.startsWith("step") && args.length > 0 && args[0] instanceof Closure) {
           return step(name, args[0]);
       }
       else {
           throw new MissingMethodException(name, FlowDelegate.class, args);
       }
	}
	
	def propertyMissing(String name) {
	   if (name.startsWith("step")) {
           return name;
       }
       else {
           throw new MissingPropertyException(name, FlowDelegate.class);
       }
	}

    Step step(String name, Closure cl) {
        Step s = new Step(name, flow);
        StepDelegate sd = new StepDelegate(s);
        cl.delegate = sd;
        cl.resolveStrategy = Closure.DELEGATE_ONLY;
        cl();
        if (flow.steps.size() == 0) {
            //By convention first step is entry step
            flow.entryStepName = name;
        }
        flow.steps.put(name, s);
        return s;
    }
}

public class StepDelegate implements Serializable {

    Step step;

    public StepDelegate(Step step) {
        this.step = step;
    }

    void job(String name) {
        this.step.addJob(name);
    }
}

