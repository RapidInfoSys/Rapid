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

package com.rapid.actions;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.rapid.soa.SOADataReader.SOAJSONReader;
import com.rapid.soa.SOADataReader.SOAXMLReader;
import com.rapid.soa.SOADataWriter;
import com.rapid.soa.SOADataWriter.SOARapidWriter;
import com.rapid.soa.SOAElement;
import com.rapid.utils.Strings;
import com.rapid.utils.XML;

public class Webservice extends Action {

	// details of the request (inputs, sql, outputs)
	public static class Request {

		private String _type, _url, _headers, _action, _body, _transform, _root;
		private ArrayList<Parameter> _inputs, _outputs;

		public ArrayList<Parameter> getInputs() { return _inputs; }
		public void setInputs(ArrayList<Parameter> inputs) { _inputs = inputs; }

		public String getType() { return _type; }
		public void setType(String type) { _type = type; }

		public String getUrl() { return _url; }
		public void setUrl(String url) { _url = url; }

		public String getAction() { return _action; }
		public void setAction(String action) { _action = action; }

		public String getHeaders() { return _headers; }
		public void setHeaders(String headers) { _headers = headers; }

		public String getBody() { return _body; }
		public void setBody(String body) { _body = body; }

		public String getTransform() { return _transform; }
		public void setTransform(String transform) { _transform = transform; }

		public String getRoot() { return _root; }
		public void setRoot(String root) { _root = root; }

		public ArrayList<Parameter> getOutputs() { return _outputs; }
		public void setOutputs(ArrayList<Parameter> outputs) { _outputs = outputs; }

		public Request() {};
		public Request(ArrayList<Parameter> inputs, String type, String url, String action, String headers, String body, String transform, String root, ArrayList<Parameter> outputs) {
			_inputs = inputs;
			_type = type;
			_url = url;
			_action = action;
			_headers = headers;
			_body = body;
			_transform = transform;
			_root = root;
			_outputs = outputs;
		}

	}

	// static variables
	private static Logger _logger = LogManager.getLogger(Webservice.class);

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
			if (!"request".equals(key) && !"root".equals(key) && !"showLoading".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// try and build the query object
		JSONObject jsonQuery = jsonAction.optJSONObject("request");

		// check we got one
		if (jsonQuery != null) {
			// get the parameters
			ArrayList<Parameter> inputs = getParameters(jsonQuery.optJSONArray("inputs"));
			String type = jsonQuery.optString("type");
			String url = jsonQuery.optString("url").trim();
			String action = jsonQuery.optString("action").trim();
			String headers = jsonQuery.optString("headers").trim();
			String body = jsonQuery.optString("body");
			String transform = jsonQuery.optString("transform");
			String root = jsonQuery.optString("root").trim();
			ArrayList<Parameter> outputs = getParameters(jsonQuery.optJSONArray("outputs"));
			// make the object
			_request = new Request(inputs, type, url, action, headers, body, transform, root, outputs);
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
			parameters = new ArrayList<>();
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

	// overrides

	@Override
	public List<Action> getChildActions() {
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<>();
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

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		String js = "";

		if (_request != null) {

			// get the rapid servlet
			RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();

			// get the most recent sequence number for this action to stop slow-running early requests overwriting the results of fast later requests
			js += "var sequence = getWebserviceActionSequence('" + getId() + "');\n";

			// drop in the query variable which holds our inputs and sequence
			js += "var query = { inputs:[], sequence:sequence };\n";

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
							js += "  query.inputs.push({id:'" + itemId + "',value:" + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, itemId, null) + "});\n";
						} else {
							// got field so let in appear in the inputs for matching later
							js += "  query.inputs.push({id:'" + itemId + "',value:" + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, itemId, field) + ",field:'" + field + "'});\n";
						}
					}
				}
			} // got inputs

			// control can be null when the action is called from the page load
			String controlParam = "";
			if (control != null) controlParam = "&c=" + control.getId();

			// get the outputs
			ArrayList<Parameter> outputs = _request.getOutputs();

			// instantiate the jsonDetails if required
			if (jsonDetails == null) jsonDetails = new JSONObject();
			// look for a working page in the jsonDetails
			String workingPage = jsonDetails.optString("workingPage", null);
			// look for an offline page in the jsonDetails
			String offlinePage = jsonDetails.optString("offlinePage", null);

			// get the js to show the loading (if applicable)
			if (_showLoading) js += "  " + getLoadingJS(page, outputs, true);

			// stringify the query
			js += "query = JSON.stringify(query);\n";

			// open the ajax call
			js += "$.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', contentType: 'application/json', dataType: 'json',\n";
			js += "  data: query,\n";
			js += "  error: function(server, status, message) {\n";

			// if there is a working page
			if (workingPage != null) {
				// remove any working page dialogue
				js += "    $(" + workingPage + ").hideDialogue(false,'" + workingPage + "');\n";
			}

			// hide the loading javascript (if applicable)
			if (_showLoading) js += "    " + getLoadingJS(page, outputs, false);

			// this avoids doing the errors if the page is unloading or the back button was pressed
			js += "    if (server.readyState > 0) {\n";

			// retain if error actions
			boolean errorActions = false;

			// prepare a default error hander we'll show if no error actions, or pass to child actions for them to use
			String defaultErrorHandler = "alert('Error with webservice action\" + (control == null ? page.getId() : \" for control \" + control.getId()) + \"\\\\n\\\\n' + server.responseText||message);";
			// if we have an offline page
			if (offlinePage != null) {
				// update defaultErrorHandler to navigate to offline page
				defaultErrorHandler = "if (Action_navigate && !(typeof _rapidmobile == 'undefined' ? navigator.onLine : _rapidmobile.isOnline())) {\n        Action_navigate('~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + offlinePage + "&action=dialogue',true,'" + getId() + "');\n      } else {\n         " + defaultErrorHandler + "\n      }";
				// remove the offline page so we don't interfere with actions down the three
				jsonDetails.remove("offlinePage");
			}

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
					js += "         " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n         ") + "\n";
					// if this is the last error action and the default error handler is still present, remove it so it isn't sent down the success path
					if (i == _errorActions.size() - 1 && jsonDetails.optString("defaultErrorHandler", null) != null) jsonDetails.remove("defaultErrorHandler");
					// increase the count
					i++;
				}
			}
			// add default error handler if none in collection
			if (!errorActions) js += "      " + defaultErrorHandler + "\n";

			// close unloading check
			js += "    }\n";

			// close error actions
			js += "  },\n";

			// open success function
			js += "  success: function(data) {\n";

			// get the js to hide the loading (if applicable)
			if (_showLoading) js += "  " + getLoadingJS(page, outputs, false);

			// check there are outputs
			if (outputs != null) {
				// open if data check
				js += "    if (data) {\n";
				// the outputs array we're going to make
				String jsOutputs = "";
				// loop the output parameters
				for (int i = 0; i < outputs.size(); i++) {
					// get the parameter
					Parameter output = outputs.get(i);
					// get the id
					String outputId = output.getItemId();
					// get the id parts
					String[] idParts = outputId.split("\\.");
					// if there is more than 1 part we are dealing with set properties, for now just update the output id
					if (idParts.length > 1) outputId = idParts[0];

					// get the control the data is going into
					Control outputControl = page.getControl(outputId);
					// assume we found it
					boolean pageControl = true;
					// if not found in the page
					if (outputControl == null) {
						// try the application
						outputControl = application.getControl(rapidServlet.getServletContext(), outputId);
						// set page control to false
						pageControl = false;
					}
					// check we got one
					if (outputControl == null) {
						js += "      // output not found for " + outputId + "\n";
					} else {

						// get any details we may have
						String details = outputControl.getDetailsJavaScript(application, page);

						// if there are two parts this is a property
						if (idParts.length > 1) {

							// if we have some details
							if (details != null) {
								// if this is a page control
								if (pageControl) {
									// the details will already be in the page so we can use the short form
									details = outputControl.getId() + "details";
								}
							}

							// get the property from the second id part
							String property = idParts[1];
							// append the set property call
							js += "      setProperty_" + outputControl.getType() +  "_" + property + "(ev,'" + outputControl.getId() + "','" + output.getField() + "'," + details + ",data);\n";

						} else {

							// set to empty string or clean up
							if (details == null) {
								details = "";
							} else {
								details = ", details: " + details;
							}
							// append the javascript outputs
							jsOutputs += "{id: '" + outputControl.getId() + "', type: '" + outputControl.getType() + "', field: '" + output.getField() + "'" + details + "},";
						} // property / control check
					} // control found check
				} // outputs loop
				// if we added to outputs
				if (jsOutputs.length() > 0) {
					// remove the last comma
					jsOutputs = jsOutputs.substring(0, jsOutputs.length() - 1);
					// add the outputs property
					js += "      var outputs = [" + jsOutputs + "];\n";
					// send them them and the data to the database action
					js += "      Action_database(ev,'" + getId() + "', data, outputs);\n";
				} // outputs js length check
				// close if data check
				js += "    }\n";
			} // outputs null check

			// if there is a working page (from the details)
			if (workingPage != null) {
				// remove any working page dialogue
				js += "    $(" + workingPage + ").hideDialogue(false,'" + workingPage + "');\n";
				// remove the working page so as not to affect actions further down the tree
				jsonDetails.remove("workingPage");
			}

			// add any sucess actions
			if (_successActions != null) {
				for (Action action : _successActions) {
					js += "    " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n    ") + "\n";
				}
			}

			// close success function
			js += "  }\n";

			// close ajax call
			js += "});";
		}

		// return what we built
		return js;
	}

	// escape by type
	private String escapeType(String type, String value) {
		// escape " if JSON
		if ("JSON".equals(_request.getType())) value = value.replaceAll("\"", "\\\"");
		// escape XML
		if ("SOAP".equals(_request.getType()) || "XML".equals(_request.getType())) value = XML.escape(value);
		// return
		return value;
	}

	// insert / replace inputs to ? in headers / body
	private String replaceInputs(RapidRequest rapidRequest, JSONArray jsonInputs, int index, String type, String placeHolder, String val) throws JSONException {

		// retain the position of the first ?
		int pos = val.indexOf(placeHolder);

		// if there are any question marks
		if (pos > 0 && jsonInputs.length() > index) {
			// loop, but check condition at the end
			do {
				// get the input
				JSONObject input = jsonInputs.getJSONObject(index);
				// get the input id
				String id = input.getString("id");
				// get the input field
				String field = input.optString("field");
				// add field to id if present
				if (field != null && !"".equals(field)) id += "." + field;
				// retain the value
				String value = null;
				// if it looks like a control, or a system value (bit of extra safety checking)
				if (id.indexOf("_C") > 0 || id.indexOf("System.") == 0) {
					// device is a special case
					if (id.equals("System.device")) {
						// get the device from the request
						value = rapidRequest.getDevice();
					} else {
						// get the value from the json inputs
						value = escapeType(type, input.optString("value"));
					}
				} else {
					// didn't look like a control so check page variables
					if (rapidRequest.getPage() != null) {
						// check for page variables
						if (rapidRequest.getPage().getSessionVariables() != null) {
							// loop them
							for (String variable : rapidRequest.getPage().getSessionVariables()) {
								// if this is the variable
								if (variable.equalsIgnoreCase(id)) {
									// get the value from the inputs
									value = escapeType(type, input.optString("value"));
									// no need to keep looking in the page variables
									break;
								}
							}
						}
					}
				}
				// if still null try the session
				if (value == null) value = (String) rapidRequest.getSessionAttribute(id);
				// replace the ? with the input value
				val = val.substring(0, pos) + value + val.substring(pos + placeHolder.length());
				// look for the next question mark
				pos = val.indexOf(placeHolder,pos + placeHolder.length());
				// inc the index for the next round
				index ++;
				// stop looping if no more ?
			} while (pos > 0);
		}

		// return the replaced value
		return val;
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

				// create a placeholder for the request url
				URL url = null;
				// get the request url
				String requestURL  = _request.getUrl();
				// get the headers into a string
				String headers = _request.getHeaders();
				if (headers == null) headers = "";
				// get the body into a string
				String body = _request.getBody().trim();
				// remove prolog if present
				if (body.indexOf("\"?>") > 0) body = body.substring(body.indexOf("\"?>") + 3).trim();

				// merge in any application parameters
				body = application.insertParameters(rapidRequest.getRapidServlet().getServletContext(), body);

				// get the number of escaped ? parameters in header
				int pUrlCount = Strings.occurrences(requestURL, "[[?]]");
				// get the number of ? parameters in header
				int pHeaderCount = Strings.occurrences(headers, "?");
				// check number of parameters in headers and body
				int pBodyCount = Strings.occurrences(body, "?");
				// throw error if incorrect
				if (pUrlCount + pHeaderCount + pBodyCount != jsonInputs.length()) throw new Exception("Request has " + (pUrlCount + pHeaderCount + pBodyCount) + " parameter" + (pHeaderCount + pBodyCount > 1 ? "s" : "") + ", " + jsonInputs.length() + " provided");

				// replace inputs in url
				requestURL = replaceInputs(rapidRequest, jsonInputs, 0, _request.getType(), "[[?]]", requestURL);
				// replace inputs in headers
				headers = replaceInputs(rapidRequest, jsonInputs, pUrlCount, _request.getType(), "?", headers);
				// replace inputs in body using index from headers
				body = replaceInputs(rapidRequest, jsonInputs, pUrlCount + pHeaderCount, _request.getType(), "?", body);

				// retrieve the action
				String action = _request.getAction();

				// if we got one
				if (requestURL != null) {
					// trim it
					requestURL = requestURL.trim();
					// insert any parameters
					requestURL = application.insertParameters(rapidRequest.getRapidServlet().getServletContext(), requestURL);
					// if the given request url starts with http, or ~
					if (_request.getUrl().toLowerCase().startsWith("http")) {
						// use the url as is
						url = new URL(requestURL);
					} else {
						// this must be the soa servlet get our request
						HttpServletRequest httpRequest = rapidRequest.getRequest();
						// if this is dirctly to the Rapid servlet
						if (_request.getUrl().toLowerCase().startsWith("~")) {
							// make a url for the soa servlet
							url = new URL(httpRequest.getScheme(), httpRequest.getServerName(), httpRequest.getServerPort(), httpRequest.getContextPath() + "/" + _request.getUrl());
						} else {
							// make a url for the soa servlet
							url = new URL(httpRequest.getScheme(), httpRequest.getServerName(), httpRequest.getServerPort(), httpRequest.getContextPath() + "/soa");
							// check whether we have any id / version separators
							String[] actionParts = action.split("/");
							// add this app and version if none
							if (actionParts.length < 2) action = application.getId() + "/" + application.getVersion() +  "/" + action;
						}

					}

					// establish the connection
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();

					// if we are requesting from ourself
					if (url.getPath().startsWith(rapidRequest.getRequest().getContextPath())) {
						// get our session id
						String sessionId = rapidRequest.getRequest().getSession().getId();
						// add it to the call for internal authentication
						connection.setRequestProperty("Cookie", "JSESSIONID=" + sessionId);
					}

					// set the content type and action header accordingly
					if ("SOAP".equals(_request.getType())) {
						connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
						connection.setRequestProperty("SOAPAction", action);
					} else if ("JSON".equals(_request.getType()) || "XML".equals(_request.getType()) || "TEXT".equals(_request.getType())) {
						// if there is an action
						if (action.length() > 0) {
							// get it in upper case
							String actionUpper = action.toUpperCase();
							// if it's one of the special restful verbs
							if ("GET".equals(actionUpper)
								|| "POST".equals(actionUpper)
								|| "HEAD".equals(actionUpper)
								|| "OPTIONS".equals(actionUpper)
								|| "PUT".equals(actionUpper)
								|| "DELETE".equals(actionUpper)
								|| "TRACE".equals(actionUpper)
							) {
								// set the request method
								connection.setRequestMethod(actionUpper);
							} else {
								// set it as was
								connection.setRequestProperty("Action", action);
							}
						}
						// now check the type and set headers accordingly
						if ("JSON".equals(_request.getType())) {
							connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
							connection.setRequestProperty("Accept", "application/json, text/json, text/text");
						} else if ("TEXT".equals(_request.getType())) {
							connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
						} else {
							connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
							connection.setRequestProperty("Accept", "application/xml, text/xml, text/text");
						}
					}

					// look for any headers
					if (headers.trim().length() > 0) {
						// split on ;
						String[] headerParts = headers.split(";");
						// loop them
						for (String header : headerParts) {
							// get their pairs
							String[] headerKeyPair = header.split(":");
							// assume no value
							String value = "";
							// get second part as value if it exists
							if (headerKeyPair.length > 1) value = headerKeyPair[1];
							// set header!
							connection.setRequestProperty(headerKeyPair[0], value);
						}
					}

					// look for any authentication - only basic for the time being but others could be supported in future
			        String auth = getProperty("auth");
			        if("true".equalsIgnoreCase(auth)) {
			        	String authType = getProperty("authType");
			        	if("basic".equalsIgnoreCase(authType)) {
				        	String username = getProperty("authUsername");
				        	String password = getProperty("authPassword");
			        		String encoded = DatatypeConverter.printBase64Binary((username+":"+password).getBytes(StandardCharsets.UTF_8));
			        		connection.setRequestProperty("Authorization", "Basic "+encoded);
			        	}
			        }

			        // log
			        _logger.debug("Web service action request : " + url + " " + action + " " + headers + " " + body);

					// if a body has been specified
					if (body.length() > 0) {

						// Triggers POST.
						connection.setDoOutput(true);

						// get the output stream from the connection into which we write the request
						OutputStream output = connection.getOutputStream();

						// write the processed body string into the request output stream
						output.write(body.getBytes("UTF8"));

					}

					// check the response code
					int responseCode = connection.getResponseCode();

					_logger.debug("Web service action response code : " + responseCode);

					// read input stream if all ok, otherwise something meaningful should be in error stream
					if (responseCode == 200 || responseCode == 201) {

						// get the input stream
						InputStream response = connection.getInputStream();

						// prepare an soaData object
						SOAData soaData = null;

						if ("TEXT".equals(_request.getType())) {

							// read the string response
							String value = Strings.getString(response);

							// create a simple soaElement with it
							SOAElement soaElement = new SOAElement("value",value);

							// use element to soa data object
							soaData = new SOAData(soaElement);

						} else {

							// read the response accordingly
							if ("JSON".equals(_request.getType())) {
								// get the response
								String jsonResponse = Strings.getString(response);
								// log
								_logger.debug("Web service action JSON response : " + jsonResponse);
								// get a JSON reader
								SOAJSONReader jsonReader = new SOAJSONReader();
								// read the data
								soaData = jsonReader.read(jsonResponse);
							} else {
								SOAXMLReader xmlReader = new SOAXMLReader(_request.getRoot());
								String transform = _request.getTransform();
								if (transform != null) {
									if (transform.length() > 0) {
										_logger.debug("Applying transform :" + transform);
										response = XML.transform(response, transform);
									}
								}
								soaData = xmlReader.read(response);
							}

						}

						SOADataWriter rapidDataWriter = new SOARapidWriter(soaData);

						String jsonDataString = rapidDataWriter.write();

						jsonData = new JSONObject(jsonDataString);

						if (actionCache != null) actionCache.put(application.getId(), getId(), jsonInputs.toString(), jsonData);

						response.close();

					} else {

						// get the error stream
						InputStream response = connection.getErrorStream();

						// assume no message
						String errorMessage = null;

						// reader the response body
						String responseBody = Strings.getString(response);

						// log
						_logger.debug("Web service action error response : " + responseBody);

						// if SOAP
						if ("SOAP".equals(_request.getType())) {

							// read the fault code as error message
							errorMessage = XML.getElementValue(responseBody, "faultcode");

							// if the was a faultcode
							if (errorMessage != null) {

								// read read the fault string
								String faultString = XML.getElementValue(responseBody, "faultstring");

								// combine if both present
								if (faultString != null) errorMessage += " " + faultString;

							}

						}

						// if still no message use the body
						if (errorMessage == null) errorMessage = responseBody;

						// only if there is no application cache show the error, otherwise it sends an empty response
						if (actionCache == null) {

							throw new JSONException(" response code " + responseCode + " from server : " + errorMessage);

						} else {

							_logger.debug("Error not shown to user due to cache : " + errorMessage);

						}

					}

					// disconnect
					connection.disconnect();

				} // request url != null

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
