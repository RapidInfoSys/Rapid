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
import com.rapid.server.RapidRequest;

public class RapidFormAdapter extends FormAdapter {

	//  static finals
	private static final String NEXT_FORM_ID = "nextFormId";
	private static final String USER_FORM_PAGE_VARIABLE_VALUES = "userFormPageVariableValues";
	private static final String USER_FORM_PAGE_CONTROL_VALUES = "userFormPageControlValues";
	private static final String USER_FORM_COMPLETE_VALUES = "userFormCompleteValues";
	private static final String USER_FORM_SUBMIT_DETAILS = "userFormSubmitDetails";

	// constructor

	public RapidFormAdapter(ServletContext servletContext, Application application, String type) {
		super(servletContext, application, type);
	}

	// class methods

	// the RapidFormAdapter holds all values in the user session so this method just gets them from there
	protected Map<String,FormPageControlValues> getUserFormPageControlValues(RapidRequest rapidRequest) throws Exception {
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
		// get the form id
		String formId = getFormId(rapidRequest);
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
				// form found we're good
				return new UserFormDetails(application.getId(), application.getVersion(), formId, null, null, false, null);
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
	public Map<String, String> getFormPageVariableValues( 	RapidRequest rapidRequest, String formId) throws Exception {
		// use our reusable function
		return getUserFormPageVariableValues(rapidRequest, formId);
	}

	// uses our user session method to get the form page control values
	@Override
	public FormPageControlValues getFormPageControlValues(RapidRequest rapidRequest, String formId, String pageId) throws Exception	{
		// retrieve
		return getUserFormPageControlValues(rapidRequest).get(pageId);
	}

	// uses our user session method to set the form page control values (for hidden pages pageControlValues will be null)
	@Override
	public void setFormPageControlValues(RapidRequest rapidRequest, String formId, String pageId, FormPageControlValues pageControlValues) throws Exception {
		// store them
		getUserFormPageControlValues(rapidRequest).put(pageId, pageControlValues);
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
			Map<String,FormPageControlValues> userFormPageControlValues = getUserFormPageControlValues(rapidRequest);
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
