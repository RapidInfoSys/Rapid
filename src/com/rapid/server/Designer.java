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

package com.rapid.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.Role;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.utils.Bytes;
import com.rapid.utils.Files;
import com.rapid.utils.XML;
import com.rapid.utils.ZipFile;
import com.rapid.actions.Logic.Condition;
import com.rapid.core.Application;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.core.Applications.Versions;
import com.rapid.core.Page;
import com.rapid.core.Page.Lock;
import com.rapid.core.Control;
import com.rapid.core.Pages.PageHeader;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.data.DataFactory.Parameters;

public class Designer extends RapidHttpServlet {
			
	private static final long serialVersionUID = 2L;
			
	// this byte buffer is used for reading the post data
	byte[] _byteBuffer = new byte[1024];
       
    public Designer() { super(); }
    
    private void sendJsonOutput(HttpServletResponse response, String output) throws IOException {
    	
    	// set response as json
		response.setContentType("application/json");
		
		// get a writer from the response
		PrintWriter out = response.getWriter();
		
		// write the output into the response
		out.print(output);
		
		// close the writer
		out.close();
		
		// send it immediately
		out.flush();
    	
    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
							
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		try {
			
			getLogger().debug("Designer GET request : " + request.getQueryString());
											
			String actionName = rapidRequest.getActionName();
			
			String output = "";
			
			// get the rapid application
			Application rapidApplication = getApplications().get("rapid");
			
			// check we got one
			if (rapidApplication != null) {
				
				// get rapid security
				SecurityAdapter rapidSecurity = rapidApplication.getSecurityAdapter();
				
				// check we got some
				if (rapidSecurity != null) {
					
					// get the user name
					String userName = rapidRequest.getUserName();
					
					// get the rapid user
					User rapidUser = rapidSecurity.getUser(rapidRequest);

					// check permission
					if (rapidSecurity.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) {
						
						// whether we're trying to avoid caching
				    	boolean noCaching = Boolean.parseBoolean(getServletContext().getInitParameter("noCaching"));
				    	
				    	if (noCaching) {
						
							// try and avoid caching
							response.setHeader("Expires", "Sat, 15 March 1980 12:00:00 GMT");
				
							// Set standard HTTP/1.1 no-cache headers.
							response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
				
							// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
							response.addHeader("Cache-Control", "post-check=0, pre-check=0");
				
							// Set standard HTTP/1.0 no-cache header.
							response.setHeader("Pragma", "no-cache");
							
				    	}
															
						if ("getSystemData".equals(actionName)) {
							
							// create a system data object
							JSONObject jsonSystemData = new JSONObject();
							
							// add the controls
							jsonSystemData.put("controls", getJsonControls());
							
							// add the actions
							jsonSystemData.put("actions", getJsonActions());
							
							// add the devices
							jsonSystemData.put("devices", getDevices());
							
							// put into output string
							output = jsonSystemData.toString();
							
							// send output as json
							sendJsonOutput(response, output);							
						
						} else if ("getApps".equals(actionName)) {
							
							// create a json array for holding our apps
							JSONArray jsonApps = new JSONArray();
							
							// get a sorted list of the applications
							for (String id : getApplications().getIds()) {
								
								// loop the versions
								for (String version : getApplications().getVersions(id).keySet()) {
									
									// get the this application version
									Application application = getApplications().get(id, version);
									
									// get the security									
									SecurityAdapter security = application.getSecurityAdapter();
									
									// recreate the request in the name of this app
									RapidRequest appRequest = new RapidRequest(this, request, application);
									
									// check the users password
									if (security.checkUserPassword(appRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
										
										// check the users permission to design this application
										boolean designPermission = security.checkUserRole(appRequest, Rapid.DESIGN_ROLE);
										
										// if app is rapid do a further check
										if (designPermission && "rapid".equals(application.getId())) designPermission = security.checkUserRole(appRequest, Rapid.SUPER_ROLE);
										
										// if we got permssion - add this application to the list 
										if (designPermission) {
											// create a json object
											JSONObject jsonApplication = new JSONObject();
											// add the details we want
											jsonApplication.put("id", application.getId());
											jsonApplication.put("name", application.getName());
											jsonApplication.put("title", application.getTitle());											
											// add the object to the collection
											jsonApps.put(jsonApplication);
											// no need to check any further versions
											break;
										}
										
									}
									
								}
																									
							}
							
							output = jsonApps.toString();
							
							sendJsonOutput(response, output);
							
						} else if ("getVersions".equals(actionName)) {
							
							// create a json array for holding our apps
							JSONArray jsonVersions = new JSONArray();
							
							// get the app id
							String appId = rapidRequest.getAppId();
									
							// get the versions
							Versions versions = getApplications().getVersions(appId);
							
							// if there are any
							if (versions != null) {
																													
								// loop the list of applications sorted by id (with rapid last)
								for (Application application : versions.sort()) {
									
									// get the security									
									SecurityAdapter security = application.getSecurityAdapter();
									
									// recreate the request in the name of this app
									RapidRequest appRequest = new RapidRequest(this, request, application);
									
									// check the users password
									if (security.checkUserPassword(appRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
										
										// check the users permission to design this application
										boolean designPermission = application.getSecurityAdapter().checkUserRole(appRequest, Rapid.DESIGN_ROLE);
										
										// if app is rapid do a further check
										if (designPermission && "rapid".equals(application.getId())) designPermission = security.checkUserRole(appRequest, Rapid.SUPER_ROLE);
										
										// check the RapidDesign role is present in the users roles for this application
										if (designPermission) {												
											
											// make a json object for this version
											JSONObject jsonVersion = new JSONObject();
											// add the app id
											jsonVersion.put("id", application.getId());
											// add the version
											jsonVersion.put("version", application.getVersion());
											// add the status
											jsonVersion.put("status", application.getStatus());
											// add the title
											jsonVersion.put("title", application.getTitle());
											// add a formAdapter if present
											if (application.getFormAdapter() != null) jsonVersion.put("formAdapter", true);
											// add whether to show control Ids
											jsonVersion.put("showControlIds", application.getShowControlIds());
											// add whether to show action Ids
											jsonVersion.put("showActionIds", application.getShowActionIds());
											// add the web folder so we can update the iframe style sheets
											jsonVersion.put("webFolder", Application.getWebFolder(application));
											
											// get the database connections
											List<DatabaseConnection> databaseConnections = application.getDatabaseConnections();
											// check we have any
											if (databaseConnections != null) {
												// make an object we're going to return
												JSONArray jsonDatabaseConnections = new JSONArray();
												// loop the connections
												for (DatabaseConnection databaseConnection : databaseConnections) {
													// add the connection name
													jsonDatabaseConnections.put(databaseConnection.getName());
												}
												// add the connections to the app 
												jsonVersion.put("databaseConnections", jsonDatabaseConnections);
											}
											
											// make an object we're going to return
											JSONArray jsonRoles = new JSONArray();
											// retrieve the roles
											List<Role> roles = security.getRoles(rapidRequest);														
											// check we got some
											if (roles != null) {			
												// create a collection of names
												ArrayList<String> roleNames = new ArrayList<String>();
												// copy the names in if non-null
												for (Role role : roles) if (role.getName() != null) roleNames.add(role.getName());
												// sort them
												Collections.sort(roleNames);
												// loop the sorted connections
												for (String roleName : roleNames) {
													// only add role if this is the rapid app, or it's not a special rapid permission
													if ("rapid".equals(application.getId()) || (!Rapid.ADMIN_ROLE.equals(roleName) && !Rapid.DESIGN_ROLE.equals(roleName)&& !Rapid.SUPER_ROLE.equals(roleName))) jsonRoles.put(roleName);
												}
											}							
											// add the security roles to the app 
											jsonVersion.put("roles", jsonRoles);
																												
											// get all the possible json actions
											JSONArray jsonActions = getJsonActions();
											// make an array for the actions in this app
											JSONArray jsonAppActions = new JSONArray();
											// get the types used in this app
											List<String> actionTypes = application.getActionTypes();
											// if we have some
											if (actionTypes != null) {
												// loop the types used in this app						
												for (String actionType : actionTypes) {
													// loop all the possible actions
													for (int i = 0; i < jsonActions.length(); i++) {
														// get an instance to the json action
														JSONObject jsonAction = jsonActions.getJSONObject(i);
														// if this is the type we've been looking for
														if (actionType.equals(jsonAction.getString("type"))) {
															// create a simple json object for thi action
															JSONObject jsonAppAction = new JSONObject();
															// add just what we need
															jsonAppAction.put("type", jsonAction.getString("type"));
															jsonAppAction.put("name", jsonAction.getString("name"));
															// add it to the app actions collection
															jsonAppActions.put(jsonAppAction);
															// start on the next app action
															break;
														}
													}
												}
											}						
											// put the app actions we've just built into the app
											jsonVersion.put("actions", jsonAppActions);						
											
											// get all the possible json controls
											JSONArray jsonControls = getJsonControls();
											// make an array for the controls in this app
											JSONArray jsonAppControls = new JSONArray();
											// get the control types used by this app
											List<String> controlTypes = application.getControlTypes();
											// if we have some
											if (controlTypes != null) {
												// loop the types used in this app						
												for (String controlType : controlTypes) {
													// loop all the possible controls
													for (int i = 0; i < jsonControls.length(); i++) {
														// get an instance to the json control
														JSONObject jsonControl = jsonControls.getJSONObject(i);
														// if this is the type we've been looking for
														if (controlType.equals(jsonControl.getString("type"))) {
															// create a simple json object for this control
															JSONObject jsonAppControl = new JSONObject();
															// add just what we need
															jsonAppControl.put("type", jsonControl.getString("type"));
															jsonAppControl.put("name", jsonControl.getString("name"));
															jsonAppControl.put("image", jsonControl.optString("image"));
															jsonAppControl.put("canUserAdd", jsonControl.optString("canUserAdd"));
															// add it to the app controls collection
															jsonAppControls.put(jsonAppControl);
															// start on the next app control
															break;
														}
													}
												}
											}						
											// put the app controls we've just built into the app
											jsonVersion.put("controls", jsonAppControls);
											
											
											// create a json object for the images
											JSONArray jsonImages = new JSONArray();									
											// get the directory in which the control xml files are stored	
											File dir = new File (application.getWebFolder(getServletContext()));
											// create a filter for finding image files
											FilenameFilter xmlFilenameFilter = new FilenameFilter() {
										    	public boolean accept(File dir, String name) {
										    		return name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg");
										    	}
										    };
										    // a array to hold the images as they come out of the filter
										    List<String> images = new ArrayList<String>();
											// loop the image files in the folder
											for (File imageFile : dir.listFiles(xmlFilenameFilter)) {
												images.add(imageFile.getName());												
											}											
											// sort the images
											Collections.sort(images);
											// loop the sorted images and add to json
											for (String image : images) jsonImages.put(image);
											
											// put the images collection we've just built into the app
											jsonVersion.put("images", jsonImages);
											
											// create a json array for our style classes
											JSONArray jsonStyleClasses = new JSONArray();
											// get all of the possible style classes
											List<String> styleClasses = application.getStyleClasses();
											// if we had some
											if (styleClasses != null) {
												// loop and add to json array
												for (String styleClass : styleClasses) jsonStyleClasses.put(styleClass);
											}
											// put them into our application object
											jsonVersion.put("styleClasses", jsonStyleClasses);
											
											// put the app into the collection
											jsonVersions.put(jsonVersion);	
											
										} // design permission
										
									} // check user password
																																																					
								} // versions loop
									
							} // got versions check
							
							output = jsonVersions.toString();
							
							sendJsonOutput(response, output);
							
						} else if ("getPages".equals(actionName)) {
							
							Application application = rapidRequest.getApplication();
							
							if (application != null) {
								
								JSONArray jsonPages = new JSONArray();
								
								String startPageId = "";
								Page startPage = application.getStartPage(getServletContext());
								if (startPage != null) startPageId = startPage.getId();
								
								// loop the page headers
								for (PageHeader pageHeader : application.getPages().getSortedPages()) {
									// get the page - yip this means that apps loaded in the designer load all of their pages
									Page page = application.getPages().getPage(getServletContext(), pageHeader.getId());
									// create a simple json object for the page
									JSONObject jsonPage = new JSONObject();
									// add simple properties
									jsonPage.put("id", page.getId());
									jsonPage.put("name", page.getName());
									jsonPage.put("title", page.getTitle());
									jsonPage.put("label", page.getLabel());		
									// get a list of page session variables
									List<String> pageSessionVariables = page.getSessionVariables();
									// add them if there are some
									if (pageSessionVariables != null) if (pageSessionVariables.size() > 0) 	jsonPage.put("sessionVariables", pageSessionVariables);
									// assume we don't need to know page visibilty
									boolean includePageVisibiltyControls = false;
									// if there is a form adapter
									if (application.getFormAdapter() != null) {
										// set to true
										includePageVisibiltyControls = true;
										// add visibility conditions
										List<Condition> pageVisibilityConditions = page.getVisibilityConditions();
										// add them if there are some
										if (pageVisibilityConditions != null) if (pageVisibilityConditions.size() > 0) jsonPage.put("visibilityConditions", pageVisibilityConditions); 
									}
									// get a collection of other page controls in this page
									JSONArray jsonOtherPageControls = page.getOtherPageControls(this, includePageVisibiltyControls);
									// only add the property if there are some
									if (jsonOtherPageControls.length() > 0) jsonPage.put("controls", jsonOtherPageControls);
									// check if the start page and add property if so
									if (startPageId.equals(page.getId())) jsonPage.put("startPage", true);
									// add the page to the collection
									jsonPages.put(jsonPage);						
								}
								
								// set the output to the collection turned into a string			
								output = jsonPages.toString();
								
								sendJsonOutput(response, output);
								
							}
							
						} else if ("getPage".equals(actionName)) {
							
							Application application = rapidRequest.getApplication();
							
							Page page = rapidRequest.getPage();
							
							if (page != null) {
								
								// assume we can't find the user
								String userDescription = "unknown";
								// get the user 
								User user = application.getSecurityAdapter().getUser(rapidRequest);
								// if we had one and they have a description use it
								if (user != null) if (user.getDescription() != null) userDescription = user.getDescription();
																								
								// remove any existing page locks for this user
								application.removeUserPageLocks(getServletContext(), userName);
								
								// check the page lock (which removes it if it has expired)
								page.checkLock();
								
								// if there is no current lock add a fresh one for the current user
								if (page.getLock() == null)	page.setLock(new Lock(userName, userDescription, new Date()));
											
								// turn it into json
								JSONObject jsonPage = new JSONObject(page);
								
								// remove the bodyHtml property as it in the designer
								jsonPage.remove("htmlBody");
								// remove the rolesHtml property as it is rebuilt in the designer
								jsonPage.remove("rolesHtml");
								// remove allControls (the single all-control list) it is not required
								jsonPage.remove("allControls");
								// remove the otherPageControls property as it is sent with getPages
								jsonPage.remove("otherPageControls");
								
								// add a nicely formatted lock time
								if (page.getLock() != null && jsonPage.optJSONObject("lock") != null) {
									// get the date time formatter and format the lock date time
									String formattedDateTime = getLocalDateTimeFormatter().format(page.getLock().getDateTime());
									// add a special property to the json
									jsonPage.getJSONObject("lock").put("formattedDateTime", formattedDateTime);
								}
								
								// add the css
								jsonPage.put("css", page.getAllCSS(getServletContext(), application));
								
								// add the device as page properties (even though we store this in the app)
								jsonPage.put("device", 1);
								jsonPage.put("zoom", 1);
								jsonPage.put("orientation", "P");
																															
								// print it to the output
								output = jsonPage.toString();
								
								// send as json response
								sendJsonOutput(response, output);
								
								// override for now as logging seems really slow
								//output = "[json object - " + output.length() + " characters ]";
								
							}
															
						} else if ("checkApp".equals(actionName)) {
							
							String appName = request.getParameter("name");
							
							if (appName != null) {
								
								// retain whether we have an app with this name
								boolean exists =  getApplications().exists(Files.safeName(appName));
																					
								// set the response
								output = Boolean.toString(exists);
								// send response as json
								sendJsonOutput(response, output);
								
							}
															
						} else if ("checkVersion".equals(actionName)) {
							
							String appName = request.getParameter("name");
							String appVersion = request.getParameter("version");
							
							if (appName != null && appVersion != null ) {
								
								// retain whether we have an app with this name
								boolean exists =  getApplications().exists(Files.safeName(appName), Files.safeName(appVersion));
																					
								// set the response
								output = Boolean.toString(exists);
								// send response as json
								sendJsonOutput(response, output);
								
							}
															
						} else if ("checkPage".equals(actionName)) {
							
							String pageName = request.getParameter("name");
							
							if (pageName != null) {
								
								// retain whether we have an app with this name
								boolean pageExists = false;
								
								// get the application
								Application application = rapidRequest.getApplication();
								
								if (application != null) {
									
									for (PageHeader page : application.getPages().getSortedPages()) {
										if (pageName.toLowerCase().equals(page.getName().toLowerCase())) {
											pageExists = true;
											break;
										}					
									}
									
								}
													
								// set the output
								output = Boolean.toString(pageExists);
								// send response as json
								sendJsonOutput(response, output);
								
							}
															
						} else if ("export".equals(actionName)) {
							
							// get the application
							Application application = rapidRequest.getApplication();
							
							// check we've got one
							if (application != null) {
																		
								// create the zip file
								application.zip(this, rapidRequest, rapidUser, application.getId() + ".zip");
								
								// set the type as a .zip
								response.setContentType("application/x-zip-compressed");
								
								// Shows the download dialog
								response.setHeader("Content-disposition","attachment; filename=" + application.getId() + ".zip");
																		
								// get the file for the zip we're about to create
								File zipFile = new File(getServletContext().getRealPath("/WEB-INF/temp/" + application.getId() + ".zip"));
								
								// send the file to browser
								OutputStream out = response.getOutputStream();
								FileInputStream in = new FileInputStream(zipFile);
								byte[] buffer = new byte[1024];
								int length;
								while ((length = in.read(buffer)) > 0){
								  out.write(buffer, 0, length);
								}
								in.close();
								out.flush();	
								
								// delete the .zip file
								zipFile.delete();
								
								output = "Zip file sent";
													
							} // got application
												
						} // export
						
					} else {
						
						// not authenticated
						response.setStatus(403);
						
					} // got design role
					
				} // rapidSecurity != null
				
			} // rapidApplication != null
																					
			// log the response
			getLogger().debug("Designer GET response : " + output);
															
		} catch (Exception ex) {
			
			getLogger().debug("Designer GET error : " + ex.getMessage(), ex); 
			
			sendException(rapidRequest, response, ex);
			
		}
		
	}
		
	private Control createControl(JSONObject jsonControl) throws JSONException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		
		// instantiate the control with the JSON
		Control control = new Control(jsonControl);
		
		// look in the JSON for a validation object
		JSONObject jsonValidation = jsonControl.optJSONObject("validation");
		// add the validation object if we got one
		if (jsonValidation != null) control.setValidation(Control.getValidation(this, jsonValidation));
		
		// look in the JSON for an event array
		JSONArray jsonEvents = jsonControl.optJSONArray("events");
		// add the events if we found one
		if (jsonEvents != null) control.setEvents(Control.getEvents(this, jsonEvents));
				
		// look in the JSON for a styles array
		JSONArray jsonStyles = jsonControl.optJSONArray("styles");
		// if there were styles
		if (jsonStyles != null) control.setStyles(Control.getStyles(this, jsonStyles));
		
		// look in the JSON for any child controls
		JSONArray jsonControls = jsonControl.optJSONArray("childControls");
		// if there were child controls loop and create controls interatively
		if (jsonControls != null) {
			for (int i = 0; i < jsonControls.length(); i++) {								
				control.addChildControl(createControl(jsonControls.getJSONObject(i)));
			}
		}
			
		// return the control we just made
		return control;		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		try {
			
			String output = "";		
			
			// read bytes from request body into our own byte array (this means we can deal with images) 
			InputStream input = request.getInputStream();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();												
			for (int length = 0; (length = input.read(_byteBuffer)) > -1;) outputStream.write(_byteBuffer, 0, length);			
			byte[] bodyBytes = outputStream.toByteArray();
												
			// get the rapid application
			Application rapidApplication = getApplications().get("rapid");
			
			// check we got one
			if (rapidApplication != null) {
				
				// get rapid security
				SecurityAdapter rapidSecurity = rapidApplication.getSecurityAdapter();
				
				// check we got some
				if (rapidSecurity != null) {
					
					// get user name
					String userName = rapidRequest.getUserName();		
					if (userName == null) userName = "";
					
					// check permission
					if (rapidSecurity.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) {
						
						Application application = rapidRequest.getApplication();
						
						if (application != null) {
													
							if ("savePage".equals(rapidRequest.getActionName())) {
								
								String bodyString = new String(bodyBytes, "UTF-8");
								
								getLogger().debug("Designer POST request : " + request.getQueryString() + " body : " + bodyString);
								
								JSONObject jsonPage = new JSONObject(bodyString);	
								
								// instantiate a new blank page
								Page newPage = new Page();
								
								// set page properties
								newPage.setId(jsonPage.optString("id"));
								newPage.setName(jsonPage.optString("name"));
								newPage.setTitle(jsonPage.optString("title"));
								newPage.setLabel(jsonPage.optString("label"));
								newPage.setDescription(jsonPage.optString("description"));
																
								// look in the JSON for an event array
								JSONArray jsonEvents = jsonPage.optJSONArray("events");
								// add the events if we found one
								if (jsonEvents != null) newPage.setEvents(Control.getEvents(this, jsonEvents));
										
								// look in the JSON for a styles array
								JSONArray jsonStyles = jsonPage.optJSONArray("styles");
								// if there were styles
								if (jsonStyles != null) newPage.setStyles(Control.getStyles(this, jsonStyles));
																
								// if there are child controls from the page loop them and add to the pages control collection
								JSONArray jsonControls = jsonPage.optJSONArray("childControls");
								if (jsonControls != null) {
									for (int i = 0; i < jsonControls.length(); i++) {
										// get the JSON control
										JSONObject jsonControl = jsonControls.getJSONObject(i);
										// call our function so it can go iterative
										newPage.addControl(createControl(jsonControl));						
									}
								}
								
								// if there are roles specified for this page
								JSONArray jsonUserRoles = jsonPage.optJSONArray("roles");
								if (jsonUserRoles != null) {
									List<String> userRoles = new ArrayList<String>(); 
									for (int i = 0; i < jsonUserRoles.length(); i++) {
										// get the JSON role
										String jsonUserRole = jsonUserRoles.getString(i);
										// add to collection
										userRoles.add(jsonUserRole);
									}
									// assign to page
									newPage.setRoles(userRoles);
								}
								
								// look in the JSON for a sessionVariables array
								JSONArray jsonSessionVariables = jsonPage.optJSONArray("sessionVariables");
								// if we found one
								if (jsonSessionVariables != null) {
									List<String> sessionVariables = new ArrayList<String>();
									for (int i = 0; i < jsonSessionVariables.length(); i++) {
										sessionVariables.add(jsonSessionVariables.getString(i));
									}
									newPage.setSessionVariables(sessionVariables);
								}
								
								// look in the JSON for a pageVisibilityRules array
								JSONArray jsonVisibilityConditions = jsonPage.optJSONArray("visibilityConditions");
								// if we found one
								if (jsonVisibilityConditions != null) {
									List<Condition> visibilityConditions = new ArrayList<Condition>();
									for (int i = 0; i < jsonVisibilityConditions.length(); i++) {										
										visibilityConditions.add(new Condition(jsonVisibilityConditions.getJSONObject(i)));										
									}
									newPage.setVisibilityConditions(visibilityConditions);
								}
	
								// retrieve the html body
								String htmlBody = jsonPage.optString("htmlBody");
								// if we got one trim it and retain in page
								if (htmlBody != null) newPage.setHtmlBody(htmlBody.trim());
								
								// look in the JSON for rolehtml
								JSONArray jsonRolesHtml = jsonPage.optJSONArray("rolesHtml");
								// if we found some
								if (jsonRolesHtml != null) {
									// instantiate the roles html collection
									ArrayList<Page.RoleHtml> rolesHtml = new ArrayList<Page.RoleHtml>();
									// loop the entries
									for (int i =0; i < jsonRolesHtml.length(); i++) {
										// get the entry
										JSONObject jsonRoleHtml = jsonRolesHtml.getJSONObject(i);
										// retain the html
										String html = jsonRoleHtml.optString("html");
										// trim it if there is one
										if (html != null) html = html.trim();
										// create an array to hold the roles
										ArrayList<String> roles = new ArrayList<String>(); 
										// get the roles
										JSONArray jsonRoles = jsonRoleHtml.optJSONArray("roles");
										// if we got some
										if (jsonRoles != null) {
											// loop them
											for (int j = 0; j < jsonRoles.length(); j++) {
												// get the role
												String role = jsonRoles.getString(j);
												// add it to the roles collections
												roles.add(role);
											}
										}
										// create and add a new roleHtml  object
										rolesHtml.add(new Page.RoleHtml(roles, html));
									}
									// add it to the page
									newPage.setRolesHtml(rolesHtml);
								}
								
								// fetch a copy of the old page (if there is one)
								Page oldPage = application.getPages().getPage(getServletContext(), newPage.getId());
								// if the page's name changed we need to remove it
								if (oldPage != null) {
									if (!oldPage.getName().equals(newPage.getName())) {
										oldPage.delete(this, rapidRequest, application);
									}
								}
																
								// save the new page to file
								newPage.save(this, rapidRequest, application, true);		
								
								// get any pages collection (we're only sent it if it's been changed)
								JSONArray jsonPages = jsonPage.optJSONArray("pages");
								// if we got some
								if (jsonPages != null) {
									// make a new map for the page orders
									Map<String, Integer> pageOrders = new HashMap<String, Integer>();									
									// loop the page orders
									for (int i = 0; i < jsonPages.length(); i++) {
										// add the order to the map
										pageOrders.put(jsonPages.getJSONObject(i).getString("id"), i);
									}
									// replace the application pageOrders map
									application.setPageOrders(pageOrders);			
									// save the application and the new orders
									application.save(this, rapidRequest, true);
								}
								boolean jsonPageOrderReset = jsonPage.optBoolean("pageOrderReset");
								if (jsonPageOrderReset) {
									// empty the application pageOrders map so everything goes alphabetical
									application.setPageOrders(null);
								}
								
								// send a positive message
								output = "{\"message\":\"Saved!\"}";
								
								// set the response type to json
								response.setContentType("application/json");
							
							} else if ("testSQL".equals(rapidRequest.getActionName())) {
								
								// turn the body bytes into a string
								String bodyString = new String(bodyBytes, "UTF-8");
								
								JSONObject jsonQuery = new JSONObject(bodyString);
								
								JSONArray jsonInputs = jsonQuery.optJSONArray("inputs");
								
								JSONArray jsonOutputs = jsonQuery.optJSONArray("outputs");
													
								Parameters parameters = new Parameters();
								
								if (jsonInputs != null) {
								
									for (int i = 0; i < jsonInputs.length(); i++) parameters.addNull();
									
								}
								
								DatabaseConnection databaseConnection = application.getDatabaseConnections().get(jsonQuery.optInt("databaseConnectionIndex",0));
								
								ConnectionAdapter ca = databaseConnection.getConnectionAdapter(getServletContext(), application);			
								
								DataFactory df = new DataFactory(ca);
								
								int outputs = 0;
								
								if (jsonOutputs != null) outputs = jsonOutputs.length();
								
								String sql = jsonQuery.getString("SQL");
								// some jdbc drivers need the line breaks removing before they'll work properly - here's looking at you MS SQL Server!
								sql = sql.replace("\n", " ");
																												
								if (outputs == 0) {
									
									df.getPreparedStatement(rapidRequest,sql , parameters);
															
								} else {
									
									ResultSet rs = df.getPreparedResultSet(rapidRequest, sql, parameters);
									
									ResultSetMetaData rsmd = rs.getMetaData();
									
									int cols = rsmd.getColumnCount();
									
									if (outputs > cols) throw new Exception(outputs + " outputs, but only " + cols + " column" + (cols > 1 ? "s" : "") + " selected");
									
									for (int i = 0; i < outputs; i++) {
																								
										JSONObject jsonOutput = jsonOutputs.getJSONObject(i);
										
										String field = jsonOutput.optString("field","");
										
										if (!"".equals(field)) {
											
											field = field.toLowerCase();
										
											boolean gotOutput = false;
											
											for (int j = 0; j < cols; j++) {
												
												String sqlField = rsmd.getColumnLabel(j + 1).toLowerCase();
												
												if (field.equals(sqlField)) {
													gotOutput = true;
													break;
												}
												
											}
											
											if (!gotOutput) throw new Exception("Field \"" + field + "\" from output " + (i + 1) + " is not present in selected columns");
											
										}
										
									}
															
								}
																		
								// send a positive message
								output = "{\"message\":\"OK\"}";
								
								// set the response type to json
								response.setContentType("application/json");
								
							} else if ("uploadImage".equals(rapidRequest.getActionName()) || "import".equals(rapidRequest.getActionName())) {
											
								// get the content type from the request
								String contentType = request.getContentType();
								// get the position of the boundary from the content type
								int boundaryPosition = contentType.indexOf("boundary=");				
								// derive the start of the meaning data by finding the boundary
								String boundary = contentType.substring(boundaryPosition + 10);
								// this is the double line break after which the data occurs
								byte[] pattern = {0x0D, 0x0A, 0x0D, 0x0A};
								// find the position of the double line break
								int dataPosition = Bytes.findPattern(bodyBytes, pattern );
								// the body header is everything up to the data
								String header = new String(bodyBytes, 0, dataPosition, "UTF-8");
								// find the position of the filename in the data header
								int filenamePosition = header.indexOf("filename=\"");
								// extract the file name
								String filename = header.substring(filenamePosition + 10, header.indexOf("\"", filenamePosition + 10));					
								// find the position of the file type in the data header
								int fileTypePosition = header.toLowerCase().indexOf("type:");
								// extract the file type
								String fileType = header.substring(fileTypePosition + 6);
													
								if ("uploadImage".equals(rapidRequest.getActionName())) {

									// check the file type
									if (!fileType.equals("image/jpeg") && !fileType.equals("image/gif") && !fileType.equals("image/png")) throw new Exception("Unsupported file type");
									
									// get the web folder from the application
									String path = rapidRequest.getApplication().getWebFolder(getServletContext());
									// create a file output stream to save the data to
									FileOutputStream fos = new FileOutputStream (path + "/" +  filename);
									// write the file data to the stream
									fos.write(bodyBytes, dataPosition + pattern.length, bodyBytes.length - dataPosition - pattern.length - boundary.length() - 9);
									// close the stream
									fos.close();
									
									// log the file creation
									getLogger().debug("Saved image file " + path + filename);
									
									// create the response with the file name and upload type
									output = "{\"file\":\"" + filename + "\",\"type\":\"" + rapidRequest.getActionName() + "\"}";
									
								} else if ("import".equals(rapidRequest.getActionName())) {
									
									// check the file type
									if (!"application/x-zip-compressed".equals(fileType) && !"application/zip".equals(fileType)) throw new Exception("Unsupported file type");
									
									// get the name
									String appName = request.getParameter("name");
									
									// check we were given one
									if (appName == null) throw new Exception("Name must be provided");
									
									// get the version
									String appVersion = request.getParameter("version");
									
									// check we were given one
									if (appVersion == null) throw new Exception("Version must be provided");
									
									// make the id from the safe and lower case name
									String appId = Files.safeName(appName).toLowerCase();
									
									// make the version from the safe and lower case name
									appVersion = Files.safeName(appVersion);
									
									// get application destination folder
									File appFolderDest = new File(Application.getConfigFolder(getServletContext(), appId, appVersion));
									// get web contents destination folder
									File webFolderDest = new File(Application.getWebFolder(getServletContext(), appId, appVersion));
									
									// look for an existing application of this name and version
									Application existingApplication = getApplications().get(appId, appVersion); 
									// if we have an existing application 
									if (existingApplication != null) {
										// back it up first
										existingApplication.backup(this, rapidRequest, false);										
									}
									
									// get a file for the temp directory
									File tempDir = new File(getServletContext().getRealPath("/WEB-INF/temp"));
									// create it if not there
									if (!tempDir.exists()) tempDir.mkdir();
									
									// the path we're saving to is the temp folder
									String path = getServletContext().getRealPath("/WEB-INF/temp/" + appId + ".zip");
									// create a file output stream to save the data to
									FileOutputStream fos = new FileOutputStream (path);
									// write the file data to the stream
									fos.write(bodyBytes, dataPosition + pattern.length, bodyBytes.length - dataPosition - pattern.length - boundary.length() - 9);
									// close the stream
									fos.close();
									
									// log the file creation
									getLogger().debug("Saved import file " + path);
									
									// get a file object for the zip file
									File zipFile = new File(path);
									// load it into a zip file object
									ZipFile zip = new ZipFile(zipFile);
									// unzip the file
									zip.unZip();
									// delete the zip file
									zipFile.delete();
									
									// unzip folder (for deletion)
									File unZipFolder = new File(getServletContext().getRealPath("/WEB-INF/temp/" + appId));
									// get application folders
									File appFolderSource = new File(getServletContext().getRealPath("/WEB-INF/temp/" + appId + "/WEB-INF"));						
									// get web content folders
									File webFolderSource = new File(getServletContext().getRealPath("/WEB-INF/temp/" + appId + "/WebContent" ));																		
																		
									// check we have the right source folders
									if (webFolderSource.exists() && appFolderSource.exists()) {
																												
										// get application.xml file
										File appFileSource = new File (appFolderSource + "/application.xml");
										
										if (appFileSource.exists()) {		
											
											// delete the appFolder if it exists
											if (appFolderDest.exists()) Files.deleteRecurring(appFolderDest);
											// delete the webFolder if it exists
											if (webFolderDest.exists()) Files.deleteRecurring(webFolderDest);
																						
											// copy application content
											Files.copyFolder(appFolderSource, appFolderDest);
																						
											// copy web content
											Files.copyFolder(webFolderSource, webFolderDest);
													
											try {
											
												// load the new application (but don't initialise, nor load pages)
												Application appNew = Application.load(getServletContext(), new File (appFolderDest + "/application.xml"), false);
															
												// update application name
												appNew.setName(appName);
												
												// get the old id
												String appOldId = appNew.getId();
												
												// make the new id
												appId = Files.safeName(appName).toLowerCase();
												
												// update the id
												appNew.setId(appId);
												
												// get the old version
												String appOldVersion = appNew.getVersion();
												
												// make the new version
												appVersion = Files.safeName(appVersion);
												
												// update the version
												appNew.setVersion(appVersion);
																																		
												// update the created date
												appNew.setCreatedDate(new Date());			
												
												// set the status to In development
												appNew.setStatus(Application.STATUS_DEVELOPMENT);
												
												// a map of actions that might be removed from any of the pages
												Map<String,Integer> removedActions = new HashMap<String, Integer>();
																																		
												// look for page files
												File pagesFolder = new File(appFolderDest.getAbsolutePath() + "/pages");
												// if the folder is there
												if (pagesFolder.exists()) {
													
													// create a filter for finding .page.xml files
													FilenameFilter xmlFilenameFilter = new FilenameFilter() {
												    	public boolean accept(File dir, String name) {
												    		return name.toLowerCase().endsWith(".page.xml");
												    	}
												    };
												    
												    // loop the .page.xml files 
												    for (File pageFile : pagesFolder.listFiles(xmlFilenameFilter)) {
												    	
												    	BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream(pageFile), "UTF-8"));
												        String line = null;
												        StringBuilder stringBuilder = new StringBuilder();
												        
												        while ((line = reader.readLine()) != null ) {
												            stringBuilder.append(line);
												            stringBuilder.append("\n");
												        }
												        reader.close();
												        
												        // retrieve the xml into a string
												        String fileString = stringBuilder.toString();
												        
												        // prepare a new file string which will update into
												        String newFileString = null;

												        // if the old app did not have a version (for backwards compatibility)
												        if (appOldVersion == null) {
												        	
												        	// replace all properties that appear to have a url, and all created links - note the fix for cleaning up the double encoding
													        newFileString = fileString
													        	.replace("applications/" + appOldId + "/", "applications/" + appId + "/" + appVersion  + "/")
													        	.replace("~?a=" + appOldId + "&amp;", "~?a=" + appId + "&amp;v=" + appVersion + "&amp;")
													        	.replace("~?a=" + appOldId + "&amp;amp;", "~?a=" + appId + "&amp;v=" + appVersion + "&amp;");
												        	
												        } else {
												        	
												        	// replace all properties that appear to have a url, and all created links - note the fix for double encoding 
													        newFileString = fileString
													        	.replace("applications/" + appOldId + "/" + appOldVersion + "/", "applications/" + appId + "/" + appVersion  + "/")
													        	.replace("~?a=" + appOldId + "&amp;v=" + appOldVersion + "&amp;", "~?a=" + appId + "&amp;v=" + appVersion + "&amp;")
														        .replace("~?a=" + appOldId + "&amp;amp;v=" + appOldVersion + "&amp;amp;", "~?a=" + appId + "&amp;v=" + appVersion + "&amp;");												        	
												        }
												        												        												        
												        // now open the string into a document
														Document pageDocument = XML.openDocument(newFileString);
														// get an xpath factory
														XPathFactory xPathfactory = XPathFactory.newInstance();
														XPath xpath = xPathfactory.newXPath();
														// an expression for any attributes with a local name of "type"
														XPathExpression expr = xpath.compile("//@*[local-name()='type']");
														// get them
														NodeList nl = (NodeList) expr.evaluate(pageDocument, XPathConstants.NODESET);
														// get out system actions
														JSONArray jsonActions = getJsonActions();
														// if we found any elements with a type attribute and we have system actions
														if (nl.getLength() > 0 && jsonActions.length() > 0) {				
															// a list of action types
															List<String> types = new ArrayList<String>();															
															// loop the json actions
															for (int i = 0; i < jsonActions.length(); i++) types.add(jsonActions.getJSONObject(i).optString("type").toLowerCase());																				
															// loop the action attributes we found
															for (int i = 0; i < nl.getLength(); i++) {
																// get this attribute
																Attr a = (Attr) nl.item(i);
																// get the value of the type 
																String type = a.getTextContent().toLowerCase();
																// get the element the attribute is in
																Node n = a.getOwnerElement();
																// if we don't know about this action type
																if (!types.contains(type)) {
																	// get the parent node
																	Node p = n.getParentNode();
																	// remove this node
																	p.removeChild(n);
																	// if we have removed this type already
																	if (removedActions.containsKey(type)) {
																		// increment the entry for this type
																		removedActions.put(type, removedActions.get(type) + 1);
																	} else {
																		// add an entry for this type
																		removedActions.put(type, 1);
																	}
																} // got type check																																															
															} // attribute loop
																														
														} // attribute and system action check
																										        
														// use the transformer to write to disk
														TransformerFactory transformerFactory = TransformerFactory.newInstance();
														Transformer transformer = transformerFactory.newTransformer();
														DOMSource source = new DOMSource(pageDocument);
														StreamResult result = new StreamResult(pageFile);
														transformer.transform(source, result);
												        								    	
												    } // page xml file loop
													
												} // pages folder check
																						
												// now initialise with the new id but don't make the resource files (this reloads the pages and sets up the security adapter)
												appNew.initialise(getServletContext(), false);
												
												// get the security
												SecurityAdapter security = appNew.getSecurityAdapter();
												
												// if we have one
												if (security != null) {									
													
													// get the current users record from the adapter
													User user = security.getUser(rapidRequest);
													// check the current user is present in the app's security adapter
													if (user == null) {
														// get the Rapid user object
														User rapidUser = rapidApplication.getSecurityAdapter().getUser(rapidRequest);
														// create a new user based on the Rapid user
														user = new User(userName, rapidUser.getDescription(), rapidUser.getPassword());
														// add the new user 
														security.addUser(rapidRequest, user);
													}
													
													// add Admin and Design roles for the new user if required
													if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE)) 
														security.addUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
													
													if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE)) 
														security.addUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);									
												}
												
												// if any items were removed
												if (removedActions.keySet().size() > 0) {
													// a description of what was removed
													String removed = "";
													// loop the entries
													for (String type : removedActions.keySet()) {
														int count = removedActions.get(type);
														removed += "removed " + count + " " + type + " action" + (count == 1 ? "" : "s") + " on import\n";
													}
													// get the current description
													String description = appNew.getDescription();
													// if null set to empty string
													if (description == null) description = "";
													// add a line break if need be
													if (description.length() > 0) description += "\n";
													// add the removed
													description += removed;
													// set it back
													appNew.setDescription(description);
												}
												
												// reload the pages (actually clears down the pages collection and reloads the headers)
												appNew.getPages().loadpages(getServletContext());
												
												// save application (this will also initialise and rebuild the resources)
												appNew.save(this, rapidRequest, false);
																																												
												// add application to the collection
												getApplications().put(appNew);
												
												// delete unzip folder
												Files.deleteRecurring(unZipFolder);
																								
												// send a positive message
												output = "{\"id\":\"" + appNew.getId() + "\",\"version\":\"" + appNew.getVersion() + "\"}";
												
											} catch (Exception ex) {
												
												// delete the appFolder if it exists
												if (appFolderDest.exists()) Files.deleteRecurring(appFolderDest);
												// if the parent is empty delete it too
												if (appFolderDest.getParentFile().list().length <= 1) Files.deleteRecurring(appFolderDest.getParentFile());
												
												// delete the webFolder if it exists
												if (webFolderDest.exists()) Files.deleteRecurring(webFolderDest);
												// if the parent is empty delete it too
												if (webFolderDest.getParentFile().list().length <= 1) Files.deleteRecurring(webFolderDest.getParentFile());
												
												// rethrow exception
												throw ex;
												
											}
																						
										} else {
											
											// delete unzip folder
											Files.deleteRecurring(unZipFolder);
											
											// throw excpetion
											throw new Exception("Must be a valid Rapid " + Rapid.VERSION + " file");
											
										}
										
									} else {
										
										// delete unzip folder
										Files.deleteRecurring(unZipFolder);
										
										// throw excpetion
										throw new Exception("Must be a valid Rapid file");
										
									}
									
								}
																		
							}
							
							getLogger().debug("Designer POST response : " + output);
																
							PrintWriter out = response.getWriter();
							out.print(output);
							out.close();
							
						} // got an application
												
					} // got rapid design role
					
				} // got rapid security
											
			} // got rapid application
											
		} catch (Exception ex) {
			
			getLogger().error("Designer POST error : ",ex);
			
			sendException(rapidRequest, response, ex);
			
		}
		
	}

}
