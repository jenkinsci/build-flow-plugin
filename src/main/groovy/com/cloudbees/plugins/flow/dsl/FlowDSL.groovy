package com.cloudbees.plugins.flow.dsl;

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
                ScriptDelegate sd = new ScriptDelegate(f);
                cl.delegate = sd
                cl.resolveStrategy = Closure.DELEGATE_FIRST
                cl()
                return f;
            }
        })
        return dslScript.run();
    }
}


public class ScriptDelegate implements Serializable {

    Flow flow;

    public ScriptDelegate(Flow flow) {
        this.flow = flow;
    }

    Step entrystep(String name, Closure c) {
        def jobStep = step(name, c);
        this.flow.entryStepNames.add(name);
        return jobStep;
    }

    Step step(String name, Closure c) {
        def res = c();
        if (res instanceof Job) {
            def jobStep = new JobStep(name, res, this.flow);
            flow.steps.put(name, jobStep);
            return jobStep;
        }
        else {
            throw new RuntimeException("Job expected");
        }
    }

    Job job(String name) {
        return new Job(name);
    }
}

