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

public class Job {

    String name;
    Map params = [:];
    Closure and;
	Closure then;

    public Job(String name) {
        this.name=name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public Job with(Map map) {
		assert map.containsKey("params");
		assert map["params"] instanceof Map
        this.params.putAll(map["params"]);
        return this;
    }
    
    public Job and(Closure cl) {
        this.and = cl;
        return this;
    }
	
	public void then(Closure cl) {
		this.then = cl;
	}

}