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
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.actions.Logic.Condition;
import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Validation.LogicMessage;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Validation extends Action {

	// instance variables
	private ArrayList<String> _controls;
	private ArrayList<Action> _passActions, _failActions, _childActions;

	// properties

	public ArrayList<String> getControls() { return _controls; }
	public void setControls(ArrayList<String> controls) { _controls = controls; }

	public ArrayList<Action> getPassActions() { return _passActions; }
	public void setPassActions(ArrayList<Action> passActions) { _passActions = passActions; }

	public ArrayList<Action> getFailActions() { return _failActions; }
	public void setFailActions(ArrayList<Action> failActions) { _failActions = failActions; }

	// constructors

	// jaxb
	public Validation() { super(); }
	// designer
	public Validation(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// set the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for controls
			if (!"controls".equals(key) && !"passActions".equals(key) && !"failActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}
		// grab any controls
		JSONArray jsonControls = jsonAction.optJSONArray("controls");
		// if we had some
		if (jsonControls != null) {
			// instantiate our contols collection
			_controls = new ArrayList<>();
			// loop the json Controls
			for (int i = 0; i < jsonControls.length(); i++) {
				// add into our collection
				_controls.add(jsonControls.getString(i));
			}
		}

		// grab any passActions
		JSONArray jsonPassActions = jsonAction.optJSONArray("passActions");
		// if we had some
		if (jsonPassActions != null) {
			// instantiate our pass actions collection
			_passActions = Control.getActions(rapidServlet, jsonPassActions);
		}

		// grab any failActions
		JSONArray jsonFailActions = jsonAction.optJSONArray("failActions");
		// if we had some
		if (jsonFailActions != null) {
			// instantiate our fail actions collection
			_failActions = Control.getActions(rapidServlet, jsonFailActions);
		}

	}

	// methods

	@Override
	public List<Action> getChildActions() {
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<>();
			// add child success actions
			if (_passActions != null) {
				for (Action action : _passActions) _childActions.add(action);
			}
			// add child error actions
			if (_failActions != null) {
				for (Action action : _failActions) _childActions.add(action);
			}
		}
		return _childActions;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		String js = "";

		// check we have some controls
		if (_controls != null) {
			// the validations is a JavaScript array
			JSONArray jsonValidations = new JSONArray();
			// loop the controls (which are actually controlIds)
			for (String controlId : _controls) {
				// find the control proper
				Control validateControl = page.getControl(controlId);
				// check we have a control
				if (validateControl != null) {
					// get the control validation
					com.rapid.core.Validation controlValidation = validateControl.getValidation();
					// check there is control validation
					if (controlValidation != null) {

						// find the control class
						JSONObject jsonControl = rapidRequest.getRapidServlet().getJsonControl(validateControl.getType());

						// check a type has been specified and we could find it
						if (jsonControl != null && !"".equals(controlValidation.getType())) {
							// this method can't throw so catch here
							try {

								// build the validations object
								JSONObject jsonValidation = new JSONObject();

								// add the common properties
								jsonValidation.put("controlId", validateControl.getId());
								jsonValidation.put("controlType", validateControl.getType());
								jsonValidation.put("validationType", controlValidation.getType());
								jsonValidation.put("validationProperty", jsonControl.opt("validationProperty"));
								if (controlValidation.getPassHidden()) jsonValidation.put("passHidden", true);
								if (controlValidation.getAllowNulls()) jsonValidation.put("allowNulls", true);

								// check the type and add accordingly
								switch (controlValidation.getType()) {
									case "logic" :
										// start creating JavaScript
										String jsLogic = "";
										// get the messages
										List<LogicMessage> logicMessages = controlValidation.getLogicMessages();
										// loop them
										for (LogicMessage logicMessage : logicMessages) {
											// get the logic conditions
											List<Condition> conditions = logicMessage.getConditions();
											// if we got some
											if (conditions != null && conditions.size() > 0) {
												// open the null logic
												jsLogic += "if (";
												// if nulls not allowed
												if (!controlValidation.getAllowNulls()) {
													// list of arguments
													List<String> arguments = new ArrayList<>();
													// loop the conditions
													for (Condition condition : conditions) {
														// check arg1 is a control
														if ("CTL".equals(condition.getValue1().getType())) {
															// get arg1
															String argument = condition.getValue1().getArgument(rapidRequest, application, page);
															// add the argument if we don't have it already
															if (!arguments.contains(argument)) arguments.add(argument);
														}
														// check arg 2 is a control
														if ("CTL".equals(condition.getValue2().getType())) {
															// get arg2
															String argument = condition.getValue2().getArgument(rapidRequest, application, page);
															// add the argument if we don't have it already
															if (!arguments.contains(argument)) arguments.add(argument);
														}
													}
													// loop the arguments
													for (String argument : arguments) {
														// add the null check
														jsLogic += argument + " == null || ";
													}
												}
												// open the condition logic
												jsLogic += "!(";
												// loop them
												for (int i = 0; i < conditions.size(); i++) {
													// add the condition
													jsLogic += conditions.get(i).getJavaScript(rapidRequest, application, page);
													// if there is going to be another condition
													if (i < conditions.size() - 1) {
														// add the separator
														if ("or".equals(logicMessage.getConditionsType())) {
															jsLogic += " || ";
														} else {
															jsLogic += " && ";
														}
													}
												}
												// close the null logic and condition logic
												jsLogic += ")) { " + "return '" + logicMessage.getText().replaceAll("'", "''") + "'; }\n";
											} else {
												// no conditions so just send the message
												jsLogic += "return '" + logicMessage.getText().replaceAll("'", "''") + "';\n";
											}
										}
										// add the JavaScript
										jsonValidation.put("javaScript",jsLogic);
									break;
									case "javascript" :
										// add the user JavaScript - it contains its own message
										if (!"".equals(controlValidation.getJavaScript())) jsonValidation.put("javaScript", controlValidation.getJavaScript());
									break;
									default :
										// add the message
										jsonValidation.put("message", controlValidation.getMessage());
										// add the regex
										if (!"".equals(controlValidation.getRegEx())) jsonValidation.put("regEx", controlValidation.getRegEx());
								}

								// add optional properties
								if (validateControl.getDetails() != null) jsonValidation.put("details", validateControl.getDetailsJavaScript(application, page));

								// add it to the array
								jsonValidations.put(jsonValidation);

							} catch (JSONException ex) {}
						}
					}
				}
			}
			js = "if (Action_validation(ev, " + jsonValidations.toString() + "," + getProperty("showMessages") + ")) {\n";

			// insert pass actions
			if (_passActions != null) {
				for (Action action : _passActions) js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails) + "\n";
			}

			js += "} else {\n";

			// insert fail actions
			if (_failActions != null) {
				for (Action action : _failActions) js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails) + "\n";
			}

			// check whether to stop further actions
			boolean stopActions = Boolean.parseBoolean(getProperty("stopActions"));
			if (stopActions) js += "  return false;\n";

			js += "}\n";
		}

		return js;

	}

}