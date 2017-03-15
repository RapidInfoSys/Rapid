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

package com.rapid.server;

/*

This class wraps an HttpRequest and provides a number of useful methods for retrieving common Rapid objects

*/

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.filter.RapidFilter;

// this class provides some utility functions for easily accessing common Rapid objects from a request
public class RapidRequest {

	// instance variables

	private RapidHttpServlet _rapidServlet;
	private HttpServletRequest _request;
	private HttpSession _session;
	private String _actionName, _appId, _version, _userName;
	private Application _application;
	private Page _page;
	private Control _control;
	private Action _action;

	// properties

	public RapidHttpServlet getRapidServlet() { return _rapidServlet; }
	public HttpServletRequest getRequest() { return _request; }
	public String getAppId() { return _appId; }
	public String getVersion() { return _version; }
	public Application getApplication() { return _application; }
	public Page getPage() { return _page; }
	public Control getControl() { return _control; }
	public Action getAction() { return _action; }

	// allow overriding the action name - useful for later logic
	public String getActionName() { return _actionName; }
	public void setActionName(String actionName) { _actionName = actionName; }

	// allow overriding the user name in the rare event we want to do something in the name of someone else (like modifying another users details in the Security Adapter)
	public String getUserName() { return _userName; }
	public void setUserName(String userName) { _userName = userName; }

	// methods

	// get a specified session attribute
	public Object getSessionAttribute(String name) {
		if (_session == null) {
			return null;
		} else {
			return _session.getAttribute(name);
		}
	}

	// get the device details
	public String getDevice() {  return (String) getSessionAttribute(RapidFilter.SESSION_VARIABLE_USER_DEVICE); }

	// decrypt the password
	public String getUserPassword() throws GeneralSecurityException, IOException {
		String raw = (String) getSessionAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD);
		String password = RapidHttpServlet.getEncryptedXmlAdapter().unmarshal(raw);
		if (password == null) {
			return "";
		} else {
			return password;
		}
	}

	// encrypt the password
	public void setUserPassword(String password) throws GeneralSecurityException, IOException {
		if (password != null && _session != null) {
			String raw = RapidHttpServlet.getEncryptedXmlAdapter().marshal(password);
			_session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD, raw);
		}
	}

	// most likely to construct a rapidRequest from a servlet and an http request
	public RapidRequest(RapidHttpServlet rapidServlet, HttpServletRequest request) {
		// retain the servlet
		_rapidServlet = rapidServlet;
		// retain the http request
		_request = request;
		// retain the session
		_session = request.getSession(false);
		// store the user name from the session
		_userName = (String) getSessionAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
		// look for an action parameter
		_actionName = request.getParameter("action");
		// look for an appId
		_appId = request.getParameter("a");
		// look for a version
		_version = request.getParameter("v");
		// look for the application
		_application = _rapidServlet.getApplications().get(_appId, _version);
		// if we've found the application look for the page
		if (_application != null) {
			// get the requested page id
			String pageId = request.getParameter("p");
			// try and get the specified page
			try {
				// if no page was specifically requested
				if (pageId == null) {
					// get the start page
					_page = _application.getStartPage(rapidServlet.getServletContext());
				} else {
					// get the requested page
					_page = _application.getPages().getPage(rapidServlet.getServletContext(), pageId);
				}
				// if there is no control parameter could still have a page action
				if (request.getParameter("c") == null) {
					if (request.getParameter("act") != null) {
						// get action from the page
						_action = _page.getAction(request.getParameter("act"));
					}
				} else {
					_control = _page.getControl(request.getParameter("c"));
					// if we've found the control and have an action parameter
					if (_control != null && request.getParameter("act") != null) {
						// get action from the control
						_action = _control.getAction(request.getParameter("act"));
					}
				}
			} catch (RapidLoadingException ex) {

				// get the logger
				Logger logger = rapidServlet.getLogger();
				// log the exception
				logger.error("Failed to load page when creating request", ex);
			}

		}
		// retain all the query string parameter values in the session
		Enumeration<String> names = _request.getParameterNames();
		while (names.hasMoreElements()) {
			// get the name
			String name = names.nextElement();
			// get any values
			String[] values = _request.getParameterValues(name);
			// store the first value
			if (values != null) _request.getSession().setAttribute(name, values[0]);
		}
	}

	// can also instantiate a rapid request with just an application object (this is used by the rapid action)
	public RapidRequest(RapidHttpServlet rapidServlet, HttpServletRequest request, Application application) {
		// store the servlet
		_rapidServlet = rapidServlet;
		// store the request
		_request = request;
		// retain the session
		_session = request.getSession(false);
		// store the user name from the session
		_userName = (String) getSessionAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
		// store the application
		_application = application;
		// if we got an application
		if (application != null) {
			// store the application id
			_appId = application.getId();
			// store the version
			_version = application.getVersion();
		}
	}

	// can also instantiate a rapid request with just an HttpServletRequest and an application
	public RapidRequest(HttpServletRequest request, Application application) {
		if (request != null) {
			// store the request
			_request = request;
			// retain the session
			_session = request.getSession(false);
			// store the user name from the session
			_userName = (String) getSessionAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
		}
		// store the application
		_application = application;
		// if we got an application
		if (application != null) {
			// store the application id
			_appId = application.getId();
			// store the version
			_version = application.getVersion();
		}
	}

	// can also instantiate a rapid request with just an HttpServletRequest a session
	public RapidRequest(RapidHttpServlet rapidServlet, HttpServletRequest request, HttpSession session, Application application) {
		// store the servlet
		_rapidServlet = rapidServlet;
		// store the request
		_request = request;
		// retain the session
		_session = session;
		// store the user name from the session
		_userName = (String) getSessionAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
		// store the application
		_application = application;
		// if we got an application
		if (application != null) {
			// store the application id
			_appId = application.getId();
			// store the version
			_version = application.getVersion();
		}
	}

	// good for printing details of the Rapid request into logs and error messages
	public String getDetails() {

		// assume there was no application
		String details = " no application";

		// if there is one
		if (_application != null) {

			// set the details response to the application title and id
			details = " app = " + _application.getTitle() + " (" + _application.getId() + ")";

			// if there is a page involved
			if (_page != null) {

				// add the page title and id
				details += "\n page = " + _page.getTitle() + " (" + _page.getId() + ")";

				// check whether there was a control
				if (_control == null) {
					// no control, probably the page
					details += "\n control = page";
				} else {
					// add details of the control
					details += "\n control = " + _control.getName() + "(" + _control.getId() + ")";
				}

				// check whether there was an action
				if (_action == null) {
					// no action, say so
					details += "\n action = no action";
				} else {
					// add details of the action
					details += "\n action = (" + _action.getId() + ")";
				}

			}

		}

		return details;

	}

}