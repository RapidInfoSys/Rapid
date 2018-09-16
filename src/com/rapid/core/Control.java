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

package com.rapid.core;

/*

 The control object is a discreetly functioning html ui "widget", from an input box, or span, to a table, tabs, or calendar

 Controls are described in .control.xml files in the /controls folder. This description data is used to create JavaScript class objects
 with which the designer instantiates specific JavaScript control objects in the control tree. When the page is saved the control tree is
 sent in JSON and a series of these java objects are created in the page object, so the whole thing can be serialised into .xml and saved
 to disk

 */
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import com.rapid.core.Application.Value;
import com.rapid.core.Application.ValueList;
import com.rapid.server.RapidHttpServlet;
import com.rapid.utils.Numbers;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Control {

	// the version of this class's xml structure when marshelled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)
	public static final int XML_VERSION = 1;

	// we can this version to be written into the xml when marshelled so we can upgrade any xml before marshelling
	private int _xmlVersion;

	// these are instance variables that all the different controls provide
	protected HashMap<String,String> _properties;
	protected Validation _validation;
	protected ArrayList<Event> _events;
	protected ArrayList<Style> _styles;
	protected ArrayList<Control> _childControls;

	// the xml version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }

	// all properties are stored here (allowing us to define them just in the .control.xml files)
	public HashMap<String,String> getProperties() { return _properties; }
	public void setProperties(HashMap<String,String> properties) { _properties = properties; }

	// every control can have events (and actions)
	public ArrayList<Event> getEvents() { return _events; }
	public void setEvents(ArrayList<Event> events) { _events = events; }

	// every control can have validation (not all do)
	public Validation getValidation() { return _validation; }
	public void setValidation(Validation validation) { _validation = validation; }

	// every control can have styles
	public ArrayList<Style> getStyles() { return _styles; }
	public void setStyles(ArrayList<Style> styles) { _styles = styles;	}

	// every control can have child components
	public ArrayList<Control> getChildControls() { return _childControls; }
	public void setChildControls(ArrayList<Control> childControls) { _childControls = childControls; }

	// these are some helper methods for common properties
	public void addProperty(String key, String value) {
		if (_properties == null) _properties = new HashMap<String,String>();
		_properties.put(key, value);
	}
	// returns the value of a specific, named property
	public String getProperty(String key) { return _properties.get(key); }
	// the type of this object
	public String getType() { return getProperty("type"); }
	// the id of this object
	public String getId() { return getProperty("id"); }
	// the name of this object
	public String getName() { return getProperty("name"); }
	// the form summary label of this object
	public String getLabel() {
		String label = getProperty("label");
		if (label == null) {
			return null;
		} else if (label.trim().length() == 0) {
			return null;
		} else {
			return label;
		}
	}
	// the details used by the getData and setData method to map the data to the control
	public String getDetails() { return getProperty("details"); }
	// whether this control can be used from other pages
	public boolean getCanBeUsedFromOtherPages() { return Boolean.parseBoolean(getProperty("canBeUsedFromOtherPages")); }
	// whether this control can be used for page visibilty rules
	public boolean getCanBeUsedForFormPageVisibilty() { return Boolean.parseBoolean(getProperty("canBeUsedForFormPageVisibilty")); }
	// whether there is javascript that must be run to initialise the control when the page loads
	public boolean hasInitJavaScript() {
		String js = getProperty("initJavaScript");
		if (js == null) return false;
		if (js.trim().replace("false", "").length() == 0) return false;
		return true;
	}

	// helper method for child components
	public void addChildControl(Control childControl) {
		if (_childControls == null) _childControls = new ArrayList<Control>();
		_childControls.add(childControl);
	}

	// helper methods for eventActions
	public Event getEvent(String eventType) {
		if (_events != null) {
			for (Event event : _events) {
				if (eventType.equals(event.getType())) return event;
			}
		}
		return null;
	}

	private Action getActionRecursive(String actionId, List<Action> actions) {
		Action returnAction = null;
		for (Action action : actions) {
			// return the action if it matches
			if (actionId.equals(action.getId())) return action;
			// if the action has child actions
			if (action.getChildActions() != null) {
				// check them too
				returnAction = getActionRecursive(actionId, action.getChildActions());
				// bail here if we got one
				if (returnAction != null) break;
			}
		}
		return returnAction;
	}

	public Action getAction(String actionId) {
		Action action = null;
		if (_events != null) {
			for (Event event : _events) {
				if (event.getActions() != null) {
					action = getActionRecursive(actionId, event.getActions());
					if (action != null) break;
				}
			}
		}
		return action;
	}

	// helper method for styles
	public void addStyle(Style style) {
		if (_styles == null) _styles = new ArrayList<Style>();
		_styles.add(style);
	}

	// helper method for looking up code values
	// (for now dropdowns and radiobuttons are hardcoded, at some point we can add a lookup interface and lookup class specification in the control.xml, or just properties for the code check, collection, code and label)
	public String getCodeText(Application application, String code) {
		// get control type
		String type = getType();
		// if we got one
		if (code != null && type != null) {
			// change type to all lower case
			type = type.toLowerCase();
			// try to look up the user-friendly value and silently fail if any problems
			try {
				// if this is a drop down, or derived from one
				if (type.contains("dropdown")) {
					if (Boolean.parseBoolean(getProperty("codes"))) {
						// look for a value list
						String valueListName = getProperty("valueList");
						// check we got one
						if (valueListName == null) {
							JSONArray jsonCodes = new JSONArray(getProperty("options"));
							for (int i = 0; i < jsonCodes.length(); i++) {
								JSONObject jsonCode = jsonCodes.getJSONObject(i);
								if (code.equals(jsonCode.optString("value"))) return jsonCode.optString("text");
							}
						} else {
							List<ValueList> valueLists = application.getValueLists();
							if (valueLists != null) {
								for (ValueList valueList : valueLists) {
									if (valueListName.equals(valueList.getName())) {
										if (valueList.getUsesCodes()) {
											for (Value value : valueList.getValues()) {
												if (code.equals(value.getValue())) {
													return value.getText();
												}
											}
										}
									}
								}
							}
						}
					}
				} else if (type.contains("radiobuttons")) {
					if (Boolean.parseBoolean(getProperty("codes"))) {
						JSONArray jsonCodes = new JSONArray(getProperty("buttons"));
						for (int i = 0; i < jsonCodes.length(); i++) {
							JSONObject jsonCode = jsonCodes.getJSONObject(i);
							if (code.equals(jsonCode.optString("value"))) return jsonCode.optString("label");
						}
					}
				}
			} catch (JSONException ex) {}
		}
		return code;
	}

	// a parameterless constructor is required so they can go in the JAXB context and be unmarshalled
	public Control() {
		// set the xml version
		_xmlVersion = XML_VERSION;
	};

	// this constructor is used when saving from the designer
	public Control(JSONObject jsonControl) throws JSONException {
		// set the xml version
		_xmlVersion = XML_VERSION;
		// save all key/values from the json into the properties, except for class variables such as childControls and eventActions
		for (String key : JSONObject.getNames(jsonControl)) {
			// don't save complex properties such as validation, childControls, events, and styles into simple properties (they are turned into objects in the Designer.java savePage method)
			if (!key.equals("validation") && !key.equals("events") && !key.equals("styles") && !key.equals("childControls")) addProperty(key, jsonControl.get(key).toString());
		}
	}

	// static methods

	public static Control getControl(ServletContext servletContext, Application application, Page page, String id) {
		Control control = null;
		// split by escaped .
		String idParts[] = id.split("\\.");
		// if there is more than 1 part we are dealing with set properties, for now just update the id
		if (idParts.length > 1) id = idParts[0];

		// first try and look for the control in the page
		control = page.getControl(id);
		// if still no control look for the control in the application
		if (control == null) control = application.getControl(servletContext, id);
		// return
		return control;
	}

	public static Control searchChildControl(ArrayList<Control> controls, String controlId) {
		Control returnControl = null;
		if (controls != null) {
			for (Control childControl : controls) {
				if (childControl.getId().equals(controlId)) {
					returnControl = childControl;
					break;
				}
				returnControl = searchChildControl(childControl.getChildControls(), controlId);
			}
		}
		return returnControl;
	}

	// this is here as a static to match getEvents, and getActions, even though there isn't currently a need to reuse it between pages/controls
	public static Validation getValidation(RapidHttpServlet rapidServlet, JSONObject jsonValidation) throws JSONException {

		// check we where given something
		if (jsonValidation != null) {

			// make a validation object from the json
			Validation validation = new Validation(
				jsonValidation.optString("type"),
				jsonValidation.optBoolean("passHidden"),
				jsonValidation.optBoolean("allowNulls"),
				jsonValidation.optString("regEx"),
				jsonValidation.optString("message"),
				jsonValidation.optString("logicMessages"),
				jsonValidation.optString("javaScript")
			);

			// return the validation object
			return validation;

		}

		// return nothing
		return null;
	}

	// this is here as a static so it used when creating the page object, or a control object
	public static ArrayList<Event> getEvents(RapidHttpServlet rapidServlet, JSONArray jsonEvents) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, JSONException {

		// the array of events we're about to return
		ArrayList<Event> events = new ArrayList<Event>();

		// if we have events
		if (jsonEvents != null) {

			// loop them
			for (int i = 0; i < jsonEvents.length(); i++) {

				// get the jsonEvent
				JSONObject jsonEvent = jsonEvents.getJSONObject(i);

				// create an event object
				Event event = new Event(
					jsonEvent.getString("type"),
					jsonEvent.optString("extra"),
					jsonEvent.optString("filterFunction")
				);

				// get any actions
				ArrayList<Action> actions = getActions(rapidServlet, jsonEvent.optJSONArray("actions"));

				// check we got some
				if (actions != null) {
					// loop them
					for (Action action : actions) {
						// add action object to this event collection
						event.getActions().add(action);
					}
					// retain the event
					events.add(event);
				}

			}
		}
		return events;
	}

	// this is here as a static so it can be used when creating control event actions, or child actions
	public static ArrayList<Action> getActions(RapidHttpServlet rapidServlet, JSONArray jsonActions) throws JSONException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		// the array we are going to return
		ArrayList<Action> actions = null;
		// if any came in
		if (jsonActions != null) {
			// instantiate our return
			actions = new ArrayList<Action>();
			// loop them
			for (int j = 0; j < jsonActions.length(); j++) {
				// get an action
				JSONObject jsonAction = jsonActions.getJSONObject(j);
				// fetch the constructor for this type of object
				Constructor actionConstructor = rapidServlet.getActionConstructor(jsonAction.getString("type"));
				// instantiate the object
				Action action = (Action) actionConstructor.newInstance(rapidServlet, jsonAction);
				// add action object to this event collection
				actions.add(action);
			}

		}
		// return the actions
		return actions;
	}

	// this is here as a static so it used when creating the page object, or a control object
	public static ArrayList<Style> getStyles(RapidHttpServlet rapidServlet, JSONArray jsonStyles) throws JSONException {
		// the styles we are making
		ArrayList<Style> styles = new ArrayList<Style>();
		// if not null
		if (jsonStyles != null) {
			// loop jsonStyles
			for (int i = 0; i < jsonStyles.length(); i++) {
				// get this json style
				JSONObject jsonStyle = jsonStyles.getJSONObject(i);
				// get the applies to
				String appliesTo = jsonStyle.getString("appliesTo");
				// create a new style
				Style style = new Style(appliesTo);
				// loop the rules and add
				JSONArray jsonRules = jsonStyle.optJSONArray("rules");
				// check we got something
				if (jsonRules != null) {
					// loop it
					for (int j = 0; j < jsonRules.length(); j++) style.getRules().add(jsonRules.getString(j));
				}
				// add style into Control
				styles.add(style);
			}

		}
		return styles;
	}

	public String getDetailsJavaScript(Application application, Page page) {
		String js = null;
		String details = getProperty("details");
		if (details != null) {
			// get the id
			String id = getId();
			// if the control is from this page
			if (id.startsWith(page.getId() + "_")) {
				// we can safely use the global variable
				js = id + "details";
			} else {
				// print them in full
				js = details;
			}
		}
		return js;
	}

	@Override
	public String toString() {
		return getType() + " - " + getId();
	}

	// this method returns JavaScript for retrieving a control's data, or runtime property value
	public static String getDataJavaScript(ServletContext servletContext, Application application, Page page, String id, String field) {

		// assume an empty string
		String js = "";

		// if id is not null
		if (id != null) {

			// split by escaped .
			String idParts[] = id.split("\\.");

			// if this is a system value
			if ("System".equals(idParts[0])) {

				// just check that there is a type
				if (idParts.length > 1) {

					// get the type from the second part
					String type = idParts[1];

					// the available system values are specified above getDataOptions in designer.js
					if ("app id".equals(type)) {

						// the global in-page app id variable
						return "_appId";

					} else if ("app version".equals(type)) {

						// the global in-page app version variable
						return "_appVersion";

					} else if ("parameter".equals(type)) {

						// get the parameter value for what was specified in the field box
						String value = application.getParameterValue(field);
						// check we got one
						if (value == null) {
							// send the null if not
							return value;
						} else {
							// wrap the parameter in quotes
							return "'" + value.replace("'", "\\'")  + "'";
						}

					} else if ("page id".equals(type)) {

						// the global in-page page id variable
						return "_pageId";

					} else if ("page name".equals(type)) {

						// the global in-page page name variable
						return "_pageName";

					} else if ("page title".equals(type)) {

						// the global in-page page title variable
						return "_pageTitle";

					} else if ("device".equals(type)) {

						// the user agent string from the front-end, note that database and webservice actions use the back-end user agent string from the action request
						return "navigator.userAgent";

					} else if ("mobile".equals(type)) {

						// whether rapid mobile is present
						return "(typeof _rapidmobile == 'undefined' ? false : true)";

					} else if ("mobile version".equals(type)) {

						// if rapid mobile and the client version method are present call it, otherwise unknown
						return "(typeof _rapidmobile == 'undefined' ? 'unknown' : (_rapidmobile.getClientVersion ? _rapidmobile.getClientVersion() : 'unknown'))";

					} else if ("online".equals(type)) {

						// whether we are online (presumed true if no rapid mobile)
						return "(typeof _rapidmobile == 'undefined' ? true : _rapidmobile.isOnline())";

					} else if ("user".equals(type) || "user name".equals(type)) {

						// if rapid mobile and the getUserName method is present use that, otherwise use the in-page variable
						return "(typeof _rapidmobile == 'undefined' ? _userName : (_rapidmobile.getUserName ? _rapidmobile.getUserName() : _userName))";

					} else if ("empty".equals(type)) {

						// empty equates to null
						return "null";

					} else if ("field".equals(type)) {

						// work out if field is numeric
						if (Numbers.isNumber(field)) {

							// pass the field as a numeric value
							return field;

						} else {

							// pass the field as a string value
							return "'" + (field == null ? "" : field.replace("'", "\\'")) + "'";

						}

					} else {

						// pass through as literal
						return idParts[1];

					}

				}  else {

					// return error
					return "null; /* error finding system value, no type */";

				}

			} else {

				// find the control in the page
				Control control = page.getControl(idParts[0]);
				// assume it is in the page
				boolean pageControl = true;
				// if not found
				if (control == null) {
					// have another last go in the whole application
					control = application.getControl(servletContext, idParts[0]);
					// mark as not in the page
					pageControl = false;
				}
				// check control
				if (control == null) {
					// if still null look for it in page variables
					return " getPageVariableValue('" + id + "','" + page.getId() + "')";
				} else {
					// assume no field
					String fieldJS = "null";
					// add if present
					if (field != null) fieldJS = "'" + field + "'";
					// assume no control details
					String detailsJS = control.getDetailsJavaScript(application, page);
					// look for them
					if (control.getDetails() == null) {
						// update to empty string
						detailsJS = "";
					} else {
						// check if control is in the page
						if (pageControl) {
							// use the abbreviated details
							detailsJS = "," + control.getId() + "details";
						} else {
							// use the long details
							detailsJS = "," + detailsJS;
						}
					}
					// check if there was another
					if (idParts.length > 1) {
						// get the runtime property
						return "getProperty_" + control.getType() + "_" + idParts[1] + "(ev,'" + control.getId() + "'," + fieldJS + detailsJS + ")";
					} else {
						// no other parts return getData call
						return "getData_" + control.getType() + "(ev,'" + control.getId() + "'," + fieldJS + detailsJS + ")";
					}

				} // control check

			} // system value check

		}
		return js;
	}

	// this method returns JavaScript for retrieving a control's data, or runtime property value
	public static String setDataJavaScript(ServletContext servletContext, Application application, Page page, String id, String field) {

		// assume an empty string
		String js = "";

		// if id is not null
		if (id != null) {

			// split by escaped .
			String idParts[] = id.split("\\.");

			// find the control in the page
			Control control = page.getControl(idParts[0]);
			// assume it is in the page
			boolean pageControl = true;
			// if not found
			if (control == null) {
				// have another last go in the whole application
				control = application.getControl(servletContext, idParts[0]);
				// mark as not in the page
				pageControl = false;
			}
			// check control
			if (control != null) {
				// assume no field
				String fieldJS = "null";
				// add if present
				if (field != null) fieldJS = "'" + field + "'";
				// assume no control details
				String detailsJS = control.getDetailsJavaScript(application, page);
				// look for them
				if (control.getDetails() == null) {
					// update to null
					detailsJS = ", null";
				} else {
					// check if control is in the page
					if (pageControl) {
						// use the abbreviated details
						detailsJS = "," + control.getId() + "details";
					} else {
						// use the long details
						detailsJS = "," + detailsJS;
					}
				}
				// check if there was another
				if (idParts.length > 1) {
					// get the runtime property
					return "setProperty_" + control.getType() + "_" + idParts[1] + "(ev,'" + control.getId() + "'," + fieldJS + detailsJS + ", data, true)";
				} else {
					// no other parts return getData call
					return "setData_" + control.getType() + "(ev,'" + control.getId() + "'," + fieldJS + detailsJS + ", data, true)";
				}

			} // control check

		}
		return js;
	}

	// this method checks the xml versions and upgrades any xml nodes before the xml document is unmarshalled
	public static Node upgrade(Node actionNode) { return actionNode; }

}
