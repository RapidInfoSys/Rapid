/*

Copyright (C) 2017 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.security;

import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletContext;

import com.rapid.core.Application;
import com.rapid.server.RapidRequest;

public class FormSecurityAdapter extends RapidSecurityAdapter {

	// constructor

	public FormSecurityAdapter(ServletContext servletContext, Application application) throws SecurityException,IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		// call the super constructor
		super(servletContext, application);
	}

	// overrides

	@Override
	public User getUser(RapidRequest rapidRequest) throws SecurityAdapaterException {
		// first try and get the user with the super method
		User user = super.getUser(rapidRequest);

		// if that didn't find anyone set to a password-less user from the session / connection adapter, unless new app
		if (user == null && !"newapp".equals(rapidRequest.getActionName())) user = new User(rapidRequest.getUserName(),"Public form user","", "", "");

		// return
		return user;
	}

	@Override
	public boolean checkUserPassword(RapidRequest rapidRequest,	String userName, String password) throws SecurityAdapaterException {
		// assume we don't need to check the password properly
		boolean check = false;
		// get the action
		String action = rapidRequest.getActionName();
		// if there was one
		if (action != null) {
			// if this is an import we want to check the password properly so a fail will add the current user to the app
			if ("import".equals(action)) check = true;
		}
		// get the request uri
		String uri = rapidRequest.getRequest().getRequestURI();
		// if there was one
		if (uri != null) {
			// if this was a login .jsp page we need to check
			if (uri.toLowerCase().contains("login") && uri.toLowerCase().endsWith(".jsp")) check = true;
		}
		// check will be true if the request to check came from a more sensitive area
		if (check) {
			// check the password properly
			return super.checkUserPassword(rapidRequest, userName, password);
		} else {
			// everyone is allowed
			return true;
		}
	}

}