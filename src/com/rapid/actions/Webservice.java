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

package com.rapid.actions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Parameter;
import com.rapid.server.ActionCache;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.soa.SOAData;
import com.rapid.soa.SOADataWriter;
import com.rapid.soa.SOADataReader.SOAXMLReader;
import com.rapid.soa.SOADataWriter.SOARapidWriter;

public class Webservice extends Action {
	
	// details of the request (inputs, sql, outputs)
	public static class Request {
		
		private String _type, _url, _action, _body;
		private ArrayList<Parameter> _inputs, _outputs;				
				
		public ArrayList<Parameter> getInputs() { return _inputs; }
		public void setInputs(ArrayList<Parameter> inputs) { _inputs = inputs; }
		
		public String getType() { return _type; }
		public void setType(String type) { _type = type; }
		
		public String getUrl() { return _url; }
		public void setUrl(String url) { _url = url; }
		
		public String getAction() { return _action; }
		public void setAction(String action) { _action = action; }
		
		public String getBody() { return _body; }
		public void setBody(String body) { _body = body; }
		
		public ArrayList<Parameter> getOutputs() { return _outputs; }
		public void setOutputs(ArrayList<Parameter> outputs) { _outputs = outputs; }
		
		public Request() {};
		public Request(ArrayList<Parameter> inputs, String type, String url, String action, String body, ArrayList<Parameter> outputs) {
			_inputs = inputs;
			_type = type;
			_url = url;
			_action = action;
			_body = body;
			_outputs = outputs;
		}
				
	}
	
	// static variables
	private static Logger _logger = Logger.getLogger(Webservice.class);
	
	// instance variables
	
	private Request _request;
	private boolean _showLoading;
	private ArrayList<Action> _successActions, _errorActions, _childActions;
	
	// properties
	
	public Request getRequest() { return _request; }
	public void setRequest(Request request) { _request = request; }
	
	public boolean getShowLoading() { return _showLoading; }
	public void setShowLoading(boolean showLoading) { _showLoading = showLoading; }
	
	public ArrayList<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(ArrayList<Action> successActions) { _successActions = successActions; }
	
	public ArrayList<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(ArrayList<Action> errorActions) { _errorActions = errorActions; }
		
	// constructors
	
	// jaxb
	public Webservice() { 
		super(); 
	}
	// designer
	public Webservice(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		// set the xml version
		super();
		
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for query
			if (!"request".equals(key) && !"showLoading".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		} 
		
		// try and build the query object
		JSONObject jsonQuery = jsonAction.optJSONObject("request");
		
		// check we got one
		if (jsonQuery != null) {
			// get the parameters						
			ArrayList<Parameter> inputs = getParameters(jsonQuery.optJSONArray("inputs"));
			String type = jsonQuery.optString("type");
			String url = jsonQuery.optString("url");
			String action = jsonQuery.optString("action");
			String body = jsonQuery.optString("body");
			ArrayList<Parameter> outputs = getParameters(jsonQuery.optJSONArray("outputs"));
			// make the object
			_request = new Request(inputs, type, url, action, body, outputs);
		}
		
		// look for showLoading
		_showLoading = jsonAction.optBoolean("showLoading");
		
		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some
		if (jsonSuccessActions != null) {
			// instantiate our success actions collection
			_successActions = Control.getActions(rapidServlet, jsonSuccessActions);
		}
		
		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some
		if (jsonErrorActions != null) {
			// instantiate our error actions collection
			_errorActions = Control.getActions(rapidServlet, jsonErrorActions);
		}
				
	}
	
	// this is used to get both input and output parameters
	private ArrayList<Parameter> getParameters(JSONArray jsonParameters) throws Exception {
		// prepare return
		ArrayList<Parameter> parameters = null;
		// check
		if (jsonParameters != null) {
			// instantiate collection
			parameters = new ArrayList<Parameter>();
			// loop
			for (int i = 0; i < jsonParameters.length(); i++) {
				// instaniate member
				Parameter parameter = new Parameter(
					jsonParameters.getJSONObject(i).optString("itemId"),
					jsonParameters.getJSONObject(i).optString("field")
				);
				// add member
				parameters.add(parameter);
			}
		}
		// return
		return parameters;
	}
		
	// overrides
	
	@Override
	public List<Action> getChildActions() {			
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<Action>();
			// add child success actions
			if (_successActions != null) {
				for (Action action : _successActions) _childActions.add(action);			
			}
			// add child error actions
			if (_errorActions != null) {
				for (Action action : _errorActions) _childActions.add(action);			
			}
		}
		return _childActions;	
	}
	
	public String getLoadingJS(Page page, List<Parameter> parameters, boolean show) {
		String js = "";
		// check there are parameters
		if (parameters != null) {			
			// loop the output parameters
			for (int i = 0; i < parameters.size(); i++) {					
				// get the parameter
				Parameter output = parameters.get(i);
				// get the control the data is going into
				Control control = page.getControl(output.getItemId());
				// check the control still exists
				if (control != null) {
					if ("grid".equals(control.getType())) {
						if (show) {
							js += "  $('#" + control.getId() + "').showLoading();\n";						
						} else {
							js += "  $('#" + control.getId() + "').hideLoading();\n";
						}
					}
				}
			}
			
		}
		return js;
	}
	
	@Override
	public String getJavaScript(Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		
		String js = "";
		
		if (_request != null) {
			
			// get the most recent sequence number for this action to stop slow-running early requests overwriting the results of fast later requests
			js += "  var sequence = getWebserviceActionSequence('" + getId() + "');\n";
			
			// drop in the query variable which holds our inputs and sequence
			js += "  var query = { inputs:[], sequence:sequence };\n";
						
			// build the inputs
			if (_request.getInputs() != null) {
				for (Parameter parameter : _request.getInputs()) {
					String itemId = parameter.getItemId();
					if (itemId != null) {
						// get any parameter field
						String field = parameter.getField();
						// check if there was one
						if (field == null) {
							// no field
							js += "  query.inputs.push({id:'" + itemId + "',value:" + Control.getDataJavaScript(application, page, itemId, null) + "});\n";
						} else {
							// got field so let in appear in the inputs for matching later
							js += "  query.inputs.push({id:'" + itemId + "',value:" + Control.getDataJavaScript(application, page, itemId, field) + ",field:'" + field + "'});\n";
						}
					}
				}
			} // got inputs
			
			// control can be null when the action is called from the page load
			String controlParam = "";
			if (control != null) controlParam = "&c=" + control.getId();
			
			// get the outputs
			ArrayList<Parameter> outputs = _request.getOutputs();
			
			// get the js to show the loading (if applicable)
			if (_showLoading) js += "  " + getLoadingJS(page, outputs, true);
			
			// stringify the query
			js += "  query = JSON.stringify(query);\n";
									
			// open the ajax call
			js += "  $.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', dataType: 'json',\n";
			js += "    data: query,\n";
			js += "    error: function(error, status, message) {\n";
			
			// hide the loading javascript (if applicable)
			if (_showLoading) js += "      " + getLoadingJS(page, outputs, false);
							
			// this avoids doing the errors if the page is unloading or the back button was pressed
			js += "      if (server.readyState > 0) {\n";
			
			// retain if error actions
			boolean errorActions = false;
			
			// prepare a default error hander we'll show if no error actions, or pass to child actions for them to use
			String defaultErrorHandler = "alert('Error with database action : ' + server.responseText||message);";
			
			// add any error actions
			if (_errorActions != null) {
				// instantiate the jsonDetails if required
				if (jsonDetails == null) jsonDetails = new JSONObject();
				// count the actions
				int i = 0;
				// loop the actions
				for (Action action : _errorActions) {
					// retain that we have custom error actions
					errorActions = true;
					// if this is the last error action add in the default error handler
					if (i == _errorActions.size() - 1) jsonDetails.put("defaultErrorHandler", defaultErrorHandler);						
					// add the js
					js += "         " + action.getJavaScript(application, page, control, jsonDetails).trim().replace("\n", "\n         ") + "\n";					
					// increase the count
					i++;
				}
			}
			// add default error handler if none in collection
			if (!errorActions) js += "        " + defaultErrorHandler + "\n";
						
			// close unloading check
			js += "      }\n";
			
			// close error actions
			js += "    },\n";
			
			// open success function
			js += "    success: function(data) {\n";	
			// get the js to hide the loading (if applicable)
			if (_showLoading) js += "      " + getLoadingJS(page, outputs, false);
			// open if data check
			js += "      if (data) {\n";
									
			// check there are outputs
			if (outputs != null) {
				// the outputs array we're going to make
				String jsOutputs = "";				
				// loop the output parameters
				for (int i = 0; i < outputs.size(); i++) {
					
					// get the parameter
					Parameter output = outputs.get(i);
					// get the control the data is going into
					Control outputControl = page.getControl(output.getItemId());
					// check the control is still on the page
					if (outputControl != null) {
						// get any mappings we may have
						String details = outputControl.getDetailsJavaScript(application, page);
						// set to empty string or clean up
						if (details == null) {
							details = "";
						} else {
							details = ", details: " + details;
						}
						// append the javascript outputs
						jsOutputs += "{id: '" + outputControl.getId() + "', type: '" + outputControl.getType() + "', field: '" + output.getField() + "'" + details + "}";
						// add a comma if not the last
						if (i < outputs.size() - 1) jsOutputs += ","; 
					}
										
				}			
				js += "       var outputs = [" + jsOutputs + "];\n";
				// send them them and the data to the database action				
				js += "       Action_webservice('" + getId() + "', data, outputs);\n";
				// add any sucess actions
				if (_successActions != null) {
					for (Action action : _successActions) {
						js += "       " + action.getJavaScript(application, page, control, jsonDetails).trim().replace("\n", "\n       ") + "\n";
					}
				}
			}
			
			// close if data check
			js += "      }\n";						
			// close success function
			js += "    }\n";
			
			// close ajax call
			js += "  });";
		}
				
		// return what we built			
		return js;
	}
	
	@Override
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonAction) throws Exception {
		
		_logger.trace("Webservice action : " + jsonAction);
		
		// get the application
		Application application = rapidRequest.getApplication();
		
		// get the page
		Page page = rapidRequest.getPage();
		
		// get the webservice action call sequence
		int sequence = jsonAction.optInt("sequence",1);
				
		// placeholder for the object we're about to return
		JSONObject jsonData = null;
			
		// only proceed if there is a request and application and page
		if (_request != null && application != null && page != null) {
			
			// get any json inputs 
			JSONArray jsonInputs = jsonAction.optJSONArray("inputs");
			
			// placeholder for the action cache
			ActionCache actionCache = rapidRequest.getRapidServlet().getActionCache();
				
			// if an action cache was found
			if (actionCache != null) {
				
				// log that we found action cache
				_logger.debug("Webservice action cache found");
				
				// attempt to fetch data from the cache
				jsonData = actionCache.get(application.getId(), getId(), jsonInputs.toString());
				
			}
			
			// if there is either no cache or we got no data
			if (jsonData == null) {
				
				// get the body into a string
				String body = _request.getBody();
				// retain the current position
				int pos = body.indexOf("?");
				// keep track of the index of the ?
				int index = 0;
				// if there are any question marks
				if (pos > 0) {				
					// loop, but check condition at the end
					do {
						// get the input
						JSONObject input = jsonInputs.getJSONObject(index);
						// replace the ? with the input value
						body = body.substring(0, pos) + input.getString("value") + body.substring(pos + 1);
						// look for the next question mark
						pos = body.indexOf("?",pos + 1);
						// inc the index for the next round
						index ++;
						// stop looping if no more ?
					} while (pos > 0);
				}
				
				// create a placeholder for the request url
				URL url = null;				
				// if the given request url starts with http use it as is, otherwise use the soa servlet
				if (_request.getUrl().startsWith("http")) {
					url = new URL(_request.getUrl());
				} else {
					HttpServletRequest httpRequest = rapidRequest.getRequest();
					url = new URL(httpRequest.getScheme(), httpRequest.getServerName(), httpRequest.getServerPort(), httpRequest.getContextPath() + "/" + _request.getUrl());
				}
				
				// retrieve the action
				String action = _request.getAction();
				// check whether we have any id / version seperators
				String[] actionParts = action.split("/");
				// add them if none
				if (actionParts.length < 2) action = application.getId() + "/" + application.getVersion() +  "/" + action;
				
				// establish the connection
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoOutput(true); // Triggers POST.
				
				// set the content type and action header accordingly
				if ("SOAP".equals(_request.getType())) {
					connection.setRequestProperty("Content-Type", "text/xml");
					connection.setRequestProperty("SOAPAction", action);
				} else if ("JSON".equals(_request.getType())) {
					connection.setRequestProperty("Content-Type", "application/json");
					connection.setRequestProperty("Action", action);
				} else if ("XML".equals(_request.getType())) {
					connection.setRequestProperty("Content-Type", "text/xml");
					connection.setRequestProperty("Action", action);
				}
								
				// get the output stream from the connection into which we write the request
				OutputStream output = connection.getOutputStream();		
				
				// write the processed body string into the request output stream
				output.write(body.getBytes("UTF8"));
				
				// check the response code
				int responseCode = connection.getResponseCode();
				
				// read input stream if all ok, otherwise something meaningful should be in error stream
				if (responseCode == 200) {
					
					InputStream response = connection.getInputStream();
					
					SOAXMLReader xmlReader = new SOAXMLReader();
					
					SOAData soaData = xmlReader.read(response);
					
					SOADataWriter jsonWriter = new SOARapidWriter(soaData);
					
					String jsonString = jsonWriter.write();
					
					jsonData = new JSONObject(jsonString);
					
					if (actionCache != null) actionCache.put(application.getId(), getId(), jsonInputs.toString(), jsonData);
					
					response.close();
					
				} else {
					
					InputStream response = connection.getErrorStream();
					
					BufferedReader rd  = new BufferedReader( new InputStreamReader(response));
					
					String errorMessage = rd.readLine();
					
					rd.close();
					
					// log the error
					_logger.error(errorMessage);
					
					// only if there is no application cache show the error, otherwise it sends an empty response
					if (actionCache == null) {
														        
						throw new JSONException(" response code " + responseCode + " from server : " + errorMessage);
						
					} else {
						
						_logger.debug("Error not shown to user due to cache : " + errorMessage);
						
					}
																		
				}
				
				connection.disconnect();
				
			} // jsonData == null
																							
		} // got app and page
							
		// if the jsonData is still null make an empty one
		if (jsonData == null) jsonData = new JSONObject();
		
		// add the sequence
		jsonData.put("sequence", sequence);
		
		// return the object
		return jsonData;
		
	}
	
}
