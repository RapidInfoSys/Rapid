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

package com.rapid.soa;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.rapid.core.Application;
import com.rapid.core.Applications;
import com.rapid.server.RapidRequest;
import com.rapid.server.RapidSessionListener;
import com.rapid.server.filter.RapidFilter;
import com.rapid.soa.SOASchema.SOASchemaElement;

public class AuthenticationWebservice extends Webservice {

	public AuthenticationWebservice() {

		// this is the authentication webservice
		_isAuthenticate = true;

		// create the request schema
		SOASchema requestSchema = new SOASchema();
		requestSchema.setRootElement(new SOASchemaElement("0","authenticate"));
		requestSchema.addChildElement("username", SOASchema.STRING);
		requestSchema.addChildElement("password", SOASchema.STRING);
		setRequestSchema(requestSchema);

		// create the response schema
		SOASchema responseSchema = new SOASchema();
		responseSchema.setRootElement(new SOASchemaElement("0","authentication", SOASchema.STRING));
		setResponseSchema(responseSchema);

	}

	@Override
	public SOAData getResponseData(RapidRequest rapidRequest,	SOAData requestData)	throws WebserviceException {

		List<SOAElement> requestElements = requestData.getRootElement().getChildElements();
		String username = null;
		String password = null;
		for (SOAElement element : requestElements) {
			if ("username".equals(element.getName())) username = element.getValue();
			if ("password".equals(element.getName())) password = element.getValue();
		}

		// remember whether we are authorised for at least one application
		boolean authorised = false;

		// get the applications collection
		Applications applications = rapidRequest.getRapidServlet().getApplications();
		
		// if there are some applications
		if (applications != null) {
			// loop them
			for (Application app : applications.get()) {
				try {
					// create a new rapid request for this application
					RapidRequest rapidApplicationRequest = new RapidRequest(rapidRequest.getRapidServlet(), rapidRequest.getRequest(), app);
					// see if the user is known to this application
					authorised = app.getSecurityAdapter().checkUserPassword(rapidApplicationRequest, username, password);
					// we can exit if so as we only need one
					if (authorised) break;
				} catch (Exception ex) {
					// log the error
					rapidRequest.getRapidServlet().getLogger().error("Authentication webservice error checking user", ex);
				}
			}
		}

		// throw an error if not authorised
		if (!authorised) throw new WebserviceException("Not authorised");

		// assume there is no existing session for this user
		HttpSession session = null;
		// get the current sessions
		Map<String, HttpSession> sessions = RapidSessionListener.getSessions();
		// loop the keys
		for (String key : sessions.keySet()) {
			// get this session
			HttpSession s = sessions.get(key);
			// often get an illegal state exception which can't be predicted
			try {
				// get the user
				String u = (String) s.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
				// check user name
				if (u != null) {
					if (u.equals(username)) {
						// get the password
						String p = (String) s.getAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD);
						// check the password
						if (p != null) {
							if (p.equals(password)) {
								session = s;
								break;
							}
						}
					}
				}

			} catch (IllegalStateException ex) {

				rapidRequest.getRapidServlet().getLogger().error("Error checking session", ex);

			}

		}

		// if we couldn't find an existing session for this user
		if (session == null) {

			// get the request
			HttpServletRequest request = rapidRequest.getRequest();

			// get a new session
			session = request.getSession();

			// set the user name
			session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME, username);
			// set the password
			session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD, password);

			// start the request device details
			String deviceDetails = "agent=" + request.getHeader("User-Agent");

			try {
				// get the address from the request host
				InetAddress inetAddress = InetAddress.getByName(request.getRemoteHost());
				// get the request device details
				deviceDetails += ",ip=" + inetAddress.getHostAddress() + ",name=" + inetAddress.getHostName();
			} catch (UnknownHostException ex) {}

			// retain device id in the session so it's used when check app authorisation
			session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_DEVICE, deviceDetails);

		}

		// create the response element
		SOAElement responseElement = new SOAElement("authentication",session.getId());
		// create the response data
		SOAData responseData= new SOAData(responseElement);
		// return the response
		return responseData;
	}

}
