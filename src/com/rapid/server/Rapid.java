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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.core.Page;
import com.rapid.security.SecurityAdapater;
import com.rapid.security.SecurityAdapater.User;
import com.rapid.utils.Files;

public class Rapid extends RapidHttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	// these are held here and referred to globally
	public static final String VERSION = "2.2.1";
	public static final String DESIGN_ROLE = "RapidDesign";
	public static final String ADMIN_ROLE = "RapidAdmin";
	public static final String SUPER_ROLE = "RapidSuper";
						
	// this byte buffer is used for reading the post data - maybe we'll use streams one day...
	private byte[] _byteBuffer = new byte[1024];
					                        
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				
		// get a logger
		getLogger().debug("Rapid GET request : " + request.getQueryString());
														
		// get a new rapid request passing in this servelet and the http request
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		// set the page html to an empty string
		String pageHtml = "";
					
		try {
			
			// get the application object
			Application app = rapidRequest.getApplication();
			
			// check app exists
			if (app == null) {
				
				// send message
				sendMessage(rapidRequest, response, 404, "Application not found on this server");
								
				//log
				getLogger().debug("Rapid GET response (404) : Application not found on this server");
				
			} else {
				
				// get the application security
				SecurityAdapater security = app.getSecurity();
				
				// get the userName
				String userName = rapidRequest.getUserName();
				
				// get the user
				User user = security.getUser(rapidRequest);
				
				// check we got a user
				if (user == null) {
										
					// send message
					sendMessage(rapidRequest, response, 403, "User not authorised for application");
					
					//log
					getLogger().debug("Rapid GET response (403) : User " + userName +  " not authorised for application");
					
				} else {
			
					// check if there is a Rapid action
					if ("download".equals(rapidRequest.getActionName())) {
						
						// set the file name
						String fileName = app.getId() + "_" + rapidRequest.getUserName() + ".zip";
										
						// create the zip file
						app.zip(this, rapidRequest, user, fileName, true);
						
						// set the type as a .zip
						response.setContentType("application/x-zip-compressed");
						
						// Shows the download dialog
						response.setHeader("Content-disposition","attachment; filename=" + app.getId() + ".zip");
																
						// get the file for the zip we're about to create
						File zipFile = new File(getServletContext().getRealPath("/WEB-INF/temp/" + fileName));
						
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
						
						// delete the file
						zipFile.delete();
						
					} else {
																	
						// get the page object
						Page page = rapidRequest.getPage();
								
						// check we got one
						if (page == null) { 
							
							// send message
							sendMessage(rapidRequest, response, 404, "Page not found");
							
							// log
							getLogger().debug("Rapid GET response (404) : Page not found");
							
						} else {
							
							// create a writer
							PrintWriter out = response.getWriter();
							
							// write the page html
							page.writeHtml(this, rapidRequest, app, user, out);
														
							// output the page
							out.print(pageHtml);
																										
							// close the writer
							out.close();
												
						} // page check
																																					
					} // action name check
							
				} // security check
				
			} // app exists check
								
		} catch (Exception ex) {
		
			getLogger().error("Rapid GET error : ",ex);
			
			sendException(rapidRequest, response, ex);
		
		} 
																			
		getLogger().trace("Rapid GET response : " + pageHtml);
					
	}
	
	private JSONObject getJSONObject(byte[] bodyBytes) throws UnsupportedEncodingException, JSONException {
		
		// assume we weren't passed any json				
		JSONObject jsonData = null;
		
		// read the body into a string
		String bodyString = new String(bodyBytes, "UTF-8");
						
		// if there is something in the body string it must be json so parse it
		if (!"".equals(bodyString)) {
			// log the body string
			getLogger().debug(bodyString);
			// get the data
			jsonData = new JSONObject(bodyString);
		}
		
		return jsonData;
		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				
		// read bytes from request body into our own byte array (this means we can deal with images) 
		InputStream input = request.getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();												
		for (int length = 0; (length = input.read(_byteBuffer)) > -1;) outputStream.write(_byteBuffer, 0, length);			
		byte[] bodyBytes = outputStream.toByteArray();
				
		// create a Rapid request				
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		// log
		getLogger().debug("Rapid POST request : " + request.getQueryString() + " bytes=" + bodyBytes.length);
					
		try {
			
			// if an application action was found in the request						
			if (rapidRequest.getAction() != null) {			
			
				// get the application
				Application app = rapidRequest.getApplication();
				
				// check we got one
				if (app == null) {
					
					// send forbidden response			
					sendMessage(rapidRequest, response, 403, "Application not found");
					
					// log
					getLogger().debug("Rapid POST response (403) : Application not found");
					
				} else {
					
					// get the security
					SecurityAdapater security = app.getSecurity();
					
					// get the user
					User user = security.getUser(rapidRequest);
					
					// check the user
					if (user == null) {
						
						// send forbidden response			
						sendMessage(rapidRequest, response, 403, "User not authorised for application");
						
						// log
						getLogger().debug("Rapid POST response (403) : User not authorised for application");
						
					} else {
							
						// assume we weren't passed any json				
						JSONObject jsonData = getJSONObject(bodyBytes);
																		
						// if we got some data
						if (jsonData != null) {
							
							// fetch the action result
							JSONObject jsonResult = rapidRequest.getAction().doAction(rapidRequest, jsonData);
							
							// set response to json
							response.setContentType("application/json");
							
							// create a writer
							PrintWriter out = response.getWriter();
							
							// print the results
							out.print(jsonResult.toString());
							
							// close the writer
							out.close();
							
							// log response
							getLogger().debug("Rapid POST response : " + jsonResult);
							
						}
																																																												
					}
					
				}				
											
			} else if ("getApps".equals(rapidRequest.getActionName())) {
				
				// create an empty array which we will populate
				JSONArray jsonApps = new JSONArray();
				
				// get all available applications
				List<Application> apps = getApplications().sort();
				
				// if there were some
				if (apps != null) {
					
					// assume the request wasn't for testing
					boolean forTesting = false;
					
					// assume we weren't passed any json				
					JSONObject jsonData = getJSONObject(bodyBytes);
																	
					// if we got some data, look for a test = true entry
					if (jsonData != null) forTesting = jsonData.optBoolean("test");
											
					// loop the apps
					for (Application app : apps) {
									
						// get the relevant security adapter
						SecurityAdapater security = app.getSecurity();
						
						// fail silently if there was an issue
						try {
						
							// fetch a user object in the name of the current user for the current app
							User user = security.getUser(rapidRequest);
							
							// if we got one
							if (user != null) {
								
								// create a json object for the details of this application
								JSONObject jsonApp = new JSONObject();
								// add details
								jsonApp.put("id", app.getId());
								jsonApp.put("version", app.getVersion());
								jsonApp.put("title", app.getTitle());								
								// add app to our main array
								jsonApps.put(jsonApp);
								
								// check if we are testing
								if (forTesting) {
									
									// if the user has Rapid Design for this application
									if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) {
										
										// loop the versions
										for (Application version :	getApplications().getVersions(app.getId()).sort()) {
											// create a json object for the details of this version
											jsonApp = new JSONObject();
											// add details
											jsonApp.put("id", version.getId());
											jsonApp.put("version", version.getVersion());
											jsonApp.put("status", version.getStatus());										
											jsonApp.put("title", version.getTitle());
											jsonApp.put("test", true);
											// add app to our main array
											jsonApps.put(jsonApp);
										}
										
									} // got design role
																		
								} // forTesting check
								
							} // user check
							
						} catch (Exception ex) {}
																									
					} // apps loop
					
				} // apps check
				
				// set response to json
				response.setContentType("application/json");
				
				// create a writer
				PrintWriter out = response.getWriter();
				
				// print the results
				out.print(jsonApps.toString());
				
				// close the writer
				out.close();
				
				// log response
				getLogger().debug("Rapid POST response : " + jsonApps.toString());
											
			} else if ("uploadImage".equals(rapidRequest.getActionName())) {
				
				// get the application
				Application app = rapidRequest.getApplication();
				
				// check we got one
				if (app == null) {
					
					// send forbidden response			
					sendMessage(rapidRequest, response, 403, "Application not found");
					
					// log
					getLogger().debug("Rapid POST response (403) : Application not found");
					
				} else {
					
					// get the security
					SecurityAdapater security = app.getSecurity();
					
					// get the user
					User user = security.getUser(rapidRequest);
					
					// check the user
					if (user == null) {
						
						// send forbidden response			
						sendMessage(rapidRequest, response, 403, "User not authorised for application");
						
						// log
						getLogger().debug("Rapid POST response (403) : User not authorised for application");
						
					} else {					
					
						// get the name
						String imageName = request.getParameter("name");
						
						// if we got one
						if (imageName == null) {
							
							// send forbidden response			
							sendMessage(rapidRequest, response, 403, "Name must be provided");
							
							// log
							getLogger().debug("Rapid POST response (403) : Name must be provided");
							
						} else {
														
							// check the jpg file signature (from http://en.wikipedia.org/wiki/List_of_file_signatures)
							if (bodyBytes[0] == (byte)0xFF && bodyBytes[1] == (byte)0xD8 && bodyBytes[2] == (byte)0xFF) {
								
								// create the paht
								String imagePath = "uploads/" +  app.getId() + "/" + imageName;
								// create a file
								File imageFile = new File(getServletContext().getRealPath(imagePath));
								// create app folder if need be
								imageFile.getParentFile().mkdir();
								// create a file output stream to save the data to
								FileOutputStream fos = new FileOutputStream(imageFile);
								// write the body bytes to the stream
								fos.write(bodyBytes);
								// close the stream
								fos.close();
								
								// log the file creation
								getLogger().debug("Saved image file " + imageFile);
								
								// create a writer
								PrintWriter out = response.getWriter();
								
								// print the results
								out.print(imagePath);
								
								// close the writer
								out.close();
								
							} else {
								
								// send forbidden response			
								sendMessage(rapidRequest, response, 403, "Unrecognised file type");
								
								// log
								getLogger().debug("Rapid POST response (403) : Unrecognised file type");
								
							} // signature check
							
						} // name check
						
					} // user check
					
				} // app check
																
			} // action check
			
		} catch (Exception ex) {
		
			getLogger().error("Rapid POST error : ", ex);
			
			sendException(rapidRequest, response, ex);
		
		} 											
		
	}

}
