/*

Copyright (C) 2018 - Gareth Edwards / Rapid Information Systems

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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import com.rapid.core.Application;
import com.rapid.core.Email;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Http;

public class RapidFormAdapter extends FormAdapter {

	//  static finals
	private static final String NEXT_FORM_ID = "nextFormId";
	private static final String USER_FORM_PAGE_VARIABLE_VALUES = "userFormPageVariableValues";
	private static final String USER_FORM_PAGE_CONTROL_VALUES = "userFormPageControlValues";
	private static final String USER_FORM_COMPLETE_VALUES = "userFormCompleteValues";
	private static final String USER_FORM_SUBMIT_DETAILS = "userFormSubmitDetails";
	private static final String USER_FORM_SAVE_PASSWORDS = "userFormSavePasswords";

	// constructor

	public RapidFormAdapter(ServletContext servletContext, Application application, String type) {
		super(servletContext, application, type);
	}

	// class methods

	// the RapidFormAdapter holds all values in the user session so this method just gets them from there
	protected Map<String,FormPageControlValues> getUserFormPageControlValues(RapidRequest rapidRequest, String formId) throws Exception {
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// get all app page control values from the context
		Map<String,Map<String,FormPageControlValues>> userAppPageControlValues = (Map<String, Map<String, FormPageControlValues>>) servletContext.getAttribute(USER_FORM_PAGE_CONTROL_VALUES);
		// if null
		if (userAppPageControlValues == null) {
			// instantiate
			userAppPageControlValues = new HashMap<String, Map<String, FormPageControlValues>>();
			// add to session
			servletContext.setAttribute(USER_FORM_PAGE_CONTROL_VALUES, userAppPageControlValues);
		}
		// the page controls for specified app
		Map<String,FormPageControlValues> userPageControlValues = userAppPageControlValues.get(formId);
		// if null, instantiate
		if (userPageControlValues == null) {
			// instantiate
			userPageControlValues = new HashMap<String, FormPageControlValues>();
			// add to user app pages
			userAppPageControlValues.put(formId, userPageControlValues);
		}

		// example page control pre-population
		// userPageControlValues.put("P2", new FormPageControlValues(new FormControlValue("P2_C1_", "Hello world !!!")));

		// return!
		return userPageControlValues;
	}

	// this uses a similar technique to record whether the form is complete or not
	protected Map<String,Boolean> getUserFormCompleteValues(RapidRequest rapidRequest) {
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// get the map of completed values
		Map<String,Boolean> userFormCompleteValues = (Map<String, Boolean>) servletContext.getAttribute(USER_FORM_COMPLETE_VALUES);
		// if there aren't any yet
		if  (userFormCompleteValues == null) {
			// make some
			userFormCompleteValues = new HashMap<String,Boolean>();
			// store them
			servletContext.setAttribute(USER_FORM_COMPLETE_VALUES, userFormCompleteValues);
		}
		// return
		return userFormCompleteValues;
	}

	protected Map<String,String> getUserFormPageVariableValues(RapidRequest rapidRequest, String formId) {
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// get the map of form values
		Map<String, HashMap<String, String>> userFormPageVariableValues = (Map<String, HashMap<String, String>>) servletContext.getAttribute(USER_FORM_PAGE_VARIABLE_VALUES);
		// if there aren't any yet
		if  (userFormPageVariableValues == null) {
			// make some
			userFormPageVariableValues = new HashMap<String,HashMap<String,String>>();
			// store them
			servletContext.setAttribute(USER_FORM_PAGE_VARIABLE_VALUES, userFormPageVariableValues);
		}
		// get the map of values
		HashMap<String, String> formPageVariableValues = userFormPageVariableValues.get(formId);
		// if it's null
		if (formPageVariableValues == null) {
			// make some
			formPageVariableValues = new HashMap<String,String>();
			// store them
			userFormPageVariableValues.put(formId, formPageVariableValues);
		}
		// return
		return formPageVariableValues;
	}

	// overridden methods

	// this gets a new form id, when required, from an attribute in the servletContext
	@Override
	public UserFormDetails getNewFormDetails(RapidRequest rapidRequest) {
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// the master form id as a string
		String nextFormIdString = (String) servletContext.getAttribute(NEXT_FORM_ID);
		// if null set to "0"
		if (nextFormIdString == null) nextFormIdString = "0";
		// add 1 to the master form id
		String formId = Integer.toString(Integer.parseInt( nextFormIdString ) + 1);
		// retain it in the context
		servletContext.setAttribute(NEXT_FORM_ID, formId);
		// get the application from the request
		Application application = rapidRequest.getApplication();
		// return it
		return new UserFormDetails(application.getId(), application.getVersion(), formId, null);
	}

	@Override
	public UserFormDetails getResumeFormDetails(RapidRequest rapidRequest, String formId, String password) throws Exception {
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// get all app page control values from session
		Map<String,Map<String,FormPageControlValues>> userAppPageControlValues = (Map<String, Map<String, FormPageControlValues>>) servletContext.getAttribute(USER_FORM_PAGE_CONTROL_VALUES);
		// check we got something
		if (userAppPageControlValues == null) {
			// nothing so return null
			return null;
		} else {
			// the page controls for specified app
			Map<String,FormPageControlValues> userPageControlValues = userAppPageControlValues.get(formId);
			// null check
			if (userPageControlValues == null) {
				// form not found so fail
				return null;
			} else {
				// get the application from the request
				Application application = rapidRequest.getApplication();
				// it's important we set the max page to avoid a blank summary - and getting sent back to the start
				String maxPageId = null;
				// assume max page so far is 0
				int maxPagePos = 0;
				// loop the saved page controls
				for (String pageId : userPageControlValues.keySet()) {
					// get this pos
					int pagePos = application.getPageOrders().get(pageId);
					// check the order of this page against the max page so far
					if (pagePos > maxPagePos) {
						// remember this page id
						maxPageId = pageId;
						// remember the max position
						maxPagePos = pagePos;
					}
				}
				// form found we're good - make a new details with null password but maxPageId
				return new UserFormDetails(application.getId(), application.getVersion(), formId, null, maxPageId, false, null);
			}
		}
	}

	@Override
	public void setMaxPage(RapidRequest rapidRequest, UserFormDetails formDetails, String pageId) {
		// if we got the details
		if (formDetails != null) formDetails.setMaxPageId(pageId);
	}

	@Override
	public void setFormComplete(RapidRequest rapidRequest, UserFormDetails formDetails) throws Exception {
		// get the userPageComplete values
		Map<String, Boolean>  userFormCompleteValues = getUserFormCompleteValues(rapidRequest);
		// set it
		userFormCompleteValues.put(formDetails.getId(), true);
		// store it
		rapidRequest.getRapidServlet().getServletContext().setAttribute(USER_FORM_COMPLETE_VALUES, userFormCompleteValues);
		// update details
		formDetails.setComplete(true);
	}

	// set a form page variable
	@Override
	public void setFormPageVariableValue(RapidRequest rapidRequest, String formId, 	String name, String value) throws Exception {
		// get the userPageComplete values
		Map<String, String>  userFormPageVariableValues = getUserFormPageVariableValues(rapidRequest, formId);
		// set it
		userFormPageVariableValues.put(name, value);
		// store it
		rapidRequest.getRapidServlet().getServletContext().setAttribute(USER_FORM_PAGE_VARIABLE_VALUES, userFormPageVariableValues);
	}

	// return form page variables
	@Override
	public Map<String, String> getFormPageVariableValues(RapidRequest rapidRequest, String formId) throws Exception {
		// use our reusable function
		return getUserFormPageVariableValues(rapidRequest, formId);
	}

	// uses our user session method to get the form page control values
	@Override
	public FormPageControlValues getFormPageControlValues(RapidRequest rapidRequest, String formId, String pageId) throws Exception	{
		// retrieve
		return getUserFormPageControlValues(rapidRequest, formId).get(pageId);
	}

	// uses our user session method to set the form page control values (for hidden pages pageControlValues will be null)
	@Override
	public void setFormPageControlValues(RapidRequest rapidRequest, String formId, String pageId, FormPageControlValues pageControlValues) throws Exception {
		// store them
		getUserFormPageControlValues(rapidRequest, formId).put(pageId, pageControlValues);
	}

	// uses our user session method to get a control value
	@Override
	public String getFormControlValue(RapidRequest rapidRequest, String formId, String controlId, boolean notHidden) throws Exception {
		// find the last underscore
		int controlIdPos = controlId.substring(0, controlId.length() - 1).lastIndexOf("_");
		// check we have enough to include the page
		if (controlIdPos > 1) {
			// get the page id from the first part of the id
			String pageId = controlId.substring(0, controlIdPos);
			// get all user form page values
			Map<String,FormPageControlValues> userFormPageControlValues = getUserFormPageControlValues(rapidRequest, formId);
			// if there are control values stored
			if (userFormPageControlValues.size() > 0) {
				// look for values from our page
				FormPageControlValues pageControlValues = userFormPageControlValues.get(pageId);
				// if we have some
				if (pageControlValues != null) {
					// loop them
					for (FormControlValue controlValue : pageControlValues) {
						// look for an id match, but not if hidden and not hidden is true
						if (controlValue.getId().equals(controlId) && !(controlValue.getHidden() && notHidden)) return controlValue.getValue();
					}
				}
			} // page has values
		} // parts > 1
		return null;
	}

	// called by the form action when saving the form
	@Override
	public synchronized boolean saveForm(RapidRequest rapidRequest, String email, String password) throws Exception {

		// get email settings
		Email emailSettings = Email.getEmailSettings();

		// if we have some
		if (emailSettings != null) {

			// get the form details
			UserFormDetails formDetails = getUserFormDetails(rapidRequest);

			// get the servlet context
			ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
			// get the map of form values
			Map<String, String> userFormSavePasswords = (Map<String, String>) servletContext.getAttribute(USER_FORM_SAVE_PASSWORDS);
			// if there aren't any yet make some
			if  (userFormSavePasswords == null) userFormSavePasswords = new HashMap<String,String>();

			// retain the user save password
			userFormSavePasswords.put(formDetails.getId(), password);
			// store them
			servletContext.setAttribute(USER_FORM_SAVE_PASSWORDS, userFormSavePasswords);

			// get the application
			Application application = rapidRequest.getApplication();

			// get our base url
			String url = Http.getBaseUrl(rapidRequest.getRequest());
			// check url ends with /
			if (!url.endsWith("/")) url += "/";
			// add the rest
			url = url + "~?a=" + application.getId() + "&v=" + application.getVersion() + "&action=resume&id=" + formDetails.getId();

			// get save subject
			String saveSubject = "Rapid form saved";
			// get save body
			String saveBody = "Use this link to resume your form " + url + "\n\n";

			// if we did
			Email.send(Email.getEmailSettings().getUserName(), email, saveSubject, saveBody);

			return true;

		} else {

			return false;

		}

	}

	// called by the form action when resuming forms
	@Override
	public synchronized boolean resumeForm(RapidRequest rapidRequest, String formId, String password) throws Exception {

		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// get the map of form values
		Map<String, String> userFormSavePasswords = (Map<String, String>) servletContext.getAttribute(USER_FORM_SAVE_PASSWORDS);
		// if we got them and a password to check against
		if (userFormSavePasswords != null && password != null) {

			// check the supplied user password against the saved user password
			if (password.equals(userFormSavePasswords.get(formId))) {

				// retrieve the form details into the session - the back-office password is not used in this implementation
				UserFormDetails formDetails = doResumeForm(rapidRequest, formId, null);

				// check we got some and resume accordingly
				if (formDetails != null) return true;

			}

		}

		// wasn't possible to resume
		return false;

	}

	// submit the form - for the RapidFormAdapter nothing special happens, more sophisticated ones will write to databases, webservices, etc
	@Override
	public SubmissionDetails submitForm(RapidRequest rapidRequest) throws Exception {
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// get all submitted details
		Map<String,SubmissionDetails> submissionDetails = (Map<String, SubmissionDetails>) servletContext.getAttribute(USER_FORM_SUBMIT_DETAILS);
		// if null
		if (submissionDetails == null) {
			// instantiate
			submissionDetails = new HashMap<String, SubmissionDetails>();
			// add to session
			servletContext.setAttribute(USER_FORM_SUBMIT_DETAILS, submissionDetails);
		}
		// simple submission detail
		SubmissionDetails submissionDetail = new SubmissionDetails("Form submitted", "Submitted on " + rapidRequest.getRapidServlet().getLocalDateTimeFormatter().format(new Date()));
		// get the form id
		String formId = getFormId(rapidRequest);
		// add to collection
		submissionDetails.put(formId, submissionDetail);
		// return
		return submissionDetail;
	}

	@Override
	public String getFormSubmittedDate(RapidRequest rapidRequest, String formId) throws Exception {
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// get all submitted details
		Map<String,SubmissionDetails> submissionDetails = (Map<String, SubmissionDetails>) servletContext.getAttribute(USER_FORM_SUBMIT_DETAILS);
		// if null
		if (submissionDetails != null) {
			// get the form submission details
			SubmissionDetails submissionDetail = submissionDetails.get(formId);
			// if we got some
			if (submissionDetail != null) return submissionDetail.getDateTime();
		}
		// didn't find anything
		return null;
	}

	// nothing to do here
	@Override
	public void close() {}

}
