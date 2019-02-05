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

import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

/*

This action runs JQuery against a specified control. Can be entered with or without the leading "." Such as hide(), or .css("disabled","disabled");

*/

public class Control extends Action {

	// parameterless constructor (required for jaxb)
	public Control() { super(); }
	// designer constructor
	public Control(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		super(rapidServlet, jsonAction);
	}

	// methods

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, com.rapid.core.Control control, JSONObject jsonDetails) {
		// get the control Id and command
		String controlId = getProperty("control");
		// get the action type
		String actionType = getProperty("actionType");
		// prepare the js
		String js = "";
		// if we have a control id
		if (controlId != null && !"".equals(controlId)) {
			// update the js to use the control
			js = "$(\"#" + getProperty("control") + "\").";
			// check the type
			if ("custom".equals(actionType) || actionType == null) {
				// get the command
				String command = getProperty("command");
				// check command
				if (command != null) {
					// trim
					command = command.trim();
					// check length and whether only comments
					if (command.length() == 0 || ((command.startsWith("//") || (command.startsWith("/*") && command.endsWith("*/"))) && command.indexOf("\n") == -1)) {
						// set to null, if empty or only comments
						command = null;
					} else {
						// command can be cleaned up - remove starting dot (we've already got it above)
						if (command.startsWith(".")) command = command.substring(1);
						// add brackets if there aren't any at the end
						if (!command.endsWith(")") && !command.endsWith(");")) command += "();";
						// add a semi colon if there isn't one on the end
						if (!command.endsWith(";")) command += ";";
					}
				}
				// check for null / empty
				if (command == null) {
					// show that there's no command
					js = "/* no command for custom control action " + getId() + " */";
				} else {
					// add the command
					js += command;
				}
			} else if ("focus".equals(actionType)) {
				js += actionType + "('rapid');";
			} else if ("slideUp".equals(actionType) || "slideDown".equals(actionType) || "slideToggle".equals(actionType)) {
				js += actionType + "(" + getProperty("duration") + ");";
			} else if ("fadeOut".equals(actionType) || "fadeIn".equals(actionType) || "fadeToggle".equals(actionType)) {
				js += actionType + "(" + getProperty("duration") + ");";
			} else if ("enable".equals(actionType)) {
				js += "enable();";
			} else if ("disable".equals(actionType)) {
				js += "disable();";
			} else if ("addClass".equals(actionType)) {
				js += "addClass('" + getProperty("styleClass") + "');";
			} else if ("removeClass".equals(actionType)) {
				js += "removeClass('" + getProperty("styleClass") + "');";
			} else if ("toggleClass".equals(actionType)) {
				js += "toggleClass('" + getProperty("styleClass") + "');";
			} else if ("removeChildClasses".equals(actionType)) {
				String styleClass = getProperty("styleClass");
				js += "find('." + styleClass + "').removeClass('" + styleClass + "');";
			} else if ("removeValidation".equals(actionType)) {
				js = "hideControlValidation('" + controlId + "');";
			} else if ("showError".equals(actionType)) {
				js += "showError(server, status, message);";
			} else if ("scrollTo".equals(actionType)) {
				// check if page (or control js not populated)
				if (page.getId().equals(controlId) || js.length() == 0) {
					// scroll to top of page
					js = "$('html, body').scrollTop(0);";
				} else {
					// scroll to control y position
					js = "$('html, body').scrollTop(" + js + "offset().top);";
				}
			} else if ("hideDialogue".equals(actionType)) {
				js += "hideDialogue(false,'" + controlId + "');";
			} else {
				// just call the action type (hide/show/toggle)
				js += actionType + "();";
			}
			// add a line break;
			js += "\n";
			// if the stopPropagation is checked
			if (Boolean.parseBoolean(getProperty("stopPropagation"))) {
				js += "ev.stopImmediatePropagation();";
			}
		} else {
			js = "/* no control specified for control action " + getId() + " */";
		}
		// return the js
		return js;
	}

}
