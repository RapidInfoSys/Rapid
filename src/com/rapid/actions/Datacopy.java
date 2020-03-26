/*

Copyright (C) 2020 - Gareth Edwards / Rapid Information Systems

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Datacopy extends Action {

	// public static class
	public static class DataCopy {

		// private instance variables
		private String _source;
		private String _sourceField;
		private String _destination;
		private String _destinationField;
		private String _type;

		// properties
		public String getSource() { return _source; }
		public void setSource(String source) { _source = source; }

		public String getSourceField() { return _sourceField; }
		public void setSourceField(String sourceField) { _sourceField = sourceField; }

		public String getDestination() { return _destination; }
		public void setDestination(String destination) { _destination = destination; }

		public String getDestinationField() { return _destinationField; }
		public void setDestinationField(String destinationField) { _destinationField = destinationField; }

		public String getType() { return _type; }
		public void setType(String type) { _type = type; }

		// constructors
		// used by jaxb
		public DataCopy() { super(); }
		// used by designer
		public DataCopy(JSONObject jsonCopy) throws Exception {
			_source = jsonCopy.optString("source", null);
			_sourceField = jsonCopy.optString("sourceField", null);
			_destination = jsonCopy.optString("destination", null);
			_destinationField = jsonCopy.optString("destinationField", null);
			_type = jsonCopy.optString("type", null);
		}

	}

	// private instance variables
	private List<DataCopy> _dataCopies;

	// properties
	public List<DataCopy> getDataCopies() { return _dataCopies; }
	public void setDataCopies(List<DataCopy> dataCopies) { _dataCopies = dataCopies; }

	// constructors

	// used by jaxb
	public Datacopy() { super(); }
	// used by designer
	public Datacopy(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for dataCopies
			if (!"dataCopies".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}
		// look for a dataCopies array
		JSONArray jsonDataCopies = jsonAction.optJSONArray("dataCopies");
		// if we got one
		if (jsonDataCopies != null) {
			// instantiate our collection
			_dataCopies = new ArrayList<DataCopy>();
			// loop it
			for (int i = 0; i < jsonDataCopies.length(); i++) {
				// add this one
				_dataCopies.add(new DataCopy(jsonDataCopies.getJSONObject(i)));
			}
		}
	}

	// overrides

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) {

		// the javascript we're making
		String js = "";

		// get the Rapid servlet
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();

		// get any copy type
		String copyType = getProperty("copyType");

		// set to replace if null (for backwards compatibility)
		if (copyType == null) copyType = "replace";

		// bulk copy is a special animal
		if ("bulk".equals(copyType)) {

			// check we have some data copies
			if (_dataCopies == null) {

				// data copies not found return a comment
				js = "// data copies not found for " + getId();

			} else {

				// compare against last getData call to avoid recalling
				String lastGetDataFunction = null;

				// loop them
				for (DataCopy dataCopy : _dataCopies) {

					// get the destination id
					String destinationId = dataCopy.getDestination();

					// check we got one
					if (destinationId.length() > 0) {

						// get the get data function
						String getDataFunction = Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, dataCopy.getSource(), dataCopy.getSourceField());

						// add the getData if different from the last one
						if (!getDataFunction.equals(lastGetDataFunction)) js += "var data = "  + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, dataCopy.getSource(), dataCopy.getSourceField()) + ";\n";

						// remember this one
						lastGetDataFunction = getDataFunction;

						// split if by escaped .
						String idParts[] = destinationId.split("\\.");
						// if there is more than 1 part we are dealing with set properties, for now just update the destintation id
						if (idParts.length > 1) destinationId = idParts[0];

						// first try and look for the control in the page
						Control destinationControl = page.getControl(destinationId);
						// assume we found it
						boolean pageControl = true;
						// check we got a control
						if (destinationControl == null) {
							// now look for the control in the application
							destinationControl = application.getControl(rapidServlet.getServletContext(), destinationId);
							// set page control to false
							pageControl = false;
						}

						// check we got one from either location
						if (destinationControl == null) {

							// data copies not found return a comment
							js += "// data destination not found for " + destinationId + "\n";

						} else {

							// get the field
							String destinationField = dataCopy.getDestinationField();
							// clean up the field
							if (destinationField == null) destinationField = "";

							// get any details we may have
							String details = destinationControl.getDetailsJavaScript(application, page);

							// if the idParts is greater then 1 this is a set property
							if (idParts.length > 1) {

								// if we have some details
								if (details != null) {
									// if this is a page control
									if (pageControl) {
										// the details will already be in the page so we can use the short form
										details = destinationControl.getId() + "details";
									}
								}

								// get the property from the second id part
								String property = idParts[1];
								// append the set property call
								js += "setProperty_" + destinationControl.getType() +  "_" + property + "(ev, '" + destinationControl.getId() + "', '" + destinationField + "', " + details + ", data, " + Boolean.parseBoolean(getProperty("changeEvents")) + ");\n";

							} else {

								// set details to empty string or clean up
								if (details == null) {
									details = "";
								} else {
									// if this is a page control
									if (pageControl) {
										// the details will already be in the page so we can use the short form
										details = ",details:" + destinationControl.getId() + "details";
									} else {
										// write the full details
										details = ",details:" + details;
									}
								}

								// try and get the type
								String type = dataCopy.getType();
								// check it
								if (type == null || "false".equals(type)) {
									// update to empty string
									type = "";
								} else {
									// update to comma-prefixed, string escaped
									type = ",'" + type + "'";
								}

								// do the data copy
								js += "Action_datacopy(ev, data, [{id:'" + destinationControl.getId() + "',type:'" + destinationControl.getType() + "',field:'" + destinationField + "'" + details + "}], " + Boolean.parseBoolean(getProperty("changeEvents")) + type + ");\n";

							} // copy / set property check

						} // destination control check

					} // destination id check

				} // data copies loop

			} // data copies check

		} else {

			// get the data source
			String dataSourceId = getProperty("dataSource");

			// check there is a datasource
			if (dataSourceId == null) {

				// no data source return a comment
				js = "// no data source for action " + getId();

			} else {

				// get the data source field
				String dataSourceField = getProperty("dataSourceField");

				// split by escaped .
				String[] idParts = dataSourceId.split("\\.");

				// if this is a datetime value, and it has a type
				if ("Datetime".equals(idParts[0]) && idParts.length > 1) {
					// get the type from the second part
					String type = idParts[1];
					// make format with dateFormat and timeFormat properties
					String format = "";
					//generate the date/time at run time
					if("current date".equals(type)){
						// get its, format
						format = getProperty("dateFormat");

					} else if ("current time".equals(type)){
						// get its, format
						format = getProperty("timeFormat");

					} else { // if the date and time format is selected
						// get its, format
						format = getProperty("dateFormat") + " " + getProperty("timeFormat");
					}

					String formatter = "current date and time".startsWith(idParts[1]) ? "formatDatetime" : "formatTime";
					// make the js!
					js = "var data = " + formatter + "('" + format + "', new Date());\n";

				} else {

					js = "var data = "  + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, dataSourceId, dataSourceField) + ";\n";

				}

				// we're going to work with the data destinations in a json array
				JSONArray jsonDataDestinations = null;

				// assume we have no need for an outputs array
				boolean outputsArray = false;

				// try and get the data destinations from the properties into a json array, silently fail if not
				try { jsonDataDestinations = new JSONArray(getProperty("dataDestinations")); }
				catch (Exception e) {}

				if (jsonDataDestinations == null) {

					// data source destinations not found return a comment
					js += "// data source destinations not found for " + getId() + "\n";

				} else {

					// prepare a string for the outputs array
					String jsOutputs = "";
					// loop the json data destination collection
					for (int i = 0; i < jsonDataDestinations.length(); i++) {

						// try and make an output for this destination
						try {

							// retrieve this data destination
							JSONObject jsonDataDesintation = jsonDataDestinations.getJSONObject(i);
							// get the control id
							String destinationId = jsonDataDesintation.getString("itemId");

							// split by escaped .
							idParts = destinationId.split("\\.");
							// if there is more than 1 part we are dealing with set properties, for now just update the destintation id
							if (idParts.length > 1) destinationId = idParts[0];

							// first try and look for the control in the page
							Control destinationControl = page.getControl(destinationId);
							// assume we found it
							boolean pageControl = true;
							// check we got a control
							if (destinationControl == null) {
								// now look for the control in the application
								destinationControl = application.getControl(rapidServlet.getServletContext(), destinationId);
								// set page control to false
								pageControl = false;
							}

							// check we got one from either location
							if (destinationControl != null) {

								// get the field
								String destinationField = jsonDataDesintation.optString("field");
								// clean up the field
								if (destinationField == null) destinationField = "";

								// get any details we may have
								String details = destinationControl.getDetailsJavaScript(application, page);

								// if the idParts is greater then 1 this is a set property
								if (idParts.length > 1) {

									// if we have some details
									if (details != null) {
										// if this is a page control
										if (pageControl) {
											// the details will already be in the page so we can use the short form
											details = destinationControl.getId() + "details";
										}
									}

									// get the property from the second id part
									String property = idParts[1];
									// append the set property call
									js += "setProperty_" + destinationControl.getType() +  "_" + property + "(ev,'" + destinationControl.getId() + "','" + destinationField + "'," + details + ",data, " + Boolean.parseBoolean(getProperty("changeEvents")) + ");\n";

								} else {

									// we will need an outputs array
									outputsArray = true;

									// set details to empty string or clean up
									if (details == null) {
										details = "";
									} else {
										// if this is a page control
										if (pageControl) {
											// the details will already be in the page so we can use the short form
											details = ",details:" + destinationControl.getId() + "details";
										} else {
											// write the full details
											details = ",details:" + details;
										}
									}

									// add the properties we need as a js object it will go into the array
									jsOutputs += "{id:'" + destinationControl.getId() + "',type:'" + destinationControl.getType() + "',field:'" + destinationField + "'" + details + "},";

								}

							}

						} catch (JSONException ex) {

							// data source destinations not found return a comment
							js = "// error creating data output for " + getId() + " : " + ex.getMessage();

						}
					}

					// if there was an outputs array
					if (outputsArray) {

						// trim the last comma
						if (jsOutputs.length() > 0) jsOutputs = jsOutputs.substring(0, jsOutputs.length() - 1);

						// add to js as an array
						js += "var outputs = [" + jsOutputs + "];\n";
						// add the start of the call
						js += "Action_datacopy(ev, data, outputs, " + Boolean.parseBoolean(getProperty("changeEvents"));

						// add the copy type to the js
						js += ", '" + copyType + "'";
						// check the copy type
						if ("child".equals(copyType)) {
							// no merge data object
							js += ", null";
							// look for a merge field
							String childField = getProperty("childField");
							// check if got we got one
							if (childField == null) {
								// call it child if not
								js += ", 'child'";
							} else {
								// add it
								js += ", '" + childField + "'";
							}

						} else if ("search".equals(copyType)) {

							// get the search data
							js += ", " + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, getProperty("searchSource"), getProperty("searchSourceField"));
							// look for a search field
							String searchField = getProperty("searchField");
							// add it if present
							if (searchField != null) js += ", '" + searchField + "'";
							// look for a maxRows field
							String maxRows = getProperty("maxRows");
							// add it if present
							if (maxRows != null) js += "," + maxRows;

						} else if ("trans".equals(copyType)) {

							// assume the key fields are null
							String keyFields = "null";

							// try and fetch the key fields
							try {
								JSONArray jsonKeyFields = new JSONArray(getProperty("keyFields"));
								keyFields = jsonKeyFields.toString();
							} catch (JSONException ex) {
								keyFields = "null /*" + ex.getMessage() + "*/";
							}

							// assume the ignore fields are null
							String ignoreFields = "null";

							// try and fetch the ignore fields, show message if issue
							try {
								JSONArray jsonIgnoreFields = new JSONArray(getProperty("ignoreFields"));
								ignoreFields = jsonIgnoreFields.toString();
							} catch (JSONException ex) {
								ignoreFields = "null /*" + ex.getMessage() + "*/";
							}

							// add the details
							js += ", null, null, {keyFields:" + keyFields + ",ignoreFields:" + ignoreFields + "}";

						} // copy type

						// close the copy data call
						js += ");\n";

					} // outputs array

				} // data destinations check

			} // data source check

		} // bulk copy check

		return js;

	}

}
