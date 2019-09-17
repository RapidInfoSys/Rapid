/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

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

import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Existing extends Action {

	// a list of redundant actions (in this case just the one existing action we are referring to)
	private List<String> _redundantActions;

	// constructors

	// used by jaxb
	public Existing() {
		super();
	}
	// used by designer
	public Existing(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		super(rapidServlet, jsonAction);
	}

	// overridden methods

	@Override
	public List<String> getRedundantActions() {
		// if the list is still null
		if (_redundantActions == null) {
			// get the action id
			String actionId = getProperty("action");
			// check it
			if (actionId != null) {
				// instantiate if so
				_redundantActions = new ArrayList<>();
				// add this actionId
				_redundantActions.add(actionId);
			}
		}
		// return the list we made on initialisation
		return _redundantActions;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		// get the action id
		String actionId = getProperty("action");
		// check we got something
		if (actionId == null) {
			return "/* no action id */";
		} else {
			// assume we can't find the action - we'll look for it two ways, depending on whether we can find the control - which is much more efficient
			Action existingAction = null;
			// get the control for this existing action
			Control actionControl = page.getActionControl(actionId);
			// if we couldn't find the control in the page
			if (actionControl == null) {
				//  ask the application for the action - which is what we want anyway
				existingAction = application.getAction(rapidRequest.getRapidServlet().getServletContext(), actionId);
				// if this failed too
				if (existingAction == null) return "/* could not find action " + actionId + " in application */";
			}
			// check we got a control, or the action
			if (actionControl == null && existingAction == null) {
				return "/* could not find control for action " + actionId + " */";
			} else {
				// if we didn't get an action from the application, now get the action object by checking the controls events
				if (existingAction == null) existingAction = page.getChildEventsAction(actionControl.getEvents(), actionId);
				// check we got something
				if (existingAction == null) {
					return "/* could not find action " + actionId + " */";
				} else {
					// get its JavaScript
					String existingJavaScript = existingAction.getJavaScriptWithHeader(rapidRequest, application, page, actionControl, jsonDetails);
					// check we got some
					if (existingJavaScript == null) {
						return "/* JavaScript for action " + actionId + " is null */";
					} else {
						// trim it
						existingJavaScript = existingJavaScript.trim();
						// check it contains something
						if (!"".equals(existingJavaScript)) {
							// check if there is redundancy avoidance on this action
							if (existingAction.getAvoidRedundancy()) {
								// return a redundancy avoiding call if so
								return "Action_" + actionId + "(ev);";
							} else {
								// redundancy avoidance function will not be created return raw js
								return existingJavaScript;
							}
						} // existingJavaScript != ""
					} // existingJavaScript check
				} // existingAction check
			} // actionControl check
		}
		// return an empty string
		return "";
	}

}
