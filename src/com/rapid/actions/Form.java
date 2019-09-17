/*

Copyright (C) 2018 - Gareth Edwards / Rapid Information Systems

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

import javax.servlet.ServletContext;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.forms.FormAdapter;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

/*

This action runs JQuery against a specified control. Can be entered with or without the leading "." Such as hide(), or .css("disabled","disabled");

*/

public class Form extends Action {

	// this action has generic inputs
	@XmlType(namespace="http://rapid-is.co.uk/form")
	public static class Input {

		private String _itemId, _field, _inputField;

		public String getItemId() { return _itemId; }
		public void setItemId(String itemId) { _itemId = itemId; }

		public String getField() { return _field; }
		public void setField(String field) { _field = field; }

		public String getInputField() { return _inputField; }
		public void setInputField(String inputField) { _inputField = inputField; }

		public Input() {};
		public Input(String itemId, String field, String inputField) {
			_itemId = itemId;
			_field = field;
			_inputField = inputField;
		}

	}

	// instance variables
	private List<Input> _inputs;
	private List<Action> _successActions, _errorActions, _childActions;

	// properties
	public List<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(List<Action> successActions) { _successActions = successActions; }

	public List<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(List<Action> errorActions) { _errorActions = errorActions; }

	// parameterless constructor (required for jaxb)
	Form() { super(); }
	// designer constructor
	public Form(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {

		// call the parameterless constructor which sets the xml version
		this();

		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, successActions and errorActions
			if (!"inputs".equals(key) && !"outputs".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// grab any inputs
		JSONArray jsonInputs = jsonAction.optJSONArray("inputs");
		// if we got some
		if (jsonInputs != null) {
			// instantiate our array
			_inputs = new ArrayList<>();
			// loop them
			for (int i = 0; i < jsonInputs.length(); i++) {
				// get the input
				JSONObject jsonInput = jsonInputs.getJSONObject(i);
				// add it
				_inputs.add(new Input(jsonInput.optString("itemId"), jsonInput.optString("field"), jsonInput.optString("inputField")));
			}

		}

		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some instantiate our collection
		if (jsonSuccessActions != null) _successActions = Control.getActions(rapidServlet, jsonSuccessActions);

		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some instantiate our collection
		if (jsonErrorActions != null) _errorActions = Control.getActions(rapidServlet, jsonErrorActions);

	}

	// methods

	// gethe input for a form input (used by save and resume)
	private String getInputJS(ServletContext servletContext, Application application, Page page, String itemId, String field) {
		return "data.inputs['" + field + "'] = " + Control.getDataJavaScript(servletContext, application, page, itemId, null) + ";\n";
	}

	// overrides

	@Override
	public List<Action> getChildActions() {
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<>();
			// add child success actions
			if (_successActions != null) {
				for (Action action : _successActions) _childActions.add(action);
			}
			// add child error actions
			if (_errorActions != null) {
				for (Action action : _errorActions) _childActions.add(action);
			}
		}
		return _childActions;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, com.rapid.core.Control control, JSONObject jsonDetails) throws Exception {

		// get the action type
		String actionType = getProperty("actionType");
		// prepare the js
		String js = "";
		// get the form adpater
		FormAdapter formAdapter = application.getFormAdapter();
		// check we got one
		if (formAdapter == null) {
			js = "// no form adapter\n";
		} else {
			// check the action type
			if ("next".equals(actionType)) {

				// next submits the form
				js = "$('#" + page.getId() + "_form').submit();\n";

			} else if ("prev".equals(actionType)) {

				// go back
				js = "window.history.back();\n";

			} else if ("maxpage".equals(actionType)) {

				// go to max page
				js = "window.location.href = '~?a=" + application.getId() + "&action=maxpage';\n";

			} else if ("summary".equals(actionType)) {

				// go to summary
				js = "window.location.href = '~?a=" + application.getId() + "&action=summary';\n";

			} else if ("savepage".equals(actionType)) {

				// add a save input and submit the form
				js = "$('#" + page.getId() + "_form').append('<input name=\"save\" value=\"save\" type=\"hidden\"/>').submit();\n";

			} else if ("save".equals(actionType) || "resume".equals(actionType)) {
				// save the form with ajax

				// start the data object with the type
				js += "var data = {action: '" + actionType + "', inputs: {}};\n";

				// get the servletcontext
				ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();

				// get any formId input
				String formIdControlId = getProperty("formId");
				// if we got one add as input
				if (formIdControlId != null) js += getInputJS(servletContext, application, page, formIdControlId, "formId");

				// get any email input
				String emailControlId = getProperty("email");
				// if we got one add as input
				if (emailControlId != null) js += getInputJS(servletContext, application, page, emailControlId, "email");

				// get any password input
				String passwordControlId = getProperty("password");
				// if we got one add as input
				if (passwordControlId != null) js += getInputJS(servletContext, application, page, passwordControlId, "password");

				// control can be null when the action is called from the page load
				String controlParam = "";
				if (control != null) controlParam = "&c=" + control.getId();

				// open the ajax call
				js += "$.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', contentType: 'application/json', dataType: 'json',\n";
				js += "  data: JSON.stringify(data),\n";
				js += "  error: function(server, status, message) {\n";

				// this avoids doing the errors if the page is unloading or the back button was pressed
				js += "    if (server.readyState > 0) {\n";

				// retain if error actions
				boolean errorActions = false;

				// prepare a default error hander we'll show if no error actions, or pass to child actions for them to use
				String defaultErrorHandler = "alert('Error with form action : ' + server.responseText||message);";

				// add any error actions
				if (_errorActions != null) {
					// instantiate the jsonDetails if required
					if (jsonDetails == null) jsonDetails = new JSONObject();
					// count the actions
					int i = 0;
					// loop the actions
					for (Action action : _errorActions) {
						// retain that we have custom error actions
						errorActions = true;
						// if this is the last error action add in the default error handler
						if (i == _errorActions.size() - 1) jsonDetails.put("defaultErrorHandler", defaultErrorHandler);
						// add the js
						js += "         " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n         ") + "\n";
						// if this is the last error action and the default error handler is still present, remove it so it isn't sent down the success path
						if (i == _errorActions.size() - 1 && jsonDetails.optString("defaultErrorHandler", null) != null) jsonDetails.remove("defaultErrorHandler");
						// increase the count
						i++;
					}
				}
				// add default error handler if none in collection
				if (!errorActions) js += "        " + defaultErrorHandler + "\n";

				// close unloading check
				js += "    }\n";

				// close error actions
				js += "  },\n";

				// open success function
				js += "  success: function(data) {\n";

				// open if data check
				js += "    if (data) {\n";

				// add any sucess actions
				if (_successActions != null) {
					for (Action action : _successActions) {
						js += "       " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n       ") + "\n";
					}
				}

				// close if data check
				js += "    }\n";
				// close success function
				js += "  }\n";

				// close ajax call
				js += "});";

			} else {

				// get the dataDestination
				String destinationId = getProperty("dataDestination");
				// check we got one
				if (destinationId == null) {
					js = "// no destination id\n" ;
				} else {
					// split the id on . for properties
					String[] idParts = destinationId.split("\\.");
					// look for the control in the page
					Control destinationControl = page.getControl(idParts[0]);
					// check we got a control
					if (destinationControl == null) {
						js = "// destination control " + destinationId + " could not be found\n" ;
					} else  {
						// the value we will get
						String value = null;
						// check the action type
						if ("id".equals(actionType)) {
							value = "_formId";
						} else if ("val".equals(actionType)) {
							// get the control value from the _formValues object which we add in the dynamic section
							value = "_formValues['" + getProperty("dataSource") + "']";
						} else if ("sub".equals(actionType)) {
							// get the form submit message
							value = "_formValues['sub']";
						} else if ("err".equals(actionType)) {
							// get the form error message
							value = "_formValues['err']";
						} else if ("res".equals(actionType)) {
							// create the resume url
							value = "'~?a=" + application.getId() + "&v=" + application.getVersion() + "&action=resume&f=' + _formId + '&pwd=' + _formValues['res']";
						} else if ("pdf".equals(actionType)) {
							// create the pdf url
							value = "'~?a=" + application.getId() + "&v=" + application.getVersion() + "&action=pdf&f='+ _formId";
						}

						// use the set data if we got something
						if (value != null) js = Control.setDataJavaScript(rapidRequest.getRapidServlet().getServletContext(), application, page, destinationId, null).replace("data,", value + ",").replace("true)", "false)") + ";\n";

					} // destination check
				} // destination id check
			} // action type
		} // form adapter type
		// return the js
		return js;
	}

	@Override
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonData) throws Exception {

		// create the response object
		JSONObject jsonResponse = new JSONObject();

		// get the action
		String action = jsonData.optString("action");

		// check we got an action
		if (action == null) {

			throw new Exception("No action provided");

		} else if ("save".equals(action)) {

			// assume no email or password
			String email = null;
			String password = null;

			// get the inputs
			JSONObject jsonInputs = jsonData.optJSONObject("inputs");

			// if we got some look for email and password
			if (jsonInputs != null) {
				email = jsonInputs.optString("email", null);
				password = jsonInputs.optString("password", null);
			}

			// have the form adapter do the save
			rapidRequest.getApplication().getFormAdapter().saveForm(rapidRequest, email, password);

		} else if ("resume".equals(action)) {

			// assume no id or password
			String formId = null;
			String password = null;

			// get the inputs
			JSONObject jsonInputs = jsonData.optJSONObject("inputs");

			// if we got some look for formId and password
			if (jsonInputs != null) {
				formId = jsonInputs.optString("formId", null);
				password = jsonInputs.optString("password", null);
			}

			// have the form adapter do the resume - it will update the rapid request
			boolean success = rapidRequest.getApplication().getFormAdapter().resumeForm(rapidRequest, formId, password);

			// if the id / password matched
			if (success) {
				// add success to the response
				jsonResponse.put("success", true);
			} else {
				// throw an exception so we use the error handler
				throw new Exception("Form id and password not found");
			}

		}

		return jsonResponse;

	}

}
