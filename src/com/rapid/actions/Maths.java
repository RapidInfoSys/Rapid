/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

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
import java.util.Map;

import javax.servlet.ServletContext;
import javax.xml.bind.annotation.XmlType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Maths extends Action {
	
	// this action has generic inputs
	@XmlType(namespace="http://rapid-is.co.uk/maths")
	public static class Input {

		private String _itemId, _field, _inputField;

		public String getItemId() { return _itemId; }
		public void setItemId(String itemId) { _itemId = itemId; }

		public String getField() { return _field; }
		public void setField(String field) { _field = field; }

		public String getInputField() { return _inputField; }
		public void setInputField(String inputField) { _inputField = inputField; }

		public Input() {};
		public Input(String itemId, String field, String inputField) {
			_itemId = itemId;
			_field = field;
			_inputField = inputField;
		}

	}
	
	// this action has generic outputs
	@XmlType(namespace="http://rapid-is.co.uk/wgbp")
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
	
	// static variables
	private static Logger _logger = LogManager.getLogger(Maths.class);

	// private instance variables

	private List<Input> _inputs;
	private List<Output> _outputs;

	// properties

	public List<Input> getInputs() { return _inputs; }
	public void setInputs(List<Input> inputs) { _inputs = inputs; }
	
	// parameterless constructor (required for jaxb)
	public Maths() { super(); }
	// designer constructor
	public Maths(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		// call the parameterless constructor which sets the xml version
		this();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for inputs, outputs, successActions and errorActions
			if (!"inputs".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// grab any inputs
		JSONArray jsonInputs = jsonAction.optJSONArray("inputs");
		// if we got some
		if (jsonInputs != null) {
			// instantiate our array
			_inputs = new ArrayList<Input>();
			// loop them
			for (int i = 0; i < jsonInputs.length(); i++) {
				// get the input
				JSONObject jsonInput = jsonInputs.getJSONObject(i);
				// add it
				_inputs.add(new Input(jsonInput.optString("itemId"), jsonInput.optString("field"), jsonInput.optString("inputField")));
			}

		}	
		
		
	}
		
	// methods
	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) {
		
		// get the JavaScript
		String js = "";
        
        // empty the outputs collection
        //js += "outputs = [];\n";
        // start the data object with the type
     	js += "var data = null;\n";
     	
     	//now get the operation type
     	String operation = getProperty("operation");
     	
        // check if we have inputs
     	if (_inputs != null) {
			// loop them
			for (int i = 0; i < _inputs.size(); i++) {
				
				// get the input
				Input input = _inputs.get(i);
				// get this item id
				String itemId = input.getItemId();
				// get this item field
				String itemField = input.getField();
				// get the inpute field
				String inputField = input.getInputField();
				// update the input field with the index if blank
				if (inputField == null || "".equals(inputField)) inputField = Integer.toString(i);
				
				// set data for first input then use assignment operator for operations
				if (i == 0) {
					js += "data = parseFloat(" + Control.getDataJavaScript(rapidRequest.getRapidServlet().getServletContext(), application, page, itemId, itemField) + ");\n";
				} else {
					js += "data " + operation + "= parseFloat(" + Control.getDataJavaScript(rapidRequest.getRapidServlet().getServletContext(), application, page, itemId, itemField) + ");\n";
				}
				
			}
     	}
     	
     	// get the output control details
     	String outputId = getProperty("output");
     	String outputField = getProperty("outputField");
     	
     	// send data into the output control
     	js += Control.setDataJavaScript(rapidRequest.getRequest().getServletContext(), application, page, outputId, outputField) + ";\n";
     	
		return js.toString();

	}
	
	
	
}
