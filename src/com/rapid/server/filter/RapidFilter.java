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

package com.rapid.server.filter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mysql.jdbc.StringUtils;
import com.rapid.core.Application;
import com.rapid.core.Applications;
import com.rapid.utils.Classes;

public class RapidFilter implements Filter {

	// different applications' security adapters will retrieve different user
	// objects
	public static final String SESSION_VARIABLE_INDEX_PATH = "index";
	public static final String SESSION_VARIABLE_USER_NAME = "user";
	public static final String SESSION_VARIABLE_USER_PASSWORD = "password";
	public static final String SESSION_VARIABLE_USER_DEVICE = "device";
	public static final String SESSION_VARIABLE_PASSWORDRESET_PATH = "passwordreset";

	private static Logger _logger = LogManager.getLogger(RapidFilter.class);

	private RapidAuthenticationAdapter _authenticationAdapter;
	private boolean _noCaching;

	private Set<String> _resourceDirs = null;
	private int _contextIdx = 1;		//keep track of the context position of the URL

	// overrides

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

		try {

			// set the value from stopCaching from the init parameter in web.xml
			_noCaching = Boolean.parseBoolean(filterConfig.getServletContext().getInitParameter("noCaching"));

			// look for a specified authentication adapter
			String authenticationAdapterClass = filterConfig.getInitParameter("authenticationAdapterClass");

			// if we didn't find one
			if (authenticationAdapterClass == null) {

				// fall back to the FormAuthenticationAdapter
				_authenticationAdapter = new FormAuthenticationAdapter(filterConfig);

			} else {

				// try and instantiate the authentication adapter
				Class classClass = Class.forName(authenticationAdapterClass);
				// check this class has the right super class
				if (!Classes.extendsClass(classClass, com.rapid.server.filter.RapidAuthenticationAdapter.class))
					throw new Exception(authenticationAdapterClass
							+ " must extend com.rapid.server.filter.RapidAuthenticationAdapter.");
				// instantiate an object and retain
				_authenticationAdapter = (RapidAuthenticationAdapter) classClass.getConstructor(FilterConfig.class)
						.newInstance(filterConfig);

			}

		} catch (Exception ex) {

			throw new ServletException("Rapid filter initialisation failed. Reason: " + ex, ex);

		}

		_logger.info("Rapid filter initialised.");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		_logger.trace("Process filter request...");

		// fake slower responses like on mobile
		// try { Thread.sleep(10000); } catch (InterruptedException e) {}

		// cast the request to http servlet
		HttpServletRequest req = (HttpServletRequest) request;

		// cast the response to http servlet
		HttpServletResponse res = (HttpServletResponse) response;

		// get the user agent
		String ua = req.getHeader("User-Agent");

		// if IE send X-UA-Compatible to prevent compatibility view
		if (ua != null && ua.indexOf("MSIE") != -1)
			res.addHeader("X-UA-Compatible", "IE=edge,chrome=1");

		// set all responses as UTF-8
		response.setCharacterEncoding("utf-8");

		// if no caching is on, try and prevent cache
		if (_noCaching)
			noCache(res);

		// assume this request requires authentication
		boolean requiresAuthentication = true;

		// 1. Get the requested URI without the hostname -- gets servlet path
		String path = req.getServletPath();

		// all webservice related requests got to the soa servelet
		if ("/soa".equals(path)) {
			// if this is a get request
			if ("GET".equals(req.getMethod())) {
				// remember we don't need authentication
				requiresAuthentication = false;
			} else {
				// get the content type (only present for POST)
				String contentType = request.getContentType();
				// if we got one
				if (contentType != null) {
					// put into lower case
					contentType = contentType.toLowerCase();
					// check this is known type of soa request xml
					if (((req.getHeader("Action") != null || req.getHeader("SoapAction") != null)
							&& contentType.contains("xml"))
							|| (req.getHeader("Action") != null && contentType.contains("json"))) {
						// remember we don't need authentication
						requiresAuthentication = false;
					}
				}
			}
		}

		// online.htm doesn't need authentication
		if ("/online.htm".equals(path))
			requiresAuthentication = false;

		// safety doesn't need authentication
		if ("/safety".equals(path))
			requiresAuthentication = false;

		// if this request requires authentication
		if (requiresAuthentication) {
			// get a filtered request
			ServletRequest filteredRequest = _authenticationAdapter.process(request, response);
			String[] pathPart = (path.replaceFirst("/", "")).split("/");

			// continue the rest of the chain with it if we got one
			if (filteredRequest != null) {

				String rapidForwardURL;
				Applications applications = (Applications) request.getServletContext().getAttribute("applications");
				
				
				// if user has provided at least 1 path part (i.e. part1/) and the first part is a known application
				if (pathPart.length >= 1 && applications.get(pathPart[0]) != null) {

					// get the resource filenames only once
					if (_resourceDirs == null) {
						setResourceDirs(req);
					}

					String appID = pathPart[0];
					rapidForwardURL = "/~?a=" + appID;
					
					if ("POST".equals(req.getMethod()) || ("GET".equals(req.getMethod()) && "~".equals(pathPart[pathPart.length - 1]))) {
						rapidForwardURL = "/~?" + req.getQueryString();
					

					} else { //any other get requests

						String version, page;
						
						switch (pathPart.length) {
						case 1:	//if URL contains only the appID
							rapidForwardURL = "/~?a=" + appID;
							break;
						
						case 2:	//if URL contains appID/p or appID/v
							// if what followed by appID is a Resource folder deal with it
							if (isResource(pathPart[1])) {
								rapidForwardURL = path.replaceFirst("/" + appID, "");
					
							} else { // otherwise it must be an application
								
								// Check whether 2nd part is a page or version only
								if ("p".equalsIgnoreCase(String.valueOf(pathPart[1].charAt(0)))) {
									page = pathPart[1];
									rapidForwardURL += "&p=" + page;

								} else {
									version = pathPart[1];
									rapidForwardURL += "&v=" + version;
								}
								
								_contextIdx = 1;

							}
							break;
							
						default:	// if more than 2 parts
							// check if this is a resource - remember the contextIdx position
							if (isResource(pathPart[_contextIdx])) {
								// restructure the url- without the applicationName/versionNumber
								if (_contextIdx < 2) {
									rapidForwardURL = path.replaceFirst("/" + appID, "");
								} else {
									rapidForwardURL = path.replaceFirst("/" + appID + "/" + pathPart[1], "");
								}

							} else { // otherwise it must be an application
								version = pathPart[1];
								page = pathPart[2];
								rapidForwardURL += "&v=" + version + "&p=" + page;
								_contextIdx = 2;
							}
							break;
						}//end of switch
						

					}// end of if
					
					// forward the newly reconstructed URL
					RequestDispatcher dispatcher = filteredRequest.getRequestDispatcher(rapidForwardURL);
					dispatcher.forward(filteredRequest, response);
					
				} else {

					// for regular rapid url formats
					filterChain.doFilter(filteredRequest, response);

				}

			}

		} else {// for requests not requiring authentication
			// continue the rest of the chain
			filterChain.doFilter(request, response);
		}
	}

	// setter method
	private void setResourceDirs(HttpServletRequest request) {

		// check whether this is a RESOURCE or application request
		String webContentString = request.getServletContext().getRealPath("/");
		File webContentDir = new File(webContentString);
		File[] directoryListing = webContentDir.listFiles();

		_resourceDirs = new HashSet<String>();

		// Loop over the root of the WebContent directory, to see whether
		// appID/** is followed by one of the folders in this directory
		// If so, then this request must be a RESOURCE--
		if (directoryListing != null) {
			for (File child : directoryListing) {
				// if
				if (child.isDirectory()) {
					_resourceDirs.add(child.getName());
				}
			}
		}
	}

	private boolean isResource(String fileName) {

		if (_resourceDirs.contains(fileName)) {
			return true;
		}

		return false;
	}

	@Override
	public void destroy() {
	}

	// public static method

	public static void noCache(HttpServletResponse response) {

		// if we were provided with a reponse object
		if (response != null) {

			// try and avoid caching
			response.setHeader("Expires", "Tue, 1 January 1980 12:00:00 GMT");

			// Set standard HTTP/1.1 no-cache headers.
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

			// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
			response.addHeader("Cache-Control", "post-check=0, pre-check=0");

			// Set standard HTTP/1.0 no-cache header.
			response.setHeader("Pragma", "no-cache");

		}

	}

}