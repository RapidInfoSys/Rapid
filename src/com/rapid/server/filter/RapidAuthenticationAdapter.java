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

package com.rapid.server.filter;

import java.io.IOException;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public abstract class RapidAuthenticationAdapter {

	public static final String INIT_PARAM_IP_CHECK = "ipcheck";
	public static final String INIT_PARAM_PUBLIC_ACCESS = "public";
	public static final String PUBLIC_ACCESS_USER = "public";

	protected ServletContext _servletContext;

	public ServletContext getServletContext() { return _servletContext; }

	public RapidAuthenticationAdapter(FilterConfig filterConfig) {
		_servletContext = filterConfig.getServletContext();
	}

	public abstract ServletRequest process(ServletRequest request, ServletResponse response) throws IOException, ServletException;

}
