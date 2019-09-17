/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.forms.FormAdapter;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Logic extends Action {

	// static classes
	public static class Value {

		// private instance variables
		private String _type, _id, _field;

		// public properties
		public String getType() { return _type; }
		public void setType(String type) { _type = type; }

		public String getId() { return _id; }
		public void setId(String id) { _id = id; }

		public String getField() { return _field; }
		public void setField(String field) { _field = field; }

		public Value() {}
		public Value(JSONObject jsonValue) {
			_type = jsonValue.optString("type");
			_id = jsonValue.optString("id");
			_field = jsonValue.optString("field");
		}

		public String getArgument(RapidRequest rapidRequest, Application application, Page page) {
			// assume null
			String arg = "null";
			// if id is set
			if (_id != null) {
				// assume no value
				String value = null;
				// if the control is not in the page
				if (page.getControl(_id) == null) {
					// get any form adapter
					FormAdapter formAdapter = application.getFormAdapter();
					// if there was one
					if (formAdapter != null) {
						// look for a value
						try { value = formAdapter.getFormControlValue(rapidRequest, formAdapter.getFormId(rapidRequest), _id); } catch (Exception e) {}
					}
				}
				// use the system-wide method if no form value
				if (value == null) {
					arg = Control.getDataJavaScript(rapidRequest.getRapidServlet().getServletContext(), application, page, _id, _field);
				} else {
					arg = "'" + value.replace("'", "\'") + "'";
				}

			}
			// return it
			return arg;

		}

		@Override
		public String toString() {
			return _id + (_field == null ? "" : (_field.length() == 0 ? "" : "/" + _field));
		}

	}

	public static class Condition {

		// private instance variables
		private Value _value1, _value2;
		private String _operation;

		// properties

		public Value getValue1() { return _value1; }
		public void setValue1(Value value) { _value1 = value; }

		public String getOperation() { return _operation; }
		public void setOperation(String operation) { _operation = operation; }

		public Value getValue2() { return _value2; }
		public void setValue2(Value value) { _value2 = value; }

		// constructors

		public Condition() {}
		public Condition(JSONObject jsonCondition) {
			JSONObject jsonValue1 = jsonCondition.optJSONObject("value1");
			if (jsonValue1 != null) _value1 = new Value(jsonValue1);
			_operation = jsonCondition.optString("operation");
			JSONObject jsonValue2 = jsonCondition.optJSONObject("value2");
			if (jsonValue2 != null) _value2 = new Value(jsonValue2);
		}

		// methods
		public String getJavaScript(RapidRequest rapidRequest, Application application, Page page) {
			String js = "false";
			// check we have everything we need to make a condition
			if (_value1 != null && _operation != null && _value2 != null) {
				// get the left side
				String leftSide = _value1.getArgument(rapidRequest, application, page);
				// get the right side
				String rightSide = _value2.getArgument(rapidRequest, application, page);
				// get the leftId
				String leftId = _value1.getId();
				// get the rightId
				String rightId = _value2.getId();
				// assume no brackets required
				boolean brackets = false;
				// empty is a special case
				if ("System.empty".equals(leftId) || "System.empty".equals(rightId)) {

					// going to need extra brackets
					brackets = true;

					// check which side the empty is on
					if ("System.empty".equals(leftId)) {
						// check for !=
						if ("!=".equals(_operation)) {
							// if != on the left set to undefined or no value with an and
							leftSide = "undefined && " + rightSide;
						} else {
							// if on the left set to undefined or no value
							leftSide = "undefined || !" + rightSide;
						}
					} else {
						// check for !=
						if ("!=".equals(_operation)) {
							// if != on the right set  to undefined or no value with an and
							rightSide = "undefined && " + leftSide;
						} else {
							// if on the right set  to undefined or no value
							rightSide = "undefined || !" + leftSide;
						}
					}

					// construct the condition with an additional = (so ===, !===, etc)
					js = leftSide + " " + _operation + "= " + rightSide;

				} else {

					// construct the condition simply
					js = leftSide + " " + _operation + " " + rightSide;

					// if left side is System.true or System.false or System.null
					if ("System.true".equals(leftId) || "System.false".equals(leftId) || "System.null".equals(leftId)) {
						// check for !=
						if ("!=".equals(_operation)) {
							// add an and clause with the literal
							js += " && " + "'" + leftId.replace("System.", "") + "' "  + _operation + " " + rightSide;
						} else {
							// add an or clause with the literal
							js += " || " + "'" + leftId.replace("System.", "") + "' "  + _operation + " " + rightSide;
						}
						// going to need extra brackets
						brackets = true;
					}


					// if right side is System.true or System.false or System.null
					if ("System.true".equals(rightId) || "System.false".equals(rightId) || "System.null".equals(rightId)) {
						// check for !=
						if ("!=".equals(_operation)) {
							// add an and clause with the literal
							js += " && " + leftSide + " " + _operation + " '" + rightId.replace("System.", "") + "'";
						} else {
							// add an or clause with the literal
							js += " || " + leftSide + " " + _operation + " '" + rightId.replace("System.", "") + "'";
						}
						// going to need extra brackets
						brackets = true;
					}

				}
				// if we needed brackets
				if (brackets) js = "(" + js + ")";
			}
			return js;
		}

		@Override
		public String toString() {
			return _value1 + " " + _operation + " " + _value2;
		}

	}

	// static variables
	private static Logger _logger = LogManager.getLogger(Logic.class);

	// instance variables
	private List<Condition> _conditions;
	private String _conditionsType;
	private List<Action> _trueActions, _falseActions, _childActions;

	// properties

	public List<Condition> getConditions() { return _conditions; }
	public void setConditions(List<Condition> conditions) { _conditions = conditions; }

	public String getConditionsType() { return _conditionsType; }
	public void setConditionsType(String conditionsType) { _conditionsType = conditionsType; }

	public List<Action> getTrueActions() { return _trueActions; }
	public void setTrueActions(List<Action> trueActions) { _trueActions = trueActions; }

	public List<Action> getFalseActions() { return _falseActions; }
	public void setFalseActions(List<Action> falseActions) { _falseActions = falseActions; }

	// constructors

	// used by jaxb
	public Logic() { super(); }

	// used by designer
	public Logic(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for the ones we want directly accessible
			if (!"conditions".equals(key) && !"conditionsType".equals(key) && !"trueActions".equals(key) && !"falseActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// initialise list
		_conditions = new ArrayList<>();
		// grab conditions from json
		JSONArray jsonConditions = jsonAction.optJSONArray("conditions");

		// if we got some
		if (jsonConditions != null) {
			// loop them
			for (int i = 0; i < jsonConditions.length(); i++) {
				// add to our list
				_conditions.add(new Condition(jsonConditions.getJSONObject(i)));
			}
		}

		// get conditions type
		_conditionsType = jsonAction.getString("conditionsType");

		// grab any successActions
		JSONArray jsonTrueActions = jsonAction.optJSONArray("trueActions");
		// if we had some
		if (jsonTrueActions != null) {
			_trueActions = Control.getActions(rapidServlet, jsonTrueActions);
		}

		// grab any errorActions
		JSONArray jsonFalseActions = jsonAction.optJSONArray("falseActions");
		// if we had some
		if (jsonFalseActions != null) {
			// instantiate our contols collection
			_falseActions = Control.getActions(rapidServlet, jsonFalseActions);
		}

	}

	// overrides

	@Override
	public List<Action> getChildActions() {
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<>();
			// add child success actions
			if (_trueActions != null) {
				for (Action action : _trueActions) _childActions.add(action);
			}
			// add child error actions
			if (_falseActions != null) {
				for (Action action : _falseActions) _childActions.add(action);
			}
		}
		return _childActions;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		String js = "";

		// assume we couldn't make a condition
		String conditionsJavaScript = "false";

		// check conditions is set
		if (_conditions != null) {
			// check we have some
			if (_conditions.size() > 0) {
				// reset conditionsJavaScript
				conditionsJavaScript = "";
				// loop them
				for (int i = 0; i < _conditions.size(); i++) {
					// add the condition
					conditionsJavaScript += _conditions.get(i).getJavaScript(rapidRequest, application, page);
					// if there is going to be another condition
					if (i < _conditions.size() - 1) {
						// add the separator
						if ("or".equals(_conditionsType)) {
							conditionsJavaScript += " || ";
						} else {
							conditionsJavaScript += " && ";
						}
					}
				}
			}
		}

		// create the if statement
		js += "if (" + conditionsJavaScript + ") {\n";

		// add any try actions
		if (_trueActions != null) {
			for (Action action : _trueActions) js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
		}

		// close the if
		js += "}";

		// assume no false actions
		boolean gotFalseActions = false;
		// if there is a collection
		if (_falseActions != null) {
			// if there are some in the collection we have false actions
			if (_falseActions.size() > 0) gotFalseActions = true;
		}

		// check for false actions
		if (gotFalseActions) {
			// add any false actions as an else
			js += " else {\n";
			for (Action action : _falseActions) js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
			js += "}";
		} else {
			// if we got some details
			if (jsonDetails != null) {
				// check the details for a defaultErrorHandler
				String defaultErrorHandler = jsonDetails.optString("defaultErrorHandler", null);
				// if we got one
				if (defaultErrorHandler != null) {
					// print it
					js += " else {\n  " + defaultErrorHandler + "\n}";
					// remove it from the jsonObject to stop it re-appearing elsewhere
					jsonDetails.remove("defaultErrorHandler");
				}
			}
		}

		// final line break
		js+= "\n";

		// return what we built
		return js;
	}

}
