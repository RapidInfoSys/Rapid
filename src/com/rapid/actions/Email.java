/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.actions;

import java.util.ArrayList;
import java.util.List;

import javax.activation.FileDataSource;
import javax.servlet.ServletContext;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Email.Attachment;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;


public class Email extends Action {
	
	// this action has generic outputs
	@XmlType(namespace="http://rapid-is.co.uk/email")
	public static class Output {

		private String _outputField, _itemId, _field;

		public String getOutputField() { return _outputField; }
		public void setOutputField(String outputField) { _outputField = outputField; }

		public String getItemId() { return _itemId; }
		public void setItemId(String itemId) { _itemId = itemId; }

		public String getField() { return _field; }
		public void setField(String field) { _field = field; }

		// Constructors
		public Output() {};

		public Output(String outputField, String itemId, String field) {
			_outputField = outputField;
			_itemId = itemId;
			_field = field;
		}

	}

	private List<Action> _successActions, _errorActions, _childActions;
	private List<Output> _outputs;
	
	public List<Output> getOutputs() { return _outputs; }
	public void setOutputs(List<Output> outputs) { _outputs = outputs; }
	
	// properties
	public List<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(List<Action> successActions) { _successActions = successActions; }

	public List<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(List<Action> errorActions) { _errorActions = errorActions; }

	// parameterless constructor (required for jaxb)
	public Email() { super(); }
	// designer constructor
	public Email(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		super(rapidServlet, jsonAction);
		
		/*OUTPUTS*/
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for outputs, successActions and errorActions
			if (!"outputs".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}
		
		// grab any outputs
		JSONArray jsonOutputs = jsonAction.optJSONArray("outputs");
		// if we got some
		if (jsonOutputs != null) {
			// instantiate our array
			_outputs = new ArrayList<Output>();
			// loop them
			for (int i = 0; i < jsonOutputs.length(); i++) {
				// get the input
				JSONObject jsonOutput = jsonOutputs.getJSONObject(i);
				// add it
				_outputs.add(new Output(jsonOutput.optString("outputField"), jsonOutput.optString("itemId"), jsonOutput.optString("field")));
			}

		}

		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for query
			if (!"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some instantiate our collection
		if (jsonSuccessActions != null) _successActions = Control.getActions(rapidServlet, jsonSuccessActions);

		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some instantiate our collection
		if (jsonErrorActions != null) _errorActions = Control.getActions(rapidServlet, jsonErrorActions);
	}

	// protected instance methods

	// produced any js required for additional data from the client
	protected String getAdditionalDataJS(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		return "";
	}

	// overrides
	@Override
	public List<Action> getChildActions() {
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<Action>();
			// add child success actions
			if (_successActions != null)
				_childActions.addAll(_successActions);
			// add child error actions
			if (_errorActions != null) {
				_childActions.addAll(_errorActions);
			}
		}
		return _childActions;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();

		// start with empty JavaScript
        String js = "";

		// control can be null when the action is called from the page load
        String controlParam = "";
        if (control != null) controlParam = "&c=" + control.getId();

        // get the from control id
        String fromControlId = getProperty("from");
        // get the from field
        String fromField = getProperty("fromField");

        // get the to control id
        String toControlId = getProperty("to");
        // get the to field
        String toField = getProperty("toField");

        //Get the attachment controls
        String attachmentsString = getProperty("attachments");

        // assume empty data
        js += "var data = {};\n";

        // get the get data js call for the to and to field
        String getFromJs = Control.getDataJavaScript(servletContext, application, page, fromControlId, fromField);

        // get the get data js call for the to and to field
        String getToJs = Control.getDataJavaScript(servletContext, application, page, toControlId, toField);

        // check we got some
        if (getToJs == null) {

        	js = "// email to control can't be found\n";

        } else {

        	// add the from address if there was one
	        if (getFromJs != null && getFromJs.length() > 0) js += "data.from = " + getFromJs + ";\n";

	        // add the to address if there was one
	        if (getToJs != null && getFromJs.length() > 0) js += "data.to = " + getToJs + ";\n";

	        // get the contents as a string
	        String stringContent = getProperty("content");
	        // if we got one
	        if (stringContent != null) {
	        	// get it into json
	        	JSONObject jsonContent = new JSONObject(stringContent);
	        	// get the inputs
	        	JSONArray jsonInputs = jsonContent.optJSONArray("inputs");
	        	// if we got some
	        	if (jsonInputs != null) {
	        		// add to the data object
	        		js += "data.inputs = [];\n";
	        		// now loop
	        		for (int i = 0; i < jsonInputs.length(); i++) {
	        			// get the input
	        			JSONObject jsonInput = jsonInputs.getJSONObject(i);
	        			// get value and add to arrray
	        			js += "data.inputs.push(" + Control.getDataJavaScript(servletContext, application, page, jsonInput.getString("itemId"), jsonInput.optString("field")) + ");\n";
	        		}
	        	}
	        }

	        // add any js for additional data
	        js += getAdditionalDataJS(rapidRequest, application, page, control, jsonDetails);
	        
	        //Lastly, check for upload controls
	        //Check if an attachment control is specified
	        if(attachmentsString != null){
	        	//Convert the string to json
	        	JSONArray jsonAttachments = new JSONArray(attachmentsString);
	        	js += "data.attachments = [];\n";
	        	//loop through the upload control ids
	        	for(int i = 0; i < jsonAttachments.length(); i++){
	        		JSONObject jsonControl = jsonAttachments.getJSONObject(i);
	        		
	        		String controlId = jsonControl.getString("itemId");
	        		String controlField = jsonControl.getString("field");
	        		
	        		String getAttachmentsJs = Control.getDataJavaScript(servletContext, application, page, controlId, controlField);

	        		if(getAttachmentsJs != null && getAttachmentsJs.length() > 0){
	        			js += "data.attachments.push(" + getAttachmentsJs + ");\n";
	        		}
	        		
	        	}
	        }

	        // instantiate the jsonDetails if required
	     	if (jsonDetails == null) jsonDetails = new JSONObject();

			// open the ajax call
	        js += "$.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', contentType: 'application/json', dataType: 'json',\n";
	        js += "  data: JSON.stringify(data),\n";
	        js += "  error: function(server, status, message) {\n";

	        // this avoids doing the errors if the page is unloading or the back button was pressed
 			js += "    if (server.readyState > 0) {\n";

 			// retain if error actions
 			boolean errorActions = false;

 			// prepare a default error hander we'll show if no error actions, or pass to child actions for them to use
 			String defaultErrorHandler = "alert('Error with email action : ' + server.responseText||message);";

 			// add any error actions
 			if (_errorActions != null) {
 				// count the actions
 				int i = 0;
 				// loop the actions
 				for (Action action : _errorActions) {
 					// retain that we have custom error actions
 					errorActions = true;
 					// if this is the last error action add in the default error handler
 					if (i == _errorActions.size() - 1) jsonDetails.put("defaultErrorHandler", defaultErrorHandler);
 					// add the js
 					js += "       " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n       ") + "\n";
 					// if this is the last error action and the default error handler is still present, remove it so it isn't sent down the success path
 					if (i == _errorActions.size() - 1 && jsonDetails.optString("defaultErrorHandler", null) != null) jsonDetails.remove("defaultErrorHandler");
 					// increase the count
 					i++;
 				}
 			}
 			// add default error handler if none in collection
 			if (!errorActions) js += "        " + defaultErrorHandler + "\n";

 			// close unloading check
 			js += "    }\n";

 			// close error actions
 			js += "  },\n";

	        js += "  success: function(data) {\n";


			// add any error actions
			if (_successActions != null) {
				for (Action action : _successActions) {
					js += "    " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
				}
			}

	        js += "  }\n";
	        js += "});\n";
        }

		return js;
	}

	@Override
    public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonData) throws Exception {

		// get the from address
		String from = jsonData.getString("from");
		// get the to address
		String to = jsonData.getString("to");
		// get the content as a string
        String stringContent = getProperty("content");
		// get the type
		String type = getProperty("emailType");
		// get the attachments
		String attachments = jsonData.optString("attachments", null);

        // if we got one
        if (from == null) {
        	throw new Exception("Email from address must be provided");
        } else if (to == null) {
        	throw new Exception("Email to address must be provided");
        } else if (stringContent == null) {
        	throw new Exception("Email content must be provided");
        } else {
        	// get it into json
        	JSONObject jsonContent = new JSONObject(stringContent);
        	// get the subject template
        	String subject = jsonContent.optString("subject");
        	// get the body template
        	String body = jsonContent.optString("body");
        	// if we got one
        	if (subject == null) {
        		throw new Exception("Email subject must be provided");
        	} else if (body == null) {
        		throw new Exception("Email body must be provided");
        	} else {

        		// update the subject template with any parameters
				if (subject.contains("[[")) subject = rapidRequest.getApplication().insertParameters(rapidRequest.getRapidServlet().getServletContext(), subject);
        		// update the body template with any parameters
				if (body.contains("[[")) body = rapidRequest.getApplication().insertParameters(rapidRequest.getRapidServlet().getServletContext(), body);

				// the index in the input values
				int i = 0;

        		// get any inputs
        		JSONArray jsonInputs = jsonData.optJSONArray("inputs");
        		// check we got inputs
        		if (jsonInputs != null) {
        			// check any inputs to look for
        			if (jsonInputs.length() > 0) {

        				// split the subject part
                		String[] subjectParts = subject.split("\\?");
                		// if there is more than 1 part
                		if (subjectParts.length > 1) {
                			// set subject to first part
                			subject = subjectParts[0];
                			// loop the remaining parts
                			for (int j = 1; j < subjectParts.length; j++) {
                				// if there is an escape character or not more inputs
                				if (subject.endsWith("\\") || i >= jsonInputs.length()) {
                					// trim the \
                					subject = subject.substring(0, body.length() - 1);
                					// add back the ?
                					subject += "?";
                				} else {
                					// add the input value
                					subject += jsonInputs.getString(i);
                					// increment for next value
                					i ++;
                				}
                				// add this part
                				subject += subjectParts[j];
                			} // loop subject parts
                		} // got subject parts
                		// if we need an input at the end
            			if (jsonContent.getString("subject").endsWith("?")) {
            				// if we have some left
            				if (i < jsonInputs.length()) {
            					// remove last ? if still there
                				if (subjectParts.length == 1) subject = subject.substring(0, subject.length() - 1);
                				// add input value
                				subject += jsonInputs.getString(i);
                				// increment
                				i ++;
            				} else {
            					// add back ? if need be
            					if (subjectParts.length > 1) subject += "?";
            				} // got inputs
            			} // subject ends in ?

                		// split the body parts
                		String[] bodyParts = body.split("\\?");
                		// if there is more than 1 part
                		if (bodyParts.length > 1) {
                			// set body to first part
                			body = bodyParts[0];
                			// loop the remaining parts
                			for (int j = 1; j < bodyParts.length; j++) {
                				// if there is an escape character or not more inputs
                				if (body.endsWith("\\") || i >= jsonInputs.length()) {
                					// trim the \
                					body = body.substring(0, body.length() - 1);
                					// add back the ?
                					body += "?";
                				} else {
                					// add the input value
                					body += jsonInputs.getString(i);
                					// increment for next value
                					i ++;
                				}
                				// add this part
                				body += bodyParts[j];
                			} // loop body parts
                		} // got body parts
                		// if we need an input at the end
            			if (jsonContent.getString("body").endsWith("?")) {
            				// if we have inputs some left
            				if (i < jsonInputs.length()) {
            					// remove last ? if still there
                				if (bodyParts.length == 1) body = body.substring(0, body.length() - 1);
                				// add input value
                				body += jsonInputs.getString(i);
                				// increment
                				i ++;
            				} else {
            					// add back ? if need be
            					if (bodyParts.length > 1) body += "?";
            				} // got inputs
            			} // body ends with ?

        			} // got inputs
        		} // inputs not null

        		// if the type is html
        		if ("html".equals(type)) {
        			// send email as html
        			com.rapid.core.Email.send(from, to, subject, "Please view this email with an application that supports HTML", body, getAttachments(rapidRequest, attachments));
        		} else {
        			// send email as text
        			com.rapid.core.Email.send(from, to, subject, body, null, getAttachments(rapidRequest, attachments));
        		}
        	}
        }

		// return an empty json object
		return new JSONObject();
	}
	
	// produces any attachments
	protected Attachment[] getAttachments(RapidRequest rapidRequest, String attachmentFiles) throws Exception {

		if(attachmentFiles == null) return null;
		
		// Decide on the base path
		String basePath = "uploads/" +  rapidRequest.getAppId();
		// servers with public access must use the secure upload location
		if (rapidRequest.getRapidServlet().isPublic()) basePath = "WEB-INF/" + basePath;
		
		JSONArray attachedFiles = new JSONArray(attachmentFiles.replace("\"", ""));
		//String[] attachedFiles = .split(",");
		int size = attachedFiles.length();
		Attachment[] attachments = new Attachment[size];
		//loop through the attachedFile strings and accumulate the attachment objects in an array
		for(int i = 0; i < size; i++){
			String file = attachedFiles.optString(i, null);
			
			if (file != null) {
				String filePath = rapidRequest.getRapidServlet().getServletContext().getRealPath(basePath + "/" + file);
				attachments[i] = new Attachment(file, new FileDataSource(filePath));
			}
			
		}
		
		return attachments;
		
	}

}
