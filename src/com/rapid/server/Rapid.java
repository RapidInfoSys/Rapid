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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Page;
import com.rapid.core.Pages.PageHeader;
import com.rapid.core.Pages.PageHeaders;
import com.rapid.forms.FormAdapter;
import com.rapid.forms.FormAdapter.FormControlValue;
import com.rapid.forms.FormAdapter.FormPageControlValues;
import com.rapid.forms.FormAdapter.ServerSideValidationException;
import com.rapid.forms.FormAdapter.UserFormDetails;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.server.filter.RapidFilter;
import com.rapid.utils.Files;

public class Rapid extends RapidHttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	// these are held here and referred to globally
	public static final String VERSION = "2.3.5.1"; // the master version of this Rapid server instance
	public static final String MOBILE_VERSION = "1"; // the mobile version. update it if you want all mobile devices to run app updates on their next version check
	public static final String DESIGN_ROLE = "RapidDesign";
	public static final String ADMIN_ROLE = "RapidAdmin";
	public static final String SUPER_ROLE = "RapidSuper";
	
	//  helper methods for forms
	private String getFirstPageForFormType(Application app, int formPageType) throws RapidLoadingException {
		// loop  the sorted page headers
		for (PageHeader pageHeader : app.getPages().getSortedPages()) {
			// get the page
			Page page = app.getPages().getPage(getServletContext(), pageHeader.getId());
			// if this is s submitted page
			if (page.getFormPageType() == formPageType) {
				// return the page id
				return page.getId();				
			}
		}
		return null;
	}
	
	public static void gotoStartPage(HttpServletRequest request, HttpServletResponse response, Application app, boolean invalidate) throws IOException {
		// clear the session if requested to
		if (invalidate) request.getSession().invalidate();
		// go to the start page
		response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion());
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// fake a delay for testing slow servers
		//try { Thread.sleep(3000); } catch (InterruptedException e) {}
				
		// get a logger
		Logger logger = getLogger();
		
		// log!
		logger.debug("Rapid GET request : " + request.getQueryString());
														
		// get a new rapid request passing in this servlet and the http request
		RapidRequest rapidRequest = new RapidRequest(this, request);		
					
		try {
			
			// get the application object
			Application app = rapidRequest.getApplication();
			
			// check app exists
			if (app == null) {
				
				// send message
				sendMessage(response, 404, "Application not found", "The application you requested can't be found");
								
				//log
				logger.debug("Rapid GET response (404) : Application not found on this server");
				
			} else {
				
				// get the application security
				SecurityAdapter security = app.getSecurityAdapter();
																							
				// check the password
				if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
																			
					// get the user
					User user = security.getUser(rapidRequest);
					
					// get the action
					String action = rapidRequest.getActionName();
			
					// check if there is a Rapid action
					if ("summary".equals(action) || "pdf".equals(action)) {
						
						// get the form adapter for both of the above
						FormAdapter formAdapter = app.getFormAdapter();
						
						// check there is one
						if (formAdapter == null) {
							
							// send message
							sendMessage(response, 500, "Not a form", "This Rapid app is not a form");
							
							// log
							logger.error("Rapid GET response (500) : Summary requested for " + app.getId() + "/" + app.getDescription() + " " + action +" but it does not have a form adapter");
							
						} else {
							
							if ("pdf".equals(action)) {
								// write the form pdf
								formAdapter.doWriteFormPDF(rapidRequest, response, request.getParameter("f"), false);
							} else {		
								// summary is never cached
								RapidFilter.noCache(response);
								// write the form summary page
								formAdapter.writeFormSummary(rapidRequest, response);							
							}
																																																								
						}

					} else if ("download".equals(action)) {
						
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
						
						// assume it's ok to write the page
						boolean pageCheck = true;
						// assume we won't be redirecting to the summary
						boolean showSummary = false;
						
						// get the requested page object
						Page page = rapidRequest.getPage();
						
						// get the form adapter (if there is one)
						FormAdapter formAdapter = app.getFormAdapter();
						
						// assume the form details are null
						UserFormDetails formDetails = null;
						
						try {																											
							
							// if there is a formAdapter, make sure there's a form id, unless it's for a simple page
							if (formAdapter != null) {
								
								// if there is a start parameter, nuke the session and then move on one page without the start parameter so users can go back to the beginning without loosing values
								if (request.getParameter("start") != null) {
									// invalidate the session 
									request.getSession().invalidate();
									// start the url
									String url = "~?";
									// start the position
									int pos = 0;
									// get the parameter map
									Enumeration<String> parameterNames = request.getParameterNames();
									// loop the current parameters
									while (parameterNames.hasMoreElements()) {
										// get the name
										String parameterName = parameterNames.nextElement();
										// ignore start
										if (!"start".equals(parameterName)) {
											// if 1 or more add &
											if (pos > 0) url += "&";
											// add to url
											url += parameterName + "=" + request.getParameter(parameterName);
											// inc pos
											pos ++;
										}
									}
									// redirect !
									response.sendRedirect(url);									
								}
																														
								// if this is a form resume
								if ("resume".equals(action)) {
									// get the form id and password from the url
									String resumeFormId = request.getParameter("f");
									String resumePassword = request.getParameter("pwd");
									// try and get the resume form details
									formDetails = formAdapter.doResumeForm(rapidRequest, resumeFormId, resumePassword);									
									// check whether we can resume this form
									if (formDetails != null)  {
										// go for the summary if no page specified
										if (request.getParameter("p") == null) showSummary  = true;										
									}
								} else {								
									// get form id from the adapter
									formDetails = formAdapter.getUserFormDetails(rapidRequest);									
								}								
								
								// if there isn't a form id, or we want to show the summary don't check the pages
								if (formDetails == null || showSummary) {
									
									// set the page check to false
									pageCheck = false;			
									
								} else if (page != null) {
																											
									// check that we have progressed far enough in the form to view this page, or we are a designer
									if (formAdapter.checkMaxPage(rapidRequest, formDetails, page.getId()) || security.checkUserRole(rapidRequest, DESIGN_ROLE)) {
										
										// only if this is not a dialogue
										if (!"dialogue".equals(action)) {
														
											// get all of the pages
											PageHeaders pageHeaders = app.getPages().getSortedPages();											
											// get this page position
											int pageIndex = pageHeaders.indexOf(page.getId());
											// check the page visibility
											while (!page.isVisible(rapidRequest, app, formDetails)) {
												// if we're here the visibility check on the current page failed so increment the index
												pageIndex ++;
												// if there are no more pages go to the summary
												if (pageIndex > pageHeaders.size() - 1) {
													// fail the check to print a page
													pageCheck = false;
													// but set the the show summary to true
													showSummary = true;
													// we're done
													break;
												} else {
													// select the next page to check the visibility of
													page = app.getPages().getPage(getServletContext(), pageHeaders.get(pageIndex).getId());		
													// if not submitted set that we're allowed to this page
													if (!formDetails.getSubmitted()) formAdapter.setMaxPage(rapidRequest, formDetails, page.getId());
													// if this page has session values
													if (page.getSessionVariables() != null) {
														// loop them
														for (String variable : page.getSessionVariables()) {
															// look for session values
															String value = (String) rapidRequest.getSessionAttribute(variable);
															// if we got one update it's value
															if (value != null) formAdapter.setFormPageVariableValue(rapidRequest, formDetails.getId(), variable, value);															
														}
													}
												} // pages remaining check									
											} // page visible loop
										} // dialogue check
										
									} else {
										
										// go back to the start
										pageCheck = false;		
										//log
										logger.debug("Not allowed on page " + page.getId() + " yet!");
										
									} // page max check
																		
								} // form id check
								
							} // form adapter check
							
						} catch (Exception ex) {
							
							// set the page to null so we show the user a not found
							page = null;
							
							// log
							logger.debug("Error with page visibility rules : " + ex.getMessage(), ex);
							
						}
																		
						// if the pageCheck was ok (or not invalidated by lack of a form id or summary page)
						if (pageCheck) {
																																	
							// check we got one
							if (page == null) { 
								
								// send message
								sendMessage(response, 404, "Page not found", "The page you requested can't be found");
								
								// log
								logger.debug("Rapid GET response (404) : Page not found");
								
							} else {
								
								// get the pageId
								String pageId = page.getId();
																															
								// if the page we're about to write is the page we asked for (visibility rules might move us on a bit)								
								if (pageId.equals(rapidRequest.getPage().getId())) {
									
									// create a writer
									PrintWriter out = response.getWriter();
									
									// assume we require the designer link
									boolean designerLink = true;
									
									// set designer link to false if action is dialogue
									if ("dialogue".equals(rapidRequest.getActionName())) designerLink = false;
									
									// set the response type
									response.setContentType("text/html");
																		
									// write the page html
									page.writeHtml(this, response, rapidRequest, app, user, out, designerLink);
									
									// close the writer
									out.close();
									
									// flush the writer
									out.flush();
									
									// if we have a form adapter and form details
									if (formAdapter != null && formDetails != null) {
										// if this is an error page we have just shown the error, remove it
										if (page.getFormPageType() == Page.FORM_PAGE_TYPE_ERROR) formDetails.setErrorMessage(null);
										// if this is a save page we have just shown it, set to not saved
										if (page.getFormPageType() == Page.FORM_PAGE_TYPE_SAVED) formDetails.setSaved(false);
									}
																		
								} else {
									
									// redirect user to correct page
									response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + page.getId());
									
								}
																																
							} // page check
							
						} else {
							
							if (showSummary) {
																
								// go to the summary
								response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&action=summary");
								
							} else {
								
								logger.debug("Returning to start - failed page check and no showSummary");
							
								// go to the start page (invalidate unless user has design role)
								gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));
								
							}
							
						} // form id check
																																					
					} // action name check
					
				} else {
					
					// send message
					sendMessage(response, 403, "No permission", "You do not have permission to use this application");
					
					//log
					logger.debug("Rapid GET response (403) : User " + rapidRequest.getUserName() +  " not authorised for application");
																
				} // password check
				
			} // app exists check
								
		} catch (Exception ex) {
		
			logger.error("Rapid GET error : ",ex);
			
			sendException(rapidRequest, response, ex);
		
		} 
																								
	}
	
	private JSONObject getJSONObject(byte[] bodyBytes) throws UnsupportedEncodingException, JSONException {
		
		// assume we weren't passed any json				
		JSONObject jsonData = null;
		
		// read the body into a string
		String bodyString = new String(bodyBytes, "UTF-8");
						
		// if there is something in the body string it must be json so parse it
		if (!"".equals(bodyString)) {
			// get a logger
			Logger logger = getLogger();
			// log the body string
			logger.debug(bodyString);
			// get the data
			jsonData = new JSONObject(bodyString);
		}
		
		return jsonData;
		
	}
			
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
		// fake a delay for testing slow servers
		//try { Thread.sleep(3000); } catch (InterruptedException e) {}
		
		// this byte buffer is used for reading the post data 
		byte[] byteBuffer = new byte[1024];
		
		// read bytes from request body into our own byte array (this means we can deal with images) 
		InputStream input = request.getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();												
		for (int length = 0; (length = input.read(byteBuffer)) > -1;) outputStream.write(byteBuffer, 0, length);			
		byte[] bodyBytes = outputStream.toByteArray();
		
		// get a logger
		Logger logger = getLogger();
				
		// log
		logger.debug("Rapid POST request : " + request.getQueryString() + " bytes=" + bodyBytes.length);
				
		// create a Rapid request				
		RapidRequest rapidRequest = new RapidRequest(this, request);
				
		try {
			
			// this is the only variant where an application isn't specified and secured first
			if ("getApps".equals(rapidRequest.getActionName())) {
				
				// create an empty array which we will populate
				JSONArray jsonApps = new JSONArray();
				
				// get all available applications
				List<Application> apps = getApplications().sort();
				
				// if there were some
				if (apps != null) {
					
					// fail silently if there was an issue
					try {
															
						// assume we weren't passed any json				
						JSONObject jsonData = getJSONObject(bodyBytes);
						
						// assume the request wasn't for testing on Rapid Mobile
						boolean forTesting = false;
																		
						// if we got some data, look for a test = true entry - this is sent from Rapid Mobile
						if (jsonData != null) forTesting = jsonData.optBoolean("test");
												
						// loop the apps
						for (Application app : apps) {
							
							// if Rapid app must not be for testing / from Rapid Mobile
							if (!"rapid".equals(app.getId()) || !forTesting) {
								
								// get the relevant security adapter
								SecurityAdapter security = app.getSecurityAdapter();

								// make a rapidRequest for this application
								RapidRequest getAppsRequest = new RapidRequest(this, request, app);
														
								// check the user password
								if (security.checkUserPassword(getAppsRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
									
									// assume can add
									boolean canAdd = true;
																											
									if ("rapid".equals(app.getId())) {
										// must have RapidAdmin or RapidSuper to see Rapid app
										canAdd = security.checkUserRole(rapidRequest, Rapid.ADMIN_ROLE) || security.checkUserRole(rapidRequest, Rapid.SUPER_ROLE);
									}
									
									if (canAdd) {
										// create a json object for the details of this application
										JSONObject jsonApp = new JSONObject();
										// add details
										jsonApp.put("id", app.getId());
										jsonApp.put("version", app.getVersion());
										jsonApp.put("title", app.getTitle());
										jsonApp.put("storePasswordDuration", app.getStorePasswordDuration());
										// add app to our main array
										jsonApps.put(jsonApp);
																		
										// check if we are testing
										if (forTesting) {
											
											// if the user has Rapid Design for this application, (or Rpaid Super if this is the rapid app)
											if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE) && (!app.getId().equals("rapid") || security.checkUserRole(rapidRequest, Rapid.SUPER_ROLE))) {
												
												// loop the versions
												for (Application version :	getApplications().getVersions(app.getId()).sort()) {
													
													// create a json object for the details of this version
													jsonApp = new JSONObject();
													// add details
													jsonApp.put("id", version.getId());
													jsonApp.put("version", version.getVersion());
													jsonApp.put("status", version.getStatus());										
													jsonApp.put("title", version.getTitle());
													jsonApp.put("storePasswordDuration", version.getStorePasswordDuration());	
													jsonApp.put("test", true);
													// add app to our main array
													jsonApps.put(jsonApp);
												}
												
											} // got design role
																				
										} // forTesting check
										
									} // rapid app extra check
									
								} // user check
							
							} // rapid app and not for testing check
																																									
						} // apps loop
						
					} catch (Exception ex) {
						// only log
						logger.error("Error geting apps : ", ex);
					}
					
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
				logger.debug("Rapid POST response : " + jsonApps.toString());
				
			} else {
			
				// get the application
				Application app = rapidRequest.getApplication();
				
				// check we got one
				if (app == null) {
					
					// send forbidden response			
					sendMessage(response, 400, "Application not found", "The application you requested can't be found");
					
					// log
					logger.debug("Rapid POST response (403) : Application not found");
					
				} else {
					
					// get the security
					SecurityAdapter security = app.getSecurityAdapter();
										
					// check the user password
					if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
				
						// if an application action was found in the request						
						if (rapidRequest.getAction() != null) {			
				
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
								logger.debug("Rapid POST response : " + jsonResult);
								
							} // jsonData
																																																													
						}  else if("application/x-www-form-urlencoded".equals(request.getContentType())) {
							
							// log
							logger.debug("Form data received");
							
							// get the form adapter
							FormAdapter formAdapter = app.getFormAdapter();
							
							// form adapter check
							if (formAdapter == null) {
								
								// send message
								sendMessage(response, 500, "Not a form", "This Rapid app is not a form");
								
								// log
								logger.debug("Rapid GET response (500) : Not a form");
								
							} else {
								
								// get the form details to test all is ok
								UserFormDetails formDetails = formAdapter.getUserFormDetails(rapidRequest);
								
								// check we got one
								if (formDetails == null) {
									
									logger.debug("Returning to start - could not retrieve form details");
									
									// we've lost the form id so start the form again
									gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));
									
								} else {
									
									// this is a form page's data being submitted
									String formData = new String(bodyBytes, "UTF-8");
									
									// log it!
									logger.trace("Form data : " + formData);
									
									// if there's a submit action
									if ("submit".equals(request.getParameter("action"))) {
										
										// if submitted already go to start (should never happen)
										if (formDetails.getSubmitted()) {
											
											logger.debug("Returning to start - submit action but form not submitted");
											
											// go to the start page
											gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));
											
										} else {
																															
											try {
												
												// do the submit (this will call the non-abstract submit, manage the form state, and retain the submit message)
												formAdapter.doSubmitForm(rapidRequest);
																																		
												// place holder for first submitted page
												String submittedPageId = getFirstPageForFormType( app, Page.FORM_PAGE_TYPE_SUBMITTED);
												
												// check we got a submitted page
												if (submittedPageId == null) {
													
													// invalidate the form 
													formAdapter.setUserFormDetails(rapidRequest, null);
													
													logger.debug("Returning to start - form has been submitted, no submission page");
													
													// go to the start page unless user has the design role
													gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));
													
												} else {
													
													// request the first submitted page
													response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + submittedPageId);
													
												}
												
											} catch (Exception ex) {
																																		
												// place holder for first submitted page
												String errrorPageId = getFirstPageForFormType( app, Page.FORM_PAGE_TYPE_ERROR);
												
												// check we got one
												if (errrorPageId == null) {
													
													// just re throw the error
													throw ex;
													
												} else {
												
													// request the first error page
													response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + errrorPageId);
													
												}
												
											}
											
										} // submit check
										
									} else {
										
										// try
										try {
											
											// get the page
											Page page = rapidRequest.getPage();
											
											// if we got one
											if (page == null) {
												
												// send error
												sendMessage(response, 403, "Page does not exist", "The page you requested does not exist");
												
											} else {
											
												// get the page id
												String requestPageId = rapidRequest.getPage().getId();
																																		
												// if form not submitted
												if (!formDetails.getSubmitted()) {
													
													// get the page control values
													FormPageControlValues pageControlValues = FormAdapter.getPostPageControlValues(rapidRequest, formData, formDetails.getId());
																																				
													// check we got some
													if (pageControlValues != null) {
													
														// loop and print them if trace on
														if (logger.isTraceEnabled()) {
															for (FormControlValue controlValue : pageControlValues) {
																logger.debug(controlValue.getId() + " = " + controlValue.getValue());
															}
														}									
																							
														// store the form page control values
														formAdapter.setFormPageControlValues(rapidRequest, formDetails.getId(), requestPageId, pageControlValues);
														
													}
													
												}
													
												// assume we're not going to go to the summary
												boolean requestSummary = false;
												
												// get all of the app pages
												PageHeaders pageHeaders = app.getPages().getSortedPages();
																																		
												// get the position of the next page in sequence
												int requestPageIndex = pageHeaders.indexOf(requestPageId) + 1;
												
												// if there are any pages next to check
												if (requestPageIndex < pageHeaders.size()) {
													
													// get the next page
													page = app.getPages().getPage(getServletContext(), pageHeaders.get(requestPageIndex).getId());
													
													// check the page visibility
													while (!page.isVisible(rapidRequest, app, formDetails)) {
														// if we're here the visibility check on the current page failed so increment the index
														requestPageIndex ++;
														// if there are no more pages go to the summary
														if (requestPageIndex > pageHeaders.size() - 1) {
															// but set the the show summary to true
															requestSummary = true;
															// we're done
															break;
														} else {
															// select the next page to check the visibility of
															page = app.getPages().getPage(getServletContext(), pageHeaders.get(requestPageIndex).getId());												
														} // pages remaining check									
													} // page visible loop
													
												} else {
													// go straight for the summary
													requestSummary = true;
												}
												
												// if this form has not been submitted update the max page id if what we're about to request is less
												if (!formDetails.getSubmitted()) {
													// get current max page id
													String maxPageId = formDetails.getMaxPageId();
													// assume not max page yet
													int maxPageIndex = -1;
													// if there was a max page update to it's index
													if (maxPageId != null) maxPageIndex = pageHeaders.indexOf(maxPageId);
													// if update value is greater than current value
													if (requestPageIndex > maxPageIndex) formAdapter.setMaxPage(rapidRequest, formDetails, page.getId());
												}
												
												// if this is the last page
												if (requestSummary) {
													
													// mark that this form is complete (if not submitted)
													if (!formDetails.getSubmitted()) {
														// update form details
														formDetails.setComplete(true);
														// update form adapter (for storage)
														formAdapter.setFormComplete(rapidRequest, formDetails);
													}
													
													// send a redirect for the summary (this also avoids ERR_CACH_MISS issues on the back button )
													response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&action=summary");
													
												} else {
																																					
													// send a redirect for the page (this avoids ERR_CACH_MISS issues on the back button )
													response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + page.getId());
													
												} // last page check		
												
											} // page check
											
										} catch (ServerSideValidationException ex) {
											
											// log it!
											logger.error("Form data failed server side validation : " + ex.getMessage(), ex);
											
											// send a redirect back to the beginning - there's no reason except for tampering  that this would happen
											gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));
											
										}
									
									} // form id check

								} // submit action check
								
							} // form adapter check
																							
						} else if ("checkVersion".equals(rapidRequest.getActionName())) {
							
							// create a json version object
							JSONObject jsonVersion = new JSONObject();
																
							// add the mobile version, followed by the app version
							jsonVersion.put("version", MOBILE_VERSION + " - " + app.getVersion());
																											
							// create a writer
							PrintWriter out = response.getWriter();
							
							// print the results
							out.print(jsonVersion.toString());
							
							// close the writer
							out.close();
							
							// log response
							logger.debug("Rapid POST response : " + jsonVersion.toString());
														
						} else if ("uploadImage".equals(rapidRequest.getActionName())) {
																			
							// get the name
							String imageName = request.getParameter("name");
							
							// if we got one
							if (imageName == null) {
								
								// send forbidden response			
								sendMessage(response, 400, "Name required", "Image name must be provided");
								
								// log
								logger.debug("Rapid POST response (403) : Name must be provided");
								
							} else {
															
								// check the jpg file signature (from http://en.wikipedia.org/wiki/List_of_file_signatures)
								if (bodyBytes[0] == (byte)0xFF && bodyBytes[1] == (byte)0xD8 && bodyBytes[2] == (byte)0xFF) {
									
									// create the path
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
									logger.debug("Saved image file " + imageFile);
									
									// create a writer
									PrintWriter out = response.getWriter();
									
									// print the results
									out.print(imagePath);
									
									// close the writer
									out.close();
									
								} else {
									
									// send forbidden response			
									sendMessage(response, 400, "Unrecognised", "Unrecognised file type");
									
									// log
									logger.debug("Rapid POST response (403) : Unrecognised file type");
									
								} // signature check
								
							} // upload file name check
							
						} // action type check

					} else {
						
						// send forbidden response			
						sendMessage(response, 403, "No permisssion", "You do not have permssion to use this application");
						
						// log
						logger.debug("Rapid POST response (403) : User not authorised for application");
											
					}  // user check
				
				} // app check
				
			} // pre app action check
		
		} catch (Exception ex) {
		
			logger.error("Rapid POST error : ", ex);
			
			sendException(rapidRequest, response, ex);
		
		} 											
		
	}

}
