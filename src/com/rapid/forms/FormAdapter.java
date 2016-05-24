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

package com.rapid.forms;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Pages;
import com.rapid.core.Pages.PageHeader;
import com.rapid.core.Pages.PageHeaders;
import com.rapid.core.Validation;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.server.Rapid;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Numbers;

public abstract class FormAdapter {
	
	// details about a user form
	public static class UserFormDetails implements Serializable {
		
		// from serializable
		private static final long serialVersionUID = 101L;
		
		// instance variables
		private final String _id, _password;
		private String _maxPageId, _submittedDateTime, _submitMessage, _errorMessage;
		boolean _saved, _complete, _showSubmitPage;
		
		// properties
		
		// id
		public String getId() { return _id; }
		// password
		public String getPassword() { return _password; }
		
		// whether this form has been saved
		public boolean getSaved() { return _saved; }
		public void setSaved(boolean saved) { _saved = saved; }
		
		// max page to which users have already been / are allowed
		public String getMaxPageId() { return _maxPageId; }
		public void setMaxPageId(String maxPageId) {_maxPageId = maxPageId; }
		
		// whether this form has been completed 
		public boolean getComplete() { return _complete; }
		public void setComplete(boolean complete) { _complete = complete; }
				
		// the date/time the form was submitted to show on the summary screen
		public String getSubmittedDateTime() { return _submittedDateTime; }
		public void setSubmittedDateTime(String submittedDateTime) { _submittedDateTime = submittedDateTime; }
		// a helper method for the above
		public boolean getSubmitted() { return _submittedDateTime == null ? false : true; }
		
		// the recently submitted message
		public String getSubmitMessage() { return _submitMessage; }
		public void setSubmitMessage(String submitMessage) { _submitMessage = submitMessage; }
		
		// whether to show the submission page (not allowed for resuming submitted forms)
		public boolean getShowSubmitPage() { return _showSubmitPage; }
		public void setShowSubmitPage(boolean showSubmitPage) { _showSubmitPage = showSubmitPage; } 
				
		// the recent submission error
		public String getErrorMessage() { return _errorMessage; }
		public void setErrorMessage(String errorMessage) { _errorMessage = errorMessage; }
		// a helper method for the above
		public boolean getError() { return _errorMessage == null ? false : true; }
						
		// constructors		
		
		// brand new forms
		public UserFormDetails(String id, String password) {
			_id = id;
			_password = password;
		}
		
		// resumed forms
		public UserFormDetails(String id, String password, String maxPageId, boolean complete, String submittedDateTime) {
			_id = id;
			_password = password;
			_maxPageId = maxPageId;
			_complete = complete;
			_submittedDateTime = submittedDateTime;
		}
		
	}
	
	// details about a submitted form
	public static class SubmissionDetails {
		
		// instance variables
		String _message, _dateTime;
		
		// properties
		public String getMessage() { return _message; }
		public String getDateTime() { return _dateTime; }
		
		// constructor
		public SubmissionDetails(String message, String dateTime) {
			_message = message;
			_dateTime = dateTime;
		}
		
	}
	
	// a single controls value
	public static class FormControlValue {
		
		// instance variables
		private String _id, _value;
		private boolean _hidden;
		
		// properties
		public String getId() { return _id; }
		public String getValue() { return _value; }
		public void setValue(String value) { _value = value;	}
		public Boolean getHidden() { return _hidden; }
		public void setHidden( boolean hidden) { _hidden = hidden; 	}
		
		// constructors
		public FormControlValue(String id, String value, boolean hidden) {
			_id = id;
			_value = value;
			_hidden = hidden;
		}
		
		public FormControlValue(String id, String value) {
			_id = id;
			_value = value;
		}
		
		// override
		
		@Override
		public String toString() {
			return _id + "  = " + _value + (_hidden ? " (hidden)"  : "");
		}
		
	}
	
	// a pages control values
	public static class FormPageControlValues extends ArrayList<FormControlValue> {
				
		// constructor
		
		public FormPageControlValues(FormControlValue... controlValues) {
			if (controlValues != null) {
				for (FormControlValue controlValue : controlValues) {
					this.add(controlValue);
				}
			}
		}
		
		// methods
		
		public void add(String controlId, String controlValue, boolean hidden) {
			this.add(new FormControlValue(controlId, controlValue, hidden));
		}
		
		public void add(String controlId, String controlValue) {
			this.add(new FormControlValue(controlId, controlValue));
		}
		
		public FormControlValue get(String controlId) {
			for (FormControlValue controlValue : this) {
				if (controlId.equals(controlValue.getId())) return controlValue;
			}
			return null;
		}
		
		public String getValue(String controlId) {
			for (FormControlValue controlValue : this) {
				if (controlId.equals(controlValue.getId())) return controlValue.getValue();
			}
			return null;
		}
						
	}
	
	// this exception class can be extended for more meaningful exceptions that may occur within the adapters
	public static class ServerSideValidationException extends Exception {
		
		private String _message;
		private Throwable _cause;
		
		public ServerSideValidationException(String message) {
			_message = message;
		}
		
		public ServerSideValidationException(String message, Throwable cause) {
			_message = message;
			_cause = cause;
		}

		@Override
		public String getMessage() {
			return _message;
		}
		
		@Override
		public Throwable getCause() {
			if (_cause == null) return super.getCause();
			return _cause;			
		}
		
		@Override
		public StackTraceElement[] getStackTrace() {
			if (_cause == null) return super.getStackTrace();
			return _cause.getStackTrace();			
		}
		
	}
	
	//  static finals
	private static final String USER_FORM_DETAILS = "userFormDetails";
	private static final String USER_FORMS_SUBMITTED = "userFormsSubmitted";
	private static final String USER_FORM_ID = "userFormId";
		
	// instance variables
	
	protected ServletContext _servletContext;
	protected Application _application;
	protected Logger _logger;
	
	// properties
	
	public ServletContext getServletContext() { return _servletContext; }
	public Application getApplication() { return _application; }
	
	// constructor
	
	public FormAdapter(ServletContext servletContext, Application application) {
		_servletContext = servletContext;
		_application = application;
		_logger = Logger.getLogger(FormAdapter.class);
	}
	
	// private instance methods
	
	private String getFormMapKey(RapidRequest rapidRequest) {
		// get the application
		Application application = rapidRequest.getApplication();
		// return the key
		return application.getId() + "-" + application.getVersion();
	}
	
	// abstract methods
					
	// this method returns a new form id, when allowed, by a given adapter, could be in memory, or database, etc
	public abstract UserFormDetails getNewFormDetails(RapidRequest rapidRequest) throws Exception;
	
	// this method checks a form id against a password for resuming 
	public abstract UserFormDetails getResumeFormDetails(RapidRequest rapidRequest, String formId, String password) throws Exception;
				
	// sets the maximum page id the user is allowed to see
	public abstract void setMaxPage(RapidRequest rapidRequest, UserFormDetails formDetails, String pageId) throws Exception;
	
	// sets that a form has been completed (and we can show the submit button on the summary)
	public abstract void setFormComplete(RapidRequest rapidRequest, UserFormDetails formDetails) throws Exception;
	
	// gets any page/session variables for this form
	public abstract Map<String,String> getFormPageVariableValues(RapidRequest rapidRequest, String formId) throws Exception;
	
	// sets any page/session variables for this form
	public abstract void setFormPageVariableValue(RapidRequest rapidRequest, String formId, String name, String value) throws Exception;
					
	// returns all the form control values for a given page
	public abstract FormPageControlValues getFormPageControlValues(RapidRequest rapidRequest, String formId, String pageId) throws Exception;
	
	// sets all the form control values for a given page - pages that fail the isVisible method will be sent  a null pageControlValues
	public abstract void setFormPageControlValues(RapidRequest rapidRequest, String formId, String pageId, FormPageControlValues pageControlValues) throws Exception;
	
	// gets the value of a form control value	
	public abstract String getFormControlValue(RapidRequest rapidRequest, String formId, String controlId, boolean notHidden) throws Exception;
						
	// this html is written after the body tag
	public abstract String getSummaryStartHtml(RapidRequest rapidRequest, Application application);
	
	// this html is written after the submit button, before the body close tag 
	public abstract String getSummaryEndHtml(RapidRequest rapidRequest, Application application);
		
	// this html is written for the start of each page
	public abstract String getSummaryPageStartHtml(RapidRequest rapidRequest, Application application, Page page);
	
	// this html is written for the end of each page
	public abstract String getSummaryPageEndHtml(RapidRequest rapidRequest, Application application, Page page);
	
	// this html contains the control value
	public abstract String getSummaryControlValueHtml(RapidRequest rapidRequest, Application application, Page page, FormControlValue controlValue);
	
	// this html is written after the end of the pages, before the submit button
	public abstract String getSummaryPagesEndHtml(RapidRequest rapidRequest, Application application);
		
	// submits the form and receives a message for the submitted page
	public abstract SubmissionDetails submitForm(RapidRequest rapidRequest) throws Exception;
	
	// closes any resources used by the form adapter when the server shuts down
	public abstract void close() throws Exception;
				
	// protected instance methods
	
	// this writes the form pdf
	protected void writeFormPDF(RapidRequest rapidRequest, HttpServletResponse response, String formId) throws Exception {}
		
	// this gets the list of submitted forms from the users session
	protected List<String> getSubmittedForms(RapidRequest rapidRequest) {
		// first try and get from the session
		List<String> submittedForms = (List<String>) rapidRequest.getSessionAttribute(USER_FORMS_SUBMITTED);
		// if this is null
		if (submittedForms == null) {
			// make a new one
			submittedForms = new ArrayList<String>();
			// store it
			rapidRequest.getRequest().getSession().setAttribute(USER_FORMS_SUBMITTED, submittedForms);
		}
		// return 
		return submittedForms;
	}
	
	// this adds a form id to the submitted list
	protected void addSubmittedForm(RapidRequest rapidRequest, String formId) {
		// get  the list
		List<String> submittedForms = getSubmittedForms(rapidRequest);
		// if not there already
		if (!submittedForms.contains(formId)) {
			// add it
			submittedForms.add(formId);
			// store it
			rapidRequest.getRequest().getSession().setAttribute(USER_FORMS_SUBMITTED, submittedForms);
		}
		
	}
	
	// public instance methods
			
	// sets whether the form has been saved
	public synchronized void setUserFormSaved(RapidRequest rapidRequest, boolean saved) throws Exception {
		// get the details
		UserFormDetails details = getUserFormDetails(rapidRequest);
		// update if we got some
		if (details != null) details.setSaved(saved);
	}
	
	// returns the form id in the user session for a given application id and version
	public synchronized UserFormDetails getUserFormDetails(RapidRequest rapidRequest) throws Exception {		
		// get the user session (without making a new one)
		HttpSession session = rapidRequest.getRequest().getSession(false);
		// get the form ids map from the session
		Map<String,UserFormDetails> allFormDetails = (Map<String, UserFormDetails>) session.getAttribute(USER_FORM_DETAILS);
		// if null
		if (allFormDetails == null) {
			// log
			_logger.debug("Creating user session form details store for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr());
			// make some
			allFormDetails = new HashMap<String, UserFormDetails>();
			// add to session
			session.setAttribute(USER_FORM_DETAILS, allFormDetails);
		}
		// get the form key
		String formKey = getFormMapKey(rapidRequest);
		// get the details for this form
		UserFormDetails formDetails = allFormDetails.get(formKey);
		// check we got some
		if (formDetails == null) {
			// log
			_logger.debug("No form details of " + formKey + " for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr());
			// get the application
			Application application = rapidRequest.getApplication();			
			// assume no start page
			String startPageId = ""; 
			// if there are pages
			if (application.getPages() != null) {
				// get the id of the first one
				if (application.getPages().size() > 0) startPageId = application.getPages().getSortedPages().get(0).getId();
			}						
			// get the requested Page
			Page requestPage = rapidRequest.getPage();
			// get the request page id
			String requestPageId = null;
			// if there was a page get the id
			if (requestPage  != null) requestPageId = requestPage.getId();
			// assume no new id allowed
			boolean newFormAllowed = false;
			// if this is the start page
			if  (startPageId.equals(requestPageId)) {
				// we're ok to request new form details
				newFormAllowed = true;
				// log
				_logger.debug("New form allowed");
			} else {
				// get the security adapter
				SecurityAdapter security = application.getSecurityAdapter();
				// if the user has design 
				try {
					if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) {
						// we're ok to request new form details
						newFormAllowed = true;
						// log
						_logger.debug("New form allowed for designer");
					}
				} catch (SecurityAdapaterException e) {}				
			}
			// there are some rules for creating new form ids - there must be no action and the page must be the start page
			if (rapidRequest.getRequest().getParameter("action") == null && newFormAllowed) {				
				// get a new form details from the adapter
				formDetails = getNewFormDetails(rapidRequest);
				// set the new user form details
				setUserFormDetails(rapidRequest, formDetails);		
				// store the form id in the session - THIS IS A TEMPORARY MEASURE TO ENSURE USER FORM DETAILS ARE NOT CROSSING OVER TO OTHER USERS !!!!!!!!!!!
				rapidRequest.getRequest().getSession(false).setAttribute(USER_FORM_ID, formDetails.getId());
				// log
				_logger.debug("New form details requested, form id is " + formDetails.getId());
			}
		} else {
			// log
			_logger.debug("Form details retrived for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr() + ", form id is " + formDetails.getId());
			try {
				// get the session form id
				String formId = (String) rapidRequest.getRequest().getSession(false).getAttribute(USER_FORM_ID);
				// check it
				if (formId == null) {
					throw new Exception("Form session id has not been set for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr() + ", but form details object id is " + formDetails.getId());
				} else {
					// compare them
					if (!formId.equals(formDetails.getId())) {
						// they're different so check for designer security before throwing an exception, assume not designer
						boolean isDesigner = false;
						// get the application
						Application app = rapidRequest.getApplication();
						// check it
						if (app != null) {
							// get it's security
							SecurityAdapter security = app.getSecurityAdapter();
							// check we got one
							if (security != null) {
								// check the role (fail silently)
								try {
									if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) isDesigner = true;
								} catch (Exception ex) {}
							}
						}
						// throw the exception if not the designer
						if (!isDesigner) throw new Exception("Form session id mismatch for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr() + ", form session id is " + formId + " but form details object id is " + formDetails.getId());
					}
				}
			} catch (Exception ex) {
				// log
				_logger.error("Error checking session form id and form details ", ex);
				// empty the session - THIS IS A TEMPORARY MEASURE TO ENSURE USER FORM DETAILS ARE NOT CROSSING OVER TO OTHER USERS !!!!!!!!!!!
				rapidRequest.getRequest().getSession().invalidate();
				// set the form details to null - THIS IS A TEMPORARY MEASURE TO ENSURE USER FORM DETAILS ARE NOT CROSSING OVER TO OTHER USERS !!!!!!!!!!!
				formDetails = null;
			}
			
		}
		// return the user form details
		return formDetails;
	}
						
	// sets the form details in the user session for a given application id and version
	public synchronized void setUserFormDetails(RapidRequest rapidRequest, UserFormDetails details) {
		// get the user session (making a new one if need be)
		HttpSession session = rapidRequest.getRequest().getSession();
		// get all user form details
		Map<String,UserFormDetails> allDetails = (Map<String, UserFormDetails>) session.getAttribute(USER_FORM_DETAILS);
		// make some if we didn't get
		if (allDetails == null) allDetails = new HashMap<String, UserFormDetails>(); 	
		// store the form if for a given app id / version
		allDetails.put(getFormMapKey(rapidRequest), details);	
		// put the updated forms details back in the session
		session.setAttribute(USER_FORM_DETAILS, allDetails);
		// if we were given details
		if (details == null) {
			// put the new form id in the session
			session.setAttribute(USER_FORM_ID, null);
		} else {
			// put the new form id in the session
			session.setAttribute(USER_FORM_ID, details.getId());
		}		
	}
	
	// a helper method to get the form id via the details
	public synchronized String getFormId(RapidRequest rapidRequest) throws Exception {
		// get the user form details
		UserFormDetails formDetails = getUserFormDetails(rapidRequest);
		// check we got some
		if (formDetails == null) {
			return null;
		} else {
			return formDetails.getId();
		}		
	}
	
	// checks a given page id against the maximum
	public synchronized boolean checkMaxPage(RapidRequest rapidRequest, UserFormDetails formDetails, String pageId) throws Exception {
		// assume not completed
		boolean check = false;
		// get the application
		Application application = rapidRequest.getApplication();
		// check we got one
		if (formDetails != null && application != null) {
			// get the sorted pages
			PageHeaders pages = application.getPages().getSortedPages();			
			// get a scaler
			String maxPageId = formDetails.getMaxPageId();			
			// check we got something
			if (maxPageId == null) {
				// fine if the first page
				if (pageId.equals(pages.get(0).getId())) check = true;
			} else {
				// check we got some pages
				if (pages != null) {
					if (pages.size() > 0) {										
						// get the position of the maxPage
						int maxPageIndex = pages.indexOf(maxPageId);
						// get the position of this page
						int pageIndex = pages.indexOf(pageId);
						// if we're allowed at this point
						if (pageIndex <= maxPageIndex) check = true;
					}
				}				
			}
		}		
		// return
		return check;
	}

	public synchronized void doSubmitForm(RapidRequest rapidRequest) throws Exception {
		// get the form details
		UserFormDetails formDetails = getUserFormDetails(rapidRequest);
		try {
			// if submitted already throw exception
			if (formDetails.getSubmitted()) throw new Exception("This form has already been submitted");
			// get the submission details
			SubmissionDetails submissionDetails = submitForm(rapidRequest);						
			// retain the submitted date/time in the details
			formDetails.setSubmittedDateTime(submissionDetails.getDateTime());
			// retain the submit message in the details
			formDetails.setSubmitMessage(submissionDetails.getMessage());
			// allow the submission page to be seen
			formDetails.setShowSubmitPage(true);
			// retain that this form was submitted
			addSubmittedForm(rapidRequest, formDetails.getId());
		} catch (Exception ex) {
			// retain the error message in the details
			formDetails.setErrorMessage(ex.getMessage());
			// rethrow
			throw ex;
		}		
	}
		
	// used when resuming forms
	public synchronized UserFormDetails doResumeForm(RapidRequest rapidRequest, String formId, String password) throws Exception {
		// get the application
		Application application = rapidRequest.getApplication();
		// if there was one
		if (application != null) {
			// get the pages
			Pages pages = application.getPages();
			// if we got some
			if (pages != null) {
				// loop them
				for (String pageId : pages.getPageIds()) {
					// get the page
					Page page = pages.getPage(rapidRequest.getRapidServlet().getServletContext(), pageId);
					// if it has variables
					List<String> variables = page.getSessionVariables();
					// if it has some
					if (variables != null) {
						// loop them
						for (String variable : variables) {
							// set them to null
							rapidRequest.getRequest().getSession().setAttribute(variable, null);
						}
					}
				}
			}
		}
		// check the password against the formId using the user-implemented method
		UserFormDetails details = getResumeFormDetails(rapidRequest, formId, password);
		// check success
		if (details == null) {
			// invalidate any current form
			setUserFormDetails(rapidRequest, null);
		} else {
			// set the form details
			setUserFormDetails(rapidRequest, details);
			// get the user page variable values
			Map<String, String> pageVariableValues = getFormPageVariableValues(rapidRequest, formId);
			// if we got some
			if (pageVariableValues != null) {
				// loop them
				for (String variable  : pageVariableValues.keySet()) {
					// get the value
					String value = pageVariableValues.get(variable);
					// set the value
					rapidRequest.getRequest().getSession().setAttribute(variable, value);
				}
			}
		} 	
		// return the result
		return details;
	}
	
	// used when resuming forms
	public void doWriteFormPDF(RapidRequest rapidRequest, HttpServletResponse response, String formId) throws Exception {
		
		// assume not passed
		boolean passed = false;
		
		// check we got a form id
		if (formId != null) {
			// check this has been submitted
			if (getSubmittedForms(rapidRequest).contains(formId)) passed = true;
		}
		
		// check for a form id - should be null if form not commence properly
		if (passed) {
			
			// write the pdf
			writeFormPDF(rapidRequest, response, formId);
			
		} else {
			
			// send users back to the start
			response.sendRedirect("~?a=" + _application.getId() + "&v=" + _application.getVersion());
															
		}
		
	}
		
	// this writes the form summary page
	public void writeFormSummary(RapidRequest rapidRequest, HttpServletResponse response) throws Exception {
		
		// get the user form details
		UserFormDetails formDetails = getUserFormDetails(rapidRequest);
		
		// check for a form id - should be null if form not commence properly
		if (formDetails == null) {
			
			// send users back to the start
			response.sendRedirect("~?a=" + _application.getId() + "&v=" + _application.getVersion());
			
		} else {
		
			// create a writer
			PrintWriter writer = response.getWriter();
					
			// set the response type
			response.setContentType("text/html");
			
			// this doctype is necessary (amongst other things) to stop the "user agent stylesheet" overriding styles
			writer.write("<!DOCTYPE html>\n");
																							
			// open the html
			writer.write("<html>\n");
			
			// open the head
			writer.write("  <head>\n");
			
			// write a title
			writer.write("    <title>Form summary - by Rapid</title>\n");
			
			// get the servletContext
			ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
			
			// get app start page
			Page startPage = _application.getStartPage(servletContext);
						
			// write the start page head (and it's resources)
			writer.write(startPage.getResourcesHtml(_application, true));
			
			// close the head
			writer.write("  </head>\n");
			
			// open the body
			writer.write("  <body>\n");
			
			// write the summary start
			writer.write(getSummaryStartHtml(rapidRequest, _application));
			
			// get the sorted pages
			PageHeaders pageHeaders = _application.getPages().getSortedPages();
			
			// assume the page return link is edit
			String pageReturn = "edit";
			// update to view if submitted
			if (formDetails.getSubmitted()) pageReturn = "view";
						
			// loop the page headers
			for (PageHeader pageHeader : pageHeaders) {
																						
				// a string builder for the page values
				StringBuilder valuesStringBuilder = new StringBuilder();
				
				// get any page control values
				FormPageControlValues pageControlValues = _application.getFormAdapter().getFormPageControlValues(rapidRequest, formDetails.getId(), pageHeader.getId());
				
				// if non null
				if (pageControlValues != null) {
					
					// if we got some
					if (pageControlValues.size() > 0) {
						
						// get the page with this id
						Page page = _application.getPages().getPage(servletContext, pageHeader.getId());
					
						// get all page controls (in display order)
						List<Control> pageControls = page.getAllControls();
					
						// loop the page controls
						for (Control control : pageControls) {
							
							// loop the page control values
							for (FormControlValue controlValue : pageControlValues) {
																						
								// look for an id match
								if (control.getId().equals(controlValue.getId())) {
							
									// write the control value!
									valuesStringBuilder.append(getSummaryControlValueHtml(rapidRequest, _application, page, controlValue));

									// exit this loop
									break;
																		
								}
								
							}
							
							// if there are no controlValues left we can stop entirely
							if (pageControlValues.size() == 0) break;
							
						} // page control loop
						
						// if there are some values in thre string builder
						if (valuesStringBuilder.length() > 0) {
							
							// write the page start html
							writer.write(getSummaryPageStartHtml(rapidRequest, _application, page));
							
							// write the values
							writer.write(valuesStringBuilder.toString());
														
							// write the edit link
							writer.write("<a href='~?a=" + _application.getId() + "&v=" + _application.getVersion() + "&p=" + page.getId() + "'>" + pageReturn + "</a>\n");
							
							// write the page end html
							writer.write(getSummaryPageEndHtml(rapidRequest, _application, page));
							
						} // values written check
						
					} // control values length > 0
					
				} // control values non null
				
				// stop here if this is the max page that they got to
				if (pageHeader.getId().equals(formDetails.getMaxPageId())) break;
																				
			} // page loop
						
			// write the pages end
			writer.write(getSummaryPagesEndHtml(rapidRequest, _application));
						
			// if the form has been completed
			if (formDetails.getComplete()) {
				// if it has been submitted
				if (formDetails.getSubmitted()) {
					// look for a submitted date/time
					String submittedDateTime = formDetails.getSubmittedDateTime();
					// add if we got one
					if (submittedDateTime != null) writer.write("<span class='formSubmittedDateTime'>" + submittedDateTime + "</span>");					
				} else {
					// add the submit button
					writer.write("<form action='~?a=" + _application.getId() + "&v=" + _application.getVersion()  + "&action=submit' method='POST'><button type='submit' class='formSummarySubmit'>Submit</button></form>");
				}
			}
			
			// write the summary end 
			writer.write(getSummaryEndHtml(rapidRequest, _application));
									
			// close the remaining elements
			writer.write("  </body>\n</html>");
																																		
			// close the writer
			writer.close();
			
			// flush the writer
			writer.flush();
			
		} // form id check
		
	}
		
	// static methods
	
	public static FormPageControlValues getPostPageControlValues(RapidRequest rapidRequest, String postBody, String formId) throws ServerSideValidationException  {
		// check for a post body
		if (postBody == null) {
			// send null if nothing 
			return null;
		} else {
			// create our pageControlValues
			FormPageControlValues pageControlValues = new FormPageControlValues();
			// split into name value pairs
			String[] params = postBody.split("&");			
			// hidden control values
			String[] hiddenControls = null;
			// loop the pairs
			for (int i = 0; i < params.length; i++) {
				// get the param
				String param = params[i];
				// split on =
				String[] parts = param.split("=");						
				// the key/name is the control id
				String id = null;
				// assume it's not hidden
				boolean hidden = false;
				// try and decode the if with a silent fail
				try { id = URLDecoder.decode(parts[0],"UTF-8");	} catch (UnsupportedEncodingException e) {}		
				// check we got something
				if (id != null) {
					// if there was a name but not the _hiddenControls
					if (id.length() > 0) {
						// assume there are no more of this parameter
						boolean lastValue = true;
						// now check there are no more (check boxes get sent with a null in front, in case they are not ticked so we know it update their value)
						for (int j = i + 1; j < params.length; j++) {
							// get the check param
							String checkParam = params[j];
							// if this starts with the id
							if (checkParam.startsWith(id)) {
								// update last value
								lastValue = false;
								// we're done
								break;
							}
						}
						// if this was the last value for the control
						if (lastValue) {
							// assume no value
							String value = null;	
							// if more than 1 part
							if (parts.length > 1) {
								// url decode value 
								try { value = URLDecoder.decode(parts[1],"UTF-8"); } catch (UnsupportedEncodingException ex) {}				
							} // parts > 0	
							// null can't do any harm so don't check them
							if (value != null) {
								// find the control in the page
								Control control = rapidRequest.getPage().getControl(id);
								// only if we found it
								if (control != null) {																										
									// get any control validation
									Validation validation = control.getValidation();
									// if there was some
									if (validation != null) {
										// get the RegEx
										String regEx = validation.getRegEx();
										// set to empty string if null (most seem to be empty)
										if (regEx == null) regEx = "";
										// not if none, and not if javascript
										if (regEx.length() > 0 && !"".equals(validation.getType()) && !"none".equals(validation.getType()) && !"javascript".equals(validation.getType())) {										
											// check for null
											if (value != null) {
												// place holder for the patter
												Pattern pattern = null;
												// this exception is uncaught but we want to know about it
												try {
													// we recognise a small subset of switches
													if (regEx.endsWith("/i")) {
														// trim out the switch
														regEx = regEx.substring(0, regEx.length() - 2);
														// build the pattern with case insensitivity
														pattern = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);		
													} else {
														// build the patter as-is
														pattern = Pattern.compile(regEx);
													}
												} catch (PatternSyntaxException ex) {
													// rethrow
													throw new ServerSideValidationException("Server side validation error - regex for control " + id + " in form " + formId + " failed regex syntax for " + regEx + " - regex PatternSyntaxException", ex);
												} catch (IllegalArgumentException  ex) {
													// rethrow
													throw new ServerSideValidationException("Server side validation error - value '" + value + "' for control " + id + " in  form " + formId + " failed regex " + regEx + " - regex ServerSideValidationException", ex);
												}											
												// compile and check it
												if (!pattern.matcher(value).find()) throw new ServerSideValidationException("Server side validation error - value " + id + " for  form " + formId+ " failed regex");										
											} // javascript type check		
										} // regex check
									} // validation check
																		
									// look for a maxLength property
									String maxLength = control.getProperty("maxLength");
									// if we got one
									if (maxLength != null) {
										if (Numbers.isInteger(maxLength)) {
											// convert to int
											int max = Integer.parseInt(maxLength);
											// make line breaks \n instead of \n\r so the Java length matches the front end
											value = value.replace("\r\n", "\n");
											// check length
											if (value.length() > max) throw new ServerSideValidationException("Server side validation error - value " + id + " for  form " + formId+ " failed regex");
										}
									}
									
								} // found control in page
							} // null check
							// if this is the hidden values
							if (id.endsWith("_hiddenControls") && value != null) {
								// retain the hidden values
								hiddenControls = value.split(",");
							} else	{
								// if we have hidden controls to check
								if (hiddenControls != null) {								
									// loop the hidden controls
									for (String hiddenControl : hiddenControls) {
										// if there's a match
										if (id.equals(hiddenControl)) {
											// retain as hidden
											hidden = true;
											// we're done
											break;
										} // this is a hidden control
									} // loop the hidden controls
								} // got hidden controls to check
								// add name value pair
								pageControlValues.add(id, value, hidden);
							} // ends with hidden controls	
						} // last value						
					}	// id .length > 0
				} // id != null																
			} // param loop			
			return pageControlValues;						
		} // postBody check				
	}
	
}
