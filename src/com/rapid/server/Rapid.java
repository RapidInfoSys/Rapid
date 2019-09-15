/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import com.rapid.forms.PaymentGateway;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.server.filter.RapidFilter;
import com.rapid.utils.Bytes;
import com.rapid.utils.Files;

public class Rapid extends RapidHttpServlet {

	private static final long serialVersionUID = 1L;

	// these are held here and referred to globally

	public static final String VERSION = "2.4.4.1"; // the master version of this Rapid server instance
	public static final String MOBILE_VERSION = "1"; // the mobile version. update it if you want all mobile devices to run app updates on their next version check
	public static final String JQUERY = "jquery-3.3.1.js"; // the version of jquery we have which we write into the pages, jquery ui is in each control
	public static final String JQUERYUI = "jquery-ui-1.12.1.js"; // the version of jquery ui we have which we write into the design.jsp page, amongst others
	public static final String ADMIN_ROLE = "RapidAdmin"; // allows use the Rapid Admin tool
	public static final String DESIGN_ROLE = "RapidDesign"; // allows use of the Rapid Design tool
	public static final String WORKFLOW_ROLE = "RapidWorkflow"; // allows use of the Rapid Workflow tool
	public static final String USERS_ROLE = "RapidUsers"; // allows user management in Rapid Admin
	public static final String SUPER_ROLE = "RapidSuper"; // allows design of the Rapid Admin app
	public static final String MASTER_ROLE = "RapidMaster"; // allows all apps to appear in Rapid Admin

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
		if (invalidate) {
			// get the session without creating a new one
			HttpSession session = request.getSession(false);
			// invalidate the session if we are asked to
			if (session != null) session.invalidate();
		}
		// go to the start page
		response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion());
	}

	// print the config information of a directory
	private void printConfig(PrintWriter out, MessageDigest digest, int rootLength, File dir) throws IOException {

		// get the dir contents
		File[] files = dir.listFiles();

		// sort the files, folder at the top
		Arrays.sort(files, new Comparator<File>() {

			@Override
			public int compare(File file1, File file2) {

				// get file name 1
				String fileName1 = file1.getName();

				// get file name 2
				String fileName2 = file2.getName();

				// if file 1 is a directory add zzz to get the files to the top
				if (file1.isDirectory()) fileName1 = "zzz" + fileName1;

				// if file 2 is a directory add zzz to get the files to the top
				if (file2.isDirectory()) fileName2 = "zzz" + fileName2;

				// get standard difference
				int diff = com.rapid.utils.Comparators.AsciiCompare(fileName1, fileName2, false);

				// return the difference
				return diff;

			}

		});

		// loop the files
		for (File file : files) {

			// assume no checksum
			String checksum = "";

			// if not a diretory, get checksum
			if (!file.isDirectory()) checksum = "\t" + Files.getChecksum(digest, file);

			// if not .gitignore print it (from the end of the root)
			if (!".gitignore".equals(file.getName())) out.print(file.getPath().substring(rootLength) + "\t" + file.length() + "\t" + getLocalDateTimeFormatter().format(new Date(file.lastModified())) + checksum + "\r\n");

			// if it is a directory, but not the applications nor logs nor temp nor update nor uploads one go iterative
			if (file.isDirectory() && !"applications".equals(file.getName()) && !"logs".equals(file.getName()) && !"temp".equals(file.getName()) && !"update".equals(file.getName()) && !"uploads".equals(file.getName())) printConfig(out, digest, rootLength, file);

		}

	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// fake a delay for testing slow servers
		//try { Thread.sleep(3000); } catch (InterruptedException e) {}

		// get a logger
		Logger logger = getLogger();

		// log!
		logger.debug("Rapid GET request : " + request.getQueryString());

		// get a new rapid request passing in this servlet and the http request
		RapidRequest rapidRequest = new RapidRequest(this, request);

		// if monitor is alive then log the event
		if(_monitor!=null && _monitor.isAlive(rapidRequest.getRapidServlet().getServletContext()) && _monitor.isLoggingAll())
			_monitor.openEntry();

		// we will store the length of the item we are adding
		long responseLength = 0;

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

					// get the action name
					String actionName = rapidRequest.getActionName();

					// check if there is a Rapid action
					if ("summary".equals(actionName) || "pdf".equals(actionName)) {

						// get the form adapter for both of the above
						FormAdapter formAdapter = app.getFormAdapter();

						// check there is one
						if (formAdapter == null) {

							// send message
							sendMessage(response, 500, "Not a form", "This Rapid app is not a form");

							// log
							logger.error("Rapid GET response (500) : Summary requested for " + app.getId() + "/" + app.getDescription() + " " + actionName +" but it does not have a form adapter");

						} else {

							// get the form id from the f parameter
							String formId = request.getParameter("f");

							// if this was the pdf with a form id - otherwise its the summary
							if ("pdf".equals(actionName) && formId != null) {

								// write the form pdf
								formAdapter.doWriteFormPDF(rapidRequest, response, formId, false);

							} else {

								// get the form details
								UserFormDetails formDetails = formAdapter.getUserFormDetails(rapidRequest);

								// check we got form details
								if (formDetails == null) {

									// go to the start page (invalidate session unless user has design role)
									gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));

								} else {

									// if user just hit the back button on a submitted form there will not be a max page yet
									if (formDetails.getMaxPageId() == null) {

										// go to the start page, but keep the session
										gotoStartPage(request, response, app, false);

									} else {

										// if we have a max page some and this is the correct version of the form
										if (app.getId().equals(formDetails.getAppId()) && app.getVersion().equals(formDetails.getVersion())) {

											// if we are showing the form summary and have form summary labels
											if (app.getFormShowSummary() && formAdapter.getHasSummaryLabels(rapidRequest, formDetails.getId())) {
												// summary is never cached
												RapidFilter.noCache(response);
												// write the form summary page
												formAdapter.writeFormSummary(rapidRequest, response);
											} else {
												// no summary to show so got to the start page, without invalidating the session
												gotoStartPage(request, response, app, false);
											}

										} else {
											// request the correct version for the summary (this also avoids ERR_CACH_MISS issues on the back button )
											response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&action=summary");
										}

									}

								}

							}

						}

					} else if ("download".equals(actionName)) {

						// set the file name
						String fileName = app.getId() + "_" + rapidRequest.getUserName() + ".zip";

						// create the zip file
						File zipFile = app.zip(this, rapidRequest, user, fileName, true);

						// set the type as a .zip
						response.setContentType("application/x-zip-compressed");

						// Shows the download dialog
						response.setHeader("Content-disposition","attachment; filename=" + app.getId() + ".zip");

						// download it to the response
						long fileLength = downloadFile(zipFile, response);

						// add to response length for monitoring
						responseLength += fileLength;

						// delete the file
						zipFile.delete();

					} else if ("file".equals(actionName)) {

						// get the requested file name from the n parameter
						String fileName = request.getParameter("n");

						// get the application id that the file should be sitting in
						String applicationId = request.getParameter("a");

						// if we had both of what we needed
						if (fileName!=null && applicationId!=null) {

							// determine the storage from the applications id, also different if server is public
							String uploadPath = (rapidRequest.getRapidServlet().isPublic() ? "WEB-INF/" : "") + "uploads/" + applicationId;

							// get the file path
							String filePath = getServletContext().getRealPath("/") + "/" + uploadPath + "/" + fileName;

							// get the mime type
							response.setContentType(URLConnection.guessContentTypeFromName(fileName));

							// assume inline content disposition which will attempt to display the file in a tab as a web document
							String contentDisposition = "inline";

							// if there was a "d" parameter switch to attachment which will force a download
							if ("Y".equalsIgnoreCase(request.getParameter("d")) || "true".equalsIgnoreCase(request.getParameter("d"))) contentDisposition = "attachment";

							// set for streaming (then download)
							response.setHeader("Content-disposition", contentDisposition + "; filename=" + fileName);

							// download the file to the response
							long fileLength = downloadFile(filePath, response);

							// append the file length for monitoring
							responseLength += fileLength;
						}

					} else if ("config".equals(actionName)) {

						// check this is the Rapid app
						if ("rapid".equals(app.getId())) {

							// check we have admin permission
							if (security.checkUserRole(rapidRequest, ADMIN_ROLE)) {

								// set response to text
								response.setContentType("text/text;charset=UTF-8");

								// get the root path
								String rootPath = getServletContext().getRealPath("/");

								// get the root file
								File root = new File(rootPath);

								// create a writer
								PrintWriter out = response.getWriter();

								// get the md5 digest
								MessageDigest digest = MessageDigest.getInstance("MD5");

								// start printing!
								printConfig(out, digest, root.getAbsolutePath().length(), root);

								// flush and close writer
								out.flush();

								out.close();

							} else {

								// tell user must be Rapid
								sendMessage(response, 403, "No permission", "You must have the admin role for this request");

							}

						} else {

							// tell user must be Rapid
							sendMessage(response, 403, "Incorrect application", "You must use the Rapid application for this request");

						}

					} else {

						// assume it's ok to write the page
						boolean pageCheck = true;
						// assume we won't be redirecting to the summary
						boolean showSummary = false;
						// assume we won't be showing the save
						boolean showSave = false;
						// assume we won't be showing the resume
						boolean showResume = false;

						// get the requested page object
						Page page = rapidRequest.getPage();

						// get the form adapter (if there is one)
						FormAdapter formAdapter = app.getFormAdapter();

						// place holder for the form details
						UserFormDetails formDetails = null;

						try {

							// if there is a formAdapter, make sure there's a form id, unless it's for a simple page
							if (formAdapter != null) {

								// get the form details
								formDetails = formAdapter.getUserFormDetails(rapidRequest);

								// if there are no form details, or this is the correct version
								if (formDetails == null || app.getId().equals(formDetails.getAppId()) && app.getVersion().equals(formDetails.getVersion())) {

									// if there is a start parameter, nuke the session and then move on one page without the start parameter so users can go back to the beginning without loosing values
									if (request.getParameter("start") != null) {
										// empty the user form details
										formAdapter.emptyUserFormDetails(rapidRequest);
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

									// check special form actions
									if ("save".equals(actionName)) {

										// get the save page
										page = app.getPages().getPageByFormType(Page.FORM_PAGE_TYPE_SAVE);
										// if we got one set showSave
										if (page != null) showSave = true;

									} else if ("resume".equals(actionName)) {

										// look for the form id and password from the url
										String resumeFormId = request.getParameter("f");
										String resumePassword = request.getParameter("pwd");
										// if we didn't get the back-office resume parameters we need
										if (resumeFormId == null || resumePassword == null) {
											// get the resume page
											page = app.getPages().getPageByFormType(Page.FORM_PAGE_TYPE_RESUME);
											// if we got one set showResume
											if (page != null) showResume = true;
										} else {
											// try and get the resume form details
											formDetails = formAdapter.doResumeForm(rapidRequest, resumeFormId, resumePassword);
											// check whether we can resume this form
											if (formDetails != null)  {
												// go for the summary if no page specified
												if (request.getParameter("p") == null) showSummary  = true;
											}
										}

									}

									// if there isn't a form id, or we want to show the summary don't check the pages
									if (showSave || showResume) {

										// skip the max page block below
										pageCheck = true;

									} else if (formDetails == null || showSummary) {

										// skip the page check / write
										pageCheck = false;

									} else if (page != null) {

										// check that we have progressed far enough in the form to view this page, or we are a designer
										if (formAdapter.checkMaxPage(rapidRequest, formDetails, page.getId()) || security.checkUserRole(rapidRequest, DESIGN_ROLE)) {

											// only if this is not a dialogue
											if (!"dialogue".equals(actionName)) {

												// get all of the pages
												PageHeaders pageHeaders = app.getPages().getSortedPages();
												// get this page position
												int pageIndex = pageHeaders.indexOf(page.getId());
												// check the page visibility -
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
													} // pages remaining check
												} // page visible loop

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

											} // dialogue check

										} else {

											// go back to the start
											pageCheck = false;
											//log
											logger.debug("Not allowed on page " + page.getId() + " yet!");

										} // page max check

									} // form id check

								} else {

									// redirect to the correct version with the same parameters
									String url = "~?a=" + formDetails.getAppId() + "&v=" + formDetails.getVersion();
									// get the parameter map
									Enumeration<String> parameterNames = request.getParameterNames();
									// loop the current parameters
									while (parameterNames.hasMoreElements()) {
										// get the name
										String parameterName = parameterNames.nextElement();
										// ignore a and v
										if (!"a".equals(parameterName) && !"v".equals(parameterName)) {
											// add to url
											url += "&" + parameterName + "=" + request.getParameter(parameterName);
										}
									}
									// redirect !
									response.sendRedirect(url);

								} // form details and version check

								// if this is a form resume
								if ("resume".equals(actionName)) {
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

							} // form adapter check

						} catch (Exception ex) {

							// set the page to null so we show the user a not found
							page = null;

							// log
							logger.debug("Error with form page : " + ex.getMessage(), ex);

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
								if (pageId.equals(rapidRequest.getPage().getId()) || showSave || showResume) {

									// get any if-none-match - no caching must be set to true in web.xml for this to be sent
									String ifNoneMatch = request.getHeader("If-None-Match");

									// if difference from ETag (including null)
									if (page.getETag().equals(ifNoneMatch)) {

										// send a 304 - not modified
										response.setStatus(304);

									} else {

										// add the etag
										response.addHeader("ETag", page.getETag());

										// create a writer
										PrintWriter out = response.getWriter();

										// assume we require the designer link
										boolean designerLink = true;

										// set designer link to false if action is dialogue
										if ("dialogue".equals(rapidRequest.getActionName())) designerLink =  false;

										// set the response type
										response.setContentType("text/html");

										// write the page html
										page.writeHtml(this, response, rapidRequest, app, user, out, designerLink, false);

										// close the writer
										out.close();

										// flush the writer
										out.flush();

									}

									// if we have a form adapter and form details
									if (formAdapter != null && formDetails != null) {
										// now the page has been printed invalidate the form if this was a submission page
										if (page.getFormPageType() == Page.FORM_PAGE_TYPE_SUBMITTED) formAdapter.setUserFormDetails(rapidRequest, null);
										// if this is an error page we have just shown the error, remove the error
										if (page.getFormPageType() == Page.FORM_PAGE_TYPE_ERROR) formDetails.setErrorMessage(null);
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

								logger.debug("Returning to start - failed page check and no showSummary, showSave, or showResume");

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

			// if monitor is alive then log the event
			if(_monitor!=null && _monitor.isAlive(rapidRequest.getRapidServlet().getServletContext()) && _monitor.isLoggingAll())
				_monitor.commitEntry(rapidRequest, response, responseLength);

		} catch (Exception ex) {

			// if monitor is alive then log the event
			if(_monitor!=null && _monitor.isAlive(rapidRequest.getRapidServlet().getServletContext()) && _monitor.isLoggingExceptions())
				_monitor.commitEntry(rapidRequest, response, responseLength, ex.getMessage());

			logger.error("Rapid GET error : ",ex);

			sendException(rapidRequest, response, ex);

		}

	}

	private long downloadFile(File file, HttpServletResponse response) throws IOException {

		// get the size of the file
		long fileSize = Files.getSize(file);

		// add size to response headers if small enough
		if (fileSize < Integer.MAX_VALUE) response.setContentLength((int) fileSize);

		// assume no length
		long responseLength = 0;

		// send the file to browser
		OutputStream os = response.getOutputStream();
		FileInputStream in = new FileInputStream(file);
		byte[] buffer = new byte[1024];
		int length;
		while ((length = in.read(buffer)) > 0){
			os.write(buffer, 0, length);
			responseLength += length;
		}
		in.close();
		os.flush();

		return responseLength;

	}

	// an override to the above
	private long downloadFile(String filePath, HttpServletResponse response) throws IOException {

		// get the file from the path
		File file = new File(filePath);

		// use that
		return downloadFile(file, response);

	}

	// gets a JSON object from the body bytes - also remove any passwords when logging
	private JSONObject getJSONObject(byte[] bodyBytes) throws UnsupportedEncodingException, JSONException {

		// assume we weren't passed any json
		JSONObject jsonData = null;

		// read the body into a string
		String bodyString = new String(bodyBytes, "UTF-8");

		// get a logger
		Logger logger = getLogger();

		// if there is something in the body string it must be json so parse it
		if (bodyString != null && bodyString.trim().length() > 0) {
			try {
				// get the data
				jsonData = new JSONObject(bodyString);
				// only if debugging enabled
				if (logger.isDebugEnabled()) {
					// if the data contains a password key
					if (jsonData.has("password")) {
						// make a new json object
						JSONObject cleanJsonData = new JSONObject(bodyString);
						// clean the password
						cleanJsonData.put("password", "***");
						// log the clean version
						logger.debug(cleanJsonData);
					} else {
						// log  the bodyString as usual
						logger.debug(bodyString);
					}
				}
			} catch (JSONException ex) {
				// rethrow the exception
				throw ex;
			}

		} else {
			// log the body string
			logger.debug("No request body");
		}

		return jsonData;

	}

	// writes a JSON string to the response and returns the string it used so this is only done once for logging and getting its length
	private String writeJSONResponse(HttpServletResponse response, String jsonString) throws IOException {

		// create a writer
		PrintWriter out = response.getWriter();
		// set response to json
		response.setContentType("application/json");
		// print the results
		out.print(jsonString);
		// close the writer
		out.close();

		// return the response length for logging
		return jsonString;

	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// fake a delay for testing slow servers
		//try { Thread.sleep(3000); } catch (InterruptedException e) {}

		// get a logger
		Logger logger = getLogger();

		// log
		logger.debug("Rapid POST request : " + request.getQueryString() + " bytes=" + request.getContentLength());

		// create a Rapid request
		RapidRequest rapidRequest = new RapidRequest(this, request);

		// read back the body bytes
		byte[] bodyBytes = rapidRequest.getBodyBytes();

		// if monitor is alive then log the event
		if(_monitor!=null && _monitor.isAlive(rapidRequest.getRapidServlet().getServletContext()) && _monitor.isLoggingAll())
			_monitor.openEntry();

		// we will store the length of the item we are adding
		long responseLength = 0;

		// get any action name
		String actionName = rapidRequest.getActionName();

		try {

			// this is the only variant where an application isn't specified and secured first
			if ("getApps".equals(actionName) || "getForms".equals(actionName)) {

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

						// assume the request wasn't from Rapid Mobile
						boolean isRapidMobile = false;

						// if we got some data, look for a test = true entry - this is sent from Rapid Mobile
						if (jsonData != null) isRapidMobile = jsonData.optBoolean("test");

						// loop the apps
						for (Application app : apps) {

							// if this is from the Rapid Mobile client, do not send forms
							if (!isRapidMobile || !app.getIsForm()) {

								// allow unless when we are asking for forms, make sure this is a form - also stop forms going to Rapid Mobile
								if (!"getForms".equals(actionName) || app.getIsForm()) {

									// if Rapid app must not be for testing / from Rapid Mobile
									if (!"rapid".equals(app.getId()) || !isRapidMobile) {

										// get the relevant security adapter
										SecurityAdapter security = app.getSecurityAdapter();

										// make a rapidRequest for this application
										RapidRequest getAppsRequest = new RapidRequest(this, request, app);

										// check the user password
										if (security.checkUserPassword(getAppsRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

											// assume can add
											boolean canAdd = true;

											if ("rapid".equals(app.getId())) {
												// must have one of the official Rapid roles to see Rapid Admin
												canAdd = security.checkUserRole(rapidRequest, Rapid.ADMIN_ROLE) || security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE) || security.checkUserRole(rapidRequest, Rapid.USERS_ROLE) || security.checkUserRole(rapidRequest, Rapid.SUPER_ROLE);
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

												// check if we are from Rapid Mobile
												if (isRapidMobile) {

													// if the user has Rapid Design for this application, (or Rapid Super if this is the rapid app)
													if (security.checkUserRole(getAppsRequest, Rapid.DESIGN_ROLE) && (!app.getId().equals("rapid") || security.checkUserRole(rapidRequest, Rapid.SUPER_ROLE))) {

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

								} // getForms form check

							} // isMobile not form check

						} // apps loop

					} catch (Exception ex) {
						// only log
						logger.error("Error geting apps : ", ex);
					}

				} // apps check

				// write the json apps to the response and retain the string
				String responseString = writeJSONResponse(response, jsonApps.toString());

				// store the length
				responseLength = responseString.length();

				// log response
				logger.trace("Rapid POST response : " + responseString);

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

							// assume no json data
							JSONObject jsonData = null;

							try {

								// read the body bytes with a silent fail - this is so not to throw application errors
								jsonData = getJSONObject(bodyBytes);

							} catch (Exception ex) {

								logger.error("Error reading JSON request body for " + request.getQueryString(), ex);

							}

							// if we got some data
							if (jsonData != null) {

								// fetch the action result
								JSONObject jsonResult = rapidRequest.getAction().doAction(rapidRequest, jsonData);

								// write the json string to the response and retain the length
								String responseString = writeJSONResponse(response, jsonResult.toString());

								// store the response length
								responseLength = responseString.length();

								// log response

								if (logger.isTraceEnabled()) {
									logger.trace("Rapid POST response : " + responseString);
								} else {
									logger.debug("Rapid POST response : length " + responseString.length() + " bytes");
								}

							} // jsonData

						} else if ("checkVersion".equals(actionName)) {

							// create a json version object
							JSONObject jsonVersion = new JSONObject();

							// add the mobile version, followed by the app version
							jsonVersion.put("version", MOBILE_VERSION + " - " + app.getVersion());

							// write the json string to the response and retain the length
							String responseString = writeJSONResponse(response, jsonVersion.toString());

							// store the response length
							responseLength = responseString.length();

							// log response
							logger.debug("Rapid POST response : " + responseString);

						} else if ("getPages".equals(actionName)) {

							// create a json version object
							JSONArray jsonPages = new JSONArray();

							// loop the application pages
							for (PageHeader pageHeader : app.getPages().getSortedPages()) {

								// get the page
								Page page = app.getPages().getPage(pageHeader.getId());

								// assume the user has permission to access the page
								boolean gotPagePermission = true;

								try {

									// get any page roles
									List<String> pageRoles = page.getRoles();

									// if this page has roles
									if (pageRoles != null && pageRoles.size() > 0) {
										// check if the user has any of them
										gotPagePermission = security.checkUserRole(rapidRequest, pageRoles);
									}

								} catch (SecurityAdapaterException ex) {

									logger.error("Error checking for page roles", ex);

								}

								// if we have permission we can add the page
								if (gotPagePermission) {

									// a json object for the page
									JSONObject jsonPage = new JSONObject();

									// add the id
									jsonPage.put("id", page.getId());

									// ad the last modified date
									jsonPage.put("modifiedDate", this.getXMLDateTimeFormatter().format(page.getModifiedDate()));

									// add this page to the collection
									jsonPages.put(jsonPage);

								}

							}

							// write the json apps to the response and retain the length
							String responseString = writeJSONResponse(response, jsonPages.toString());

							// retain the length
							responseLength = responseString.length();

							// log response
							logger.debug("Rapid POST response : " + responseString);

						} else if ("uploadImage".equals(actionName)) {

							// get the name
							String imageName = request.getParameter("name");
							// get the content / mime type
							String contentType = request.getHeader("content-type");
							// assume bytes offset is 0
							int bytesOffset = 0;
							// assume no boundary
							String boundary = "";

							// if name not in the parameter try the more complex boundary way
							if (imageName == null) {
								// if we have a content type
								if (contentType != null) {
									// check boundary
									if (contentType.contains("boundary=")) {
										// get the boundary
										boundary  = contentType.substring(contentType.indexOf("boundary=") + 10);
										// find the end of the double break
										bytesOffset = Bytes.findPattern(bodyBytes, Bytes.DOUBLE_BREAK_BYTES, boundary.length()) + Bytes.DOUBLE_BREAK_BYTES.length;
										// get the headers string
										String headersString = new String(bodyBytes, boundary.length() + 5, bytesOffset - boundary.length() - 8);
										// split the parts
										String[] headers = headersString.split("\r\n");
										// assume no extension
										String ext = null;
										// loop them
										for (String header : headers) {
											// get the parts
											String[] headerParts = header.split(":");
											// if we had a pair
											if (headerParts.length > 1) {
												// content disposition - where the filename is, but only if this server isn't allowing public access
												if (!this.isPublic()) {
													if (headerParts[0].toLowerCase().trim().equals("content-disposition")) {
														// get content parts
														String[] contentParts = headerParts[1].split(";");
														// loop them
														for (String contentPart : contentParts) {
															// if this is the file name
															if (contentPart.trim().toLowerCase().startsWith("filename=")) {
																// split by =
																String[] fileNameParts = contentPart.split("=");
																// if got enough
																if (fileNameParts.length > 1) imageName = fileNameParts[1].trim().replace("\"", "");
															}
														}
													}
												}
												// content type
												if (headerParts[0].toLowerCase().trim().equals("content-type")) {
													// get content parts
													String[] contentParts = headerParts[1].split("/");
													// update content part to exclude
													contentType = headerParts[1].trim();
													// if there are enough parts
													if (contentParts.length > 1) {
														// set the file extension
														ext = contentParts[1].toLowerCase().trim();
														// adjust jpeg to jpg
														if ("jpeg".equals(ext)) ext = "jpg";
													}
												}
											}
										}
										// if we got an extension
										if (ext != null) {
											// instances with public access have their files renamed for safety - if non-public we will already have dug the name out of the headers above, and not set the imageName
											if (imageName == null) {
												// date formatter
												SimpleDateFormat df = new SimpleDateFormat("yyMMddhhmmssS");
												// get the form adapter
												FormAdapter formAdapter = app.getFormAdapter();
												// check if we got one
												if (formAdapter == null) {
													// update the file name with random number
													imageName = df.format(new Date()) + "-" + (new Random().nextInt(89999) + 10000) + "." + ext;
												} else {
													// update the file name with form id and random number
													imageName = df.format(new Date()) + "-" + formAdapter.getFormId(rapidRequest) + "-" + (new Random().nextInt(899) + 100) + "." + ext;
												}
											}
										}
										// add closer to boundary as we take the bytes off later
										boundary += "--";
									}
								}
							}

							// create a writer
							PrintWriter out = response.getWriter();
							// assume not passed
							boolean passed = false;

							// check we got one
							if (imageName == null) {

								// send forbidden response
								sendMessage(response, 400, "Name required", "Image name must be provided");

								// log
								logger.debug("Rapid POST response (400) : Image name must be provided");

							} else {

								// check image name does not contain any control characters
								if (imageName.indexOf("..") < 0 && imageName.indexOf("/") < 0 && imageName.indexOf("\\") < 0) {

									// check the content type is allowed
									if (getUploadMimeTypes().contains(contentType)) {

										// get the bytes
										List<byte[]> bytes = getUploadMimeTypeBytes().get(contentType);

										// if we got some
										if (bytes != null) {

											// for each byte[] in the bytes list
											for (int i = 0; i < bytes.size(); i++) {

												// check the jpg, gif, png, bmp, or pdf file signature (from http://en.wikipedia.org/wiki/List_of_file_signatures)
												if (Bytes.findPattern(bodyBytes, bytes.get(i), bytesOffset, bytes.get(i).length) > -1) {

													try {

														// create the path
														String imagePath = "uploads/" +  app.getId() + "/" + imageName;
														// servers with public access must use the secure upload location
														if (this.isPublic()) imagePath = "WEB-INF/" + imagePath;
														// create a file
														File imageFile = new File(getServletContext().getRealPath(imagePath));
														// create app folder if need be
														imageFile.getParentFile().mkdir();
														// create a file output stream to save the data to
														FileOutputStream fos = new FileOutputStream(imageFile);
														// write the body bytes to the stream
														fos.write(bodyBytes, bytesOffset, bodyBytes.length - bytesOffset - boundary.length());
														// close the stream
														fos.close();

														// store the file length
														responseLength = imageFile.length();

														// log the file creation
														logger.debug("Saved image file " + imagePath);

														// print just the file name
														out.print(imageFile.getName());

														// close the writer
														out.close();

														// we passed the checks
														passed = true;

													} catch (Exception ex) {

														// log
														logger.error("Error saving uploaded file : " + ex.getMessage(), ex);

														// rethrow
														throw new Exception("Error uploading file");

													}

												} // signature check

											} //end of bytes for

										} // bytes check

									}  // content type check

								} // control character check

							} // upload file name check

							// if we didn't pass the file checks
							if (!passed) {

								logger.debug("Rapid POST response (403) : Unrecognised file type must be .jpg, .gif, .png, .bmp, or .pdf or set in uploadMimeTypes in web.xml");

								// send forbidden response
								response.setStatus(400);
								// write message
								out.print("Unrecognised file type");

							}

						}  else if ("application/x-www-form-urlencoded".equals(request.getContentType())) {

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

									// get any form action
									String action = request.getParameter("action");

									// this is a form page's data being submitted
									String formData = new String(bodyBytes, "UTF-8");

									// log it!
									logger.trace("Form data : " + formData);

									// get all of the app pages
									PageHeaders pageHeaders = app.getPages().getSortedPages();

									// get the id of the page we just submitted
									String requestPageId = rapidRequest.getPage().getId();

									// if there's a submit or pay action
									if ("submit".equals(action) || "pay".equals(action) ) {

										// if submitted already go to start (should never happen)
										if (formDetails.getSubmitted()) {

											logger.debug("Returning to start - submit action but form not submitted");

											// go to the start page
											gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));

										} else {

											try {

												// assume no csrftoken
												boolean csrfPass = false;
												// split into name value pairs
												String[] params = formData.split("&");
												// loop params
												for (String param : params) {
													// split on =
													String[] parts = param.split("=");
													// if enough parts
													if (parts.length > 1) {
														// if csrfToken
														if ("csrfToken".equals(parts[0])) {
															// assume no value
															String value = null;
															// decode value with silent fail
															try { value = URLDecoder.decode(parts[1],"UTF-8");	} catch (UnsupportedEncodingException e) {}
															// check if we passed
															if (rapidRequest.getCSRFToken().equals(value)) {
																// set true
																csrfPass = true;
																// we're done
																break;
															}
														}
													}
												}

												// check for pay (if not must be submit)
												if ("pay".equals(action)) {

													// check csrf
													if (csrfPass) {

														// get payment gateway
														PaymentGateway paymentGateway = formAdapter.getPaymentGateway();

														// get redirect url
														String paymentUrl = paymentGateway.getPaymentUrl(rapidRequest);

														// update status to started
														formDetails.setPaymentStarted(true);

														// redirect to payment url
														response.sendRedirect(paymentUrl);

													} else {

														// go to the start page. Destroy session unless user has the design role
														gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));

													}

												} else {

													// this is the submit being requested by the submit button in the summary page. Submit can also happen from forms that have no form summary labels, or showFormSummary = false

													// do the submit (this will call the non-abstract submit, manage the form state, and retain the submit message)
													if (csrfPass) formAdapter.doSubmitForm(rapidRequest);

													// place holder for first submitted page specified in the designer for the app
													String submittedPageId = getFirstPageForFormType(app, Page.FORM_PAGE_TYPE_SUBMITTED);
													// place holder for the submitted page from the adapter
													String submittedPage = formAdapter.getSubmittedPage();

													// check for neither app nor adapter submit page or crf fail
													if ((submittedPageId == null && submittedPage == null) || !csrfPass) {

														// invalidate the form
														formAdapter.setUserFormDetails(rapidRequest, null);

														// if we passed csrf
														if (csrfPass) {

															logger.debug("Returning to start - form has been submitted, no submitted page");

														} else {

															logger.debug("Returning to start - csrf failed");

														}

														// go to the start page. Destroy session unless user has the design role
														gotoStartPage(request, response, app, !security.checkUserRole(rapidRequest, DESIGN_ROLE));

													} else {

														// look for an app submitted page
														if (submittedPageId == null) {

															// request the adapter submitted page
															response.sendRedirect(submittedPage);

														} else {

															// no adapter page so request the first designer submitted page
															response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + submittedPageId);

														} // submittedPageId check

													} // submitted page or csrf fail check

												} // pay / submit check

											} catch (Exception ex) {

												// log the error
												logger.error("Error with form submit " + action + " : " + ex.getMessage(), ex);

												// place holder for first submitted page specified in designer for app
												String errrorPageId = getFirstPageForFormType( app, Page.FORM_PAGE_TYPE_ERROR);
												// place holder for the submitted page from the adapter
												String errorPage = formAdapter.getErrorPage();

												// check we got an error page from the application
												if (errrorPageId == null) {

													// check we got an error page from the adapter
													if (errorPage == null) {

														// no error pages from adapter or designer just rethrow the error
														throw ex;

													} else {

														// request the adapter error page
														response.sendRedirect(errorPage);

													} // error page check

												} else {

													// request the first application error page
													response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + errrorPageId);

												} // errorPageId check

											} // try / catch

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

												// assume we are not requesting a save
												boolean requestSave = false;

												// if form not submitted
												if (!formDetails.getSubmitted()) {

													// if we end save=save this is save request
													requestSave = formData.endsWith("save=save");

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

												// if this is a request for the save page
												if (requestSave) {

													// get the save page
													page = app.getPages().getPageByFormType(Page.FORM_PAGE_TYPE_SAVE);

												} else {

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

												} // request save check

												// if this is a request for the summary page
												if (requestSummary) {

													// mark that this form is complete (if not submitted)
													if (!formDetails.getSubmitted()) {
														// update form details
														formDetails.setComplete(true);
														// update form adapter (for storage)
														formAdapter.setFormComplete(rapidRequest, formDetails);
													}

													// if this form has a summary page, and labels
													if (app.getFormShowSummary() && formAdapter.getHasSummaryLabels(rapidRequest, formDetails.getId())) {

														// send a redirect for the summary (this also avoids ERR_CACH_MISS issues on the back button )
														response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&action=summary");

													} else {

														// this submit occurs as we have run out of pages and would have requested the summary but either formShowSummary is false, or there are no labels

														// do the submit (this will call the non-abstract submit, manage the form state, and retain the submit message)
														formAdapter.doSubmitForm(rapidRequest);

														// place holder for first submitted page specified in the designer for the app
														String submittedPageId = getFirstPageForFormType(app, Page.FORM_PAGE_TYPE_SUBMITTED);
														// place holder for the submitted page from the adapter
														String submittedPage = formAdapter.getSubmittedPage();

														// check for neither app nor adapter submit page or crf fail
														if (submittedPageId == null && submittedPage == null) {

															// invalidate the form
															formAdapter.emptyUserFormDetails(rapidRequest);

															// log
															logger.debug("Returning to start - form has been submitted, no submitted page");

															// go to the start page. Destroy session unless user has the design role
															gotoStartPage(request, response, app, false);

														} else {

															// look for an app submitted page
															if (submittedPageId == null) {

																// request the adapter submitted page
																response.sendRedirect(submittedPage);

															} else {

																// no adapter page so request the first designer submitted page
																response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + submittedPageId);

															} // submittedPageId check

														} // submitted page or csrf fail check

													} // form label check

												} else if (requestSave) {

													// send a redirect for the save  (this avoids ERR_CACH_MISS issues on the back button )
													response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&action=save");

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

						} else {

							// we should never get here under normal operation so we can assume something silly is happening: we'll invalidate the sesssion if there is one
							HttpSession session = request.getSession(false);
							// if we got one invalidate it
							if (session != null) session.invalidate();
							// say not allowed
							response.setStatus(403);

							// log it!
							logger.error("Form submitted without crsf protection");

						} // action type check

					} else {

						// send forbidden response
						sendMessage(response, 403, "No permisssion", "You do not have permssion to use this application");

						// log
						logger.debug("Rapid POST response (403) : User not authorised for application");

					}  // user check

				} // app check

			} // pre app action check

			// if monitor is alive then log the event
			if(_monitor!=null && _monitor.isAlive(rapidRequest.getRapidServlet().getServletContext()) && _monitor.isLoggingAll())
				_monitor.commitEntry(rapidRequest, response, responseLength);

		} catch (Exception ex) {

			// if monitor is alive then log the event
			if(_monitor!=null && _monitor.isAlive(rapidRequest.getRapidServlet().getServletContext()) && _monitor.isLoggingExceptions())
				_monitor.commitEntry(rapidRequest, response, responseLength, ex.getMessage());

			logger.error("Rapid POST error : ", ex);

			sendException(rapidRequest, response, ex);

		}

	}

}
