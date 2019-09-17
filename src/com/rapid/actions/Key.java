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

/*

This action runs child actions if a specific key or keys are detected in the event

*/

public class Key extends Action {

	// instance variables
	private List<Action>  _actions;

	// properties
	public List<Action> getActions() { return _actions; }
	public void setActions(List<Action> actions) { _actions = actions; }

	// parameterless constructor (required for jaxb)
	public Key() { super(); }
	// designer constructor
	public Key(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
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

	// methods

	@Override
	public List<Action> getChildActions() {
		// just return  the actions
		return _actions;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, com.rapid.core.Control control, JSONObject jsonDetails) throws Exception {

		// get the control Id and command
		String js = "";

		// assume no addional indent
		String indent = "";

		// if there is no control, i.e. it's been attached to the page add a listener
		if (control == null) {
			// add global listener
			js += "$(window).keydown( function(ev) {\n";
			// set indent
			indent = "  ";
		}

		// get the key
		String keyCode = getProperty("keyCode");
		// get the extra
		String extra = getProperty("extra");

		// if keyCode is null set to empty string
		if (keyCode == null) keyCode = "";

		// if no key code set to to true
		if (keyCode.length() == 0) {
			// always true
			js += indent + "if (true) {\n";
		} else {
			// open logic with key code
			js += indent + "if (ev.which == " + keyCode;
			// if there was an extra
			if (extra != null) {
				if (extra.length() > 0) {
					// split it
					String[] extraParts = extra.split(",");
					// loop and add to logic
					for (String part : extraParts) {
						// add to logic
						js += " && ev['" + part + "']";
					}
				}
			}
			// close logic
			js += ") {\n";
		}

		// add any actions
		if (_actions != null) {
			for (Action action : _actions) js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
		}

		// add any prevent default
		if (Boolean.parseBoolean(getProperty("preventDefault"))) js += "  ev.preventDefault();\n";

		// close the key check
		js += indent + "}\n";

		// close the global listener if no control
		if (control == null) js += "});\n";

		// return the js
		return js;
	}

}
