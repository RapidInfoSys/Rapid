/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Timer extends Action {

	// instance variables

	private List<Action> _actions;

	// properties

	public List<Action> getActions() { return _actions; }
	public void setActions(List<Action> actions) { _actions = actions; }

	// parameterless constructor for jaxb
	public Timer() { super(); }
	// json constructor for designer
	public Timer(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call super constructor to set xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties (except for sessionVariables)
			if (!"actions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}
		// grab any actions
		JSONArray jsonActions = jsonAction.optJSONArray("actions");
		// if we had some instantiate our collection
		if (jsonActions != null) _actions = Control.getActions(rapidServlet, jsonActions);

	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		String js = "";
		if (_actions != null) {
			if (_actions.size() > 0) {
				// get whether we repeat
				boolean repeat = Boolean.parseBoolean(getProperty("repeat"));
				// open the function depending on whether repeat or not
				if (repeat) {
					js = "setInterval(";
				} else {
					js = "setTimeout(";
				}
				// start the function
				js += " function() {\n";
				// loop the actions
				for (Action action : _actions) {
					js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
				}
				// close the function and add the duration
				js += "}," + getProperty("duration") + ");\n";
			}
		}
		return js;
	}

	@Override
	public List<Action> getChildActions() {
		return _actions;
	}

}
