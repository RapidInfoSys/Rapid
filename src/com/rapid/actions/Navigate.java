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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Navigate extends Action {

	// details of the inputs
	public static class SessionVariable {

		private String _name, _itemId, _field;

		public String getName() { return _name; }
		public void setName(String name) { _name = name; }

		public String getItemId() { return _itemId; }
		public void setItemId(String itemId) { _itemId = itemId; }

		public String getField() { return _field; }
		public void setField(String field) { _field = field; }

		public SessionVariable() {};
		public SessionVariable(String name, String itemId, String field) {
			_name = name;
			_itemId = itemId;
			_field = field;
		}

	}

	// instance variables

	private ArrayList<SessionVariable> _sessionVariables;

	// properties

	public ArrayList<SessionVariable> getSessionVariables() { return _sessionVariables; }
	public void setSessionVariables(ArrayList<SessionVariable> sessionVariables) { _sessionVariables = sessionVariables; }

	// parameterless constructor for jaxb
	public Navigate() { super(); }
	// json constructor for designer
	public Navigate(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call super constructor to set xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties (except for sessionVariables)
			if (!"sessionVariables".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}
		// get the inputs collections
		JSONArray jsonInputs = jsonAction.optJSONArray("sessionVariables");
		// check it
		if (jsonInputs == null) {
			// empty down the collection
			_sessionVariables = null;
		} else {
			// initialise the collection
			_sessionVariables = new ArrayList<>();
			// loop it
			for (int i = 0; i < jsonInputs.length(); i++) {
				// get this input
				JSONObject jsonInput = jsonInputs.getJSONObject(i);
				// add it to the collection
				_sessionVariables.add(new SessionVariable(
					jsonInput.getString("name"),
					jsonInput.getString("itemId"),
					jsonInput.optString("field")
				));
			}
		}
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) {

		// the JavaScript we are making
		String js = "";

		// check type, P or U
		String navigationType = getProperty("navigationType");
		String popup = getProperty("popup");

		if ("U".equals(navigationType)) {

			// get the url
			String url = getProperty("url");
			// set the js, including optional popup
			js = "Action_navigate('" + url + "', false, null," +  popup + ");\n";

		} else {

			// this code only for type == P
			String pageId = getProperty("page");
			if (pageId == null) {
				return "// Page must be specified";
			} else {
				// string into which we're about to build the session variables
				String sessionVariables = "";
				// check we have some
				if (_sessionVariables != null) {
					// loop
					for (SessionVariable sessionVariable : _sessionVariables) {
						// get the item id from which we are to get the value for this session/page variable
						String itemId = sessionVariable.getItemId();
						// null check
						if (itemId != null) {
							// length check
							if (itemId.length() > 0) {
								// get the data getter command
								String getter = Control.getDataJavaScript(rapidRequest.getRapidServlet().getServletContext(), application, page, itemId, sessionVariable.getField());
								// if we didn't get anything update to empty string
								if (getter == null) getter = "''";
								if (getter.length() == 0) getter = "''";
								// build the concatenating string
								sessionVariables += "&" + sessionVariable.getName() + "=' + encodeURIComponent(" +  getter + ") + '";
							} // length check
						} // null check
					} // loop
				}

				// get the application id
				String appId = application.getId();
				// assume no version
				String version = "";
				// if this is not the latest live version, add version to url
				if (application.getStatus() != Application.STATUS_LIVE || !application.getVersion().equals(rapidRequest.getRapidServlet().getApplications().get(appId).getVersion())) version = "&v=" + application.getVersion();

				// build the action string (also used in mobile action for online check type)
				js = "Action_navigate('~?a=" + appId + version + "&p=" + pageId;

				// check if this is a dialogue
				boolean dialogue = Boolean.parseBoolean(getProperty("dialogue"));
				// if so add the action parameter to the url
				if (dialogue) js += "&action=dialogue";
				// now add the other parameters
				js += sessionVariables + "'," + dialogue + ",'" + pageId + "'," + popup + ");\n";
				// replace any unnecessary characters
				js = js.replace(" + ''", "");

			}

		}

		// return false if we're stopping further actions
		if (Boolean.parseBoolean(getProperty("stopActions"))) js += "return false;\n";
		// stop event bubbling (both navigation types need this)
		js += "ev.stopPropagation();\n";
		// stop the form being submitted
		js += "ev.preventDefault();\n";
		// return it into the page!
		return js;

	}

}
