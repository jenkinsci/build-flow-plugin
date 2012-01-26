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

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import hudson.model.AbstractBuild
import hudson.model.Result;

import static org.codehaus.groovy.syntax.Types.*;

public class FlowDSL {

	private FlowDSL() {

	}

	private static ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
		ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false);
		cl(emc)
		emc.initialize()
		return emc
	}

	/**
	 * Evaluate the groovy script describing the flow (DSL).
	 * @param script
	 * @return
	 */
	public static Flow readFlow(String script) {

		Binding binding = new Binding();
		ClassLoader parent = FlowDSL.class.getClassLoader();
		
		final ImportCustomizer imports = new ImportCustomizer().addStaticStars('hudson.model.Result')
		final SecureASTCustomizer secure = new SecureASTCustomizer()
		secure.with {
			closuresAllowed = true
			methodDefinitionAllowed = false
			packageAllowed = false

			importsWhitelist = []
			staticImportsWhitelist = []
			staticStarImportsWhitelist = ['hudson.model.Result'] // only java.lang.Math is allowed

			tokensWhitelist = [
					PLUS,
					MINUS,
					MULTIPLY,
					DIVIDE,
					MOD,
					POWER,
					PLUS_PLUS,
					MINUS_MINUS,
					COMPARE_EQUAL,
					COMPARE_NOT_EQUAL,
					COMPARE_LESS_THAN,
					COMPARE_LESS_THAN_EQUAL,
					COMPARE_GREATER_THAN,
					COMPARE_GREATER_THAN_EQUAL,
					ASSIGN
			].asImmutable()

			constantTypesClassesWhiteList = [
					Integer,
					Float,
					Long,
					Double,
					BigDecimal,
					String,
					Boolean,
					Integer.TYPE,
					Long.TYPE,
					Float.TYPE,
					Double.TYPE,
					Boolean.TYPE
			].asImmutable()

			receiversClassesWhiteList = [
					Math,
					Integer,
					Float,
					Double,
					Long,
					BigDecimal,
					String,
					Boolean,
					Object
			].asImmutable()
		}
		CompilerConfiguration config = new CompilerConfiguration()
		config.addCompilationCustomizers(imports, secure)
		
		
		Script dslScript = new GroovyShell(parent, binding, config).parse(script);
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

	/**
	 * Evaluate the "then" closure after a job execution
	 * @param flow
	 */
	public static void evalJobThen(Closure jobThenCl, Flow flow, AbstractBuild build) {
		JobThenDelegate sd = new JobThenDelegate(flow, build);
		jobThenCl.delegate = sd;
		jobThenCl.resolveStrategy = Closure.DELEGATE_ONLY;
		jobThenCl();
	}
	
	/**
	* Evaluate the "and" closure before a job execution
	* @param flow
	*/
   public static void evalJobAnd(Closure jobAndCl, Flow flow) {
	   JobAndDelegate ad = new JobAndDelegate(flow);
	   jobAndCl.delegate = ad;
	   jobAndCl.resolveStrategy = Closure.DELEGATE_ONLY;
	   jobAndCl();
   }

	/**
	 * Evaluate the closure of a step transition
	 * @param flow
	 */
	public static boolean evalCondition(Closure condition, Flow flow) {
		ConditionDelegate cd = new ConditionDelegate(flow);
		condition.delegate = cd;
		condition.resolveStrategy = Closure.DELEGATE_ONLY;
		return condition();
	}

	/**
	 * Evaluate the closure of a parameter value
	 * @param flow
	 */
	public static Object evalParam(Closure paramCl, Flow flow) {
		ParamDelegate pd = new ParamDelegate(flow);
		paramCl.delegate = pd;
		paramCl.resolveStrategy = Closure.DELEGATE_ONLY;
		return paramCl();
	}
}


public class FlowDelegate implements Serializable {

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

	private Step step;

	public StepDelegate(Step step) {
		this.step = step;
	}

	Job trigger(String name) {
		return this.step.addJob(name);
	}
}

public class JobThenDelegate implements Serializable {

	private Flow flow;
	private AbstractBuild build;

	public JobThenDelegate(Flow flow, AbstractBuild build) {
		this.flow = flow;
		this.build = build;
	}

	def propertyMissing(String name, value) { this.flow.storage[name] = value }
	def propertyMissing(String name) { this.flow.storage[name] }
	
	public int buildNumber() {
		return this.build.number;
	}
	
	public Result buildResult() {
		return this.build.result;
	}

}

public class JobAndDelegate implements Serializable {

	private Flow flow;

	public JobAndDelegate(Flow flow) {
		this.flow = flow;
	}

	def propertyMissing(String name, value) { flow.storage[name] = value }
	def propertyMissing(String name) { flow.storage[name] }

}

public class ConditionDelegate implements Serializable {

	private Flow flow;

	public ConditionDelegate(Flow flow) {
		this.flow = flow;
	}

	def propertyMissing(String name) { flow.storage[name] }

}

public class ParamDelegate implements Serializable {

	private Flow flow;

	public ParamDelegate(Flow flow) {
		this.flow = flow;
	}

	def propertyMissing(String name) { flow.storage[name] }

}
