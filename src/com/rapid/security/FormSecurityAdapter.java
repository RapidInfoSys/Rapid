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

package com.rapid.security;

import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletContext;

import com.rapid.core.Application;
import com.rapid.server.RapidRequest;

public class FormSecurityAdapter extends RapidSecurityAdapter {
	
	// private instance methods
	protected User _formUser;

	// constructor
	
	public FormSecurityAdapter(ServletContext servletContext, Application application) throws SecurityException,IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		// call the super constructor
		super(servletContext, application);
		// make the form user which all anonymous users will use
		_formUser = new User("public","Public form user","");
	}
	
	// overrides
	
	@Override
	public User getUser(RapidRequest rapidRequest) throws SecurityAdapaterException {
		// first try and get the user with the super method
		User user = super.getUser(rapidRequest);
		// if that didn'y find anyone set to an anonymous user
		if (user == null) user = _formUser;
		// return
		return user;
	}

	@Override
	public boolean checkUserPassword(RapidRequest rapidRequest,	String userName, String password) throws SecurityAdapaterException {
		// everyone is allowed
		return true;
	}
						
}
