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

package com.rapid.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.rapid.actions.Logic.Condition;
import com.rapid.actions.Logic.Value;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Application.Resource;
import com.rapid.core.Application.ResourceDependency;
import com.rapid.forms.FormAdapter;
import com.rapid.forms.FormAdapter.FormControlValue;
import com.rapid.forms.FormAdapter.FormPageControlValues;
import com.rapid.forms.FormAdapter.UserFormDetails;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.server.Rapid;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.server.filter.RapidFilter;
import com.rapid.utils.Files;
import com.rapid.utils.Html;
import com.rapid.utils.Minify;
import com.rapid.utils.XML;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Page {

	// the version of this class's xml structure when marshalled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)
	public static final int XML_VERSION = 1;

	// form page types
	public static final int FORM_PAGE_TYPE_NORMAL = 0;
	public static final int FORM_PAGE_TYPE_SUBMITTED = 1;
	public static final int FORM_PAGE_TYPE_ERROR = 2;
	public static final int FORM_PAGE_TYPE_SAVE = 3;
	public static final int FORM_PAGE_TYPE_RESUME = 4;

	// a class for retaining page html for a set of user roles - this structure is now depreciated as of Rapid 2.3.5.2 in favour of a more efficient tree structure
	public static class RoleHtml {

		// instance variables

		private List<String> _roles;
		private String _html;

		// properties

		public List<String> getRoles() { return _roles; }
		public void setRoles(List<String> roles) { _roles = roles; }

		public String getHtml() { return _html; }
		public void setHtml(String html) { _html = html; }

		// constructors
		public RoleHtml() {}

		public RoleHtml(List<String> roles, String html) {
			_roles = roles;
			_html = html;
		}

	}

	// a class for retaining control html that has user roles
	public static class RoleControlHtml {

		// instance variables

		private String _startHtml, _endHtml;
		private List<String> _roles;
		private List<RoleControlHtml> _children;

		// properties

		public String getStartHtml() { return _startHtml; }
		public void setStartHtml(String startHtml) { _startHtml = startHtml; }

		public String getEndHtml() { return _endHtml; }
		public void setEndHtml(String endHtml) { _endHtml = endHtml; }

		public List<String> getRoles() { return _roles; }
		public void setRoles(List<String> roles) { _roles = roles; }

		public List<RoleControlHtml> getChildren() { return _children; }
		public void setChildren(List<RoleControlHtml> children) { _children = children; }

		// constructors
		public RoleControlHtml() {}

		public RoleControlHtml(JSONObject jsonRoleControlHtml) throws JSONException {
			_startHtml = jsonRoleControlHtml.optString("startHtml", null);
			_endHtml = jsonRoleControlHtml.optString("endHtml", null);
			JSONArray jsonRoles = jsonRoleControlHtml.optJSONArray("roles");
			if (jsonRoles != null) {
				_roles = new ArrayList<String>();
				for (int i = 0; i < jsonRoles.length(); i++) _roles.add(jsonRoles.getString(i));
			}
			JSONArray jsonChildren = jsonRoleControlHtml.optJSONArray("children");
			if (jsonChildren != null) {
				_children = new ArrayList<RoleControlHtml>();
				for (int i = 0; i < jsonChildren.length(); i++) _children.add( new RoleControlHtml(jsonChildren.getJSONObject(i)));
			}
		}

	}

	// details of a lock that might be on this page
	public static class Lock {

		private String _userName, _userDescription;
		private Date _dateTime;

		public String getUserName() { return _userName; }
		public void setUserName(String userName) { _userName = userName; }

		public String getUserDescription() { return _userDescription; }
		public void setUserDescription(String userDescription) { _userDescription = userDescription; }

		public Date getDateTime() { return _dateTime; }
		public void setDateTime(Date dateTime) { _dateTime = dateTime; }

		// constructors

		public Lock() {}

		public Lock(String userName, String userDescription, Date dateTime) {
			_userName = userName;
			_userDescription = userDescription;
			_dateTime = dateTime;
		}

	}

	// instance variables

	private int _xmlVersion, _formPageType;
	private String _id, _name, _title, _label, _description, _createdBy, _modifiedBy, _htmlBody, _bodyStyleClasses, _cachedHeadLinks, _cachedHeadCSS, _cachedHeadReadyJS, _cachedHeadJS, _eTag;
	private boolean _simple, _hideHeaderFooter;
	private Date _createdDate, _modifiedDate;
	private List<Control> _controls, _reCaptchaControls;
	private List<Event> _events;
	private List<Style> _styles;
	private List<String> _controlTypes, _actionTypes, _sessionVariables, _roles;
	private List<RoleHtml> _rolesHtml;
	private RoleControlHtml _roleControlHtml;
	private List<Condition> _visibilityConditions;
	private String _conditionsType;
	private Lock _lock;
	private List<String> _formControlValues;
	private List<String> _dialoguePageIds;

	// this array is used to collect all of the lines needed in the pageload before sorting them
	private List<String> _pageloadLines;

	// properties

	// the xml version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }

	// the id uniquely identifies the page (it is quiet short and is concatinated to control id's so more than one page's control's can be working in a document at one time)
	public String getId() { return _id; }
	public void setId(String id) { _id = id; }

	// this is expected to be short name, probably even a code that is used by users to simply identify pages (also becomes the file name)
	public String getName() { return _name; }
	public void setName(String name) { _name = name; }

	// this is a user-friendly, long title
	public String getTitle() { return _title; }
	public void setTitle(String title) { _title = title; }

	// the form page type, most will be normal but we show special pages for after submission, error, and saved
	public int getFormPageType() { return _formPageType; }
	public void setFormPageType(int formPageType) { _formPageType = formPageType; }

	// this is a the label to use in the form summary
	public String getLabel() { return _label; }
	public void setLabel(String label) { _label = label; }

	// an even longer description of what this page does
	public String getDescription() { return _description; }
	public void setDescription(String description) { _description = description; }

	// simple pages do not have any events and can be used in page panels without dynamically loading them via ajax
	public boolean getSimple() { return _simple; }
	public void setSimple(boolean simple) { _simple = simple; }

	// whether to hide any theme header / footer
	public boolean getHideHeaderFooter() { return _hideHeaderFooter; }
	public void setHideHeaderFooter(boolean hideHeaderFooter) { _hideHeaderFooter = hideHeaderFooter; }

	// the user that created this page (or archived page)
	public String getCreatedBy() { return _createdBy; }
	public void setCreatedBy(String createdBy) { _createdBy = createdBy; }

	// the date this page (or archive) was created
	public Date getCreatedDate() { return _createdDate; }
	public void setCreatedDate(Date createdDate) { _createdDate = createdDate; }

	// the last user to save this application
	public String getModifiedBy() { return _modifiedBy; }
	public void setModifiedBy(String modifiedBy) { _modifiedBy = modifiedBy; }

	// the date this application was last saved
	public Date getModifiedDate() { return _modifiedDate; }
	public void setModifiedDate(Date modifiedDate) { _modifiedDate = modifiedDate; }

	// the html for this page
	public String getHtmlBody() { return _htmlBody; }
	public void setHtmlBody(String htmlBody) { _htmlBody = htmlBody; }

	// any style classes for this pages body element
	public String getBodyStyleClasses() { return _bodyStyleClasses; }
	public void setBodyStyleClasses(String bodyStyleClasses) { _bodyStyleClasses = bodyStyleClasses; }

	// the child controls of the page
	public List<Control> getControls() { return _controls; }
	public void setControls(List<Control> controls) { _controls = controls; }

	// the page events and actions
	public List<Event> getEvents() { return _events; }
	public void setEvents(List<Event> events) { _events = events; }

	// the page styles
	public List<Style> getStyles() { return _styles; }
	public void setStyles(List<Style> styles) { _styles = styles; }

	// session variables used by this page (navigation actions are expected to pass them in)
	public List<String> getSessionVariables() { return _sessionVariables; }
	public void setSessionVariables(List<String> sessionVariables) { _sessionVariables = sessionVariables; }

	// the roles required to view this page
	public List<String> getRoles() { return _roles; }
	public void setRoles(List<String> roles) { _roles = roles; }

	// list of different page html for different possible role combinations - this is depreciated from Rapid 2.3.5.3
	public List<RoleHtml> getRolesHtml() { return _rolesHtml; }
	public void setRolesHtml(List<RoleHtml> rolesHtml) { _rolesHtml = rolesHtml; }

	// page html for different possible role combinations - this is depreciated from Rapid 2.3.5.3
	public RoleControlHtml getRoleControlHtml() { return _roleControlHtml; }
	public void setRoleControlHtml(RoleControlHtml roleControlHtml) { _roleControlHtml = roleControlHtml; }

	// any lock that might be on this page
	public Lock getLock() { return _lock; }
	public void setLock(Lock lock) { _lock = lock; }

	// the page visibility rule conditions
	public List<Condition> getVisibilityConditions() { return _visibilityConditions; }
	public void setVisibilityConditions(List<Condition> visibilityConditions) { _visibilityConditions = visibilityConditions; }

	// the type (and/or) of the page visibility conditions - named so can be shared with logic action
	public String getConditionsType() { return _conditionsType; }
	public void setConditionsType(String conditionsType) { _conditionsType = conditionsType; }

	// the etag used to send 304 not modified if caching is turned off
	public String getETag() { return _eTag; }

	// constructor

	public Page() {
		// set the xml version
		_xmlVersion = XML_VERSION;
		// set the eTag
		_eTag = Long.toString(new Date().getTime());
	}

	// instance methods

	public String getFile(ServletContext servletContext, Application application) {
		return application.getConfigFolder(servletContext) + "/" + "/pages/" + Files.safeName(_name) + ".page.xml";
	}

	public void addControl(Control control) {
		if (_controls == null) _controls = new ArrayList<Control>();
		_controls.add(control);
	}

	public Control getControl(int index) {
		if (_controls == null) return null;
		return _controls.get(index);
	}

	// an iterative function for tree-walking child controls when searching for one
	public Control getChildControl(List<Control> controls, String controlId) {
		Control foundControl = null;
		if (controls != null) {
			for (Control control : controls) {
				if (controlId.equals(control.getId())) {
					foundControl = control;
					break;
				} else {
					foundControl = getChildControl(control.getChildControls(), controlId);
					if (foundControl != null) break;
				}
			}
		}
		return foundControl;
	}

	// uses the tree walking function above to find a particular control
	public Control getControl(String id) {
		return getChildControl(_controls, id);
	}

	public void getChildControls(List<Control> controls, List<Control> childControls) {
		if (controls != null) {
			for (Control control : controls) {
				childControls.add(control);
				getChildControls(control.getChildControls(), childControls);
			}
		}
	}

	public List<Control> getAllControls() {
		ArrayList<Control> controls = new ArrayList<Control>();
		getChildControls(_controls, controls);
		return controls;
	}

	// find an action from a list of actions, including checking child actions
	public Action getChildAction(List<Action> actions, String actionId) {
		Action foundAction = null;
		if (actions != null) {
			for (Action action : actions) {
				if (action != null) {
					if (actionId.equals(action.getId())) return action;
					foundAction = getChildAction(action.getChildActions(), actionId);
					if (foundAction != null) break;
				}
			}
		}
		return foundAction;
	}

	// find an action amongst a controls events - faster to use if we have the control already
	public Action getChildEventsAction(List<Event> events, String actionId) {
		Action foundAction = null;
		if (events != null) {
			for (Event event : events) {
				if (event.getActions() != null) {
					foundAction = getChildAction(event.getActions(), actionId);
					if (foundAction != null) break;
				}
			}
		}
		return foundAction;
	}

	// an iterative function for tree-walking child controls when searching for a specific action
	public Action getChildControlsAction(List<Control> controls, String actionId) {
		Action foundAction = null;
		if (controls != null) {
			for (Control control : controls) {
				// look in the control events for the action
				foundAction = getChildEventsAction(control.getEvents(), actionId);
				// if we didn't get the action
				if (foundAction == null) {
					// look in the child controls
					foundAction = getChildControlsAction(control.getChildControls(), actionId);
				}
				// we're done!
				if (foundAction != null) break;
			}
		}
		return foundAction;
	}

	// find an action in the page by its id
	public Action getAction(String id) {
		// check the page actions first
		if (_events != null) {
			for (Event event : _events) {
				if (event.getActions() != null) {
					Action action = getChildAction(event.getActions(), id);
					if (action != null) return action;
				}
			}
		}
		// uses the tree walking function above to the find a particular action
		return getChildControlsAction(_controls, id);
	}

	// recursively append to a list of actions from an action and it's children
	public void getChildActions(List<Action> actions, Action action, String type) {
		// if there is a type
		if (type == null) {
			// add this one action
			actions.add(action);
		} else {
			// if types match
			if (type.equals(action.getType())) {
				// add action
				actions.add(action);
			}
		}
		// check there are child actions
		if (action.getChildActions() != null) {
			// loop them
			for (Action childAction : action.getChildActions()) {
				// add their actions too
				getChildActions(actions, childAction, type);
			}
		}
	}

	// overide for the above
	public void getChildActions(List<Action> actions, Action action) {
		getChildActions(actions, action, null);
	}

	// recursively append to a list of actions from a control and it's children
	public void getChildActions(List<Action> actions, Control control, String type) {
		// check this control has events
		if (control.getEvents() != null) {
			for (Event event : control.getEvents()) {
				// add any actions to the list
				if (event.getActions() != null) {
					// loop the actions
					for (Action action : event.getActions()) {
						// add any child actions too
						getChildActions(actions, action, type);
					}
				}
			}
		}
		// check if we have any child controls
		if (control.getChildControls() != null) {
			// loop the child controls
			for (Control childControl : control.getChildControls()) {
				// add their actions too
				getChildActions(actions, childControl, type);
			}
		}
	}

	// override for the above
	public void getChildActions(List<Action> actions, Control control) {
		getChildActions(actions, control, null);
	}

	// add actions and child actions
	public void addAction(List<Action> actions, Action action) {
		// if we have an action
		if (action != null) {
			// add it
			actions.add(action);
			// get any child actions
			List<Action> childActions = action.getChildActions();
			// if there where some
			if (childActions != null) {
				// loop the children
				for (Action childAction : childActions) {
					// add this action recursively
					addAction(actions, childAction);
				}
			}
		}
	}

	// get all actions in the page of a specified type
	public List<Action> getAllActions(String type) {
		// instantiate the list we're going to return
		List<Action> actions = new ArrayList<Action>();
		// check the page events first
		if (_events != null) {
			for (Event event : _events) {
				// get any event actions
				List<Action> eventActions = event.getActions();
				// if we got some
				if (eventActions != null) {
					// if type is null
					if (type == null) {
						// loop actions
						for (Action eventAction : eventActions) {
							// add this action, including it's children
							addAction(actions, eventAction);
						}
					} else {
						// loop them
						for (Action eventAction : eventActions) {
							// if right type
							if (type.equals(eventAction.getType())) {
								// add this action, including it's children
								addAction(actions, eventAction);
							}
						}
					}
				}
			}
		}
		// uses the tree walking function above to add all actions
		if (_controls != null) {
			for (Control control : _controls) {
				getChildActions(actions, control, type);
			}
		}
		// sort them by action id
		Collections.sort(actions, new Comparator<Action>() {
			@Override
			public int compare(Action obj1, Action obj2) {
				if (obj1 == null) return -1;
				if (obj2 == null) return 1;
				if (obj1.equals(obj2)) return 0;
				String id1 = obj1.getId();
				String id2 = obj2.getId();
				if (id1 == null) return -1;
				if (id2 == null) return -1;
				int startPos = id1.lastIndexOf("_A");
				if (startPos < 0) return -1;
				int endPos = id1.indexOf("_", startPos + 2);
				if (endPos < 0) endPos = id1.length();
				id1 = id1.substring(startPos + 2, endPos);
				startPos = id2.lastIndexOf("_A");
				if (startPos < 0) return 1;
				endPos = id2.indexOf("_", startPos + 2);
				if (endPos < 0) endPos = id2.length();
				id2 = id2.substring(startPos + 2, endPos);
				return (Integer.parseInt(id1) - Integer.parseInt(id2));
			}
		});
		return actions;
	}

	// get all actions in the page
	public List<Action> getAllActions() {
		// override for the above
		return getAllActions(null);
	}

	// an iterative function for tree-walking child controls when searching for a specific action's control
	public Control getChildControlActionControl(List<Control> controls, String actionId) {
		Control foundControl = null;
		if (controls != null) {
			for (Control control : controls) {
				if (control.getEvents() != null) {
					for (Event event : control.getEvents()) {
						if (event.getActions() != null) {
							for (Action action : event.getActions()) {
								if (actionId.equals(action.getId())) return control;
							}
						}
					}
				}
				foundControl = getChildControlActionControl(control.getChildControls(), actionId);
				if (foundControl != null) break;
			}
		}
		return foundControl;
	}

	// find an action's control in the page by its id
	public Control getActionControl(String actionId) {
		// uses the tree walking function above to the find a particular action
		return getChildControlActionControl(_controls, actionId);
	}

	// an iterative function for tree-walking child controls when searching for a specific action's control
	public Event getChildControlActionEvent(List<Control> controls, String actionId) {
		Event foundEvent = null;
		if (controls != null) {
			for (Control control : controls) {
				if (control.getEvents() != null) {
					for (Event event : control.getEvents()) {
						if (event.getActions() != null) {
							for (Action action : event.getActions()) {
								if (actionId.equals(action.getId())) return event;
							}
						}
					}
				}
				foundEvent = getChildControlActionEvent(control.getChildControls(), actionId);
				if (foundEvent != null) break;
			}
		}
		return foundEvent;
	}

	// find an action in the page by its id
	public Event getActionEvent(String actionId) {
		// check the page actions first
		if (_events != null) {
			for (Event event : _events) {
				if (event.getActions() != null) {
					for (Action action : event.getActions()) {
						if (actionId.equals(action.getId())) return event;
					}
				}
			}
		}
		// uses the tree walking function above to the find a particular action
		return getChildControlActionEvent(_controls, actionId);
	}

	// gets the pages that this page can navigate to as a dialogue - we check all pages to see which can come back
	public List<String> getDialoguePageIds() {
		// if the internal variable has not been initialised yet
		if (_dialoguePageIds == null) {
			// initialise
			_dialoguePageIds = new ArrayList<String>();
			// get all navigation actions on this page
			List<Action> actions = getAllActions("navigate");
			// loop them
			for (Action action : actions) {
				// if this is a dialogue
				if (Boolean.parseBoolean(action.getProperty("dialogue"))) {
					// get the page id
					String pageId = action.getProperty("page");
					// if we got one
					if (pageId != null) {
						// add if it is something
						if (pageId.length() > 0) _dialoguePageIds.add(pageId);
					}
				}
			}
		}
		return _dialoguePageIds;
	}

	// iterative function for building a flat JSONArray of controls that can be used on other pages, will also add events if including from a dialogue
	private void getOtherPageControls(RapidHttpServlet rapidServlet, JSONArray jsonControls, List<Control> controls, boolean includePageVisibiltyControls, Boolean includeFromDialogue) throws JSONException {
		// check we were given some controls
		if (controls != null) {
			// loop the controls
			for (Control control : controls) {
				// get if this control can be used from other pages
				boolean canBeUsedFromOtherPages = control.getCanBeUsedFromOtherPages();
				// get if this control can be used for page visibility
				boolean canBeUsedForFormPageVisibilty = control.getCanBeUsedForFormPageVisibilty() && includePageVisibiltyControls;
				// if this control can be used from other pages
				if (canBeUsedFromOtherPages || canBeUsedForFormPageVisibilty || includeFromDialogue) {

					// get the control details
					JSONObject jsonControlClass = rapidServlet.getJsonControl(control.getType());

					// check we got one
					if (jsonControlClass != null) {

						// get the name
						String controlName = control.getName();

						// no need to include if we don't have one
						if (controlName != null && controlName.trim().length() > 0) {

							// make a JSON object with what we need about this control
							JSONObject jsonControl = new JSONObject();
							jsonControl.put("id", control.getId());
							jsonControl.put("type", control.getType());
							jsonControl.put("name", controlName);
							if (jsonControlClass.optString("getDataFunction", null) != null) 	jsonControl.put("input", true);
							if (jsonControlClass.optString("setDataJavaScript", null) != null) jsonControl.put("output", true);
							if (canBeUsedFromOtherPages) jsonControl.put("otherPages", true);
							if (canBeUsedForFormPageVisibilty) jsonControl.put("pageVisibility", true);
							if (control.getProperty("formObjectAddressNumber") != null) jsonControl.put("formObjectAddressNumber", control.getProperty("formObjectAddressNumber"));
							if (control.getProperty("formObjectPartyNumber") != null) jsonControl.put("formObjectPartyNumber", control.getProperty("formObjectPartyNumber"));

							// look for any runtimeProperties
							JSONObject jsonProperty = jsonControlClass.optJSONObject("runtimeProperties");
							// if we got some
							if (jsonProperty != null) {
								// create an array to hold the properties
								JSONArray jsonRunTimeProperties = new JSONArray();
								// look for an array too
								JSONArray jsonProperties = jsonProperty.optJSONArray("runtimeProperty");
								// assume
								int index = 0;
								int count = 0;
								// if an array
								if (jsonProperties != null) {
									// get the first item
									jsonProperty = jsonProperties.getJSONObject(index);
									// set the count
									count = jsonProperties.length();
								}
								// look for a single object
								JSONObject jsonPropertySingle = jsonProperty.optJSONObject("runtimeProperty");
								// assume this one if not null
								if (jsonPropertySingle != null) jsonProperty = jsonPropertySingle;

								// do once and loop until no more left
								do {

									// create a json object for this runtime property
									JSONObject jsonRuntimeProperty = new JSONObject();
									jsonRuntimeProperty.put("type", jsonProperty.get("type"));
									jsonRuntimeProperty.put("name", jsonProperty.get("name"));
									if (jsonProperty.optString("getPropertyFunction", null) != null) jsonRuntimeProperty.put("input", true);
									if (jsonProperty.optString("setPropertyJavaScript", null) != null) jsonRuntimeProperty.put("output", true);
									if (jsonProperty.optBoolean("canBeUsedForFormPageVisibilty")) jsonRuntimeProperty.put("visibility", true);

									// add to the collection - note further check for dialogue controls having to add in
									jsonRunTimeProperties.put(jsonRuntimeProperty);

									// increment the index
									index ++;

									// get the next item if there's one there
									if (index < count) jsonProperty = jsonProperties.getJSONObject(index);

								} while (index < count);
								// add the properties to what we're returning
								jsonControl.put("runtimeProperties", jsonRunTimeProperties);
							} // property loop

							// if we are including from dialogue
							if (includeFromDialogue) {
								// set the other pages property so we see it in the designer
								jsonControl.put("otherPages", true);
								// get any events for this control
								List<Event> events = control.getEvents();
								// if we got some
								if (events != null) {
									// an array of events
									JSONArray jsonEvents = new JSONArray();
									// loop them
									for (Event event : events) {
										// get any actions
										List<Action> actions = event.getActions();
										// if there were some
										if (actions != null) {
											// if there were some
											if (actions.size() > 0) {
												// make a jsonArray for the actions
												JSONArray jsonActions = new JSONArray();
												// loop the actions
												for (Action action : actions) {
													// make a json object for the action
													JSONObject jsonAction = new JSONObject();
													// add id
													jsonAction.put("id", action.getId());
													// add type
													jsonAction.put("type", action.getType());
													// add to array
													jsonActions.put(jsonAction);
												}
												// make a jsonObject for this event
												JSONObject jsonEvent = new JSONObject();
												// add the event id
												jsonEvent.put("type", event.getType());
												// add the jsonActions to the event
												jsonEvent.put("actions", jsonActions);
												// as jsonEvent to collection
												jsonEvents.put(jsonEvent);
											}
										}
									}
									// add the jsonEvents to the control if we got some
									if (jsonEvents.length() > 0) jsonControl.put("events",jsonEvents);
								}
							}
							// add it to the collection we are returning straight away
							jsonControls.put(jsonControl);
						} // name check
					} // control class check
				} // other page or visibility check
				// run for any child controls
				getOtherPageControls(rapidServlet, jsonControls, control.getChildControls(), includePageVisibiltyControls, includeFromDialogue);
			}
		}
	}

	// uses the above iterative method to return an object with flat array of controls in this page that can be used from other pages, for use in the designer
	public JSONArray getOtherPageComponents(RapidHttpServlet rapidServlet, boolean includePageVisibiltyControls, boolean includeFromDialogue) throws JSONException {
		// the list of controls we're about to return
		JSONArray controls = new JSONArray();
		// start building the array using the page controls
		getOtherPageControls(rapidServlet, controls, _controls, includePageVisibiltyControls, includeFromDialogue);
		// return the components
		return controls;
	}

	// used to turn either a page or control style into text for the css file
	public String getStyleCSS(Style style) {
		// start the text we are going to return
		String css = "";
		// get the style rules
		ArrayList<String> rules = style.getRules();
		// check we have some
		if (rules != null) {
			if (rules.size() > 0) {
				// add the style
				css = style.getAppliesTo().trim() + " {\n";
				// check we have
				// loop and add the rules
				for (String rule : rules) {
					css += "\t" + rule.trim() + "\n";
				}
				css += "}\n\n";
			}
		}
		// return the css
		return css;
	}

	// an iterative function for tree-walking child controls when building the page styles
	public void getChildControlStyles(List<Control> controls, StringBuilder stringBuilder) {
		if (controls != null) {
			for (Control control : controls) {
				// look for styles
				ArrayList<Style> controlStyles = control.getStyles();
				if (controlStyles != null) {
					// loop the styles
					for (Style style  : controlStyles) {
						// get some nice text for the css
						stringBuilder.append(getStyleCSS(style));
					}
				}
				// try and call on any child controls
				getChildControlStyles(control.getChildControls(), stringBuilder);
			}
		}
	}


	public String getAllCSS(ServletContext servletContext,  Application application) {
		// the stringbuilder we're going to use
		StringBuilder stringBuilder = new StringBuilder();
		// check if the page has styles
		if (_styles != null) {
			// loop
			for (Style style: _styles) {
				stringBuilder.append(getStyleCSS(style));
			}
		}
		// use the iterative tree-walking function to add all of the control styles
		getChildControlStyles(_controls, stringBuilder);
		// return it with inserted parameters
		return application.insertParameters(servletContext, stringBuilder.toString());
	}

	public List<String> getAllActionTypes() {
		List<String> actionTypes = new ArrayList<String>();
		List<Action> actions = getAllActions();
		if (actions != null) {
			for (Action action : actions) {
				String actionType = action.getType();
				if (!actionTypes.contains(actionType)) actionTypes.add(actionType);
			}
		}
		return actionTypes;
	}

	public List<String> getAllControlTypes() {
		List<String> controlTypes = new ArrayList<String>();
		controlTypes.add("page");
		List<Control> controls = getAllControls();
		if (controls != null) {
			for (Control control : controls) {
				String controlType = control.getType();
				if (!controlTypes.contains(controlType)) controlTypes.add(controlType);
			}
		}
		return controlTypes;
	}

	// removes the page lock if it is more than 1 hour old
	public void checkLock() {
		// only check if there is one present
		if (_lock != null) {
			// get the time now
			Date now = new Date();
			// get the time an hour after the lock time
			Date lockExpiry = new Date(_lock.getDateTime().getTime() + 1000 * 60 * 60);
			// if the lock expiry has passed set the lock to null;
			if (now.after(lockExpiry)) _lock = null;
		}
	}

	public void backup(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, Application application, File pageFile, boolean delete) throws IOException {

		// get the user name
		String userName = Files.safeName(rapidRequest.getUserName());

		// create folders to archive the pages
		String archivePath = application.getBackupFolder(rapidServlet.getServletContext(), delete);
		File archiveFolder = new File(archivePath);
		if (!archiveFolder.exists()) archiveFolder.mkdirs();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateString = formatter.format(new Date());

		 // create a file object for the archive file
	 	File archiveFile = new File(archivePath + "/" + Files.safeName(_name) + "_" + dateString + "_" + userName + ".page.xml");

	 	// copy the existing new file to the archive file
	    Files.copyFile(pageFile, archiveFile);

	}

	public void deleteBackup(RapidHttpServlet rapidServlet, Application application, String backupId) {

		// create the path
		String backupPath = application.getBackupFolder(rapidServlet.getServletContext(), false) + "/" + backupId;
		// create the file
		File backupFile = new File(backupPath);
		// delete
		Files.deleteRecurring(backupFile);

	}

	public long save(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, Application application, boolean backup) throws JAXBException, IOException {

		// create folders to save the pages
		String pagePath = application.getConfigFolder(rapidServlet.getServletContext()) + "/pages";
		File pageFolder = new File(pagePath);
		if (!pageFolder.exists()) pageFolder.mkdirs();

		// create a file object for the new file
	 	File newFile = new File(pagePath + "/" + Files.safeName(getName()) + ".page.xml");

	 	// if we want a backup and the new file already exists it needs archiving
	 	if (backup && newFile.exists()) backup(rapidServlet, rapidRequest, application, newFile, false);

	 	// create a file for the temp file
	    File tempFile = new File(pagePath + "/" + Files.safeName(getName()) + "-saving.page.xml");

	    // update the modified by and date
	    _modifiedBy = rapidRequest.getUserName();
	    _modifiedDate = new Date();

		// get a buffered writer for our page with UTF-8 file format
		BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(tempFile), "UTF-8"));

		try {
			// marshall the page object into the temp file
			rapidServlet.getMarshaller().marshal(this, bw);
		} catch (JAXBException ex) {
			// close the file writer
		    bw.close();
		    // re throw the exception
		    throw ex;
		}

		// close the file writer
	    bw.close();

	 	// copy the tempFile to the newFile
	    Files.copyFile(tempFile, newFile);

	    // store the size of the file writter
	    long fileSize = tempFile.length();

	    // delete the temp file
	    tempFile.delete();

		// replace the old page with the new page
		application.getPages().addPage(this, newFile, application.getIsForm());

		// empty the cached page html
		_cachedHeadLinks = null;
		_cachedHeadCSS = null;
		_cachedHeadReadyJS = null;
		_cachedHeadJS = null;
		// empty the cached action types
		_actionTypes = null;
		// empty the cached control types
		_controlTypes = null;

		// empty the page variables so they are rebuilt the next time
		application.emptyPageVariables();

		return fileSize;
	}

	public void delete(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, Application application) throws JAXBException, IOException {

		// create folders to delete the page
		String pagePath = application.getConfigFolder(rapidServlet.getServletContext()) + "/pages";

		// create a file object for the delete file
	 	File delFile = new File(pagePath + "/" + Files.safeName(getName()) + ".page.xml");

	 	// if the new file already exists it needs archiving
	 	if (delFile.exists()) {
	 		// archive the page file
	 		backup(rapidServlet, rapidRequest, application, delFile, false);
	 		// delete the page file
	 		delFile.delete();
	 		// remove it from the current list of pages
		 	application.getPages().removePage(_id);
	 	}

	 	// get the resources path
	 	String resourcesPath = application.getWebFolder(rapidServlet.getServletContext());

	 	// create a file object for deleting the page css file
	 	File delCssFile = new File(resourcesPath + "/" + Files.safeName(getName()) + ".css");

	 	// delete if it exists
	 	if (delCssFile.exists()) delCssFile.delete();

	 	// create a file object for deleting the page css file
	 	File delCssFileMin = new File(resourcesPath + "/" + Files.safeName(getName()) + ".min.css");

	 	// delete if it exists
	 	if (delCssFileMin.exists()) delCssFileMin.delete();

	}

	// this includes functions to iteratively call any control initJavaScript and set up any event listeners
    private void getPageLoadLines(List<String> pageloadLines, List<Control> controls) throws JSONException {
    	// if we have controls
    	if (controls != null) {
    		// loop controls
    		for (Control control : controls) {
    			// check for any initJavaScript to call
    			if (control.hasInitJavaScript()) {
    				// get any details we may have
					String details = control.getDetails();
					// set to empty string or clean up
					if (details == null) {
						details = "";
					} else {
						details = ", " + control.getId() + "details";
					}
    				// write an init call method
    				pageloadLines.add("Init_" + control.getType() + "('" + control.getId() + "'" + details + ");\n");
    			}
    			// check event actions
    			if (control.getEvents() != null) {
    				// loop events
    				for (Event event : control.getEvents()) {
    					// only if event is non-custom and there are actually some actions to invoke
    					if (!event.isCustomType() && event.getActions() != null) {
    						if (event.getActions().size() > 0) {
    							// add any page load lines from this
    							pageloadLines.add(event.getPageLoadJavaScript(control));
    						}
    					}
    				}
    			}
    			// now call iteratively for child controls (of this [child] control, etc.)
    			if (control.getChildControls() != null) getPageLoadLines(pageloadLines, control.getChildControls());
    		}
    	}
    }

    // the html for a specific resource
    public String getResourceHtml(Application application, Resource resource) {

    	// assume we couldn't make the resource html
    	String resourceHtml = null;

    	// set the link according to the type
		switch (resource.getType()) {
			case Resource.JAVASCRIPT:
				if (application.getStatus() == Application.STATUS_LIVE) {
					try {
						resourceHtml = "    <script type='text/javascript'>" + Minify.toString(resource.getContent(),Minify.JAVASCRIPT, "JavaScript resource " + resource.getName()) + "</script>";
					} catch (IOException ex) {
						resourceHtml = "    <script type='text/javascript'>/* Failed to minify resource " + resource.getName() + " JavaScript : " + ex.getMessage() + "*/</script>";
					}
				} else {
					resourceHtml = "    <script type='text/javascript'>\n" + resource.getContent() + "\n    </script>";
				}
			break;
			case Resource.CSS:
				if (application.getStatus() == Application.STATUS_LIVE) {
					try {
						resourceHtml = "    <style>" + Minify.toString(resource.getContent(), Minify.CSS, "") + "</style>";
					} catch (IOException ex) {
						resourceHtml = "    <style>/* Failed to minify resource " + resource.getName() + " CSS : " + ex.getMessage() + "*/<style>";
					}
				} else {
					resourceHtml = "    <style>" + resource.getContent() + "<style>";
				}
			break;
			case Resource.JAVASCRIPTFILE : case Resource.JAVASCRIPTLINK :
				resourceHtml = "    <script type='text/javascript' src='" + resource.getContent() + "'></script>";
			break;
			case Resource.CSSFILE : case Resource.CSSLINK :
				resourceHtml = "    <link rel='stylesheet' type='text/css' href='" + resource.getContent() + "'></link>";
			break;
		}
    	// return it
    	return resourceHtml;

    }

    // the resources for the page
    public String getResourcesHtml(Application application, boolean allResources) {

    	StringBuilder stringBuilder = new StringBuilder();

    	// get all action types used in this page
    	if (_actionTypes == null) _actionTypes = getAllActionTypes();
    	// get all control types used in this page
    	if (_controlTypes == null) _controlTypes = getAllControlTypes();

    	// manage the resources links added already so we don't add twice
    	ArrayList<String> addedResources = new ArrayList<String>();

		// if this application has resources add during initialisation
		if (application.getResources() != null) {
			// loop and add the resources required by this application's controls and actions (created when application loads)
			for (Resource resource : application.getResources()) {
				// if we want all the resources (for the designer) or there is a dependency for this resource
				if (allResources || resource.hasDependency(ResourceDependency.RAPID) || resource.hasDependency(ResourceDependency.ACTION, _actionTypes) || resource.hasDependency(ResourceDependency.CONTROL, _controlTypes)) {
					// the html we're hoping to get
					String resourceHtml = getResourceHtml(application, resource);
					// if we got some html and don't have it already
					if (resourceHtml != null && !addedResources.contains(resourceHtml)) {
						// append it
						stringBuilder.append(resourceHtml + "\n");
						// remember we've added it
						addedResources.add(resourceHtml);
					}

				} // dependency check

			} // resource loop

		} // has resources

		return stringBuilder.toString();

    }

    private void getEventJavaScriptFunction(RapidRequest rapidRequest, StringBuilder stringBuilder, Application application, Control control, Event event) {
    	// check actions are initialised
		if (event.getActions() != null) {
			// check there are some to loop
			if (event.getActions().size() > 0) {

				// create actions separately to avoid redundancy
				StringBuilder actionStringBuilder = new StringBuilder();
				StringBuilder eventStringBuilder = new StringBuilder();

				// start the function name
				String functionName = "Event_" + event.getType() + "_";
				// if this is the page (no control) use the page id, otherwise use the controlId
				if (control == null) {
					// append the page id
					functionName += _id;
				} else {
					// append the control id
					functionName += control.getId();
				}

				// create a function for running the actions for this controls events
				eventStringBuilder.append("function " + functionName + "(ev) {\n");
				// open a try/catch
				eventStringBuilder.append("  try {\n");

				// get any filter javascript
				String filter = event.getFilter();
				// if we have any add it now
				if (filter != null) {
					// only bother if not an empty string
					if (!"".equals(filter)) {
						eventStringBuilder.append("    " + filter.trim().replace("\n", "\n    ") + "\n");
					}
				}

				// loop the actions and produce the handling JavaScript
				for (Action action : event.getActions()) {

					try {
						// get the action client-side java script from the action object (it's generated there as it can contain values stored in the object on the server side)
						String actionJavaScript = action.getJavaScript(rapidRequest, application, this, control, null);
						// if non null
						if (actionJavaScript != null) {
							// trim it to avoid tabs and line breaks that might sneak in
							actionJavaScript = actionJavaScript.trim();
							// only if what we got is not an empty string
							if (!("").equals(actionJavaScript)) {
								// if this action has been marked for redundancy avoidance
								if (action.getAvoidRedundancy()) {
									// add the action function to the action stringbuilder so it's before the event
									actionStringBuilder.append("function Action_" + action.getId() + "(ev) {\n"
									+ "  " + actionJavaScript.trim().replace("\n", "\n  ") + "\n"
									+ "  return true;\n"
									+ "}\n\n");
									// add an action function call to the event string builder
									eventStringBuilder.append("    if (!Action_" + action.getId() + "(ev)) return false;\n");
									//eventStringBuilder.append("    Action_" + action.getId() + "(ev);\n");
								} else {
									// go straight into the event
									eventStringBuilder.append("    " + actionJavaScript.trim().replace("\n", "\n    ") + "\n");
								}
							}

						}

					} catch (Exception ex) {

						// print a commented message
						eventStringBuilder.append("//    Error creating JavaScript for action " + action.getId() + " : " + ex.getMessage() + "\n");

					}

				}
				// close the try/catch
				if (control == null) {
					// page
					eventStringBuilder.append("  } catch(ex) { Event_error('" + event.getType() + "',null,ex); }\n");
				} else {
					// control
					eventStringBuilder.append("  } catch(ex) { Event_error('" + event.getType() + "','" + control.getId() +  "',ex); }\n");
				}
				// close event function
				eventStringBuilder.append("}\n\n");

				// add the action functions
				stringBuilder.append(actionStringBuilder);

				// add the event function
				stringBuilder.append(eventStringBuilder);
			}
		}
    }

    // build the event handling page JavaScript iteratively
    private void getEventHandlersJavaScript(RapidRequest rapidRequest, StringBuilder stringBuilder, Application application, List<Control> controls) throws JSONException {
    	// check there are some controls
    	if (controls != null) {
			// if we're at the root of the page
    		if (controls.equals(_controls)) {
    			// check for page events
    			if (_events != null) {
    				// loop page events and get js functions
        			for (Event event : _events) getEventJavaScriptFunction(rapidRequest, stringBuilder, application, null, event);
    			}
    		}
    		for (Control control : controls) {
    			// check event actions
    			if (control.getEvents() != null) {
    				// loop page events and get js functions
    				for (Event event : control.getEvents()) getEventJavaScriptFunction(rapidRequest, stringBuilder, application, control, event);
    			}
    			// now call iteratively for child controls (of this [child] control, etc.)
    			if (control.getChildControls() != null) getEventHandlersJavaScript(rapidRequest, stringBuilder, application, control.getChildControls());
    		}
    	}
    }

    // this method produces the start of the head (which is shared by the no permission respone)
    private String getHeadStart(Application application) {
    	return
    	"  <head>\n" +
		"    <title>" + Html.escape(_title) + " - by Rapid</title>\n" +
		"    <meta description=\"Created using Rapid - www.rapid-is.co.uk\"/>\n" +
		"    <meta charset=\"utf-8\"/>\n" +
		"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />\n" +
		(application != null ? "    <meta name=\"theme-color\" content=\"" + application.getStatusBarColour() + "\" />\n" : "" )+
		"    <link rel=\"icon\" href=\"favicon.ico\"></link>\n";
    }

    // this private method produces the head of the page which is often cached, if resourcesOnly is true only page resources are included which is used when sending no permission
	private String getHeadLinks(RapidHttpServlet rapidServlet, Application application, boolean isDialogue) throws JSONException {

		// create a string builder containing the head links
    	StringBuilder stringBuilder = new StringBuilder(getHeadStart(application));

		// if you're looking for where the jquery link is added it's the first resource in the page.control.xml file
		stringBuilder.append("    " + getResourcesHtml(application, false).trim() + "\n");

		// add a JavaScript block with important global variables - this removed by the pagePanel loader and navigation action when showing dialogues, by matching to the various variables so be careful changing anything below
		stringBuilder.append("    <script type='text/javascript'>\n");
		if (application != null) {
			stringBuilder.append("var _appId = '" + application.getId() + "';\n");
			stringBuilder.append("var _appVersion = '" + application.getVersion() + "';\n");
		}
		stringBuilder.append("var _pageId = '" + _id + "';\n");
		stringBuilder.append("var _pageName = '" + _name.replace("'", "\\'") + "';\n");
		stringBuilder.append("var _pageTitle = '" + _title.replace("'", "\\'") + "';\n");
		// this flag indicates if the Rapid Mobile client is regaining the foreground
		stringBuilder.append("var _mobileResume = false;\n");
		// this flag indicates if any controls are loading asynchronously and the page load method can't be called
		stringBuilder.append("var _loadingControls = 0;\n");
		stringBuilder.append("var _loadingPages = [];\n");
		stringBuilder.append("    </script>\n");

		return stringBuilder.toString();

    }

	// this private method writes JS specific to the user
	private void writeUserJS(Writer writer, RapidRequest rapidRequest, Application application, User user, Boolean download) throws RapidLoadingException, IOException, JSONException {

		// open js
		writer.write("    <script type='text/javascript'>\n");
		// if download
		if (download) {
			// just write RapidMobile
			//writer.write("var _userName = 'RapidMobile';\n");
			// print user - the above is turned off for now until enough people download Rapid Mobile 2.4.1.3 which can send the authenticated user
			if (user != null) writer.write("var _userName = '" + user.getName() + "';\n");
		} else {
			// print user
			if (user != null) writer.write("var _userName = '" + user.getName() + "';\n");
			// get app page variables
			List<String> pageVariables = application.getPageVariables(rapidRequest.getRapidServlet().getServletContext());
			// if we got some
			if (pageVariables != null) {
				// prepare json to hold page variables
				JSONObject jsonPageVariables = new JSONObject();
				// loop them
				for (String pageVariable : pageVariables) {
					// look for a value in the session
					String value = (String) rapidRequest.getSessionAttribute(pageVariable);
					// if we got one add it
					if (value != null) jsonPageVariables.put(pageVariable, value);
				}
				// write page variables
				writer.write("var _pageVariables_" + _id + " = " + jsonPageVariables + ";\n");
			}
		}
		// close js
		writer.write("    </script>\n");
	}

	// this private method produces the head of the page which is often cached, if resourcesOnly is true only page resources are included which is used when sending no permission
	private String getHeadCSS(RapidRequest rapidRequest, Application application, boolean isDialogue) throws JSONException {

		// create an empty string builder
    	StringBuilder stringBuilder = new StringBuilder();

		// fetch all page control styles
		String pageCss = getAllCSS(rapidRequest.getRapidServlet().getServletContext(), application);

		// only if there is some
		if (pageCss.length() > 0) {

			// open style blocks
			stringBuilder.append("    <style>\n");

			// if live we're going to try and minify
			if (application.getStatus() == Application.STATUS_LIVE) {
				try {
					// get string to itself minified
					pageCss = Minify.toString(pageCss, Minify.CSS, "Page head CSS");
				} catch (IOException ex) {
					// add error and resort to unminified
					pageCss = "\n/*\n\n Failed to minify the css : " + ex.getMessage() + "\n\n*/\n\n" + pageCss;
				}
			} else {
				// prefix with minify message
				pageCss = "\n/* The code below is minified for live applications */\n\n" + pageCss;

			}
			// add it to the page
			stringBuilder.append(pageCss);
			// close the style block
			stringBuilder.append("    </style>\n");

		}

		// return it
		return stringBuilder.toString();

    }

	// this private method produces the head of the page which is often cached, if resourcesOnly is true only page resources are included which is used when sending no permission
	private String getHeadReadyJS(RapidRequest rapidRequest, Application application, boolean isDialogue, FormAdapter formAdapter) throws JSONException {

		// make a new string builder just for the js (so we can minify it independently)
		StringBuilder jsStringBuilder = new StringBuilder();

		// add an extra line break for non-live applications
		if (application.getStatus() != Application.STATUS_LIVE) jsStringBuilder.append("\n");

		// get all controls
		List<Control> pageControls = getAllControls();

		// if we got some
		if (pageControls != null) {
			// loop them
			for (Control control : pageControls) {
				// get the details
				String details = control.getDetails();
				// check if null
				if (details != null) {
					// create a gloabl variable for it's details
					jsStringBuilder.append("var " + control.getId() + "details = " + details + ";\n");
				}
			}
			// add a line break again if we printed anything
			if (jsStringBuilder.length() > 0) jsStringBuilder.append("\n");
		}

		// initialise the form controls that need their values added in the dynamic part of the page script
		_formControlValues = new ArrayList<String>();
		// get all actions
		List<Action> actions = getAllActions();
		// loop them
		for (Action action : actions) {
			// if form type
			if ("form".equals(action.getType())) {
				// get the action type
				String type = action.getProperty("actionType");
				// if value copy
				if ("val".equals(type)) {
					// get control id
					String controlId = action.getProperty("dataSource");
					// add to collection if all in order
					if (controlId != null) _formControlValues.add(controlId);
				} else if ("sub".equals(type)) {
					// add sub as id
					_formControlValues.add("sub");
				} else if ("err".equals(type)) {
					// add err as id
					_formControlValues.add("err");
				} else if ("res".equals(type)) {
					// add res as id
					_formControlValues.add("res");
				}
			}
		}

		// initialise our pageload lines collections
		_pageloadLines = new ArrayList<String>();

		// get any control initJavaScript event listeners into he pageloadLine (goes into $(document).ready function)
		getPageLoadLines(_pageloadLines, _controls);

		// get a synchronised list to avoid concurrency exception in sort
		List<String> pageLoadLines = Collections.synchronizedList(_pageloadLines);

		// synchronised block for sorting in thread-safe manner
		synchronized (pageLoadLines) {
			// sort the page load lines
			Collections.sort(pageLoadLines, new Comparator<String>() {
				@Override
				public int compare(String l1, String l2) {
					if (l1.isEmpty()) return -1;
					if (l2.isEmpty()) return 1;
					char i1 = l1.charAt(0);
					char i2 = l2.charAt(0);
					return i2 - i1;
				}}
			);
		}

		// if there is a form adapter in place
		if (formAdapter != null) {
			// add a line to set any form values before the load event is run
			pageLoadLines.add("Event_setFormValues($.Event('setValues'));\n");
			// add an init form function - in extras.js
			pageLoadLines.add("Event_initForm('" + _id + "');\n");
		}

		// check for page events (this is here so all listeners are registered by now) and controls (there should not be none but nothing happens without them)
		if (_events != null && _controls != null) {
			// loop page events
			for (Event event : _events) {
				// only if there are actually some actions to invoke
				if (event.getActions() != null) {
					if (event.getActions().size() > 0) {
						// page is a special animal so we need to do each of it's event types differently
						if ("pageload".equals(event.getType())) {
							// call the page load if safe to do so - controls with asynchronous loading will need to check and call this method themselves
							pageLoadLines.add("if (!_mobileResume) { if (_loadingControls < 1) { Event_pageload_" + _id + "($.Event('pageload')) } else { _loadingPages.push('" + _id + "');} };\n");
        				}
						// resume is also a special animal
						if ("resume".equals(event.getType())) {
							// fire the resume event immediately if there is no rapidMobile (it will be done by the Rapid Mobile app if present)
							pageLoadLines.add("if (!window['_rapidmobile']) Event_resume_" + _id + "($.Event('resume'));\n");
						}
						// reusable action is only invoked via reusable actions on other events - there is no listener
					}
				}
			}
		}

		// if there is a form adapter in place
		if (formAdapter != null) {
			// add a line to check the form now all load events have been run
			pageLoadLines.add("Event_checkForm();\n");
		}

		// if this is not a dialogue or there are any load lines
		if (!isDialogue || pageLoadLines.size() > 0) {

			// open the page loaded function
			jsStringBuilder.append("$(document).ready( function() {\n");

			// add a try
			jsStringBuilder.append("  try {\n");

			// print any page load lines such as initialising controls
			for (String line : pageLoadLines) jsStringBuilder.append("    " + line);

			// close the try
			jsStringBuilder.append("  } catch(ex) { $('body').html(ex.message || ex); }\n");

			// after 200 milliseconds show and trigger a window resize for any controls that might be listening (this also cuts out any flicker), we also call focus on the elements we marked for focus while invisible (in extras.js)
			jsStringBuilder.append("  window.setTimeout( function() {\n    $(window).resize();\n    $('body').css('visibility','visible');\n    $('[data-focus]').focus();\n  }, 200);\n");

			// end of page loaded function
			jsStringBuilder.append("});\n\n");

		}

		// return it
		return jsStringBuilder.toString();

    }

	// this private method produces the head of the page which is often cached, if resourcesOnly is true only page resources are included which is used when sending no permission
	private String getHeadJS(RapidRequest rapidRequest, Application application, boolean isDialogue) throws JSONException {

		// a string builder
		StringBuilder stringBuilder = new StringBuilder();

		// make a new string builder just for the js (so we can minify it independently)
		StringBuilder jsStringBuilder = new StringBuilder();

		// get all actions in the page
		List<Action> pageActions = getAllActions();

		// only proceed if there are actions in this page
		if (pageActions != null) {

			// loop the list of actions
			for (Action action : pageActions) {
				// if this is a form action
				// indentify potential redundancies before we create all the event handling JavaScript
				try {
					// look for any page javascript that this action may have
					String actionPageJavaScript = action.getPageJavaScript(rapidRequest, application, this, null);
					// print it here if so
					if (actionPageJavaScript != null) jsStringBuilder.append(actionPageJavaScript.trim() + "\n\n");
					// if this action adds redundancy to any others
					if (action.getRedundantActions() != null) {
						// loop them
						for (String actionId : action.getRedundantActions()) {
							// try and find the action
							Action redundantAction = getAction(actionId);
							// if we got one
							if (redundantAction != null) {
								// update the redundancy avoidance flag
								redundantAction.avoidRedundancy(true);
							}
						}
					} // redundantActions != null
				} catch (Exception ex) {
					// print the exception as a comment
					jsStringBuilder.append("// Error producing page JavaScript : " + ex.getMessage() + "\n\n");
				}

			} // action loop

			// add event handlers, staring at the root controls
			getEventHandlersJavaScript(rapidRequest, jsStringBuilder, application, _controls);
		}

		// if there was any js
		if (jsStringBuilder.length() > 0) {

			// check the application status
			if (application.getStatus() == Application.STATUS_LIVE) {
				try {
					// minify the js before adding
					stringBuilder.append(Minify.toString(jsStringBuilder.toString(),Minify.JAVASCRIPT, "Page JavaScript"));
				} catch (IOException ex) {
					// add the error
					stringBuilder.append("\n\n/* Failed to minify JavaScript : " + ex.getMessage() + " */\n\n");
					// add the js as is
					stringBuilder.append(jsStringBuilder);
				}
			} else {
				// add the js as is
				stringBuilder.append("/* The code below is minified for live applications */\n\n" + jsStringBuilder.toString().trim() + "\n");
			}

		}

		// get it into a string and insert any parameters
		String headJS = application.insertParameters(rapidRequest.getRapidServlet().getServletContext(), stringBuilder.toString());

		// return it
		return headJS;

    }

	// this routine will write the no page permission - used by both page permission and if control permission permutations fail to result in any html
	public void writeMessage(Writer writer, String title, String message) throws IOException {

		// write the head html without the JavaScript and CSS (index.css is substituted for us)
		writer.write(getHeadStart(null));

		// add the icon
		writer.write("    <link rel='icon' href='favicon.ico'></link>\n");

		// add the jQuery link
		writer.write("    <script type='text/javascript' src='scripts/" + Rapid.JQUERY + "'></script>\n");

		// add the index.css
		writer.write("    <link rel='stylesheet' type='text/css' href='styles/index.css'></link>\n");

		// close the head
		writer.write("</head>\n");

		// open the body
		writer.write("  <body>\n");

		// write no permission (body is closed at the end of this method)
		writer.write("<div class=\"image\"><img src=\"images/RapidLogo_60x40.png\" /></div><div class=\"title\"><span>" + title + "</span></div><div class=\"info\"><p>" + message + "</p></div>\n");

	}

	// this function interatively checks permission and writes control role html
	private void writeRoleControlHtml(Writer writer, List<String> userRoles, RoleControlHtml roleControlHtml) throws IOException {
		// if we have a roleControlHtml
		if (roleControlHtml != null) {
			// assume we haven't passed
			boolean passed = false;
			// check if it has roles
			if (roleControlHtml.getRoles() == null) {
				//  no roles it passes
				passed = true;
			} else {
				// loop the control roles first - likely to be smaller
				for (String controlRole : roleControlHtml.getRoles()) {
					// loop the user roles
					for (String userRole : userRoles) {
						// if they match
						if (controlRole.equalsIgnoreCase(userRole)) {
							// we've passed
							passed = true;
							// don't check any further
							break;
						}
					}
					// don't loop further if passed
					if (passed) break;
				}
			}
			// if we passed
			if (passed) {
				// write the start html if there is any
				if (roleControlHtml.getStartHtml() != null) writer.write(roleControlHtml.getStartHtml());
				// if there are children
				if (roleControlHtml.getChildren() != null) {
					// loop the children
					for (RoleControlHtml childRoleControlHtml  : roleControlHtml.getChildren()) {
						// print them
						writeRoleControlHtml(writer, userRoles, childRoleControlHtml);
					}
				}
				// write the end html if there is any
				if (roleControlHtml.getEndHtml() != null) writer.write(roleControlHtml.getEndHtml());
			} // control roles check
		} // roleControlHtml check
	}

	// this routine produces the entire page
	public void writeHtml(RapidHttpServlet rapidServlet, HttpServletResponse response, RapidRequest rapidRequest,  Application application, User user, Writer writer, boolean designerLink, boolean download) throws JSONException, IOException, RapidLoadingException {

		// this doctype is necessary (amongst other things) to stop the "user agent stylesheet" overriding styles
		writer.write("<!DOCTYPE html>\n");

		// open the html
		writer.write("<html lang=\"en\">\n");

		// get any theme
    	Theme theme = application.getTheme(rapidServlet.getServletContext());

		// check for undermaintenance status
		if (application.getStatus() == Application.STATUS_MAINTENANCE) {

			writeMessage(writer, "Rapid - Under maintenance", "This application is currently under maintenance. Please try again in a few minutes.");

		} else {

			// get the security
			SecurityAdapter security = application.getSecurityAdapter();

			// get any form adapter
	    	FormAdapter formAdapter = application.getFormAdapter();

			// assume the user has permission to access the page
			boolean gotPagePermission = true;

			try {

				// if this page has roles
				if (_roles != null) {
					if (_roles.size() > 0) {
						// check if the user has any of them
						gotPagePermission = security.checkUserRole(rapidRequest, _roles);
					}
				}

			} catch (SecurityAdapaterException ex) {

				rapidServlet.getLogger().error("Error checking for page roles", ex);

			}

			// check that there's permission
			if (gotPagePermission) {

				// whether we're rebulding the page for each request
		    	boolean rebuildPages = Boolean.parseBoolean(rapidServlet.getServletContext().getInitParameter("rebuildPages"));

		    	// check whether or not we rebuild
		    	if (rebuildPages) {
		    		// get fresh head links
		    		writer.write(getHeadLinks(rapidServlet, application, !designerLink));
		    		// write the user-specific JS
		    		writeUserJS(writer, rapidRequest, application, user, download);
		    		// get fresh js and css
		    		writer.write(getHeadCSS(rapidRequest, application, !designerLink));
		    		// open the script
					writer.write("    <script  type='text/javascript'>\n");
		    		// write the ready JS
		    		writer.write(getHeadReadyJS(rapidRequest, application, !designerLink, formAdapter));
		    	} else {
		    		// rebuild any uncached
		    		if (_cachedHeadLinks == null) _cachedHeadLinks = getHeadLinks(rapidServlet, application, !designerLink);
		    		if (_cachedHeadCSS == null) _cachedHeadCSS = getHeadCSS(rapidRequest, application, !designerLink);
		    		if (_cachedHeadReadyJS == null) _cachedHeadReadyJS = getHeadReadyJS(rapidRequest, application, !designerLink, formAdapter);
		    		// get the cached head links
		    		writer.write(_cachedHeadLinks);
		    		// write the user-specific JS
		    		writeUserJS(writer, rapidRequest, application, user, download);
		    		// get the cached head js and css
		    		writer.write(_cachedHeadCSS);
		    		// open the script
					writer.write("    <script  type='text/javascript'>\n");
					// write the ready JS
					writer.write(_cachedHeadReadyJS);
		    	}

		    	// if there is a form
				if (formAdapter != null) {

					// set no cache on this page
					RapidFilter.noCache(response);

					// a placeholder for any form id
					String formId = null;
					// a placeholder for any form values
					StringBuilder formValues = null;

		    		// first do the actions that could result in an exception
					try {

						// get the form details
						UserFormDetails formDetails = formAdapter.getUserFormDetails(rapidRequest);

						// if we got some
						if (formDetails != null) {

							// set the form id
							formId = formDetails.getId();

							// create the values string builder
							formValues = new StringBuilder();

							// set whether submitted
							formValues.append("var _formSubmitted = " + formDetails.getSubmitted() + ";\n\n");

							// start the form values object (to supply previous form values)
							formValues.append("var _formValues = {");

							// if form control values to set
							if (_formControlValues != null) {

								// loop then
								for (int i = 0; i < _formControlValues.size(); i++) {

									// get the control id
									String id = _formControlValues.get(i);

									// place holder for the value
									String value = null;

									// some id's are special
									if ("id".equals(id)) {
										// the submission message
										value = formDetails.getId();
									} else if ("sub".equals(id)) {
										// the submission message
										value = formDetails.getSubmitMessage();
									} else if ("err".equals(id)) {
										// the submission message
										value = formDetails.getErrorMessage();
									} else if ("res".equals(id)) {
										// the submission message
										value = formDetails.getPassword();
									} else {
										// lookup the value
										value = formAdapter.getFormControlValue(rapidRequest, formId, id, false);
									}

									// if we got one
									if (value != null) {
										// escape it and enclose it
										value = value.replace("\\", "\\\\").replace("'", "\\'").replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "");
										// add to object
										formValues.append("'" + id + "':'" + value + "'");
										// add comma
										formValues.append(",");
									}
								}
							}

							// close it
							formValues.append("'id':_formId};\n\n");

							// start the set form values function
							formValues.append("function Event_setFormValues(ev) {");

							// get any form page values
							FormPageControlValues formControlValues = formAdapter.getFormPageControlValues(rapidRequest, formId, _id);

							// if there are any
							if (formControlValues != null) {
								if (formControlValues.size() > 0) {

									// add a line break
									formValues.append("\n");

									// loop the values
									for (FormControlValue formControlValue : formControlValues) {

										// get the control
										Control pageControl = getControl(formControlValue.getId());

										// if we got one
										if (pageControl != null) {

											// get the value
											String value = formControlValue.getValue();
											// assume using setData
											String function = "setData_" + pageControl.getType();
											// get the json properties for the control
											JSONObject jsonControl = rapidServlet.getJsonControl(pageControl.getType());
											// if we got some
											if (jsonControl != null) {
												// look for the formSetRunTimePropertyType
												String formSetRunTimePropertyType = jsonControl.optString("formSetRunTimePropertyType", null);
												// if we got one update the function to use it
												if (formSetRunTimePropertyType != null) function = "setProperty_" + pageControl.getType() + "_" + formSetRunTimePropertyType;
											}
											// get any control details
											String details = pageControl.getDetailsJavaScript(application, this);
											// if null update to string
											if (details == null) details = null;
											// if there is a value use the standard setData for it (this might change to something more sophisticated at some point)
											if (value != null) formValues.append("  if (window[\""+ function + "\"]) " + function + "(ev, '" + pageControl.getId() + "', null, " + details + ", '" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "") + "');\n");
										}
									}
								}
							}
							// close the function
							formValues.append("};\n\n");

							// write the form values
				    		writer.write(formValues.toString());

						} else {

							// set whether submitted
							writer.write("var _formSubmitted = false;\n\n");

							// a dummy setFormValues method
							writer.write("function Event_setFormValues(ev) {}\n\n");

						}

						// write the form id into the page - not necessary for dialogues
			    		if (designerLink) writer.write("var _formId = '" + formId + "';\n\n");

					} catch (Exception ex) {
						// log the error
						rapidServlet.getLogger().error("Error create page form values", ex);
						// write a dummy Event_setFormValues with alert and redirect to start page
						writer.write("var _formSubmitted = false;\nfunction Event_setFormValues() {\n  alert('Error with form values : " + ex.getMessage().replace("'", "\\'") + "');\n  window.location.href='~?a=" + application.getId() + "'\n};\n\n");
					}

				}

				if (rebuildPages) {
					// write the ready JS
		    		writer.write(getHeadJS(rapidRequest, application, !designerLink));
				} else {
					// get the rest of the cached JS
					if (_cachedHeadJS == null) _cachedHeadJS = getHeadJS(rapidRequest, application, !designerLink);
					// write the ready JS
		    		writer.write(_cachedHeadJS);
				}

				// close the script
				writer.write("\n    </script>\n");

		    	// close the head
		    	writer.write("  </head>\n");

				// start the body
		    	writer.write("  <body id='" + _id + "' style='visibility:hidden;'" + (_bodyStyleClasses == null ? "" : " class='" + _bodyStyleClasses + "'") + ">\n");

		    	// if there was a theme and we're not hiding the header / footer
		    	if (theme != null && !_hideHeaderFooter) {
		    		// get any header html
		    		String headerHtml = theme.getHeaderHtml();
		    		// write the header html if there is something to write
		    		if (headerHtml != null) if (headerHtml.length() > 0) writer.write(headerHtml);
		    	}

		    	// start the form if in use (but not for dialogues and other cases where the page is partial)
		    	if (formAdapter != null && designerLink) {
		    		writer.write("    <form id='" + _id + "_form' action='~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + _id + "' method='POST'>\n");
		    		writer.write("      <input type='hidden' name='csrfToken' value='" + rapidRequest.getCSRFToken() + "' />\n");
		    		writer.write("      <input type='hidden' id='" + _id +  "_hiddenControls' name='" + _id +  "_hiddenControls' />\n");
		    	}

				// a reference for the body html
				String bodyHtml = null;

				// check we have _rolesHtml - this has been depreciated since 2.3.5.3 but older page files may still have it this way
				if (_rolesHtml != null) {

					// get the users roles
					List<String> userRoles = user.getRoles();

					if (userRoles != null) {

						// loop each roles html entry
						for (RoleHtml roleHtml : _rolesHtml) {

							// get the roles from this combination
							List<String> roles = roleHtml.getRoles();

							// assume not roles are required (this will be updated if roles are present)
							int rolesRequired = 0;

							// keep a running count for the roles we have
							int gotRoleCount = 0;

							// if there are roles to check
							if (roles != null) {

								// update how many roles we need our user to have
								rolesRequired = roles.size();

								// check whether we need any roles and that our user has any at all
								if (rolesRequired > 0) {
									// check the user has as many roles as this combination requires
									if (userRoles.size() >= rolesRequired) {
										// loop the roles we need for this combination
										for (String role : roleHtml.getRoles()) {
											// check this role
											if (userRoles.contains(role)) {
												// increment the got role count
												gotRoleCount ++;
											} // increment the count of required roles

										} // loop roles

									} // user has enough roles to bother checking this combination

								} // if any roles are required

							} // add roles to check

							// if we have all the roles we need
							if (gotRoleCount == rolesRequired) {
								// use this html
								bodyHtml = roleHtml.getHtml();
								// no need to check any further
								break;
							}

						} // html role combo loop

					} // got userRoles

				} else {

					// check if this page has role control html
					if (_roleControlHtml == null) {

						// set this to the whole html body
						bodyHtml = _htmlBody;

					} else {

						// get the users roles
						List<String> userRoles = user.getRoles();

						// if the user has roles
						if (userRoles != null) {
							// if the application is live
							if (application.getStatus() == Application.STATUS_LIVE) {
								// write straight to the page writer
								writeRoleControlHtml(writer, userRoles, _roleControlHtml);
								// set bodyHtml to empty string indicating we had permission
								bodyHtml = "";
							} else {
								// make a StringWriter
								StringWriter swriter = new StringWriter();
								// write straight to the StringWriter
								writeRoleControlHtml(swriter, userRoles, _roleControlHtml);
								// set bodyHtml to what what written so it will be pretty printed
								bodyHtml = swriter.toString();
							}

						} // user has roles

					} // this page has role control html

				} // if our users have roles and we have different html for roles

				// check if we got any body html via the roles
				if (bodyHtml == null) {

					// didn't get any body html, show no permission
					writeMessage(writer, "Rapid - No permission", "You do not have permssion to view this page");

				} else {

					// check there is something to write - will be an empty string if already written by newer user roles code
					if (bodyHtml.length() > 0) {
						// check the status of the application
						if (application.getStatus() == Application.STATUS_DEVELOPMENT) {
							// pretty print
							writer.write(Html.getPrettyHtml(bodyHtml.trim()));
						} else {
							// no pretty print
							writer.write(bodyHtml.trim());
						}
					}

					// close the form
					if (formAdapter != null && designerLink) writer.write("    </form>\n");

				} // got body html check

			} else {

				// no page permission
				writeMessage(writer, "Rapid - No permission", "You do not have permssion to view this page");

			} // page permission check

			try {

				// whether to include the designer link - dialogues and files in the .zip do not so no need to even check permission
				if (designerLink) {

					// assume not admin link
					boolean adminLinkPermission = false;

					// check for the design role, super is required as well if the rapid app
					if ("rapid".equals(application.getId())) {
						if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE) && security.checkUserRole(rapidRequest, Rapid.SUPER_ROLE)) adminLinkPermission = true;
					} else {
						if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) adminLinkPermission = true;
					}

					// if we had the admin link
					if (adminLinkPermission) {

						// create string builder for the links
						StringBuilder designLinkStringBuilder = new StringBuilder();
						// create a string builder for the jquery
						StringBuilder designLinkJQueryStringBuilder = new StringBuilder();
						// loop all of the controls
						for (Control control : getAllControls()) {
							// get the json control definition
							JSONObject jsonControl = rapidServlet.getJsonControl(control.getType());
							// definition check
							if ( jsonControl != null) {
								// look for the design link jquery
								String designLinkJQuery = jsonControl.optString("designLinkJQuery", null);
								// if we got any design link jquery
								if (designLinkJQuery != null) {
									// get the image title from the control name
									String title = control.getName();
									// escape any apostrophes
									if (title != null) title = title.replace("'", "&apos;");
									// add the link into the string builder
									designLinkStringBuilder.append("<a id='designLink_" + control.getId() + "' data-id='" + control.getId() + "' href='#'><img src='" + jsonControl.optString("image","images/penknife_24x24.png") + "' title='" + title +  "'/></a>\n");
									// trim the JQuery
									designLinkJQuery = designLinkJQuery.trim();
									// start with a . if not
									if (!designLinkJQuery.startsWith(".")) designLinkJQuery = "." + designLinkJQuery;
									// end with ; if not
									if (!designLinkJQuery.endsWith(";")) designLinkJQuery += ";";
									// add the jquery after the object reference
									designLinkJQueryStringBuilder.append("  $('#designLink_" + control.getId() + "')" + designLinkJQuery.replace("\n", "\n  ") + "\n");
								}
							}
						}

						// using attr href was the weirdest thing. Some part of jQuery seemed to be setting the url back to v=1&p=P1 when v=2&p=P2 was printed in the html
						writer.write(
						" <link rel='stylesheet' type='text/css' href='styles/designlinks.css'></link>"
						+ " <script type='text/javascript' src='scripts/designlinks.js'></script>"
						+	"<div id='designShow'></div>\n"
						+ "<div id='designLinks' style='display:none;'>"
				    	+ "<a id='designLink' href='#'><img src='images/gear_24x24.png' title='Open Rapid Design'/></a>\n"
				    	+ "<a id='designLinkNewTab' style='margin-left:-3px;' href='#'><img src='images/triangleRight_8x8.png' title='Open Rapid Design in a new tab'/></a>\n"
						+ designLinkStringBuilder.toString()
						+ "</div>"
				    	+ "<script type='text/javascript'>\n"
				    	+ "/* designLink */\n"
				    	+ "	 var _onDesignTable = false;\n"
				    	+ "$(document).ready( function() {\n"
				    	+ "	 var _onDesignLink = false;\n"
				    	+ "  $('#designShow').mouseover ( function(ev) {\n     $('#designLink').attr('href','design.jsp?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + _id + "'); $('#designLinkNewTab').attr('target','_blank').attr('href','design.jsp?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + _id + "'); $('#designLinks').show(); _onDesignLink = true;\n  });\n"
				    	+ "  $('#designLinks').mouseleave ( function(ev) {\n   $('#designLinks').hide(); _onDesignLink = false;\n  });\n"
				    	+ "	 $('body').click(function(){\n if(!_onDesignLink && !_onDesignTable) 	$('div.designData').hide();\n });"
				    	+ designLinkJQueryStringBuilder.toString()
				    	+ "});\n"
				    	+ "</script>\n");

					}

				}

			} catch (SecurityAdapaterException ex) {

				rapidServlet.getLogger().error("Error checking for the designer link", ex);

			} // design permssion check

		} // design link check

		// if there was a theme and we're not hiding the header / footer
    	if (theme != null && !_hideHeaderFooter) {
    		// get any header html
    		String footerHtml = theme.getFooterHtml();
    		// write the header html if there is something to write
    		if (footerHtml != null) if (footerHtml.length() > 0) writer.write(footerHtml);
    	}

		// add the remaining elements
		writer.write("  </body>\n</html>");

	}

	// gets the value of a condition used in the page visibility rules
	private String getConditionValue(RapidRequest rapidRequest, String formId, FormAdapter formAdapter, Application application, Value value) throws Exception {
		String[] idParts = value.getId().split("\\.");
		if (idParts[0].equals("System")) {
			// just check that there is a type
			if (idParts.length > 1) {
				// get the type from the second part
				String type = idParts[1];
				// the available system values are specified above getDataOptions in designer.js
				if ("app id".equals(type)) {
					// whether rapid mobile is present
					return application.getId();
				} else if ("app version".equals(type)) {
					// whether rapid mobile is present
					return application.getVersion();
				} else if ("page id".equals(type)) {
					// the page
					return _id;
				} else if ("mobile".equals(type)) {
					// whether rapid mobile is present
					return "false";
				} else if ("online".equals(type)) {
					// whether we are online (presumed true if no rapid mobile)
					return "true";
				} else if ("user".equals(type) || "user name".equals(idParts[1])) {
					// pass the field as a value
					return rapidRequest.getUserName();
				} else if ("field".equals(type)) {
					// pass the field as a value
					return value.getField();
				} else {
					// pass through as literal
					return idParts[1];
				}
			}  else {
				// return null
				return null;
			}
		} else if (idParts[0].equals("Session")) {
			// if there are enough if parts
			if (idParts.length > 1) {
				return (String) rapidRequest.getSessionAttribute(idParts[1]);
			} else {
				return null;
			}
		} else {
			// get the id of the value object (should be a control Id)
			String valueId = value.getId();
			// retrieve and return it from the form adapater, but not if it's hidden
			return formAdapter.getFormControlValue(rapidRequest, formId, valueId, true);
		}
	}

	// return a boolean for page visibility
	public boolean isVisible(RapidRequest rapidRequest, Application application, UserFormDetails userFormDetails) throws Exception {

		// get a logger
		Logger logger = rapidRequest.getRapidServlet().getLogger();

		// get the form adapter
		FormAdapter formAdapter = application.getFormAdapter();

		// if we have a form adapter and visibility conditions
		if (formAdapter == null) {

			// no form adapter always visible
			logger.debug("Page " + _id + " no form adapter, always visible ");

			return true;

		}  else {

			 if (userFormDetails == null) {

				// no user form details
				logger.debug("No user form details");

				return false;

			 } else if  (_simple) {

				// simple page always invisible on forms
				logger.debug("Page " + _id + " is a simple page, always hidden on forms");

				return false;

			 } else if (_formPageType == FORM_PAGE_TYPE_SAVE) {

				// save page always invisible on forms
				logger.debug("Page " + _id + " is a save page, always hidden on forms");

				return false;

			} else if (_formPageType == FORM_PAGE_TYPE_RESUME) {

				// resume page always invisible on forms
				logger.debug("Page " + _id + " is a resume page, always hidden on forms");

				return false;

			} else if (_formPageType == FORM_PAGE_TYPE_SUBMITTED && !userFormDetails.getShowSubmitPage()) {

				// requests for submitted page are denied if show submission is not true
				logger.debug("Page " + _id + " is a submitted page but the form has not been submitted yet");

				return false;

			} else if (_formPageType == FORM_PAGE_TYPE_ERROR && !userFormDetails.getError()) {

				// requests for error page are denied if no error
				logger.debug("Page " + _id + " is an error page but the form has not had an error yet");

				return false;

			} else if (_visibilityConditions == null) {

				// no _visibilityConditions always visible
				logger.debug("Page " + _id + " _visibilityConditions is null, always visible on forms");

				return true;

			} else if (_visibilityConditions.size() == 0) {

				// no _visibilityConditions always visible
				logger.debug("Page " + _id + " _visibilityConditions size is zero, always visible on forms");
				return true;

			} else {

				// log
				logger.trace("Page " + _id + " " + _visibilityConditions.size() + " visibility condition(s) " + " : " + _conditionsType);

				// assume we have failed all conditions
				boolean pass = false;

				// loop them
				for (Condition condition : _visibilityConditions) {

					// assume we have failed this condition
					pass = false;

					logger.trace("Page " + _id + " visibility condition " + " : " + condition);

					String value1 = getConditionValue(rapidRequest, userFormDetails.getId(), formAdapter, application, condition.getValue1());

					logger.trace("Value 1 = " + value1);

					String value2 = getConditionValue(rapidRequest, userFormDetails.getId(), formAdapter, application, condition.getValue2());

					logger.trace("Value 2 = " + value2);

					String operation = condition.getOperation();

					if (value1 == null) value1 = "";
					if (value2 == null) value2 = "";

					// pass is updated from false to true if conditions match
					if ("==".equals(operation)) {
						if (value1.equals(value2)) pass = true;
					} else if ("!=".equals(operation)) {
						if (!value1.equals(value2)) pass = true;
					} else {
						// the remaining conditions all work with numbers and must not be empty strings
						if (value1.length() > 0 && value2.length() > 0) {
							try {
								// convert to floats
								float num1 = Float.parseFloat(value1);
								float num2 = Float.parseFloat(value2);
								// check the conditions
								if (">".equals(operation)) {
									if ((num1 > num2)) pass = true;
								} else if (">=".equals(operation)) {
									if ((num1 >= num2)) pass = true;
								} else if ("<".equals(operation)) {
									if ((num1 < num2)) pass = true;
								} else if ("<=".equals(operation)) {
									if ((num1 <= num2)) pass = true;
								}
							} catch (Exception ex) {
								// something went wrong - generally in the conversion - return false
								logger.error("Error assessing page visibility page " + _id + " " + condition);
							} // try
						} // empty string check
					} // operation check

					// log
					logger.debug("Visibility condition for page " + _id + " : " + value1 + " " + condition.getOperation()+ " " + value2 + " , (" + condition + ") result is " + pass);

					// for the fast fail check whether we have an or
					if ("or".equals(_conditionsType)) {
						// if the conditions are or and we've just passed, we can stop checking further as we've passed in total
						if (pass) break;
					} else {
						// if the conditions are and and we've just failed, we can stop checking further as we've failed in total
						if (!pass) break;
					}

				} // condition loop

				// log result
				logger.debug("Page " + _id + " visibility check, " + _visibilityConditions.size() + " conditions, pass = " + pass);

				// if we failed set the page values to null
				if (!pass) formAdapter.setFormPageControlValues(rapidRequest, userFormDetails.getId(), _id, null);

				// return the pass
				return pass;

			} // simple, conditions, condition checks

		} // form adapter check

	}

	// return any reCaptcha controls in the page
	public List<Control> getRecaptchaControls() {
		if (_reCaptchaControls == null) {
			// make a new list
			_reCaptchaControls = new ArrayList<Control>();
			// loop page controls
			for (Control control : getAllControls()) {
				// if this is a recapthca add it
				if ("recaptcha".equals(control.getType())) {
					_reCaptchaControls.add(control);
				}
			}
		}
		return _reCaptchaControls;
	}

	// overrides

	@Override
	public String toString() {
		return "Page " + _id + " " + _name + " - " + _title;
	}

	// static methods

	// static function to load a new page
	public static Page load(ServletContext servletContext, File file) throws JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {

		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");

		// trace log that we're about to load a page
		logger.trace("Loading page from " + file);

		// open the xml file into a document
		Document pageDocument = XML.openDocument(file);

		// specify the xmlVersion as -1
		int xmlVersion = -1;

		// look for a version node
		Node xmlVersionNode = XML.getChildElement(pageDocument.getFirstChild(), "XMLVersion");

		// if we got one update the version
		if (xmlVersionNode != null) xmlVersion = Integer.parseInt(xmlVersionNode.getTextContent());

		// if the version of this xml isn't the same as this class we have some work to do!
		if (xmlVersion != XML_VERSION) {

			// get the page name
			String name = XML.getChildElementValue(pageDocument.getFirstChild(), "name");

			// log the difference
			logger.debug("Page " + name + " with version " + xmlVersion + ", current version is " + XML_VERSION);

			//
			// Here we would have code to update from known versions of the file to the current version
			//

			// check whether there was a version node in the file to start with
			if (xmlVersionNode == null) {
				// create the version node
				xmlVersionNode = pageDocument.createElement("XMLVersion");
				// add it to the root of the document
				pageDocument.getFirstChild().appendChild(xmlVersionNode);
			}

			// set the xml to the latest version
			xmlVersionNode.setTextContent(Integer.toString(XML_VERSION));

			//
			// Here we would use xpath to find all controls and run the Control.upgrade method
			//

			//
			// Here we would use xpath to find all actions, each class has it's own upgrade method so
			// we need to identify the class, instantiate it and call it's upgrade method
			// it's probably worthwhile maintaining a map of instantiated classes to avoid unnecessary re-instantiation
			//

			// save it
			XML.saveDocument(pageDocument, file);

			logger.debug("Updated " + name + " page version to " + XML_VERSION);

		}

		// get the unmarshaller from the context
		Unmarshaller unmarshaller = RapidHttpServlet.getUnmarshaller();

		// get a buffered reader for our page with UTF-8 file format
		BufferedReader br = new BufferedReader( new InputStreamReader( new FileInputStream(file), "UTF-8"));

		// try the unmarshalling
		try {

			// unmarshall the page
			Page page = (Page) unmarshaller.unmarshal(br);

			// log that the page was loaded
			logger.debug("Loaded page " + page.getId() + " - " + page.getName() + " from " + file);

			// close the buffered reader
			br.close();

			// return the page
			return page;

		} catch (JAXBException ex) {

			// close the buffered reader
			br.close();

			// log that the page had an error
			logger.error("Error loading page from " + file);

			// re-throw
			throw ex;

		}

	}

}
