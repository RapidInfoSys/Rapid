package com.rapid.core;

import java.util.Set;
import java.util.TreeMap;

public class Workflows extends TreeMap<String, Workflow> {

	private static final long serialVersionUID = 40L;

	// return all ids
	public Set<String> getIds() {
		return this.keySet();
	}

}
