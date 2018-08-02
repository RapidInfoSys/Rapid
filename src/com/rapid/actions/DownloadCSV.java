package com.rapid.actions;

import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class DownloadCSV extends Action{
	
	//private instance variables

	// constructors
	// used by jaxb
	public DownloadCSV() {
		super();
	}
	
	// used by designer when saving
	public DownloadCSV(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for dataCopies
			addProperty(key, jsonAction.get(key).toString());
		}
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control,
			JSONObject jsonDetails) throws Exception {
		
		// get the source table ID
		String gridId = getProperty("gridId");
		//get the output file name
		String outputFile = getProperty("outputFilename");
		
		if(outputFile == null || "".equals(outputFile)) outputFile = "filename.csv";
		if(!outputFile.endsWith(".csv")) outputFile += ".csv";
		
		return "Action_downloadCSV(ev, '" + this.getId() + "', '" + gridId + "', '" + outputFile + "')";
	}
	
	
	
	
}
