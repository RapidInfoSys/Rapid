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
<<<<<<< HEAD
		// if that didn't find anyone set to a password-less user from the session / connection adapter, unless new app
		if (user == null && !"newapp".equals(rapidRequest.getActionName())) user = new User(rapidRequest.getUserName(),"Public form user","");
=======
		// if that didn't find anyone set to a password-less user from the session / connection adapter
		if (user == null) {
			user = new User(rapidRequest.getUserName(), rapidRequest.getUserName() + " form user", "", "", "");
		}
>>>>>>> refs/remotes/origin/workflow
		// return
		return user;
	}

	@Override
	public boolean checkUserPassword(RapidRequest rapidRequest,	String userName, String password) throws SecurityAdapaterException {
		// get the action
		String action = rapidRequest.getActionName();
		// if there was one
		if (action != null) {
			// if this is an import we want to check the password properly so a fail will add the current user to the app
			if ("import".equals(action)) return super.checkUserPassword(rapidRequest, userName, password);
		}
		// everyone is allowed
		return true;
	}

}