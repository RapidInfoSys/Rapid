/*

Copyright (C) 2016 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version. The terms require you
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.actions;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Group extends Action {

	// instance variables
	private List<Action> _actions;
	private List<String> _redundantActions;

	// properties

	public List<Action> getActions() { return _actions; }
	public void setActions(List<Action> actions) { _actions = actions; }

	// constructors

	// used by jaxb
	public Group() { super(); }

	// used by designer
	public Group(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for the ones we want directly accessible
			if (!"actions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// grab any actions
		JSONArray jsonActions = jsonAction.optJSONArray("actions");
		// if we had some
		if (jsonActions != null) {
			_actions = Control.getActions(rapidServlet, jsonActions);
		}

	}

	// overrides

	@Override
	public List<Action> getChildActions() {
		// child actions are all actions
		return _actions;
	}

	@Override
	public List<String> getRedundantActions() {
		// if the list is still null
		if (_redundantActions == null) {
			// instantiate if so
			_redundantActions = new ArrayList<String>();
			// add our actionId which means the group will get it's own method
			_redundantActions.add(getId());
		}
		// return the list we made on initialisation
		return _redundantActions;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		String js = "";

		// add any actions
		if (_actions != null) {
			for (Action action : _actions) js += action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim() + "\n";
		}

		// return what we built
		return js;
	}

}
