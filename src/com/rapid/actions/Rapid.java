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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Application.Parameter;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Application.Resource;
import com.rapid.core.Application.Resources;
import com.rapid.core.Applications;
import com.rapid.core.Applications.Versions;
import com.rapid.core.Control;
import com.rapid.core.Device;
import com.rapid.core.Device.Devices;
import com.rapid.core.Email;
import com.rapid.core.Page;
import com.rapid.core.Pages.PageHeader;
import com.rapid.core.Theme;
import com.rapid.core.Workflow;
import com.rapid.core.Workflows;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.data.DatabaseConnection;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.Role;
import com.rapid.security.SecurityAdapter.Roles;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.security.SecurityAdapter.Users;
import com.rapid.server.Monitor;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.server.RapidServletContextListener;
import com.rapid.server.RapidSessionListener;
import com.rapid.server.filter.RapidFilter;
import com.rapid.soa.JavaWebservice;
import com.rapid.soa.SOAElementRestriction;
import com.rapid.soa.SOAElementRestriction.EnumerationRestriction;
import com.rapid.soa.SOAElementRestriction.MaxLengthRestriction;
import com.rapid.soa.SOAElementRestriction.MaxOccursRestriction;
import com.rapid.soa.SOAElementRestriction.MinLengthRestriction;
import com.rapid.soa.SOAElementRestriction.MinOccursRestriction;
import com.rapid.soa.SOASchema;
import com.rapid.soa.SOASchema.SOASchemaElement;
import com.rapid.soa.SQLWebservice;
import com.rapid.soa.Webservice;
import com.rapid.utils.Comparators;
import com.rapid.utils.Files;

public class Rapid extends Action {

	// private static finals
	private static final String TEST_EMAIL_TO = "test@dev.rapid-is.co.uk";
	private static final String TEST_EMAIL_FROM = "test@dev.rapid-is.co.uk";

	// instance variables

	private ArrayList<Action> _successActions, _errorActions, _childActions;

	// properties

	public ArrayList<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(ArrayList<Action> successActions) { _successActions = successActions; }

	public ArrayList<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(ArrayList<Action> errorActions) { _errorActions = errorActions; }

	// enterprise monitor
	protected Monitor _monitor = new Monitor();

	// constructors

	//jaxb
	public Rapid() { super(); }
	// designer
	public Rapid(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {

		// call the super constructor to the set the xml version
		super();

		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for success, error, and child actions
			if (!"successActions".equals(key) && !"errorActions".equals(key) && !"childActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some
		if (jsonSuccessActions != null) {
			_successActions = Control.getActions(rapidServlet, jsonSuccessActions);
		}

		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some
		if (jsonErrorActions != null) {
			_errorActions = Control.getActions(rapidServlet, jsonErrorActions);
		}

	}

	// internal methods

	private Application createApplication(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, String name, String version, String title, String type, boolean responsive, String themeType, String description) throws IllegalArgumentException, SecurityException, JAXBException, IOException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, SecurityAdapaterException, ParserConfigurationException, XPathExpressionException, RapidLoadingException, SAXException {

		String newAppId = Files.safeName(name).toLowerCase();
		String newAppVersion = Files.safeName(version);

		Application newApp = new Application();

		// populate the bare-minimum of properties
		newApp.setId(newAppId);
		newApp.setVersion(newAppVersion);
		newApp.setName(name);
		newApp.setTitle(title);
		newApp.setThemeType(themeType);
		newApp.setDescription(description);
		newApp.setCreatedBy(rapidRequest.getUserName());
		newApp.setCreatedDate(new Date());

		// get the context
		ServletContext servletContext = rapidServlet.getServletContext();

		// look for the new app security adapter parameter
		String newAppSecurityAdapter = servletContext.getInitParameter("newapp.securityAdpter");
		// check we got one
		if (newAppSecurityAdapter == null) {
			// no default so go for Rapid
			newApp.setSecurityAdapterType("rapid");
		} else {
			// set the one given
			newApp.setSecurityAdapterType(newAppSecurityAdapter);
		}

		// look for any new app parameters parameter
		String newAppParameters = servletContext.getInitParameter("newapp.parameters");
		// check we got one
		if (newAppParameters != null) {
			// trim it
			newAppParameters = newAppParameters.trim();
			// if it looks like an object
			if (newAppParameters.startsWith("{")) {
				// read to object
				JSONObject jsonNewAppParameterObject = new JSONObject(newAppParameters);
				// get keys iterator
				Iterator<String> keys = jsonNewAppParameterObject.keys();
				// get app parameters
				List<Parameter> parameters = newApp.getParameters();
				// loop keys
				while (keys.hasNext()) {
					// get key
					String key = keys.next();
					// get value
					String value = jsonNewAppParameterObject.getString(key);
					// add to app parameters
					parameters.add(new Parameter(key, value));
				}
			}
		}

		// if this is a form
		if ("F".equals(type)) {
			// set the isForm flag
			newApp.setIsForm(true);
			// add standard form adapter
			newApp.setFormAdapterType("rapid");
			// add form security
			newApp.setSecurityAdapterType("form");
		}

		// initialise the application
		newApp.initialise(servletContext, true);

		// initialise the list of actions
		List<String> actionTypes = new ArrayList<>();

		// get the JSONArray of actions
		JSONArray jsonActionTypes = rapidServlet.getJsonActions();

		// if there were some
		if (jsonActionTypes != null) {
			// loop them
			for (int i = 0; i < jsonActionTypes.length(); i++) {
				// get the action
				JSONObject jsonActionType = jsonActionTypes.getJSONObject(i);
				// get it's type
				String actionType = jsonActionType.getString("type");
				// add to list if addToNewApplications is set
				if (jsonActionType.optBoolean("addToNewApplications")) actionTypes.add(actionType);
				// add to list if app type is D and action has addToNewDesktopApplications
				if ("D".equals(type) && jsonActionType.optBoolean("addToNewDesktopApplications")) actionTypes.add(actionType);
				// add to list if app type is M and action has addToNewDesktopApplications
				if ("M".equals(type) && jsonActionType.optBoolean("addToNewMobileApplications")) actionTypes.add(actionType);
				// add to list if app type is F and action has addToNewDesktopApplications
				if ("F".equals(type) && jsonActionType.optBoolean("addToNewFormApplications")) actionTypes.add(actionType);
			}
		}

		// sort them again, just to be sure
		Collections.sort(actionTypes);

		// assign the list to the application
		newApp.setActionTypes(actionTypes);

		// initialise the list of controls
		List<String> controlTypes = new ArrayList<>();

		// get the JSONArray of controls
		JSONArray jsonControlTypes = rapidServlet.getJsonControls();

		// if there were some
		if (jsonControlTypes != null) {
			// loop them
			for (int i = 0; i < jsonControlTypes.length(); i++) {
				// get the control
				JSONObject jsonControlType = jsonControlTypes.getJSONObject(i);
				// get the type
				String controlType = jsonControlType.getString("type");
				// get whether to add to new applications
				boolean addNew = jsonControlType.optBoolean("addToNewApplications");
				String addResponsiveString = jsonControlType.optString("addToNewResponsiveApplications", null);
				boolean addResponsive = jsonControlType.optBoolean("addToNewResponsiveApplications");
				// if this is a responsive app
				if (responsive) {
					// add to list if addToNewApplications is set and not addToNewResponsiveApplications, or just addToNewResponsiveApplications
					if ((addNew &&  addResponsiveString == null) || addResponsive) controlTypes.add(controlType);
				} else {
					// add to list if addToNewApplications is set and addToNewResponsiveApplications is not set
					if (addNew && !addResponsive) controlTypes.add(controlType);
				}
				// add to list if app type is D and action has addToNewDesktopApplications
				if ("D".equals(type) && jsonControlType.optBoolean("addToNewDesktopApplications")) controlTypes.add(controlType);
				// add to list if app type is M and action has addToNewDesktopApplications
				if ("M".equals(type) && jsonControlType.optBoolean("addToNewMobileApplications")) controlTypes.add(controlType);
				// add to list if app type is F and action has addToNewDesktopApplications
				if ("F".equals(type) && jsonControlType.optBoolean("addToNewFormApplications")) controlTypes.add(controlType);
			}
		}

		// sort them again, just to be sure
		Collections.sort(controlTypes);

		// assign the list to the application
		newApp.setControlTypes(controlTypes);

		// save the application to file
		newApp.save(rapidServlet, rapidRequest, false);

		// get the security
		SecurityAdapter security = newApp.getSecurityAdapter();

		// check there is one
		if (security != null) {

			// update the request action to stop the default form adapter creating the wrong user
			rapidRequest.setActionName("newapp");

			// get the current user's record from the adapter (a centralised adapter may have them already)
			User user = security.getUser(rapidRequest);

			// if user is null - for example the default
			if (user == null) {
				// get the rapid application
				Application rapidApplication = rapidServlet.getApplications().get("rapid");
				// get the user object from rapid application
				User rapidUser = rapidApplication.getSecurityAdapter().getUser(rapidRequest);
				// create a new user based on the current user
				user = new User(rapidUser);
				// add the new user to the new application
				security.addUser(rapidRequest, user);
			}

			// add Admin and Design roles for the new user if required
			if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE)) security.addUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
			if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE)) security.addUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
		}

		return newApp;

	}

	private Workflow createWorkflow(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, String name, String title, String description) throws IllegalArgumentException, SecurityException, JAXBException, IOException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, SecurityAdapaterException, ParserConfigurationException, XPathExpressionException, RapidLoadingException, SAXException {

		String newId = Files.safeName(name).toLowerCase();

		Workflow newWorkflow = new Workflow();

		// populate the bare-minimum of properties
		newWorkflow.setId(newId);
		newWorkflow.setName(name);
		newWorkflow.setTitle(title);
		newWorkflow.setDescription(description);
		newWorkflow.setCreatedBy(rapidRequest.getUserName());
		newWorkflow.setCreatedDate(new Date());

		// initialise the list of actions
		List<String> actionTypes = new ArrayList<>();

		// get the JSONArray of actions
		JSONArray jsonActionTypes = rapidServlet.getJsonActions();

		// if there were some
		if (jsonActionTypes != null) {
			// loop them
			for (int i = 0; i < jsonActionTypes.length(); i++) {
				// get the action
				JSONObject jsonActionType = jsonActionTypes.getJSONObject(i);
				// add to list if addToNewApplications is set
				if (jsonActionType.optBoolean("canUseWorkflow")) actionTypes.add(jsonActionType.getString("type"));
			}
		}

		// sort them again, just to be sure
		Collections.sort(actionTypes);

		// assign the list to the application
		newWorkflow.setActionTypes(actionTypes);

		// save the application to file
		newWorkflow.save(rapidServlet, rapidRequest, false);

		return newWorkflow;

	}

	private JSONArray getSafeUsersJSON(RapidRequest rapidRequest, SecurityAdapter security, boolean gotRapidAdmin, Users users) throws JSONException, SecurityAdapaterException {
		// prepare a JSON array to send them in
		JSONArray jsonUsers = new JSONArray();
		// loop them
		for (User user : users) {
			// assume user is safe to add
			boolean addUser = true;

			// if we don't have the high-level Rapid Admin role
			if (!gotRapidAdmin) {
				// update the rapidRequest to this user
				rapidRequest.setUserName(user.getName());
				// if they have the admin role we are not allowed to se them
				if (security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE)) {
					// don't add them
					addUser = false;
				}
			}

			// if user is still safe to add
			if (addUser) {
				// create a JSON object for them
				JSONObject jsonUser = new JSONObject();
				// add the details of this user
				jsonUser.put("name", user.getName());
				jsonUser.put("description", user.getDescription());
				// add the object to the collection
				jsonUsers.put(jsonUser);
			}
		}
		return jsonUsers;
	}

	private List<SOAElementRestriction> getRestrictions(JSONArray jsonRestrictions) throws JSONException {
		// check we have something
		if (jsonRestrictions == null) {
			return null;
		} else {
			// instantiate the list we're making
			List<SOAElementRestriction> restrictions = new ArrayList<>();
			// loop what we got
			for (int i = 0; i < jsonRestrictions.length(); i++) {
				// fetch this item
				JSONObject jsonRestriction = jsonRestrictions.getJSONObject(i);
				// get the type
				String type = jsonRestriction.getString("type").trim();
				// get the value
				String value = jsonRestriction.optString("value");

				// check the type and construct appropriate restriction
				if ("MinOccursRestriction".equals(type)) restrictions.add(new MinOccursRestriction(Integer.parseInt(value)));
				if ("MaxOccursRestriction".equals(type)) restrictions.add(new MaxOccursRestriction(Integer.parseInt(value)));
				if ("MinLengthRestriction".equals(type)) restrictions.add(new MinLengthRestriction(Integer.parseInt(value)));
				if ("MaxLengthRestriction".equals(type)) restrictions.add(new MaxLengthRestriction(Integer.parseInt(value)));
				if ("EnumerationRestriction".equals(type)) restrictions.add(new EnumerationRestriction(value));

			}
			return restrictions;
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
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		// the javascript we're about to build
		String js = "";

		// if we are getting the versions for an application remove the appId and version parameters from the url so we don't end up with a version from an app we were previously looking at
		if ("GETVERSIONS".equals(getProperty("actionType"))) js += "if ($('#rapid_P0_C43').val() != $.getUrlVar('appId') && window.history && window.history.replaceState) window.history.replaceState(\"rapid\", \"\", \"~?a=rapid\");\n";

		// write success actions variable
		js += "  var successCallback = function(data) {\n";
		// check success actions
		if (_successActions != null) {
			if (_successActions.size() > 0) {
				for (Action action : _successActions) {
					js += "    " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n    ") + "\n";
				}
			}
		}
		js += "  };\n";

		// write error actions variable
		js += "  var errorCallback = function(server, status, message) {\n";
		// check whether is an error handling routin
		boolean gotErrorHandler = false;
		// check error actions
		if (_errorActions != null) {
			if (_errorActions.size() > 0) {
				gotErrorHandler = true;
				for (Action action : _errorActions) {
					js += "    " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n    ") + "\n";
				}
			}
		}
		// if there is no error handling routine insert our own
		if (!gotErrorHandler) js += "    alert('Rapid action failed : ' + ((server && server.responseText) || message));\n";
		// close the error handler
		js += "  };\n";

		// assume this action was called from the page and there is no control id nor details
		String controlId = "null";
		if (control != null) controlId = "'" + control.getId() + "'";

		// return the JavaScript
		js += "  Action_rapid(ev, '" + application.getId() + "','" + page.getId() + "'," + controlId + ",'" + getId() + "','" + getProperty("actionType") + "', " + getProperty("rapidApp") + ", successCallback, errorCallback);";

		return js;
	}

	private void recordMonitorEvent(RapidRequest rapidRequest, JSONObject jsonAction, String actionType) throws JSONException {

		// if monitor not alive or we are not recording everything then do nothing
		if(_monitor==null || !_monitor.isAlive(rapidRequest.getRapidServlet().getServletContext()) || !_monitor.isLoggingAll())
			return;

		// if the action is not one of the types being recorded then do nothing
		if(!"NEWAPP".equals(actionType) && !"DELAPP".equals(actionType) || !"DUPAPP".equals(actionType) || !"GETAPPS".equals(actionType))
			return;

		// get the data required for recording the event
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();
		String appId = jsonAction.getString("appId");
		String appVersion = jsonAction.optString("version", null);
		Application app = rapidServlet.getApplications().get(appId, appVersion);

		// record the event
		if("NEWAPP".equals(actionType)) {
			recordMonitorEvent(rapidRequest, actionType, jsonAction.getString("name").trim(), jsonAction.getString("newVersion").trim());
		} else if("DELAPP".equals(actionType)) {
			recordMonitorEvent(rapidRequest, actionType, app.getName(), app.getVersion());
		} else if("DUPAPP".equals(actionType)) {
			recordMonitorEvent(rapidRequest, actionType, app.getName(), jsonAction.getString("newVersion").trim());
		} else if("GETAPPS".equals(actionType)) {
			recordMonitorEventGetApps(rapidRequest, rapidServlet);
		}
	}

	private void recordMonitorEventGetApps(RapidRequest rapidRequest, RapidHttpServlet rapidServlet) {
		for (String id : rapidServlet.getApplications().getIds()) {
			for (String version : rapidServlet.getApplications().getVersions(id).keySet()) {
				Application application = rapidServlet.getApplications().get(id, version);
				recordMonitorEvent(rapidRequest, "", application.getName(), application.getVersion());
			}
		}
	}

	private void recordMonitorEvent(RapidRequest rapidRequest, String actionName, String appId, String appVersion) {
		_monitor.openEntry();
		_monitor.setActionName(actionName);
		_monitor.setAppId(appId);
		_monitor.setAppVersion(appVersion);
		_monitor.commitEntry(rapidRequest, null, 0);
	}

	@Override
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonAction) throws Exception {

		// get the rapid servlet for easy reference
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();

		// get the servel context for easy reference
		ServletContext servletContext = rapidServlet.getServletContext();

		// prepare our result object
		JSONObject result = new JSONObject();

		// get the action type
		String action = jsonAction.getString("actionType");

		// assume no new app id
		String newAppId = null;

		// assume we have no special rapid roles
		boolean rapidAdmin = false;
		boolean rapidDesign = false;
		boolean rapidUsers = false;

		// get the rapid security for the current user
		SecurityAdapter security = rapidRequest.getApplication().getSecurityAdapter();

		// if we got one
		if (security != null) {
			// check the admin role
			rapidAdmin = security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
			// if no admin check designer
			if (!rapidAdmin) rapidDesign = security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
			// if no admin check designer
			if (!rapidAdmin) rapidUsers = security.checkUserRole(rapidRequest, com.rapid.server.Rapid.USERS_ROLE);
		}

		// get the id of the app we're about to manipulate
		String appId = jsonAction.getString("appId");
		// get the version of the app we're about to manipulate
		String appVersion = jsonAction.optString("version", null);
		// get the application we're about to manipulate
		Application app = rapidServlet.getApplications().get(appId, appVersion);

		// record the event to the monitor database table
		recordMonitorEvent(rapidRequest, jsonAction, action);

		// only if we had an application and one of the special Rapid roles
		if (app != null && (rapidAdmin || rapidDesign || rapidUsers)) {

			/////////////////////////////////////////////////////////

			// There is still scope for extra security to arrange the actions into rapidAdmin only, rapidAdmin or rapidDesign, and rapidAdmin or rapidUsers

			/////////////////////////////////////////////////////////

			// recreate the rapid request using the application we wish to manipulate
			RapidRequest rapidActionRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);

			// get the application security
			security = app.getSecurityAdapter();

			// check the action
			if ("GETAPPS".equals(action)) {

				// create a json array for holding our apps
				JSONArray jsonApps = new JSONArray();

				// get a sorted list of the application ids
				for (String id : rapidServlet.getApplications().getIds()) {

					// loop the versions
					for (String version : rapidServlet.getApplications().getVersions(id).keySet()) {

						// get the this application version
						Application application = rapidServlet.getApplications().get(id, version);

						// get the security for this application
						security = application.getSecurityAdapter();

						// now emulate the app we are looping
						RapidRequest appSecurityRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), application);

						// check app permission
						boolean permission = security.checkUserPassword(appSecurityRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword());

						// if we passed the password check
						if (permission) {
							// if we have rapid admin
							if (rapidAdmin) {
								// we need it in the app too
								permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.ADMIN_ROLE);
							} else {
								// if we have rapid design we need it in the app too
								if (rapidDesign) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.DESIGN_ROLE);
								// if we have rapid users we need it in the app too
								if (rapidUsers) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.USERS_ROLE);
							}
						}

						// check the user password
						if (permission) {

							// if app is rapid do a further check
							if (permission && "rapid".equals(application.getId())) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.SUPER_ROLE);

							// if we got permssion - add this application to the list
							if (permission) {
								// create a json object
								JSONObject jsonApplication = new JSONObject();
								// add the details we want
								jsonApplication.put("value", application.getId());
								jsonApplication.put("text", application.getName() + " - " + application.getTitle());
								// add the object to the collection
								jsonApps.put(jsonApplication);
								// no need to check any further versions
								break;
							}


						} // password check

					} // version loop

				} // app loop

				// add the applications to the result
				result.put("applications", jsonApps);

				// fetch the workflows
				JSONArray jsonWorkflows = new JSONArray();
				// loop the workflows
				for (String id : rapidServlet.getWorkflows().getIds()) {
					// get the workflow
					Workflow workflow = rapidServlet.getWorkflows().get(id);
					// create a json object
					JSONObject jsonWorkflow = new JSONObject();
					// add the details we want
					jsonWorkflow.put("value", workflow.getId());
					jsonWorkflow.put("text", workflow.getName() + " - " + workflow.getTitle());
					// add the object to the collection
					jsonWorkflows.put(jsonWorkflow);
				}

				// add the workflows to the result
				result.put("workflows", jsonWorkflows);

				// only send these if we have rapid admin
				if (rapidAdmin) {

					// fetch the database drivers
					JSONArray jsonDatabaseDrivers = rapidServlet.getJsonDatabaseDrivers();
					// check we have some database drivers
					if (jsonDatabaseDrivers != null) {
						// prepare the database driver collection we'll send
						JSONArray jsonDrivers = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonDatabaseDrivers.length(); i++) {
							// get the item
							JSONObject jsonDatabaseDriver = jsonDatabaseDrivers.getJSONObject(i);
							// make a simpler send item
							JSONObject jsonDriver = new JSONObject();
							// add type
							jsonDriver.put("value", jsonDatabaseDriver.get("class"));
							// add name
							jsonDriver.put("text", jsonDatabaseDriver.get("name"));
							// add to collection
							jsonDrivers.put(jsonDriver);
						}
						// add the database drivers to the result
						result.put("databaseDrivers", jsonDrivers);
					}

					// fetch the connection adapters
					JSONArray jsonConnectionAdapters = rapidServlet.getJsonConnectionAdapters();
					// check we have some database drivers
					if (jsonConnectionAdapters != null) {
						// prepare the database driver collection we'll send
						JSONArray jsonAdapters = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonConnectionAdapters.length(); i++) {
							// get the item
							JSONObject jsonConnectionAdapter = jsonConnectionAdapters.getJSONObject(i);
							// make a simpler send item
							JSONObject jsonSendAdapter = new JSONObject();
							// add type
							jsonSendAdapter.put("value", jsonConnectionAdapter.get("class"));
							// add name
							jsonSendAdapter.put("text", jsonConnectionAdapter.get("name"));
							// add to collection
							jsonAdapters.put(jsonSendAdapter);
						}
						// add the database drivers to the result
						result.put("connectionAdapters", jsonAdapters);
					}

					// security adapter are in rapidAdmin or rapidUsers block further down

					// fetch the form adapters
					JSONArray jsonFormAdapters = rapidServlet.getJsonFormAdapters();
					// prepare the collection we'll send
					JSONArray jsonAdapters = new JSONArray();
					// create an entry for no form adapter
					JSONObject jsonSendAdapter = new JSONObject();
					// no value
					jsonSendAdapter.put("value", "");
					// None as text
					jsonSendAdapter.put("text", "Please select...");
					// add the None member first
					jsonAdapters.put(jsonSendAdapter);
					// check we have some database drivers
					if (jsonFormAdapters != null) {
						// loop what we have
						for (int i = 0; i < jsonFormAdapters.length(); i++) {
							// get the item
							JSONObject jsonAdapter = jsonFormAdapters.getJSONObject(i);
							// make a simpler send item
							 jsonSendAdapter = new JSONObject();
							// add type
							jsonSendAdapter.put("value", jsonAdapter.get("type"));
							// add name
							jsonSendAdapter.put("text", jsonAdapter.get("name"));
							// add to collection
							jsonAdapters.put(jsonSendAdapter);
						}
						// add the database drivers to the result
						result.put("formAdapters", jsonAdapters);
					}

					// create a json object for our email settings
					JSONObject jsonEmail = new JSONObject();
					// get the email settings object
					Email email = Email.getEmailSettings();
					// if not null
					if (email != null) {
						// add email settings properties
						jsonEmail.put("enabled", email.getEnabled());
						jsonEmail.put("host", email.getHost());
						jsonEmail.put("port", email.getPort());
						jsonEmail.put("security", email.getSecurity());
						jsonEmail.put("userName", email.getUserName());
						// for password use ******** or empty string if not set
						if (email.getPassword() == null) {
							jsonEmail.put("password", "");
						} else {
							if ("".equals(email.getPassword())) {
								jsonEmail.put("password", "");
							} else {
								jsonEmail.put("password", "********");
							}
						}
					}
					// add the email settings
					result.put("email", jsonEmail);

				} // rapid admin check

				// either rapidAdmin or rapidDesign
				if (rapidAdmin || rapidDesign) {

					// prepare the collection we'll send
					JSONArray jsonThemes = new JSONArray();
					// create an entry for no template
					JSONObject jsonTheme = new JSONObject();
					// no value
					jsonTheme.put("value", "");
					// None as text
					jsonTheme.put("text", "None");
					// add the None member first
					jsonThemes.put(jsonTheme);
					// get the themes
					List<Theme> themes = rapidServlet.getThemes();
					// check we have some
					if (themes != null) {
						// loop what we have
						for (Theme theme : themes) {
							// only if visible
							if (theme.getVisible()) {
								// make a simpler send item
								jsonTheme = new JSONObject();
								// add type
								jsonTheme.put("value", theme.getType());
								// add name
								jsonTheme.put("text", theme.getName());
								// add to collection
								jsonThemes.put(jsonTheme);
							}
						}
						// add the database drivers to the result
						result.put("themes", jsonThemes);
					}

					// process the actions and only send the name and type
					JSONArray jsonSendActions = new JSONArray();
					JSONArray jsonActions = rapidServlet.getJsonActions();
					for (int i = 0; i < jsonActions.length(); i++) {
						JSONObject jsonSysAction = jsonActions.getJSONObject(i);
						// only if visible
						if (jsonSysAction.optBoolean("visible",true)) {
							JSONObject jsonSendAction = new JSONObject();
							jsonSendAction.put("name", jsonSysAction.get("name"));
							jsonSendAction.put("type", jsonSysAction.get("type"));
							jsonSendAction.put("helpHtml", jsonSysAction.optString("helpHtml"));
							jsonSendActions.put(jsonSendAction);
						}
					}
					// add the actions to the result
					result.put("actions", jsonSendActions);

					// process the controls and only send the name and type for canUserAdd
					JSONArray jsonSendControls = new JSONArray();
					JSONArray jsonControls = rapidServlet.getJsonControls();
					for (int i = 0; i < jsonControls.length(); i++) {
						JSONObject jsonSysControl = jsonControls.getJSONObject(i);
						// only present controls users can add, that aren't hidden
						if (jsonSysControl.optBoolean("canUserAdd") && jsonSysControl.optBoolean("visible",true)) {
							JSONObject jsonSendControl = new JSONObject();
							jsonSendControl.put("name", jsonSysControl.getString("name"));
							jsonSendControl.put("type", jsonSysControl.getString("type"));
							jsonSendControl.put("helpHtml", jsonSysControl.optString("helpHtml"));
							jsonSendControls.put(jsonSendControl);
						}
					}
					// add the controls to the result
					result.put("controls", jsonSendControls);

					// add the devices
					result.put("devices", rapidServlet.getDevices());

				}

				// either rapidAdmin or rapidUsers
				if (rapidAdmin || rapidUsers) {

					// fetch the security adapters
					JSONArray jsonSecurityAdapters = rapidServlet.getJsonSecurityAdapters();
					// check we have some security adapters
					if (jsonSecurityAdapters != null) {
						// prepare the security adapter collection we'll send
						JSONArray jsonAdapters = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonSecurityAdapters.length(); i++) {
							// get the item
							JSONObject jsonSecurityAdapter = jsonSecurityAdapters.getJSONObject(i);
							// make a simpler send item
							JSONObject jsonSendAdapter = new JSONObject();
							// add type
							jsonSendAdapter.put("value", jsonSecurityAdapter.get("type"));
							// add name
							jsonSendAdapter.put("text", jsonSecurityAdapter.get("name"));
							// add canManageRoles
							jsonSendAdapter.put("canManageRoles", jsonSecurityAdapter.get("canManageRoles"));
							// add canManageUsers
							jsonSendAdapter.put("canManageUsers", jsonSecurityAdapter.get("canManageUsers"));
							// add canManageUserRoles
							jsonSendAdapter.put("canManageUserRoles", jsonSecurityAdapter.get("canManageUserRoles"));
							// add to collection
							jsonAdapters.put(jsonSendAdapter);
						}
						// add the security adapters to the result
						result.put("securityAdapters", jsonAdapters);
					}

				}

				// add the current userName to the result
				result.put("userName", rapidRequest.getUserName());

			} else if ("GETVERSIONS".equals(action)) {

				// prepare a json array we're going to include in the result
				JSONArray jsonVersions = new JSONArray();

				// get the versions
				Versions versions = rapidServlet.getApplications().getVersions(appId);

				// if there are any
				if (versions != null) {

					// loop the list of applications sorted by id (with rapid last)
					for (Application application : versions.sort()) {

						// get the application security
						security = application.getSecurityAdapter();

						// now emulate the app we are looping
						RapidRequest appSecurityRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), application);

						// check permission
						boolean permission = security.checkUserPassword(appSecurityRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword());

						// if we passed the password check
						if (permission) {
							// if we have rapid admin
							if (rapidAdmin) {
								// we need it in the app too
								permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.ADMIN_ROLE);
							} else {
								// if we have rapid design we need it in the app too
								if (rapidDesign) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.DESIGN_ROLE);
								// if we have rapid users we need it in the app too
								if (rapidUsers) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.USERS_ROLE);
							}
						}

						// check the user password
						if (permission) {

							// if app is rapid do a further check
							if (permission && "rapid".equals(application.getId())) permission = application.getSecurityAdapter().checkUserRole(appSecurityRequest, com.rapid.server.Rapid.SUPER_ROLE);

							// check the RapidDesign role is present in the users roles for this application
							if (permission) {

								// make a json object for this version
								JSONObject jsonVersion = new JSONObject();
								// add the version
								jsonVersion.put("value", application.getVersion());
								// derive the text
								String text = application.getVersion();
								// if live add some
								if (application.getStatus() == 1) text += " - (Live)";
								// add the title
								jsonVersion.put("text", text);
								// put the entry into the collection
								jsonVersions.put(jsonVersion);

							} // design permission

						} // password check

					} // versions loop

				} // got versions check

				// add the versions to the result
				result.put("versions", jsonVersions);

			} else if ("GETVERSION".equals(action)) {

				// password check
				if (security.checkUserPassword(rapidActionRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

					// check the users permission to admin this application
					boolean adminPermission = security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.ADMIN_ROLE);

					// if app is rapid do a further check
					if (adminPermission && "rapid".equals(app.getId())) adminPermission = security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.SUPER_ROLE);

					if (adminPermission) {

						// add the name
						result.put("name", app.getName());
						// add the version
						result.put("version", app.getVersion());
						// add the status
						result.put("status", app.getStatus());
						// add the title
						result.put("title", app.getTitle());
						// add the description
						result.put("description", app.getDescription());
						// add whether to show control ids
						result.put("pageNameIds", app.getPageNameIds());
						// add whether to show control ids
						result.put("showControlIds", app.getShowControlIds());
						// add whether to show action ids
						result.put("showActionIds", app.getShowActionIds());

						// add the form settings
						result.put("isForm", app.getIsForm());
						// add the form adapter
						result.put("formAdapterType", app.getFormAdapterType());
						// add if auto complete is disabled
						result.put("formDisableAutoComplete", app.getFormDisableAutoComplete());

						// add forms email setting
						result.put("formEmail", app.getFormEmail());
						// add forms from address
						result.put("formEmailFrom", app.getFormEmailFrom());
						// add forms email to address
						result.put("formEmailTo", app.getFormEmailTo());
						// add forms attachment type
						result.put("formEmailAttachmentType", app.getFormEmailAttachmentType());

						// add whether to email the customer
						result.put("formEmailCustomer", app.getFormEmailCustomer());
						// get app input controls
						List<Control> inputControls = app.getAllControls(servletContext, "input", "responsiveinput");
						// make a json array for them
						JSONArray jsonInputControls = new JSONArray();
						// if there were controls
						if (inputControls != null && inputControls.size() > 0) {
							// loop them
							for (Control control : inputControls) {
								// get the name
								String controlName = control.getName();
								// if there is one
								if (controlName != null && controlName.trim().length() > 0) {
									// make a json object for them
									JSONObject jsonControl = new JSONObject();
									// add text
									jsonControl.put("text", controlName);
									// add value
									jsonControl.put("value", control.getId());
									// add control to collection
									jsonInputControls.put(jsonControl);
								}
							}
						}
						// add the json control collection to the result
						result.put("inputControls", jsonInputControls);

						// add the customer email control id
						result.put("formEmailCustomerControlId", app.getFormEmailCustomerControlId());
						// add the customer email subject
						result.put("formEmailCustomerSubject", app.getFormEmailCustomerSubject());
						// add the customer email type, T or H
						result.put("formEmailCustomerType", app.getFormEmailCustomerType());
						// add the customer email body
						result.put("formEmailCustomerBody", app.getFormEmailCustomerBody());
						// add the customer email attachment type
						result.put("formEmailCustomerAttachmentType", app.getFormEmailCustomerAttachmentType());

						// add form file details
						result.put("formFile", app.getFormFile());
						// add form file type
						result.put("formFileType", app.getFormFileType());
						// add form file path
						result.put("formFilePath", app.getFormFilePath());
						// add form file username
						result.put("formFileUserName", app.getFormFileUserName());
						// add form file password
						result.put("formFilePassword", app.getFormFilePassword());
						// add form webservice
						result.put("formWebservice", app.getFormWebservice());
						// add form webservice URL
						result.put("formWebserviceURL", app.getFormWebserviceURL());
						// add form webservice type
						result.put("formWebserviceType", app.getFormWebserviceType());
						// add form webservice SOAP action
						result.put("formWebserviceSOAPAction", app.getFormWebserviceSOAPAction());

						// create a simplified array to hold the pages
						JSONArray jsonPages = new JSONArray();
						// retrieve the pages
						List<PageHeader> pages = app.getPages().getSortedPages();
						// check we have some
						if (pages != null) {
							for (PageHeader page : pages) {
								JSONObject jsonPage = new JSONObject();
								jsonPage.put("text", page.getName() + " - " + page.getTitle());
								jsonPage.put("value", page.getId());
								jsonPages.put(jsonPage);
							}
						}
						// add the pages
						result.put("pages", jsonPages);

						// add the start page Id
						result.put("startPageId", app.getStartPageId());

						// add the styles
						result.put("themeType", app.getThemeType());
						result.put("styles", app.getStyles());
						result.put("statusBarColour", app.getStatusBarColour());
						result.put("statusBarHighlightColour", app.getStatusBarHighlightColour());
						result.put("statusBarTextColour", app.getStatusBarTextColour());
						result.put("statusBarIconColour", app.getStatusBarIconColour());

						// add the security adapter
						result.put("securityAdapter", app.getSecurityAdapterType());
						// add whether there is device security
						result.put("deviceSecurity", app.getDeviceSecurity());
						// add whether password is retained on Rapid Mobile
						result.put("storePasswordDuration", app.getStorePasswordDuration());
						// add action types
						result.put("actionTypes", app.getActionTypes());
						// add control types
						result.put("controlTypes", app.getControlTypes());

						// create an array for the database connections
						JSONArray jsonDatabaseConnections = new JSONArray();

						// check we have some database connections
						if (app.getDatabaseConnections() != null) {
							// remember the index
							int index = 0;
							// loop and add to jsonArray
							for (DatabaseConnection dbConnection : app.getDatabaseConnections()) {
								// create an object for the database connection
								JSONObject jsonDBConnection = new JSONObject();
								// set the index as the value
								jsonDBConnection.put("value", index);
								// set the name as the text
								jsonDBConnection.put("text", dbConnection.getName());
								// add to our collection
								jsonDatabaseConnections.put(jsonDBConnection);
								// inc the index
								index ++;
							}
						}
						// add database connections
						result.put("databaseConnections", jsonDatabaseConnections);

						// create an array for the soa webservices
						JSONArray jsonWebservices = new JSONArray();

						// check we have some webservices
						if (app.getWebservices() != null) {
							// get a synchronised list for multithreaded sorting
							List<Webservice> webservices = Collections.synchronizedList(app.getWebservices());
							// synchronise this block for thread-safety
							synchronized (this) {
								// sort them by their name
								Collections.sort(webservices, new Comparator<Webservice>() {
									@Override
									public int compare(Webservice o1, Webservice o2) {
										if (o1 == null) {
											return -1;
										} else if (o2 == null) {
											return 1;
										} else {
											return Comparators.AsciiCompare(o1.getName(), o2.getName(), false);
										}
									}
								});
							}
							// loop and add to jsonArray
							for (Webservice webservice : webservices) {
								jsonWebservices.put(webservice.getName());
							}
						}
						// add webservices connections
						result.put("webservices", jsonWebservices);

						// create an array for the parameters
						JSONArray jsonParameters = new JSONArray();

						// check we have some webservices
						if (app.getParameters() != null) {
							// get a synchronised list for multithreaded sorting
							List<Parameter> parameters = Collections.synchronizedList(app.getParameters());
							// synchronize this block
							synchronized (this) {
								// sort them by their name
								Collections.sort(parameters, new Comparator<Parameter>() {
									@Override
									public int compare(Parameter o1, Parameter o2) {
										if (o1 == null) {
											return -1;
										} else if (o2 == null) {
											return 1;
										} else {
											return Comparators.AsciiCompare(o1.getName(), o2.getName(), false);
										}
									}
								});
							}
							// loop and add to jsonArray
							for (Parameter parameter : parameters) {
								jsonParameters.put(parameter.getName());
							}
						}
						// add webservices connections
						result.put("parameters", jsonParameters);

						// create an array for the resources
						JSONArray jsonResources = new JSONArray();

						// check we have some resources
						if (app.getAppResources() != null) {
							// get a synchronised list for multithreaded sorting
							List<Resource> resources = Collections.synchronizedList(app.getAppResources());
							// synchronize this block
							synchronized (this) {
								// sort them by their name
								Collections.sort(resources, new Comparator<Resource>() {
									@Override
									public int compare(Resource o1, Resource o2) {
										if (o1 == null) {
											return -1;
										} else if (o2 == null) {
											return 1;
										} else {
											return Comparators.AsciiCompare(o1.getName(), o2.getName(), false);
										}
									}
								});
							}
							// loop and adds2 to jsonArray
							for (Resource resource : resources) {
								jsonResources.put(resource.getName());
							}
						}
						// add webservices connections
						result.put("resources", jsonResources);

						// create an array for the app backups
						JSONArray jsonAppBackups = new JSONArray();

						// check we have some app backups
						if (app.getApplicationBackups(rapidServlet) != null) {
							// loop and add to jsonArray
							for (Application.Backup appBackup : app.getApplicationBackups(rapidServlet)) {
								// create the backup json object
								JSONObject jsonBackup = new JSONObject();
								// populate it
								jsonBackup.append("id", appBackup.getId());
								jsonBackup.append("date", rapidServlet.getLocalDateTimeFormatter().format(appBackup.getDate()));
								jsonBackup.append("user", appBackup.getUser());
								jsonBackup.append("size", appBackup.getSize());
								// add it
								jsonAppBackups.put(jsonBackup);
							}
						}
						// add webservices connections
						result.put("appbackups", jsonAppBackups);

						// add the max number of application backups
						result.put("appBackupsMaxSize", app.getApplicationBackupsMaxSize());

						// create an array for the page backups
						JSONArray jsonPageBackups = new JSONArray();

						// check we have some app backups
						if (app.getPageBackups(rapidServlet) != null) {
							// loop and add to jsonArray
							for (Application.Backup appBackup : app.getPageBackups(rapidServlet)) {
								// create the backup json object
								JSONObject jsonBackup = new JSONObject();
								// populate it
								jsonBackup.append("id", appBackup.getId());
								jsonBackup.append("page", appBackup.getName());
								jsonBackup.append("date", rapidServlet.getLocalDateTimeFormatter().format(appBackup.getDate()));
								jsonBackup.append("user", appBackup.getUser());
								jsonBackup.append("size", appBackup.getSize());
								// add it
								jsonPageBackups.put(jsonBackup);
							}
						}
						// add webservices connections
						result.put("pagebackups", jsonPageBackups);

						// add the max number of page backups
						result.put("pageBackupsMaxSize", app.getPageBackupsMaxSize());

					} // permission check

				} // password check

			} else if ("GETWORKFLOWS".equals(action)) {

				// create a json array for holding our workflows
				JSONArray jsonWorkflows = new JSONArray();

				// get a sorted list of the workflows
				for (String id : rapidServlet.getWorkflows().getIds()) {

						// get the this workflow
						Workflow workflow = rapidServlet.getWorkflows().get(id);

						/*

						// get the security
						SecurityAdapter security = application.getSecurityAdapter();

						// now emulate the app we are looping
						RapidRequest appSecurityRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), application);

						// check the user password
						if (security.checkUserPassword(appSecurityRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

							// check the users permission to admin this application
							boolean adminPermission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.ADMIN_ROLE);

							// if app is rapid do a further check
							if (adminPermission && "rapid".equals(application.getId())) adminPermission = application.getSecurityAdapter().checkUserRole(appSecurityRequest, com.rapid.server.Rapid.SUPER_ROLE);

							// if we got permssion - add this application to the list
							if (adminPermission) {
								// create a json object
								JSONObject jsonApplication = new JSONObject();
								// add the details we want
								jsonApplication.put("value", application.getId());
								jsonApplication.put("text", application.getName() + " - " + application.getTitle());
								// add the object to the collection
								jsonApps.put(jsonApplication);
								// no need to check any further versions
								break;
							}

						} // password check

						*/

						// create a json object
						JSONObject jsonWorkflow = new JSONObject();
						// add the details we want
						jsonWorkflow.put("value", workflow.getId());
						jsonWorkflow.put("text", workflow.getName() + " - " + workflow.getTitle());
						// add to workflows
						jsonWorkflows.put(jsonWorkflow);

				} // app loop

				// add the actions to the result
				result.put("workflows", jsonWorkflows);

			} else if ("GETWORKFLOW".equals(action)) {

				// get the id
				String id = jsonAction.getString("wfId");

				// get the this workflow
				Workflow workflow = rapidServlet.getWorkflows().get(id);

				/*

				// get the security
				SecurityAdapter security = application.getSecurityAdapter();

				// now emulate the app we are looping
				RapidRequest appSecurityRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), application);

				// check the user password
				if (security.checkUserPassword(appSecurityRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

					// check the users permission to admin this application
					boolean adminPermission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.ADMIN_ROLE);

					// if app is rapid do a further check
					if (adminPermission && "rapid".equals(application.getId())) adminPermission = application.getSecurityAdapter().checkUserRole(appSecurityRequest, com.rapid.server.Rapid.SUPER_ROLE);

					// if we got permssion - add this application to the list
					if (adminPermission) {
						// create a json object
						JSONObject jsonApplication = new JSONObject();
						// add the details we want
						jsonApplication.put("value", application.getId());
						jsonApplication.put("text", application.getName() + " - " + application.getTitle());
						// add the object to the collection
						jsonApps.put(jsonApplication);
						// no need to check any further versions
						break;
					}

				} // pass word check

				*/

				// add the details we want
				result.put("id", workflow.getId());
				result.put("name", workflow.getName());
				result.put("title", workflow.getTitle());
				result.put("description", workflow.getDescription());

			} else if ("GETDBCONN".equals(action)) {

				// must have rapid admin
				if (rapidAdmin) {

					// get the index
					int index = jsonAction.getInt("index");

					// get the database connections
					List<DatabaseConnection> dbConns = app.getDatabaseConnections();

					// check we have database connections
					if (dbConns != null) {
						// check the index we where given will retieve a database connection
						if (index > -1 && index < dbConns.size()) {
							// get the database connection
							DatabaseConnection dbConn = dbConns.get(index);
							// add the name
							result.put("name", dbConn.getName());
							// add the driver type
							result.put("driver", dbConn.getDriverClass());
							// add the connection adapter class
							result.put("connectionString", dbConn.getConnectionString());
							// add the connection adapter class
							result.put("connectionAdapter", dbConn.getConnectionAdapterClass());
							// add the user name
							result.put("userName", dbConn.getUserName());
							// add the password
							if ("".equals(dbConn.getPassword())) {
								result.put("password", "");
							} else {
								result.put("password", "********");
							}
						}
					}

				}

			} else if ("GETSOA".equals(action)) {

				// retain the JSON object which we will return
				JSONObject jsonWebservice;

				// get the index
				int index = jsonAction.getInt("index");

				// get the database connections
				List<Webservice> webservices = app.getWebservices();

				// check we have database connections
				if (webservices != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < webservices.size()) {
						// get the webservice from the collection
						Webservice webservice = webservices.get(index);
						// convert it into a json object
						jsonWebservice = new JSONObject(webservice);
						// add the type
						jsonWebservice.put("type", webservice.getClass().getSimpleName());
						// add the user to the response
						result.put("webservice", jsonWebservice);
					}
				}

			} else if ("GETSEC".equals(action)) {

				// get all of the available security adapters
				JSONArray jsonSecurityAdapters = rapidServlet.getJsonSecurityAdapters();

				// get the securityAdapter type from the jsonAction
				String securityAdapterType = jsonAction.optString("securityAdapter", null);

				// check we got one, might not have if drop down is not visible
				if (securityAdapterType == null) {

					// check we have some security adapters
					if (security != null && jsonSecurityAdapters != null) {
						// loop what we have
						for (int i = 0; i < jsonSecurityAdapters.length(); i++) {
							// get the item
							JSONObject jsonSecurityAdapter = jsonSecurityAdapters.getJSONObject(i);
							// if this is the type that came in
							if (security.getClass().getCanonicalName().equals(jsonSecurityAdapter.getString("class"))) {
								// add the adapter index as we know we don't have a drop down
								result.put("adapterIndex", i);
								// we're done
								break;
							}
						}
					}

				} else {

					// assume the current class has not been set
					String securityAdapterClass = "";

					// check we have some security adapters
					if (jsonSecurityAdapters != null) {
						// loop what we have
						for (int i = 0; i < jsonSecurityAdapters.length(); i++) {
							// get the item
							JSONObject jsonSecurityAdapter = jsonSecurityAdapters.getJSONObject(i);
							// if this is the type that came in
							if (securityAdapterType.equals(jsonSecurityAdapter.getString("type"))) {
								// retain the name
								securityAdapterClass = jsonSecurityAdapter.getString("class");
								// we're done
								break;
							}
						}
					}

					// if it's different from what came in
					if (!securityAdapterClass.equals(security.getClass().getCanonicalName())) {
						// set the new security adapter
						app.setSecurityAdapter(servletContext, securityAdapterType);
						// read it back again
						security = app.getSecurityAdapter();
					}

				} // got security adapter type

				// if we got the security
				if (security != null) {

					// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
					rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);

					// if we high-level Rapid Admin or high-level Rapid Users
					if (rapidAdmin || rapidUsers) {

						// get the roles
						Roles roles = security.getRoles(rapidRequest);

						// add the entire roles collection to the response
						result.put("roles", roles);

						// if we had some roles
						if (roles != null) {
							// prepapre a list of just the role names (not descriptions) - these go in the drop down for new roles that can be added
							List<String> roleNames = new ArrayList<>();
							// loop the roles
							for (Role role : roles) {
								// we need the RapidAdmin role to add RapidAdmin or RapidDesign
								if ((!com.rapid.server.Rapid.ADMIN_ROLE.equals(role.getName()) && !com.rapid.server.Rapid.DESIGN_ROLE.equals(role.getName())) || security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE))
									roleNames.add(role.getName());
							}
							// add the rolenames
							result.put("roleNames", roleNames);
						}

					} // high-level Rapid Admin check

					// get the users
					Users users = security.getUsers(rapidRequest);

					// if we got some add the users safely to the response (does not include password)
					if (users != null) result.put("users", getSafeUsersJSON(rapidRequest, security, rapidAdmin, users));

				} // got security

			} else if ("GETUSER".equals(action)) {

				// get the userName from the incoming json
				String userName = jsonAction.getString("userName");

				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);

				// derive whether this is the current user
				boolean currentUser = userName.toLowerCase().equals(rapidRequest.getUserName().toLowerCase());

				// now set the rapid request user to the user we want
				rapidRequest.setUserName(userName);

				// get all of the available security adapters
				JSONArray jsonSecurityAdapters = rapidServlet.getJsonSecurityAdapters();

				// check we have some security adapters
				if (security != null && jsonSecurityAdapters != null) {
					// loop what we have
					for (int i = 0; i < jsonSecurityAdapters.length(); i++) {
						// get the item
						JSONObject jsonSecurityAdapter = jsonSecurityAdapters.getJSONObject(i);
						// if this is the type that came in
						if (security.getClass().getCanonicalName().equals(jsonSecurityAdapter.getString("class"))) {
							// add the adapter index as we know we don't have a drop down
							result.put("adapterIndex", i);
							// we're done
							break;
						}
					}
				}

				// get the user
				User user = security.getUser(rapidRequest);

				// add the user name
				result.put("userName", userName);

				// add the user description
				result.put("description", user.getDescription());

				// add the user email
				result.put("email", user.getEmail());

				// set the default password mask
				String password = "********";

				// if the password is blank reflect this in what we send
				if ("".equals(user.getPassword())) password = "";

				// add a masked password
				result.put("password", password);

				// add isLocked status
				result.put("isLocked", user.getIsLocked());

				// add the device details
				result.put("deviceDetails", user.getDeviceDetails());

				// if we got one
				if (security != null) {

					// get the users roles
					List<String> roles = security.getUser(rapidRequest).getRoles();

					// add the users to the response
					result.put("roles", roles);

				} // got security

				// if this user record is for the logged in user
				result.put("currentUser", currentUser);

			} else if ("GETUSERS".equals(action)) {

				// add the current user
				result.put("currentUser", rapidRequest.getUserName());

				// if we got one
				if (security != null) {

					// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
					rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);

					// get the users of the selected app
					Users users = security.getUsers(rapidRequest);

					// if we got some
					if (users != null) {
						// put the list of users
						result.put("users", getSafeUsersJSON(rapidRequest, security, rapidAdmin, users));
					}

				} // got security

			} else if ("GETPARAM".equals(action)) {

				// retrieve the index
				int index = jsonAction.getInt("index");

				// create the json object
				JSONObject jsonParameter = new JSONObject();

				// check the parameters
				if (app.getParameters() != null) {

					// check we have the one requested
					if (index >= 0 && index < app.getParameters().size()) {

						// get the parameter
						Parameter parameter = app.getParameters().get(index);

						// add the name and value
						jsonParameter.put("name", parameter.getName());
						jsonParameter.put("description", parameter.getDescription());
						jsonParameter.put("value", parameter.getValue());

					}
				}

				// add the parameter to the result
				result.put("parameter", jsonParameter);

			} else if ("GETRESOURCE".equals(action)) {

				// retrieve the index
				int index = jsonAction.getInt("index");

				// create the json object
				JSONObject jsonParameter = new JSONObject();

				// check the resources
				if (app.getAppResources() != null) {

					// check we have the one requested
					if (index >= 0 && index < app.getAppResources().size()) {

						// get the parameter
						Resource resource = app.getAppResources().get(index);

						// add the name and value
						jsonParameter.put("name", resource.getName());
						jsonParameter.put("type", resource.getType());
						jsonParameter.put("value", resource.getContent());

					}
				}

				// add the parameter to the result
				result.put("resource", jsonParameter);

			} else if ("GETDEVICE".equals(action)) {

				// retrieve the index
				int index = jsonAction.getInt("index");

				// create the json object
				JSONObject jsonDevice = new JSONObject();

				// reference to all devices
				Devices devices = rapidServlet.getDevices();

				// check we have devices
				if (devices != null) {
					// check the index is ok
					if (index >= 0 && index < devices.size()) {

						// get the device
						Device device = rapidServlet.getDevices().get(index);

						// add the name and value
						jsonDevice.put("name", device.getName());
						jsonDevice.put("width", device.getWidth());
						jsonDevice.put("height", device.getHeight());
						jsonDevice.put("ppi", device.getPPI());
						jsonDevice.put("scale", device.getScale());

					}
				}

				// add the parameter to the result
				result.put("device", jsonDevice);

			} else if ("GETSESSIONS".equals(action)) {

				// create the json object
				JSONObject jsonDetails= new JSONObject();

				// create a json array
				JSONArray jsonSessions = new JSONArray();

				// get the sessions
				Map<String, HttpSession> sessions = RapidSessionListener.getSessions();

				// check we got some
				if (sessions != null) {

					// get a date formatter
					SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

					// loop them
					for (String key : sessions.keySet()) {
						// get the session
						HttpSession httpSession = sessions.get(key);
						// create object
						JSONObject jsonSession = new JSONObject();
						// add name
						jsonSession.put("name", httpSession.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME));
						// get a new date from the time
						Date accessTime = new Date(httpSession.getLastAccessedTime());
						// add last access
						jsonSession.put("access", df.format(accessTime));
						// add to collections
						jsonSessions.put(jsonSession);
					}

				}

				// add sessions
				jsonDetails.put("sessions", jsonSessions);

				return jsonDetails;

			} else if ("RELOADACTIONS".equals(action)) {

				// load actions and set the result message
				result.put("message", RapidServletContextListener.loadActions(servletContext) + " actions reloaded");

			} else if ("RELOADCONTROLS".equals(action)) {

				// load controls and set the result message
				result.put("message", RapidServletContextListener.loadControls(servletContext) + " controls reloaded");

			} else if ("RELOADAPPLICATIONS".equals(action)) {

				// load applications and set the result message
				result.put("message", RapidServletContextListener.loadApplications(servletContext) + " applications reloaded");

			} else if ("RELOADADAPTERS".equals(action)) {

				// ints for number of bits of each we're reloading
				int databaseDrivers = 0;
				int connectionAdapters = 0;
				int securityAdapters = 0;
				int forms = 0;
				int themes = 0;
				int devices = 0;

				// reload adapters and set the int for the result message
				databaseDrivers = RapidServletContextListener.loadDatabaseDrivers(servletContext);
				connectionAdapters = RapidServletContextListener.loadConnectionAdapters(servletContext);
				securityAdapters = RapidServletContextListener.loadSecurityAdapters(servletContext);
				forms =  RapidServletContextListener.loadFormAdapters(servletContext);
				themes = RapidServletContextListener.loadThemes(servletContext);
				devices = Devices.load(servletContext).size();

				// reset upload mime types so they are reloaded
				rapidServlet.resetMimeTypes();

				// show result message
				result.put("message",
					databaseDrivers + " database driver" + (databaseDrivers == 1 ? "" : "s") + ", " +
					connectionAdapters + " connection adapter" + (connectionAdapters == 1 ? "" : "s") + ", " +
					securityAdapters + " security adapter" + (securityAdapters == 1 ? "" : "s") + ", " +
					forms + " form adapter" + (forms == 1 ? "" : "s") + ", " +
					themes + " theme" + (themes == 1 ? "" : "s") + ", " +
					devices + " device" + (devices == 1 ? "" : "s") + " reloaded"
				);

			} else if ("RELOADPROCESSES".equals(action)) {

				int processes = 0;

				processes = RapidServletContextListener.loadProcesses(servletContext);

				// load processes and set the result message
				result.put("message", processes + " process" + (processes == 1 ? "" : "es") + " reloaded");

			} else if ("RELOADVERSION".equals(action)) {

				// look for an application file in the application folder
				File applicationFile = new File(app.getConfigFolder(servletContext) + "/application.xml");

				// close the existing app
				app.close(servletContext);

				// reload the application from file
				Application reloadedApplication = Application.load(servletContext, applicationFile);

				// replace it into the applications collection
				rapidServlet.getApplications().put(reloadedApplication);

				// load applications and set the result message
				result.put("message", "Version reloaded");

			} else if ("SAVEAPP".equals(action)) {

				// get the new values
				String id = Files.safeName(jsonAction.getString("name")).toLowerCase();
				String version = Files.safeName(jsonAction.getString("saveVersion"));
				int status = jsonAction.optInt("status");
				String name = jsonAction.getString("name");
				String title = jsonAction.getString("title");
				String description = jsonAction.getString("description");

				boolean isForm = jsonAction.optBoolean("isForm");
				String startPageId = jsonAction.optString("startPageId","");
				boolean pageNameIds = jsonAction.optBoolean("pageNameIds");
				boolean showControlIds = jsonAction.optBoolean("showControlIds");
				boolean showActionIds = jsonAction.optBoolean("showActionIds");

				String formAdapter = jsonAction.optString("formAdapter");
				boolean formDisableAutoComplete = jsonAction.optBoolean("formDisableAutoComplete");

				boolean formEmail = jsonAction.optBoolean("formEmail");
				String formEmailFrom = jsonAction.optString("formEmailFrom");
				String formEmailTo = jsonAction.optString("formEmailTo");
				String formEmailAttachmentType = jsonAction.optString("formEmailAttachmentType");

				boolean formEmailCustomer = jsonAction.optBoolean("formEmailCustomer");
				String formEmailCustomerControlId = jsonAction.optString("formEmailCustomerControlId");
				String formEmailCustomerSubject = jsonAction.optString("formEmailCustomerSubject");
				String formEmailCustomerType = jsonAction.optString("formEmailCustomerType");
				String formEmailCustomerBody = jsonAction.optString("formEmailCustomerBody");
				String formEmailCustomerAttachmentType = jsonAction.optString("formEmailCustomerAttachmentType");

				boolean formFile = jsonAction.optBoolean("formFile");
				String formFileType = jsonAction.optString("formFileType");
				String formFilePath = jsonAction.optString("formFilePath");
				String formFileUserName = jsonAction.optString("formFileUserName");
				String formFilePassword = jsonAction.optString("formFilePassword");

				boolean formWebservice = jsonAction.optBoolean("formWebservice");
				String formWebserviceURL = jsonAction.optString("formWebserviceURL");
				String formWebserviceType = jsonAction.optString("formWebserviceType");
				String formWebserviceSOAPAction = jsonAction.optString("formWebserviceSOAPAction");

				// assume we do not need to update the applications drop down
				boolean appUpdated = false;

				// if the id or version is now different we need to move it, rebuilding all the resources as we go
				if (!app.getId().equals(id) || !app.getVersion().equals(version)) {
					// copy the app to the id/version, returning the new one for saving
					app = app.copy(rapidServlet, rapidRequest, id, version, true, true);
					// mark that it has been updated
					appUpdated = true;
				}

				// update the values
				app.setName(name);
				app.setStatus(status);
				app.setTitle(title);
				app.setDescription(description);

				app.setIsForm(isForm);
				app.setStartPageId(startPageId);
				app.setPageNameIds(pageNameIds);
				app.setShowControlIds(showControlIds);
				app.setShowActionIds(showActionIds);

				app.setFormAdapterType(formAdapter);
				app.setFormDisableAutoComplete(formDisableAutoComplete);

				app.setFormEmail(formEmail);
				app.setFormEmailFrom(formEmailFrom);
				app.setFormEmailTo(formEmailTo);
				app.setFormEmailAttachmentType(formEmailAttachmentType);

				app.setFormEmailCustomer(formEmailCustomer);
				app.setFormEmailCustomerControlId(formEmailCustomerControlId);
				app.setFormEmailCustomerSubject(formEmailCustomerSubject);
				app.setFormEmailCustomerType(formEmailCustomerType);
				app.setFormEmailCustomerBody(formEmailCustomerBody);
				app.setFormEmailCustomerAttachmentType(formEmailCustomerAttachmentType);

				app.setFormFile(formFile);
				app.setFormFileType(formFileType);
				app.setFormFilePath(formFilePath);
				app.setFormFileUserName(formFileUserName);
				app.setFormFilePassword(formFilePassword);

				app.setFormWebservice(formWebservice);
				app.setFormWebserviceURL(formWebserviceURL);
				app.setFormWebserviceType(formWebserviceType);
				app.setFormWebserviceSOAPAction(formWebserviceSOAPAction);

				// save
				app.save(rapidServlet, rapidRequest, true);

				// add the application to the response
				result.put("message", "Application details saved");
				result.put("update", appUpdated);

			} else if ("SAVESTYLES".equals(action)) {

				String themeType = jsonAction.getString("themeType");
				String styles = jsonAction.getString("styles");
				String statusBarColour = jsonAction.optString("statusBarColour");
				String statusBarHighlightColour = jsonAction.optString("statusBarHighlightColour");
				String statusBarTextColour = jsonAction.optString("statusBarTextColour");
				String statusBarIconColour = jsonAction.optString("statusBarIconColour");

				app.setThemeType(themeType);
				app.setStyles(styles);
				app.setStatusBarColour(statusBarColour);
				app.setStatusBarHighlightColour(statusBarHighlightColour);
				app.setStatusBarTextColour(statusBarTextColour);
				app.setStatusBarIconColour(statusBarIconColour);

				app.save(rapidServlet, rapidRequest, true);

				// add the application to the response
				result.put("message", "Styles saved");

			} else if ("SAVEDBCONN".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();

				// remeber whether we found the connection
				boolean foundConnection = false;

				// check we have database connections
				if (dbConns != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < dbConns.size()) {
						// get the database connection
						DatabaseConnection dbConn = dbConns.get(index);

						// set the databse connection properties
						dbConn.setName(jsonAction.getString("name"));
						dbConn.setDriverClass(jsonAction.getString("driver"));
						dbConn.setConnectionString(jsonAction.getString("connectionString"));
						dbConn.setConnectionAdapterClass(jsonAction.getString("connectionAdapter"));
						dbConn.setUserName(jsonAction.getString("userName"));
						String password = jsonAction.getString("password");
						// only set the password if it's different from the default
						if (!"********".equals(password)) dbConn.setPassword(password);

						// reset the dbconn so the adapter is re-initialised with any changes
						dbConn.reset();

						// save the app
						app.save(rapidServlet, rapidRequest, true);

						foundConnection = true;

						// add the application to the response
						result.put("message", "Database connection saved");

					}
				}

				if (!foundConnection) result.put("message", "Database connection could not be found");

			} else if ("SAVESOASQL".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// get the webservices
				List<Webservice> webservices = app.getWebservices();

				// remeber whether we found the connection
				boolean foundWebservice = false;

				// check we have database connections
				if (webservices != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < webservices.size()) {
						// get the web service connection
						Webservice webservice = webservices.get(index);
						// check the type
						if (webservice.getClass() == SQLWebservice.class) {
							// cast to our type
							SQLWebservice sqlWebservice = (SQLWebservice) webservice;

							// set the webservice properties
							sqlWebservice.setName(jsonAction.getString("name").trim());
							sqlWebservice.setDatabaseConnectionIndex(jsonAction.getInt("databaseConnectionIndex"));

							// get the rest of the complex details
							JSONObject jsonDetails = jsonAction.getJSONObject("details");

							// set the sql
							sqlWebservice.setSQL(jsonDetails.getString("SQL").trim());

							// get the json request
							JSONObject jsonRequestSchmea = jsonDetails.optJSONObject("requestSchema");
							// check it
							if (jsonRequestSchmea != null) {
								// get the root element
								JSONObject jsonElement = jsonRequestSchmea.getJSONObject("rootElement");
								// get its name
								String elementName = jsonElement.optString("name").trim();
								// create the schema
								SOASchema requestSchema = new SOASchema(elementName);
								// get any child elements
								JSONArray jsonChildElements = jsonElement.optJSONArray("childElements");
								// check
								if (jsonChildElements != null) {
									// loop
									for (int i = 0; i < jsonChildElements.length(); i++) {
										// get child element
										JSONObject jsonChildElement = jsonChildElements.getJSONObject(i);
										// get child element name
										String childElementName = jsonChildElement.getString("name").trim();
										// get its data type
										int childElementDataType = jsonChildElement.optInt("dataType",1);
										// add child element to schema (and get a reference)
										SOASchemaElement soaChildElement = requestSchema.addChildElement(childElementName);
										// set the data type
										soaChildElement.setDataType(childElementDataType);
										// add any restrictions
										soaChildElement.setRestrictions(getRestrictions(jsonChildElement.optJSONArray("restrictions")));
									}
								}
								// set the schema property
								sqlWebservice.setRequestSchema(requestSchema);
							}

							// get the json response
							JSONObject jsonResponseSchema = jsonDetails.optJSONObject("responseSchema");
							// check it
							if (jsonResponseSchema != null) {
								// get the root element
								JSONObject jsonElement = jsonResponseSchema.getJSONObject("rootElement");
								// get its name
								String elementName = jsonElement.optString("name");
								// get if array
								boolean isArray = Boolean.parseBoolean(jsonElement.optString("isArray"));
								// create the schema
								SOASchema responseSchema = new SOASchema(elementName, isArray);
								// get any child elements
								JSONArray jsonChildElements = jsonElement.optJSONArray("childElements");
								// check
								if (jsonChildElements != null) {
									// loop
									for (int i = 0; i < jsonChildElements.length(); i++) {
										// get child element
										JSONObject jsonChildElement = jsonChildElements.getJSONObject(i);
										// get child element name
										String childElementName = jsonChildElement.getString("name").trim();
										// get child element field
										String childElementField = jsonChildElement.optString("field","");
										// get its data type
										int childElementDataType = jsonChildElement.optInt("dataType",1);
										// add child element to schema (and get reference)
										SOASchemaElement soaChildElement = responseSchema.addChildElement(childElementName);
										// set field
										soaChildElement.setField(childElementField);
										// set data type
										soaChildElement.setDataType(childElementDataType);
										// add any restrictions
										soaChildElement.setRestrictions(getRestrictions(jsonChildElement.optJSONArray("restrictions")));
									}
								}
								// set the schema property
								sqlWebservice.setResponseSchema(responseSchema);
							}

							// save the app
							app.save(rapidServlet, rapidRequest, true);

							foundWebservice = true;

							// add the application to the response
							result.put("message", "SQL webservice saved");
						}
					}
				}

				if (!foundWebservice) result.put("message", "SQL webservice could not be found");

			} else if ("SAVESOAJAVA".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// get the webservices
				List<Webservice> webservices = app.getWebservices();

				// remeber whether we found the connection
				boolean foundWebservice = false;

				// check we have database connections
				if (webservices != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < webservices.size()) {
						// get the web service connection
						Webservice webservice = webservices.get(index);
						// check the type
						if (webservice.getClass() == JavaWebservice.class) {

							// cast to our type
							JavaWebservice javaWebservice = (JavaWebservice) webservice;

							// set the webservice properties
							javaWebservice.setName(jsonAction.getString("name").trim());
							javaWebservice.setClassName(jsonAction.getString("className").trim());

							// save the app
							app.save(rapidServlet, rapidRequest, true);

							foundWebservice = true;

							// add the application to the response
							result.put("message", "Java webservice saved");
						}
					}
				}

				if (!foundWebservice) result.put("message", "Java webservice could not be found");

			} else if ("SAVESECURITYADAPT".equals(action)) {

				String securityAdapter = jsonAction.getString("securityAdapter").trim();

				boolean deviceSecurity = jsonAction.optBoolean("deviceSecurity");

				String storePasswordDuration = jsonAction.optString("storePasswordDuration");

				app.setSecurityAdapterType(securityAdapter);

				app.setDeviceSecurity(deviceSecurity);

				app.setStorePasswordDuration(storePasswordDuration);

				app.save(rapidServlet, rapidRequest, true);

				// add the application to the response
				result.put("message", "Security adapter saved");

			} else if ("SAVEACTIONS".equals(action)) {

				JSONArray jsonActionTypes = jsonAction.getJSONArray("actionTypes");

				ArrayList<String> actionTypes = new ArrayList<>();

				for (int i = 0; i < jsonActionTypes.length(); i++) {
					actionTypes.add(jsonActionTypes.getString(i).trim());
				}

				// make sure some required actions are there if this is the rapid app
				if ("rapid".equals(appId)) {
					String [] requiredActionTypes = {"rapid", "ajax", "control", "custom", "dataCopy", "existing", "validation"};
					for (String actionType : requiredActionTypes) {
						if (!actionTypes.contains(actionType)) actionTypes.add(actionType);
					}
				}

				// sort the types
				Collections.sort(actionTypes);

				// put the list into the app
				app.setActionTypes(actionTypes);

				// save it
				app.save(rapidServlet, rapidRequest, true);

				// add the message to the response
				result.put("message", actionTypes.size() + " actions");

			} else if ("SAVECONTROLS".equals(action)) {

				JSONArray jsonControlTypes = jsonAction.getJSONArray("controlTypes");

				ArrayList<String> controlTypes = new ArrayList<>();

				// loop the controls
				for (int i = 0; i < jsonControlTypes.length(); i++) {
					// get the control type
					String controlType = jsonControlTypes.getString(i).trim();
					// get the json for it
					JSONObject jsonControl = rapidServlet.getJsonControl(controlType);
					// if there was one
					if (jsonControl != null) {
						// add this type
						controlTypes.add(controlType);
						// look for any required action
						String requiredActionType = jsonControl.optString("requiredActionType", null);
						// if we got one
						if (requiredActionType != null) {
							// get the action types
							List<String> actionTypes = app.getActionTypes();
							// if it doesn't exist
							if (!actionTypes.contains(requiredActionType)) {
								// get the json for it
								JSONObject jsonActionType = rapidServlet.getJsonControl(requiredActionType);
								// if we got one
								if (jsonActionType != null) {
									// add it
									actionTypes.add(requiredActionType);
									// sort the list
									Collections.sort(actionTypes);
								}
							}
						}
					}
				}

				// make sure some required controls are there if this is the rapid app
				if ("rapid".equals(appId)) {
					String [] requiredControlTypes = {"button", "dataStore", "dropdown", "grid", "image", "input", "page", "table", "tabGroup", "text"};
					for (String controlType : requiredControlTypes) {
						if (!controlTypes.contains(controlType)) controlTypes.add(controlType);
					}
				}

				// sort the types
				Collections.sort(controlTypes);

				// add the controls to the app
				app.setControlTypes(controlTypes);

				// save
				app.save(rapidServlet, rapidRequest, true);

				// add the message to the response
				result.put("message", controlTypes.size() + " controls");

			} else if ("REBUILDPAGES".equals(action)) {

				// add the message to the response
				result.put("message", "This feature is not supported");

			} else if ("NEWAPP".equals(action)) {

				// retrieve the inputs from the json
				String name = jsonAction.getString("name").trim();
				String version = jsonAction.getString("newVersion").trim();
				String title = jsonAction.optString("title").trim();
				String type = jsonAction.optString("type");
				String formAdapterType = jsonAction.optString("formAdapter", null);
				boolean responsive = jsonAction.optBoolean("responsive");
				String themeType = jsonAction.optString("themeType");
				String description = jsonAction.optString("description").trim();

				// create a new application with our reusable, private method
				Application newApp = createApplication(rapidServlet, rapidRequest, name, version, title, type, responsive, themeType, description);

				// if this is a form, add the adapter
				if ("F".equals(type)) newApp.setFormAdapter(servletContext, formAdapterType);

				// set the result message
				result.put("message", "Application " + newApp.getTitle() + " created");

				// set the result appId
				result.put("appId", newApp.getId());

				// set the result version
				result.put("version", newApp.getVersion());

			} else if ("DELAPP".equals(action)) {

				// check we have an app
				if (app != null)  {
					// get the collection of applications and versions
					Applications applications = rapidServlet.getApplications();
					// get all versions of this application
					Versions versions = applications.getVersions(app.getId());
					// get the number of version
					int versionCount = versions.size();
					// make a list of versions
					ArrayList<String> versionNumbers = new ArrayList<>();
					// loop the versions
					for (String version : versions.keySet()) {
						versionNumbers.add(version);
					}
					// loop the versionNumbers
					for (String versionNumber: versionNumbers) {
						// get this version
						Application v = applications.get(app.getId(), versionNumber);
						// delete it
						v.delete(rapidServlet, rapidActionRequest, true);
					}
					// set the result message
					result.put("message", versionCount + " application version" + (versionCount == 1 ? "" : "s") + " deleted for " + app.getName());
				}

			} else if ("DUPAPP".equals(action)) {

				String version = jsonAction.getString("newVersion").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();

				// use the application.copy routine (this updates the status and created time)
				Application dupApp = app.copy(rapidServlet, rapidRequest, app.getId(), version, false, false);

				// set the new title into the duplicate
				dupApp.setTitle(title);
				// set the new description
				dupApp.setDescription(description);

				// save the duplicate
				dupApp.save(rapidServlet, rapidRequest, false);

				// set the result message
				result.put("message", "Application " + app.getTitle() + " duplicated");
				result.put("id", dupApp.getId());
				result.put("version", dupApp.getVersion());

			} else if ("NEWVERSION".equals(action)) {

				// retrieve the inputs from the json
				String id = jsonAction.getString("appId").trim();
				String version = jsonAction.getString("newVersion").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();

				// create a new application with our reusable, private method
				Application newApp = createApplication(rapidServlet, rapidRequest, id, version, title, "", false, "", description);

				// set the result message
				result.put("message", "Version " + newApp.getVersion() + " created for " + newApp.getTitle());

				// set the result appId
				result.put("appId", newApp.getId());

				// set the result version
				result.put("version", newApp.getVersion());

				// set the result message
				result.put("message", "Application " + app.getTitle() + " duplicated");
				result.put("id", newApp.getId());
				result.put("version", newApp.getVersion());

			} else if ("DELVERSION".equals(action)) {

				// delete the application version
				if (app != null) app.delete(rapidServlet, rapidActionRequest, false);
				// set the result message
				result.put("message", "Version " + app.getVersion() + " deleted");

			} else if ("NEWPAGE".equals(action)) {

				String id = jsonAction.getString("id").trim();
				String name = jsonAction.getString("name").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();

				// assume designer set id
				boolean nameIds = false;

				// this parameter existed against the entire system for just a single version of Rapid 2.4.1 before being moved to each application from 2.4.2
				if (Boolean.parseBoolean(servletContext.getInitParameter("pageNameIds")) || rapidActionRequest.getApplication().getPageNameIds()) nameIds = true;

				// check nameIds and set accordingly
				if (nameIds) id = name;

				Page newPage = new Page();
				newPage.setId(id);
				newPage.setName(name);
				newPage.setTitle(title);
				newPage.setDescription(description);
				newPage.setCreatedBy(rapidRequest.getUserName());
				newPage.setCreatedDate(new Date());

				// save the page to file
				newPage.save(rapidServlet, rapidActionRequest, app, false);

				// put the id in the result
				result.put("id", id);

				// set the result message
				result.put("message", "Page " + newPage.getTitle() + " created");

			} else if ("DELPAGE".equals(action)) {

				// get the id
				String id = jsonAction.getString("id").trim();
				// retrieve the page
				Page delPage = app.getPages().getPage(rapidRequest.getRapidServlet().getServletContext(), id);
				// delete it if we got one
				if (delPage != null) delPage.delete(rapidServlet, rapidActionRequest, app);
				// set the result message
				result.put("message", "Page " + delPage.getName() + " delete");

			} else if ("NEWWORKFLOW".equals(action)) {

				// retrieve the inputs from the json
				String name = jsonAction.getString("name").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();

				// create a new workflow with our reusable, private method
				Workflow newWorkflow = createWorkflow(rapidServlet, rapidRequest, name, title, description);

				// set the result message
				result.put("message", "Workflow " + app.getTitle() + " created");

				// set the result appId
				result.put("wfId", newWorkflow.getId());

			} else if ("DELWORKFLOW".equals(action)) {

				// get the id
				String id = jsonAction.getString("id").trim();
				// get the collection of workflows
				Workflows workflows = rapidServlet.getWorkflows();
				// get the workflow
				Workflow workflow = workflows.get(id);
				// delete it
				workflow.delete(rapidServlet, rapidRequest);
				// set the result message
				result.put("message", "Work flow " + workflow.getName() + " deleted");

			} else if ("NEWDBCONN".equals(action)) {

				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();
				// instantiate if null
				if (dbConns == null) dbConns = new ArrayList<>();

				// make the new database connection
				DatabaseConnection dbConn = new DatabaseConnection(
					servletContext,
					app,
					jsonAction.getString("name").trim(),
					jsonAction.getString("driver").trim(),
					jsonAction.getString("connectionString").trim(),
					jsonAction.getString("connectionAdapter").trim(),
					jsonAction.getString("userName").trim(),
					jsonAction.getString("password")
				);

				// add it to the collection
				dbConns.add(dbConn);

				// save the app
				app.save(rapidServlet, rapidRequest, true);

				// add the application to the response
				result.put("message", "Database connection added");

			} else if ("DELDBCONN".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();

				// remeber whether we found the connection
				boolean foundConnection = false;

				// check we have database connections
				if (dbConns != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < dbConns.size()) {

						// remove the database connection
						dbConns.remove(index);

						// save the app
						try { app.save(rapidServlet, rapidRequest, true);	}
						catch (Exception ex) { throw new JSONException(ex);	}

						// add the application to the response
						result.put("message", "Database connection deleted");

					}
				}

				if (!foundConnection) result.put("message", "Database connection could not be found");

			} else if ("NEWSOA".equals(action)) {

				// the webservice we are about to make
				Webservice webservice = null;

				// get the type
				String type = jsonAction.getString("type");

				if ("SQLWebservice".equals(type)) {
					// make the new SQL webservice
					webservice = new SQLWebservice(
						jsonAction.getString("name").trim()
					);
				} else if ("JavaWebservice".equals(type)) {
					// make the new Java class webservice
					webservice = new JavaWebservice(
						jsonAction.getString("name").trim()
					);
				}

				// if one was made
				if (webservice != null) {

					// add it to the collection
					app.getWebservices().add(webservice);

					// save the app
					app.save(rapidServlet, rapidRequest, true);

					// add the application to the response
					result.put("message", "SOA webservice added");

				} else {
					// send message
					result.put("message", "Webservice type not recognised");
				}

			} else if ("DELSOA".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// get the webservices
				List<Webservice> webservices = app.getWebservices();

				// remeber whether we found the webservice
				boolean foundWebservice = false;

				// check we have database connections
				if (webservices != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < webservices.size()) {

						// remove the database connection
						webservices.remove(index);

						// save the app
						app.save(rapidServlet, rapidRequest, true);

						// add the application to the response
						result.put("message", "SOA webservice deleted");

					}
				}

				if (!foundWebservice) result.put("message", "SOA webservice could not be found");

			} else if ("NEWROLE".equals(action)) {

				// get the role name
				String roleName = jsonAction.getString("role").trim();
				// get the role descrition
				String description = jsonAction.getString("description").trim();

				// add the role
				security.addRole(rapidActionRequest, new Role(roleName, description));
				// set the result message
				result.put("message", "Role added");

			} else if ("DELROLE".equals(action)) {

				// get the role
				String role = jsonAction.getString("role").trim();
				// delete the role
				security.deleteRole(rapidActionRequest, role);
				// set the result message
				result.put("message", "Role deleted");

			} else if ("NEWUSER".equals(action)) {

				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// get the userDescription
				String description = jsonAction.optString("description","").trim();
				// get the email
				String email = jsonAction.optString("email");
				// get the password
				String password = jsonAction.getString("password");
				// get locked status
				boolean isLocked = jsonAction.isNull("isLocked")?false:"true".equalsIgnoreCase(jsonAction.getString("isLocked"));
				// get the device details
				String deviceDetails = jsonAction.optString("deviceDetails");
				// check for useAdmin - must have Radpid Admin to grant
				boolean useAdmin = jsonAction.optBoolean("useAdmin") && rapidAdmin;
				// check for useDesign - must have Radpid Admin to grant
				boolean useDesign = jsonAction.optBoolean("useDesign") && rapidAdmin;
				// check for useUsers
				boolean useUsers = jsonAction.optBoolean("useUsers");

				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);

				// add the user
				security.addUser(rapidRequest, new User(userName, description, email, password, isLocked, deviceDetails));

				// update the Rapid Request to have the new user name
				rapidRequest.setUserName(userName);

				// add role if we were given one
				if (useAdmin) security.addUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);

				// add role if we were given one
				if (useDesign) security.addUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);

				// add role if we were given one
				if (useUsers) security.addUserRole(rapidRequest, com.rapid.server.Rapid.USERS_ROLE);

				// set the result message
				result.put("message", "User added");

			} else if ("DELUSER".equals(action)) {

				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// override the standard request user
				rapidRequest.setUserName(userName);
				// delete the user
				security.deleteUser(rapidRequest);
				// remove any of their page locks
				app.removeUserPageLocks(servletContext, userName);
				// set the result message
				result.put("message", "User deleted");

			} else if ("SAVEROLE".equals(action)) {

				// get the role
				String roleName = jsonAction.getString("role").trim();
				// get the description
				String roleDescription = jsonAction.getString("description").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// update the role
				security.updateRole(rapidRequest, new Role(roleName, roleDescription));
				// set the result message
				result.put("message", "Role details saved");

			} else if ("NEWUSERROLE".equals(action)) {

				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// override the standard request user
				rapidRequest.setUserName(userName);
				// get the role
				String role = jsonAction.getString("role").trim();
				// add the user role - if RapidAdmin or RapidDesign, must have rapidAdmin
				if ((!com.rapid.server.Rapid.ADMIN_ROLE.equals(role) && !com.rapid.server.Rapid.DESIGN_ROLE.equals(role)) || rapidAdmin) security.addUserRole(rapidRequest, role);
				// set the result message
				result.put("message", "User role added");

			} else if ("DELUSERROLE".equals(action)) {

				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// override the standard request user
				rapidRequest.setUserName(userName);
				// get the role
				String role = jsonAction.getString("role").trim();
				// add the user role
				security.deleteUserRole(rapidRequest, role);
				// set the result message
				result.put("message", "User role deleted");

			} else if ("SAVEUSER".equals(action)) {

				// get the userName of the user being changed
				String userName = jsonAction.getString("userName").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// override the standard request user
				rapidRequest.setUserName(userName);
				// get the description
				String description = jsonAction.getString("description").trim();
				// get the email
				String email = jsonAction.optString("email");
				// get the password
				String password = jsonAction.getString("password");
				// get locked status
				String isLocked = jsonAction.isNull("isLocked")?"false":jsonAction.getString("isLocked");
				// get the device details
				String deviceDetails = jsonAction.getString("deviceDetails");

				// get the user
				User user = security.getUser(rapidRequest);
				// update the email
				user.setEmail(email);
				// update the description
				user.setDescription(description);
				// update the device details
				user.setDeviceDetails(deviceDetails);
				// update the locked status
				user.setIsLocked("true".equalsIgnoreCase(isLocked));

				// if password is different from the mask
				if ("********".equals(password)) {
					// just update the user
					security.updateUser(rapidRequest, user);
				} else {
					// get the old password
					String oldPassword = user.getPassword();
					// update the password
					user.setPassword(password);
					// update the user
					security.updateUser(rapidRequest, user);
					// update the session password as well if we are changing our own password (this is required especially when changing the rapid app password)
					if (user.getName().equals(rapidRequest.getSessionAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME))) rapidRequest.setUserPassword(password);
					// if there is an old password - should always be
					if (oldPassword != null) {
						// only if the new password is different from the old one
						if (!password.equals(oldPassword)) {
							// get all applications
							Applications applications = rapidRequest.getRapidServlet().getApplications();
							// loop them
							for (String id : applications.getIds()) {
								// get their versions
								Versions versions = applications.getVersions(id);
								// loop the versions
								for (String version : versions.keySet()) {
									// get this version
									Application v = applications.get(id, version);
									// we have updated the password in the selected app already so no need to do it again
									if (!(app.getId().equals(v.getId()) && app.getVersion().equals(v.getVersion()))) {
										// get this app versions security adapter
										SecurityAdapter s = v.getSecurityAdapter();
										// recreate the rapidRequest with the selected version (so app parameters etc are available from the app in the rapidRequest)
										rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), v);
										// override the standard request user
										rapidRequest.setUserName(userName);
										// check the user had permission to the app with their old password
										if (s.checkUserPassword(rapidRequest, userName, oldPassword)) {
											// get this user
											User u = s.getUser(rapidRequest);
											// safety check for if we found one
											if (u == null) {
												// log that it passed password check but can't be found
												rapidRequest.getRapidServlet().getLogger().debug("User " + userName + " passed password check for " + app.getId() + "/" + app.getVersion() + " but now can't be found for password update");
											} else {
												// set new user password
												u.setPassword(password);
												// update user
												s.updateUser(rapidRequest, u);
											}
										} // password match check
									} // ignore app version updated already
								} // version loop
							} // app id loop
						} // old password different from new
					} // old password null check
				} // password provided

				// if we are updating the rapid application we have used checkboxes for the Rapid Admin, Rapid Designer, and user manager roles
				if ("rapid".equals(app.getId())) {
					// get the value of rapidAdmin
					String useAdmin = jsonAction.optString("useAdmin");
					// check useAdmin was sent
					if (useAdmin != null) {
						// check the user was given the role
						if ("true".equals(useAdmin)) {
							// add the role if the user doesn't have it already
							if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE))
								security.addUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
						} else {
							// remove the role
							security.deleteUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
						}
					}
					// get the value of rapidDesign
					String useDesign = jsonAction.optString("useDesign");
					// check useDesign was sent
					if (useDesign != null) {
						// check the user was given the role
						if ("true".equals(useDesign)) {
							// add the role if the user doesn't have it already
							if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE))
								security.addUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
						} else {
							// remove the role
							security.deleteUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
						}
					}
					// get the value of rapidUsers
					String useUsers = jsonAction.optString("useUsers");
					// check useUsers was sent
					if (useUsers != null) {
						// check the user was given the role
						if ("true".equals(useUsers)) {
							// add the role if the user doesn't have it already
							if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.USERS_ROLE))
								security.addUserRole(rapidRequest, com.rapid.server.Rapid.USERS_ROLE);
						} else {
							// remove the role
							security.deleteUserRole(rapidRequest, com.rapid.server.Rapid.USERS_ROLE);
						}
					}
				}

				// set the result message
				result.put("message", "User details saved");

			} else if ("CHECKPASSWORDCOMPLEXITY".equals(action)) {

				// get the password
				String password = jsonAction.getString("password");

				// check complexity
				boolean complexPass = security.checkPasswordComplexity(rapidRequest, password);

				// add check result
				result.put("complexPass", complexPass);
				// add whether rapid app
				result.put("rapidApp", "rapid".equals(app.getId()));

				// check passed
				if (complexPass) {

					// set the result message
					result.put("message", "Password is sufficiently complex");

				} else {

					// set the result message
					result.put("message", security.getPasswordComplexityDescription(rapidRequest, password));

				}

			} else if ("NEWPARAM".equals(action)) {

				// add a new parameter to the collection
				app.getParameters().add(new Parameter());

			} else if ("DELPARAM".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// remove the parameter
				app.getParameters().remove(index);

				// save the app
				app.save(rapidServlet, rapidRequest, true);

				// set the result message
				result.put("message", "Parameter deleted");

			} else if ("SAVEPARAM".equals(action)) {

				int index = jsonAction.getInt("index");
				String name = jsonAction.getString("name");
				String description = jsonAction.getString("description");
				String value = jsonAction.getString("value");

				// fetch the parameter
				Parameter parameter = app.getParameters().get(index);

				// update it
				parameter.setName(name);
				parameter.setDescription(description);
				parameter.setValue(value);

				// save the app
				app.save(rapidServlet, rapidRequest, true);

				// set the result message
				result.put("message", "Parameter details saved");

			} else if ("NEWRESOURCE".equals(action)) {

				// get the resources
				Resources resources = app.getAppResources();
				// if null (could be from a previous version)
				if (resources == null) {
					// instantiate here
					resources = new Resources();
					// assign to the application
					app.setAppResources(resources);
				}

				// add a new parameter to the collection
				resources.add(new Resource());

			} else if ("DELRESOURCE".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// remove the parameter
				app.getAppResources().remove(index);

				// save the app
				app.save(rapidServlet, rapidRequest, true);

				// set the result message
				result.put("message", "Resource deleted");

			} else if ("SAVERESOURCE".equals(action)) {

				int index = jsonAction.getInt("index");
				String name = jsonAction.getString("name");
				int type = jsonAction.getInt("type");
				String value = jsonAction.getString("value");

				// fetch the resource
				Resource resource = app.getAppResources().get(index);

				// update it
				resource.setName(name);
				resource.setType(type);
				resource.setContent(value);

				// save the app
				app.save(rapidServlet, rapidRequest, true);

				// set the result message
				result.put("message", "Resource details saved");

			} else if ("NEWDEVICE".equals(action)) {

				// get the devices
				Devices devices = rapidServlet.getDevices();

				// add a new device to the collection
				devices.add(new Device("New device", 500, 500, 200, 1d));

				// save it
				devices.save(servletContext);

			} else if ("DELDEVICE".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// get the devices
				Devices devices = rapidServlet.getDevices();

				// remove the device
				devices.remove(index);

				// save the devices
				devices.save(servletContext);

				// set the result message
				result.put("message", "Device deleted");

			} else if ("SAVEDEVICE".equals(action)) {

				int index = jsonAction.getInt("index");
				String name = jsonAction.getString("name");
				int width = jsonAction.getInt("width");
				int height = jsonAction.getInt("height");
				int ppi = jsonAction.getInt("ppi");
				double scale = jsonAction.getDouble("scale");

				// fetch the devices
				Devices devices = rapidServlet.getDevices();
				// fetch the device
				Device device = devices.get(index);

				// update the device
				device.setName(name);
				device.setWidth(width);
				device.setHeight(height);
				device.setPPI(ppi);
				device.setScale(scale);

				// save the devices
				devices.save(servletContext);

				// set the result message
				result.put("message", "Device details saved");

			} else if ("TESTDBCONN".equals(action)) {

				// get the index
				int index = jsonAction.getInt("index");

				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();

				// remember whether we found the connection
				boolean foundConnection = false;

				// check we have database connections
				if (dbConns != null) {
					// check the index we where given will retrieve a database connection
					if (index > -1 && index < dbConns.size()) {

						// retrieve the details from the json
						String driverClass = jsonAction.getString("driver").trim();
						String connectionString =app.insertParameters(servletContext, jsonAction.getString("connectionString").trim());
						String connectionAdapterClass = jsonAction.getString("connectionAdapter").trim();
						String userName = jsonAction.getString("userName").trim();
						String password = jsonAction.getString("password");

						// if the password wasn't set retrieve it via the connection index
						if ("********".equals(password)) password = dbConns.get(index).getPassword();

						// instantiate a DatabaseConnection object for this test
						DatabaseConnection dbconnection = new DatabaseConnection(
							servletContext,
							app,
							"test",
							driverClass,
							connectionString,
							connectionAdapterClass,
							userName,
							password
						);
						// get the adapter
						ConnectionAdapter connectionAdapter = dbconnection.getConnectionAdapter(servletContext, app);
						// get a data factory
						DataFactory dataFactory = new DataFactory(connectionAdapter);
						// get a connection
						Connection connection = dataFactory.getConnection(rapidRequest);
						// close it
						dataFactory.close();

						// add the application to the response
						result.put("message", "Database connection OK");

						// retain that a connection was found
						foundConnection = true;

					}
				}

				if (!foundConnection) result.put("message", "Database connection could not be found");

			} else if ("TESTEMAIL".equals(action)) {

				String host = jsonAction.getString("host").trim();
				int port = jsonAction.getInt("port");
				String sec = jsonAction.getString("security").trim();
				String userName = jsonAction.getString("userName").trim();
				String password = jsonAction.getString("password");

				// if password is ********
				if ("********".equals(password)) {
					// get the current email settings
					Email emailSettings = Email.getEmailSettings();
					// if we got one use that password
					if (emailSettings != null) password = emailSettings.getPassword();
				}

				// set the properties we've just received
				Email.setProperties(host, port, sec, userName, password);

				// assume to email is the constant
				String from = TEST_EMAIL_FROM;
				// look for a setting in the web.xml
				String fromSetting = servletContext.getInitParameter("email.test.from");
				// if we got one use this value
				if (fromSetting != null) from = fromSetting;

				// assume to email is the constant
				String to = TEST_EMAIL_TO;
				// look for a setting in the web.xml
				String toSetting = servletContext.getInitParameter("email.test.to");
				// if we got one use this value
				if (toSetting != null) to = toSetting;

				try {

					// send a test email
			        Email.send(from, to, "Rapid test email", "It's working!");

				} catch (Exception ex) {

					// reload the saved values
			        Email.load(servletContext);

			        // rethrow
			        throw ex;

				}

		        // reload the saved values
		        Email.load(servletContext);

		        // add a meaningful response message which the success callback is expecting
		        result.put("message", "Test email sent OK");

			} else if ("SAVEEMAIL".equals(action)) {

				// get whether enabled
				boolean enabled = jsonAction.optBoolean("enabled");

				// get any current email settings
				Email email = Email.getEmailSettings();

				// check whether enabled
				if (enabled) {

					String host = jsonAction.getString("host").trim();
					int port = jsonAction.getInt("port");
					String sec = jsonAction.getString("security").trim();
					String userName = jsonAction.getString("userName").trim();
					String password = jsonAction.getString("password");
					// if we just got 8 *'s for the password
					if ("********".equals(password)) {
						// if we had an email object
						if (email == null) {
							// make it a blank space
							password = "";
						} else {
							// set it back to stop being overridden with 8 *'s
							password = email.getPassword();
						}
					}

					// set the properties we've just loaded
		            Email.setProperties(host, port, sec, userName, password);

		            // construct a new object
			        email = new Email(host, port, sec, userName, password);

			        // set enabled
			        email.setEnabled(true);

				} else {

					// for safety, make a new one if not yet set
					if (email == null) email = new Email();

					// set not enabled
			        email.setEnabled(false);

				}

		        // save it
		        email.save(servletContext);

			} else if ("DELAPPBACKUP".equals(action)) {

				// get the id
				String backupId = jsonAction.getString("backupId");

				// get the folder into a file object
				File backup = new File (app.getBackupFolder(servletContext, false) + "/" + backupId);
				// delete it
				Files.deleteRecurring(backup);

				// set the result message
				result.put("message", "Application backup " + appId + "/" + appVersion + "/" + backupId + " deleted");
				// pass back a control id from in the dialogue with which to close it
				result.put("controlId", "#rapid_P12_C13_");

			} else if ("DELPAGEBACKUP".equals(action)) {

				// get the id
				String backupId = jsonAction.getString("backupId");

				// get the folder into a file object
				File backup = new File (app.getBackupFolder(servletContext, false) + "/" + backupId);
				// delete it
				Files.deleteRecurring(backup);

				// set the result message
				result.put("message", "Page backup " + appId + "/" + backupId + " deleted");
				// pass back a control id from in  the dialogue with which to close it
				result.put("controlId", "#rapid_P13_C13_");

			} else if ("RESTOREAPPBACKUP".equals(action)) {

				// get the id
				String backupId = jsonAction.getString("backupId");

				// get this backup folder
				File backupFolder = new File(app.getBackupFolder(servletContext, false) + "/" + backupId);

				// check it exists
				if (backupFolder.exists()) {

					// back up the current state of the application
					app.backup(rapidServlet, rapidRequest, false);


					// get the config folder
					File configFolder = new File(app.getConfigFolder(servletContext));

					// get the web folder
					File webFolder = new File(app.getWebFolder(servletContext));

					// get the backups folder
					File backupsFolder = new File(app.getBackupFolder(servletContext, false));



					// create a file object for restoring the config folder
				 	File configRestoreFolder = new File(Application.getConfigFolder(servletContext, app.getId(), "_restore"));

				 	List<String> ignoreList = new ArrayList<>();
				 	ignoreList.add("WebContent");

				 	// copy the backup into the application restore folder
					Files.copyFolder(backupFolder, configRestoreFolder, ignoreList);



				 	// create a file object for the web content backup folder (which is currently sitting under the application)
					File webBackupFolder = new File(backupFolder + "/WebContent");

					// create a file object for the web content restore folder
					File webRestoreFolder = new File(Application.getWebFolder(servletContext, app.getId(), "_restore"));

					// copy the web contents backup folder to the webcontent restore folder
					Files.copyFolder(webBackupFolder, webRestoreFolder);



					// get the backups destination folder
					File backupsRestoreFolder = new File(Application.getBackupFolder(servletContext, app.getId(), "_restore", false));

					// copy in the backups
					Files.copyFolder(backupsFolder, backupsRestoreFolder);



					// delete the application config folder (this removes the webcontent and backups too so we do it here)
				 	Files.deleteRecurring(configFolder);

				 	// rename the restore folder to the application folder
				 	configRestoreFolder.renameTo(configFolder);


					// delete the webcontent folder
					Files.deleteRecurring(webFolder);

					// rename the restore folder to the webconten folder
					webRestoreFolder.renameTo(webFolder);



					// get the application file
					File applicationFile = new File(configFolder + "/application.xml");

					// reload the application
					app = Application.load(servletContext, applicationFile);

					// add it back to the collection
					rapidServlet.getApplications().put(app);


					// set the result message
					result.put("message", "Application " + backupId + " restored");
					// pass back a control id from in  the dialogue with which to close it
					result.put("controlId", "#rapid_P14_C13_");

				} else {

					// set the result message
					result.put("message", "Application backup " + backupId + " not found");

				}

			} else if ("RESTOREPAGEBACKUP".equals(action)) {

				// get the id
				String backupId = jsonAction.getString("backupId");

				// turn the id into parts
				String[] idParts = backupId.split("_");

				// start the page name
				String pageName = idParts[0];
				// loop the remaining parts and build
				for (int i = 1; i < idParts.length - 3; i++) {
					pageName += "_" + idParts[i];
				}

				// get the page
				Page page = app.getPages().getPageByName(servletContext, pageName);

				// create a file object for the page
			 	File pageFile = new File(page.getFile(servletContext, app));

			 	// create a backup for the current state
				page.backup(rapidServlet, rapidRequest, app, pageFile, false);

				// get this backup file
				File backupFile = new File(app.getBackupFolder(servletContext, false) + "/" + backupId);

				// copy it over the current page file
				Files.copyFile(backupFile, pageFile);

				// load the page from the backup
				page = Page.load(servletContext, backupFile);

				// replace the current entry
				app.getPages().addPage(page, pageFile, app.getIsForm());

				// set the result message
				result.put("message", "Page backup " + appId + "/" + backupId + " restored");
				// pass back a control id from in  the dialogue with which to close it
				result.put("controlId", "#rapid_P15_C13_");

			} else if ("SAVEAPPBACKUPSIZE".equals(action)) {

				// get the max backup size
				int backupMaxSize = jsonAction.getInt("backupMaxSize");

				// pass it to the application
				app.setApplicationBackupMaxSize(backupMaxSize);

				// save the application
				app.save(rapidServlet, rapidRequest, false);

				// set the result message
				result.put("message", "Application backup max size updated to " + backupMaxSize);

			} else if ("SAVEPAGEBACKUPSIZE".equals(action)) {

				// get the max backup size
				int backupMaxSize = jsonAction.getInt("backupMaxSize");

				// pass it to the application
				app.setPageBackupsMaxSize(backupMaxSize);

				// save the application
				app.save(rapidServlet, rapidRequest, false);

				// set the result message
				result.put("message", "Page backup max size updated to " + backupMaxSize);

			}

			// sent back the new app id for the callback load
			if (newAppId != null) result.put("id", newAppId);

		} else {

			// send back an error
			result.put("error", "Application not found");

		}

		return result;

	}

}
