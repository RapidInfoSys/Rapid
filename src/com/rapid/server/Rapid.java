/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Event;
import com.rapid.core.Page;
import com.rapid.core.Page.RoleHtml;
import com.rapid.security.SecurityAdapater;
import com.rapid.security.SecurityAdapater.Role;
import com.rapid.security.SecurityAdapater.User;
import com.rapid.utils.Files;
import com.rapid.utils.Html;

public class Rapid extends RapidHttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	// these are held here and referred to globally
	public static final String VERSION = "2.2.1";
	public static final String DESIGN_ROLE = "RapidDesign";
	public static final String ADMIN_ROLE = "RapidAdmin";
	public static final String SUPER_ROLE = "RapidSuper";
	public static final String BACKUP_FOLDER = "_backups";
					
	// this byte buffer is used for reading the post data
	private byte[] _byteBuffer = new byte[1024];
					     
    private String getAdminLink(String appId, String pageId) {
    	
    	String html = "<div id='designShow' style='position:fixed;left:0px;bottom:0px;width:30px;height:30px;z-index:1000;'></div>\n"
    	+ "<img id='designLink' style='position:fixed;left:6px;bottom:6px;z-index:1001;display: none;' src='images/gear_24x24.png'></img>\n"
    	+ "<script type='text/javascript'>\n"
    	+ "/* designLink */\n"
    	+ "$(document).ready( function() {\n"
    	+ "  $('#designShow').mouseover ( function(ev) { $('#designLink').show(); });\n"
    	+ "  $('#designLink').mouseout ( function(ev) { $('#designLink').hide(); });\n"
    	+ "  $('#designLink').click ( function(ev) { window.location='design.jsp?a=" + appId + "&p=" + pageId + "' });\n"
    	+ "})\n"
    	+ "</script>\n";
    	
    	return html;
    	
    }
                
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
										
		getLogger().debug("Rapid GET request : " + request.getQueryString());
											
		// whether we're rebulding the page for each request
    	boolean rebuildPages = Boolean.parseBoolean(getServletContext().getInitParameter("rebuildPages"));
						
		// get a new rapid request passing in this servelet and the http request
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		// set the page html to an empty string
		String pageHtml = "";
					
		try {
			
			// get the application object
			Application app = rapidRequest.getApplication();
			
			// check app exists
			if (app == null) {
				
				// set the status code
				response.setStatus(404);
				
				// create a writer
				PrintWriter out = response.getWriter();
				
				// write a friendly message
				out.print("Application not found on this server");
				
				// close
				out.close();
				
				//log
				getLogger().debug("Rapid GET response : Application not found on this server");
				
			} else {
			
				// check if there is a Rapid action
				if ("download".equals(rapidRequest.getActionName())) {
									
					// create the zip file
					app.zip(this, rapidRequest, app.getId() + ".zip", true);
					
					// set the type as a .zip
					response.setContentType("application/x-zip-compressed");
					
					// Shows the download dialog
					response.setHeader("Content-disposition","attachment; filename=" + app.getId() + ".zip");
															
					// get the file for the zip we're about to create
					File zipFile = new File(getServletContext().getRealPath("/WEB-INF/temp/" + app.getId() + ".zip"));
					
					// get it's size
					long fileSize = Files.getSize(zipFile);
					
					// add size to response headers if small enough
					if (fileSize < Integer.MAX_VALUE) response.setContentLength((int) fileSize);
					
					// send the file to browser
					OutputStream os = response.getOutputStream();
					FileInputStream in = new FileInputStream(zipFile);
					byte[] buffer = new byte[1024];
					int length;
					while ((length = in.read(buffer)) > 0){
					  os.write(buffer, 0, length);
					}
					in.close();
					os.flush();
					
				} else {
					
					// create a writer
					PrintWriter out = response.getWriter();
							
					// get the page object
					Page page = rapidRequest.getPage();
							
					// check we got one
					if (page != null) {
						
						if (rebuildPages) {
							
							// (re)generate the page start html
							pageHtml = page.getStartHtml(rapidRequest.getApplication());
							
						} else {
						
							// get any cached header html from the page object (this will regenerate and cache if not present)
							pageHtml = page.getCachedStartHtml(rapidRequest.getApplication());
													
						}
						
						// output the start of the page
						out.print(pageHtml);
						
						// get the application security
						SecurityAdapater security = app.getSecurity();
						
						// if we have some
						if (security != null) {
							
							// get the userName
							String userName = rapidRequest.getUserName();
							
							// get the user
							User user = security.getUser(rapidRequest, userName);
							
							// if we didn't get a user work with an empty one
							if (user == null) user = new User();
							
							// get the users roles
							List<String> userRoles = user.getRoles();
												
							// retrieve and rolesHtml for the page
							List<RoleHtml> rolesHtml = page.getRolesHtml();
		
							// check we have userRoles and htmlRoles
							if (userRoles != null && rolesHtml != null) {
																					
								// loop each roles html entry
								for (RoleHtml roleHtml : rolesHtml) {
																
									// get the roles from this combination
									List<String> roles = roleHtml.getRoles();
																
									// keep a running count for the roles we have
									int gotRoleCount = 0;
									
									// if there are roles to check
									if (roles != null) {
									
										// retain how many roles we need our user to have
										int rolesRequired = roles.size();
										
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
													}
												}
											}									
										}
																		
										// if we have all the roles we need
										if (gotRoleCount == rolesRequired) {
											// use this html
											out.print("  " + roleHtml.getHtml());
											// no need to check any further
											break;
										}
										
									} else {
										
										// no roles to check means we can use this html immediately 
										out.print("  " + Html.getPrettyHtml(page.getHtmlBody()).trim());
										
									}
									
								}
									
																										
							} else {
								
								out.print("  " + Html.getPrettyHtml(page.getHtmlBody()).trim());
								
							}
						
							// check for the design role, super is required as well if the rapid app
							if ("rapid".equals(app.getId())) {
								if (security.checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE) && security.checkUserRole(rapidRequest, userName, Rapid.SUPER_ROLE)) out.print(getAdminLink(app.getId(), page.getId()).trim());
							} else {
								if (security.checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE)) out.print(getAdminLink(app.getId(), page.getId()).trim());
							}
												
						} else {
							
							out.print("  " + Html.getPrettyHtml(page.getHtmlBody()).trim());
							
						} // security check
									
						// add the remaining elements
						out.print("  </body>\n</html>");
						
						// close the writer
						out.close();
																						
					} // page check
							
				} // action name check
				
			} // app exists check
								
		} catch (Exception ex) {
		
			getLogger().error("Rapid GET error : ",ex);
			
			sendException(rapidRequest, response, ex);
		
		} 
																			
		getLogger().trace("Rapid GET response : " + pageHtml);
					
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		
		// read bytes from request body into our own byte array (this means we can deal with images) 
		InputStream input = request.getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();												
		for (int length = 0; (length = input.read(_byteBuffer)) > -1;) outputStream.write(_byteBuffer, 0, length);			
		byte[] bodyBytes = outputStream.toByteArray();
		
		String bodyString = new String(bodyBytes, "UTF-8");
				
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		StringBuilder stringBuilder = new StringBuilder();
		
		getLogger().debug("Rapid POST request : " + request.getQueryString() + " body : " + bodyString);
					
		try {
			
			if (rapidRequest.getAction() != null) {								
				
				JSONObject jsonData = null;
				
				if (!"".equals(bodyString)) jsonData = new JSONObject(bodyString);
					
				JSONObject jsonResult = rapidRequest.getAction().doAction(this, rapidRequest, jsonData);
				
				stringBuilder.append(jsonResult.toString());	
								
			} else if ("getApps".equals(rapidRequest.getActionName())) {
				
				JSONArray jsonApps = new JSONArray();
				
				List<Application> apps = getSortedApplications();
				
				if (apps != null) {
					
					for (Application app : apps) {
						
						String userName = rapidRequest.getUserName();
						
						SecurityAdapater security = app.getSecurity();
						
						// fail silently if there was an issue
						try {
						
							User user = security.getUser(rapidRequest, userName);
							
							if (user != null) {
								
								JSONObject jsonApp = new JSONObject();
								jsonApp.put("id", app.getId());
								jsonApp.put("title", app.getTitle());
								jsonApps.put(jsonApp);
								
							}
							
						} catch (Exception ex) {}
																									
					}
					
				}
				
				stringBuilder.append(jsonApps.toString());
				
			}
			
		} catch (Exception ex) {
		
			getLogger().error("Rapid POST error : ",ex);
			
			sendException(rapidRequest, response, ex);
		
		} 
		
		// all responses are in json
		response.setContentType("application/json");
															
		out.print(stringBuilder.toString());
		out.close();
		
		getLogger().debug("Rapid POST response : " + stringBuilder.toString());
		
	}

}
