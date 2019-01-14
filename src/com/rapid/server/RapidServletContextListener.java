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

package com.rapid.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Applications;
import com.rapid.core.Applications.Versions;
import com.rapid.core.Device.Devices;
import com.rapid.core.Email;
import com.rapid.core.Process;
import com.rapid.core.Theme;
import com.rapid.core.Workflow;
import com.rapid.core.Workflows;
import com.rapid.utils.Classes;
import com.rapid.utils.Comparators;
import com.rapid.utils.Encryption.EncryptionProvider;
import com.rapid.utils.Files;
import com.rapid.utils.Https;
import com.rapid.utils.JAXB.EncryptedXmlAdapter;
import com.rapid.utils.Strings;

public class RapidServletContextListener implements ServletContextListener {

	// the logger which we will initialise
	private static Logger _logger;

	// the schema factory that we will load the actions and controls schemas into
	private static SchemaFactory _schemaFactory;

	// all of the classes we are going to put into our jaxb context
	private static ArrayList<Class> _jaxbClasses;

	// enterprise monitor
	protected static Monitor _monitor = new Monitor();

	// public static methods
	public static void logFileNames(File dir, String rootPath) {

		for (File file : dir.listFiles()) {

			if (file.isDirectory()) {

				logFileNames(file, rootPath);

			} else {

				String fileName = file.toString();

				_logger.info(fileName.substring(rootPath.length()));

			}

		}

	}

	public static int loadLogins(ServletContext servletContext) throws Exception {

		int loginCount = 0;

		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/logins/");

		// if the directory exists
		if (dir.exists()) {

			// create an array list of json objects to hold the logins
			ArrayList<JSONObject> logins = new ArrayList<JSONObject>();

			// create a filter for finding .control.xml files
			FilenameFilter xmlFilenameFilter = new FilenameFilter() {
		    	@Override
				public boolean accept(File dir, String name) {
		    		return name.toLowerCase().endsWith(".login.xml");
		    	}
		    };

		    // create a schema object for the xsd
		    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/login.xsd"));
		    // create a validator
		    Validator validator = schema.newValidator();

			// loop the xml files in the folder
			for (File xmlFile : dir.listFiles(xmlFilenameFilter)) {

				// read the xml into a string
				String xml = Strings.getString(xmlFile);

				// validate the control xml file against the schema
				validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

				// convert the string into JSON
				JSONObject jsonLogin = org.json.XML.toJSONObject(xml).getJSONObject("login");

				// add to array list
				logins.add(jsonLogin);

				// increment the count
				loginCount++;

			}

			// put the logins in a context attribute (this is available to the security adapters on initialisation)
			servletContext.setAttribute("jsonLogins", logins);

		}

		_logger.info(loginCount + " logins loaded from .login.xml files");

		return loginCount;

	}

	public static int loadDatabaseDrivers(ServletContext servletContext) throws Exception {

		// create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/databaseDrivers.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();

		// read the xml into a string
		String xml = Strings.getString(new File(servletContext.getRealPath("/") + "/WEB-INF/database/" + "/databaseDrivers.xml"));

		// validate the control xml file against the schema
		validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

		// convert the xml string into JSON
		JSONObject jsonDatabaseDriverCollection = org.json.XML.toJSONObject(xml).getJSONObject("databaseDrivers");

		// prepare the array we are going to popoulate
		JSONArray jsonDatabaseDrivers = new JSONArray();

		JSONObject jsonDatabaseDriver;
		int index = 0;
		int count = 0;

		if (jsonDatabaseDriverCollection.optJSONArray("databaseDriver") == null) {
			jsonDatabaseDriver = jsonDatabaseDriverCollection.getJSONObject("databaseDriver");
		} else {
			jsonDatabaseDriver = jsonDatabaseDriverCollection.getJSONArray("databaseDriver").getJSONObject(index);
			count = jsonDatabaseDriverCollection.getJSONArray("databaseDriver").length();
		}

		do {

			_logger.info("Registering database driver " + jsonDatabaseDriver.getString("name")  + " using " + jsonDatabaseDriver.getString("class"));

			try {

				// check this type does not already exist
				for (int i = 0; i < jsonDatabaseDrivers.length(); i++) {
					if (jsonDatabaseDriver.getString("name").equals(jsonDatabaseDrivers.getJSONObject(i).getString("name"))) throw new Exception(" database driver type is loaded already. Type names must be unique");
				}

				// get  the class name
				String className = jsonDatabaseDriver.getString("class");
				// get the current thread class loader (this should log better if there are any issues)
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				// check we got a class loader
				if (classLoader == null) {
					// register the class the old fashioned way so the DriverManager can find it
					Class.forName(className);
				} else {
					// register the class on this thread so we can catch any errors
					Class.forName(className, true, classLoader);
				}

				// add the jsonControl to our array
				jsonDatabaseDrivers.put(jsonDatabaseDriver);

			} catch (Exception ex) {

				_logger.error("Error registering database driver : " + ex.getMessage(), ex);

			}

			// inc the count of controls in this file
			index++;

			// get the next one
			if (index < count) jsonDatabaseDriver = jsonDatabaseDriverCollection.getJSONArray("databaseDriver").getJSONObject(index);

		} while (index < count);

		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonDatabaseDrivers", jsonDatabaseDrivers);

		_logger.info(index + " database drivers loaded from databaseDrivers.xml file");

		return index;

	}

	// loop all of the .connectionAdapter.xml files and check the injectable classes, so we can re-initialise JAXB context to be able to serialise them, and cache their constructors for speedy initialisation
	public static int loadConnectionAdapters(ServletContext servletContext) throws Exception {

		int adapterCount = 0;

		// retain our class constructors in a hashtable - this speeds up initialisation
		HashMap<String,Constructor> connectionConstructors = new HashMap<String,Constructor>();

		// create an array list of json objects which we will sort later according to the order
		ArrayList<JSONObject> connectionAdapters = new ArrayList<JSONObject>();

		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/database/");

		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	@Override
			public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".connectionadapter.xml");
	    	}
	    };

	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/connectionAdapter.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();

		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) {

			// read the xml into a string
			String xml = Strings.getString(xmlFile);

			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

			// convert the string into JSON
			JSONObject jsonConnectionAdapter = org.json.XML.toJSONObject(xml).getJSONObject("connectionAdapter");

			// get the class name from the json
			String className = jsonConnectionAdapter.getString("class");
			// get the class
			Class classClass = Class.forName(className);
			// check the class extends com.rapid.data.ConnectionAdapter
			if (!Classes.extendsClass(classClass, com.rapid.data.ConnectionAdapter.class)) throw new Exception(classClass.getCanonicalName() + " must extend com.rapid.data.ConnectionAdapter");
			// check this class is unique
			if (connectionConstructors.get(className) != null) throw new Exception(className + " connection adapter already loaded.");
			// add to constructors hashmap referenced by type
			connectionConstructors.put(className, classClass.getConstructor(ServletContext.class, String.class, String.class, String.class, String.class));

			// add to to our array list
			connectionAdapters.add(jsonConnectionAdapter);

			// increment the count
			adapterCount++;

		}

		// sort the connection adapters according to their order property
		Collections.sort(connectionAdapters, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				try {
					return o1.getInt("order") - o2.getInt("order");
				} catch (JSONException e) {
					return 999;
				}
			}
		});

		// create a JSON Array object which will hold json for all of the available security adapters
		JSONArray jsonConnectionAdapters = new JSONArray();

		// loop the sorted connection adapters and add to the json array
		for (JSONObject jsonConnectionAdapter : connectionAdapters) jsonConnectionAdapters.put(jsonConnectionAdapter);

		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonConnectionAdapters", jsonConnectionAdapters);

		// put the constructors hashmapin a context attribute (this is available via the getContructor method in RapidHttpServlet)
		servletContext.setAttribute("securityConstructors", connectionConstructors);

		_logger.info(adapterCount + " connection adapters loaded in .connectionAdapter.xml files");

		return adapterCount;

	}

	// loop all of the .securityAdapter.xml files and check the injectable classes, so we can re-initialise JAXB context to be able to serialise them, and cache their constructors for speedy initialisation
	public static int loadSecurityAdapters(ServletContext servletContext) throws Exception {

		int adapterCount = 0;

		// retain our class constructors in a hashtable - this speeds up initialisation
		HashMap<String,Constructor> securityConstructors = new HashMap<String,Constructor>();

		// create a JSON Array object which will hold json for all of the available security adapters
		JSONArray jsonSecurityAdapters = new JSONArray();

		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/security/");

		// create a filter for finding .securityadapter.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	@Override
			public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".securityadapter.xml");
	    	}
	    };

	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/securityAdapter.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();

		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) {

			// read the xml into a string
			String xml = Strings.getString(xmlFile);

			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

			// convert the string into JSON
			JSONObject jsonSecurityAdapter = org.json.XML.toJSONObject(xml).getJSONObject("securityAdapter");

			// get the type from the json
			String type = jsonSecurityAdapter.getString("type");
			// get the class name from the json
			String className = jsonSecurityAdapter.getString("class");
			// get the class
			Class classClass = Class.forName(className);
			// check the class extends com.rapid.security.SecurityAdapter
			if (!Classes.extendsClass(classClass, com.rapid.security.SecurityAdapter.class)) throw new Exception(type + " security adapter class " + classClass.getCanonicalName() + " must extend com.rapid.security.SecurityAdapter");
			// check this type is unique
			if (securityConstructors.get(type) != null) throw new Exception(type + " security adapter already loaded. Type names must be unique.");
			// add to constructors hashmap referenced by type
			securityConstructors.put(type, classClass.getConstructor(ServletContext.class, Application.class));

			// add to our collection
			jsonSecurityAdapters.put(jsonSecurityAdapter);

			// increment the count
			adapterCount++;

		}

		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonSecurityAdapters", jsonSecurityAdapters);

		// put the constructors hashmapin a context attribute (this is available via the getContructor method in RapidHttpServlet)
		servletContext.setAttribute("securityConstructors", securityConstructors);

		_logger.info(adapterCount + " security adapters loaded in .securityAdapter.xml files");

		return adapterCount;

	}

	// loop all of the .securityAdapter.xml files and check the injectable classes, so we can re-initialise JAXB context to be able to serialise them, and cache their constructors for speedy initialisation
	public static int loadFormAdapters(ServletContext servletContext) throws Exception {

		int adapterCount = 0;

		// retain our form adapter class constructors in a hashtable - this speeds up initialisation
		HashMap<String,Constructor> formConstructors = new HashMap<String,Constructor>();

		// retain our payment class constructors in a hashtable - this speeds up initialisation
		HashMap<String,Constructor> paymentConstructors = new HashMap<String,Constructor>();

		// create a JSON Array object which will hold json for all of the available security adapters
		JSONArray jsonAdapters = new JSONArray();

		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/forms/");

		// create a filter for finding .formadapter.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	@Override
			public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".formadapter.xml");
	    	}
	    };

	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/formAdapter.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();

		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) {

			// read the xml into a string
			String xml = Strings.getString(xmlFile);

			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

			// convert the string into JSON
			JSONObject jsonFormAdapter = org.json.XML.toJSONObject(xml).getJSONObject("formAdapter");

			// get the type from the json
			String type = jsonFormAdapter.getString("type");
			// get the class name from the json
			String className = jsonFormAdapter.getString("class");

			// get the class
			Class classClass = Class.forName(className);
			// check the class extends com.rapid.forms.FormAdapter
			if (!Classes.extendsClass(classClass, com.rapid.forms.FormAdapter.class)) throw new Exception(type + " form adapter class " + classClass.getCanonicalName() + " must extend com.rapid.forms.FormsAdapter");
			// check this type is unique
			if (formConstructors.get(type) != null) throw new Exception(type + " form adapter already loaded. Type names must be unique.");
			// add to constructors hashmap referenced by type
			formConstructors.put(type, classClass.getConstructor(ServletContext.class, Application.class, String.class));

			// look for a paymentGateway class
			className = jsonFormAdapter.optString("paymentClass", null);
			// if a payment class was provided and we don't yet have a constructor for this payment class
			if (className != null && paymentConstructors.get(className) == null) {
				// get the payment class
				classClass = Class.forName(className);
				// check the class implements com.rapid.forms.PaymentGateway
				if (!Classes.extendsClass(classClass, com.rapid.forms.PaymentGateway.class)) throw new Exception(type + " form adapter paymentClass " + classClass.getCanonicalName() + " must extend com.rapid.forms.PaymentGateway");
				// add to constructors hashmap referenced by type
				paymentConstructors.put(className, classClass.getConstructor(ServletContext.class, Application.class));
			}

			// add to our collection
			jsonAdapters.put(jsonFormAdapter);

			// increment the count
			adapterCount++;

		}

		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonFormAdapters", jsonAdapters);

		// put the constructors hashmap in a context attribute (this is available via the getContructor method in RapidHttpServlet)
		servletContext.setAttribute("formConstructors", formConstructors);

		// put the constructors hashmap in a context attribute (this is available via the getContructor method in RapidHttpServlet)
		servletContext.setAttribute("paymentConstructors", paymentConstructors);

		// log
		_logger.info(adapterCount + " form adapters loaded in .formAdapter.xml files");

		return adapterCount;

	}

	// loop all of the .action.xml files and check the injectable classes, so we can re-initialise JAXB context to be able to serialise them, and cache their constructors for speedy initialisation
	public static int loadActions(ServletContext servletContext) throws Exception {

		// assume no actions
		int actionCount = 0;

		// create a list of json actions which we will sort later
		List<JSONObject> jsonActions = new ArrayList<JSONObject>();

		// retain our class constructors in a hashtable - this speeds up initialisation
		HashMap<String,Constructor> actionConstructors = new HashMap<String,Constructor>();

		// build a collection of classes so we can re-initilise the JAXB context to recognise our injectable classes
		ArrayList<Action> actions = new ArrayList<Action>();

		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/actions/");

		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	@Override
			public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".action.xml");
	    	}
	    };

	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/action.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();

		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) {

			// get a scanner to read the file
			Scanner fileScanner = new Scanner(xmlFile).useDelimiter("\\A");

			// read the xml into a string
			String xml = fileScanner.next();

			// close the scanner (and file)
			fileScanner.close();

			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

			// convert the string into JSON
			JSONObject jsonActionCollection = org.json.XML.toJSONObject(xml).getJSONObject("actions");

			JSONObject jsonAction;
			int index = 0;
			int count = 0;

			// the JSON library will add a single key of there is a single class, otherwise an array
			if (jsonActionCollection.optJSONArray("action") == null) {
				jsonAction = jsonActionCollection.getJSONObject("action");
			} else {
				jsonAction = jsonActionCollection.getJSONArray("action").getJSONObject(index);
				count = jsonActionCollection.getJSONArray("action").length();
			}

			do {

				// check this type does not already exist
				for (int i = 0; i < jsonActions.size(); i++) {
					if (jsonAction.getString("type").equals(jsonActions.get(i).getString("type"))) throw new Exception(" action type is loaded already. Type names must be unique");
				}

				// add the jsonControl to our array
				jsonActions.add(jsonAction);
				// get the named type from the json
				String type = jsonAction.getString("type");
				// get the class name from the json
				String className = jsonAction.getString("class");
				// get the class
				Class classClass = Class.forName(className);
				// check the class extends com.rapid.Action
				if (!Classes.extendsClass(classClass, com.rapid.core.Action.class)) throw new Exception(type + " action class " + classClass.getCanonicalName() + " must extend com.rapid.core.Action.");
				// check this type is unique
				if (actionConstructors.get(type) != null) throw new Exception(type + " action already loaded. Type names must be unique.");
				// add to constructors hashmap referenced by type
				actionConstructors.put(type, classClass.getConstructor(RapidHttpServlet.class, JSONObject.class));
				// add to our jaxb classes collection
				_jaxbClasses.add(classClass);
				// inc the control count
				actionCount ++;
				// inc the count of controls in this file
				index++;

				// get the next one
				if (index < count) jsonAction = jsonActionCollection.getJSONArray("control").getJSONObject(index);

			} while (index < count);

		}

		// sort the list of actions by name
		Collections.sort(jsonActions, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject c1, JSONObject c2) {
				try {
					return Comparators.AsciiCompare(c1.getString("name"), c2.getString("name"), false);
				} catch (JSONException e) {
					return 0;
				}
			}

		});

		// create a JSON Array object which will hold json for all of the available controls
		JSONArray jsonArrayActions = new JSONArray(jsonActions);

		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonActions", jsonArrayActions);

		// put the constructors hashmapin a context attribute (this is available via the getContructor method in RapidHttpServlet)
		servletContext.setAttribute("actionConstructors", actionConstructors);

		_logger.info(actionCount + " actions loaded in .action.xml files");

		return actionCount;

	}

	// here we loop all of the control.xml files and instantiate the json class object/functions and cache them in the servletContext
	public static int loadControls(ServletContext servletContext) throws Exception {

		// assume no controls
		int controlCount = 0;

		// create a list for our controls
		List<JSONObject> jsonControls = new ArrayList<JSONObject>();

		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/controls/");

		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	@Override
			public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".control.xml");
	    	}
	    };

	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/control.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();

		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) {

			// get a scanner to read the file
			Scanner fileScanner = new Scanner(xmlFile).useDelimiter("\\A");

			// read the xml into a string
			String xml = fileScanner.next();

			// close the scanner (and file)
			fileScanner.close();

			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

			// convert the string into JSON
			JSONObject jsonControlCollection = org.json.XML.toJSONObject(xml).getJSONObject("controls");

			JSONObject jsonControl;
			int index = 0;
			int count = 0;

			if (jsonControlCollection.optJSONArray("control") == null) {
				jsonControl = jsonControlCollection.getJSONObject("control");
			} else {
				jsonControl = jsonControlCollection.getJSONArray("control").getJSONObject(index);
				count = jsonControlCollection.getJSONArray("control").length();
			}

			do {

				// check this type does not already exist
				for (int i = 0; i < jsonControls.size(); i++) {
					if (jsonControl.getString("type").equals(jsonControls.get(i).getString("type"))) throw new Exception(" control type is loaded already. Type names must be unique");
				}

				// add the jsonControl to our array
				jsonControls.add(jsonControl);

				// inc the control count
				controlCount ++;
				// inc the count of controls in this file
				index++;

				// get the next one
				if (index < count) jsonControl = jsonControlCollection.getJSONArray("control").getJSONObject(index);

			} while (index < count);

		}

		// sort the list of controls by name
		Collections.sort(jsonControls, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject c1, JSONObject c2) {
				try {
					return Comparators.AsciiCompare(c1.getString("name"), c2.getString("name"), false);
				} catch (JSONException e) {
					return 0;
				}
			}

		});

		// create a JSON Array object which will hold json for all of the available controls
		JSONArray jsonArrayControls = new JSONArray(jsonControls);

		// put the jsonControls in a context attribute (this is available via the getJsonControls method in RapidHttpServlet)
		servletContext.setAttribute("jsonControls", jsonArrayControls);

		_logger.info(controlCount + " controls loaded in .control.xml files");

		return controlCount;

	}

	// here we loop all of the theme.xml files and instantiate the json class object/functions and cache them in the servletContext
	public static int loadThemes(ServletContext servletContext) throws Exception {

		// assume no themes
		int themeCount = 0;

		// create a list for our themes
		List<Theme> themes = new ArrayList<Theme>();

		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/themes/");

		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	@Override
			public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".theme.xml");
	    	}
	    };

	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/theme.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();

		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) {

			// get a scanner to read the file
			Scanner fileScanner = new Scanner(xmlFile).useDelimiter("\\A");

			// read the xml into a string
			String xml = fileScanner.next();

			// close the scanner (and file)
			fileScanner.close();

			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

			// create a theme object from the xml
			Theme theme = new Theme(xml);

			// add it to our collection
			themes.add(theme);

			// inc the template count
			themeCount ++;

		}

		// sort the list of templates by name
		Collections.sort(themes, new Comparator<Theme>() {
			@Override
			public int compare(Theme t1, Theme t2) {
				return Comparators.AsciiCompare(t1.getName(), t2.getName(), false);
			}

		});

		// put the jsonControls in a context attribute (this is available via the getJsonControls method in RapidHttpServlet)
		servletContext.setAttribute("themes", themes);

		_logger.info(themeCount + " themes loaded in .theme.xml files");

		return themeCount;

	}

	// Here we loop all of the folders under "applications" looking for a application.xml file, copying to the latest version if found before loading the versions
	public static int loadApplications(ServletContext servletContext) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException, RapidLoadingException, XPathExpressionException {

		// get any existing applications
		Applications applications = (Applications) servletContext.getAttribute("applications");

		// check we got some
		if (applications != null) {
			// log
			_logger.info("Closing applications");
			// loop the application ids
			for (String appId : applications.getIds()) {
				// loop the versions
				for (String version : applications.getVersions(appId).keySet()) {
					// get the version
					Application application = applications.get(appId, version);
					// close it
					application.close(servletContext);
				}
			}
		}

		// log
		_logger.info("Loading applications");

		// assume no apps to ignore
		List<String> ignoreApps = new ArrayList<String>();
		// find any applications to ignore
		String ignoreAppsString = servletContext.getInitParameter("ignoreApps");
		// if we got any
		if (ignoreAppsString != null && ignoreAppsString.trim().length() > 0) {
			// log
			_logger.info("Ignoring applications " + ignoreAppsString);
			// split them
			String[] ignoreAppsArray = ignoreAppsString.split(",");
			// loop, trim, and add
			for (String ignoreApp : ignoreAppsArray) ignoreApps.add(ignoreApp.trim().toLowerCase());
		}

		// make a new set of applications
		applications = new Applications();

		// the application root folder
		File applicationFolderRoot = new File(servletContext.getRealPath("/") + "/WEB-INF/applications/");

		// loop the children of the application folder
		for (File applicationFolder : applicationFolderRoot.listFiles()) {

			// if this child file is a directory and not in our list of apps to ignore
			if (applicationFolder.isDirectory() && !ignoreApps.contains(applicationFolder.getName().toLowerCase())) {

				// get the list of files in this folder - should be all version folders
				File[] applicationFolders = applicationFolder.listFiles();

				// assume we didn't need to version
				boolean versionCreated = false;

				// if we got some
				if (applicationFolders != null) {

					try {

						// look for an application file in the root of the application folder
						File applicationFile = new File(applicationFolder.getAbsoluteFile() + "/application.xml");

						// set a version for this app (just in case it doesn't have one)
						String version = "1";

						// if it exists here, it's in the wrong (non-versioned) place!
						if (applicationFile.exists()) {

							// create a file for the new version folder
							File versionFolder = new File(applicationFolder + "/" + version);
							// keep appending the version if the folder already exists
							while (versionFolder.exists()) {
								// append .1 to the version 1, 1.1, 1.1.1, etc
								version += ".1";
								versionFolder = new File(applicationFolder + "/" + version);
							}

							// make the dir
							versionFolder.mkdir();
							_logger.info(versionFolder + " created");
							// copy in all files and pages folder
							for (File file : applicationFolders) {
								// copy all files and the pages folder
								if (!file.isDirectory() || (file.isDirectory() && "pages".equals(file.getName()))) {
									// make a desintation file
									File destFile = new File(versionFolder + "/" + file.getName());
									// this is not a version folder itself, copy it to the new version folder
									Files.copyFolder(file, destFile);
									// delete the file or folder
									Files.deleteRecurring(file);
									// log
									_logger.info(file + " moved to " + destFile);
								}

							}
							// record that we created a version
							versionCreated = true;

						}	// application.xml non-versioned check

						try {

							// get the version folders
							File[] versionFolders = applicationFolder.listFiles();
							// get a marshaller
							Marshaller marshaller = RapidHttpServlet.getMarshaller();

							// loop them
							for (File versionFolder : versionFolders) {
								// check is folder
								if (versionFolder.isDirectory()) {
									// look for an application file in the version folder
									applicationFile = new File(versionFolder + "/application.xml");
									// if it exists
									if (applicationFile.exists()) {

										// placeholder for the application we're going to version up or just load
										Application application = null;

										// if we had to create a version for it
										if (versionCreated) {

											// load without resources
											application = Application.load(servletContext, applicationFile, false);

											// set the new version
											application.setVersion(version);

											// re-initialise it without resources (for the security adapter)
											application.initialise(servletContext, false);

											// marshal the updated application object to it's file
											FileOutputStream fos = new FileOutputStream(applicationFile);
											marshaller.marshal(application, fos);
										    fos.close();

											// get a dir for the pages
											File pageDir = new File(versionFolder + "/pages");
											// check it exists
											if (pageDir.exists()) {
												// loop the pages files
												for (File pageFile : pageDir.listFiles()) {
													// read the contents of the file
													String pageContent = Strings.getString(pageFile);
													// replace all old file references
													pageContent = pageContent
														.replace("/" + application.getId() + "/", "/" + application.getId() + "/" + application.getVersion() + "/")
														.replace("~?a=" + application.getId() + "&amp;", "~?a=" + application.getId() + "&amp;" + application.getVersion() + "&amp;");
													// create a file writer
													FileWriter fs = new FileWriter(pageFile);
													// save the changes
													fs.write(pageContent);
													// close the writer
													fs.close();
													_logger.info(pageFile + " updated with new references");
												}
											}
											// make a dir for it's web resources
											File webDir = new File(application.getWebFolder(servletContext));
											webDir.mkdir();
											_logger.info(webDir + " created");
											// loop all the files in the parent
											for (File file : webDir.getParentFile().listFiles()) {
												// check not dir
												if (!file.isDirectory()) {
													// create a destination file for the new location
													File destFile = new File(webDir + "/" + file.getName());
													// copy it to the new destination
													Files.copyFile(file, destFile);
													// delete the file or folder
													file.delete();
													_logger.info(file + " moved to " + destFile);
												}

											}

										}

										// (re)load the application
										application = Application.load(servletContext, applicationFile);

										if(_monitor!=null && _monitor.isAlive(servletContext) && _monitor.isLoggingExceptions()) {
											long versionFolderSize = Files.getSize(versionFolder);
											File backupFolder = new File(versionFolder.getAbsoluteFile()+"/_backups");
											long versionBackupFolderSize = Files.getSize(backupFolder);
											_monitor.createEntry(servletContext, application.getName(), application.getVersion(), "loadApp", versionFolderSize-versionBackupFolderSize, versionFolderSize);
										}

										// put it in our collection
										applications.put(application);

									}

								} // folder check

							} // version folder loop

						} catch (Exception ex) {

							// log the exception
							_logger.error("Error loading app " + applicationFile, ex);

						} // version load catch

					} catch (Exception ex) {

						// log it
						_logger.error("Error creating version folder for app " + applicationFolder, ex);

					} // version folder creation catch

				} // application folders check

			} // application folder check

		} // application folder loop

		// store them in the context
		servletContext.setAttribute("applications", applications);

		_logger.info(applications.size() + " applications loaded");

		return applications.size();

	}

	// Here we loop all of the folders under "workflows" looking for .workflow.xml files
	public static int loadWorkflows(ServletContext servletContext) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException, RapidLoadingException, XPathExpressionException {

		// get any existing workflows
		Workflows workflows = (Workflows) servletContext.getAttribute("workflows");

		// check we got some
		if (workflows != null) {
			// log
			_logger.info("Closing workflows");
			// loop the application ids
			for (String workflowId : workflows.getIds()) {
				// get the workflow
				Workflow workflow = workflows.get(workflowId);
				// close it
				workflow.close(servletContext);
			}
		}

		_logger.info("Loading workflows");

		// make a new set of workflows
		workflows = new Workflows();

		// get their folder
		File folderRoot = new File(servletContext.getRealPath("/") + "/WEB-INF/workflows/");

		// if there is one
		if (folderRoot.isDirectory()) {

			// create a filter for finding .control.xml files
			FilenameFilter xmlFilenameFilter = new FilenameFilter() {
		    	@Override
				public boolean accept(File dir, String name) {
		    		return name.toLowerCase().endsWith(".workflow.xml");
		    	}
		    };

			// get the list of files in this folder - should be all workflows
			File[] files = folderRoot.listFiles(xmlFilenameFilter);

			// if we got some
			if (files != null) {

				// get a marshaller
				Marshaller marshaller = RapidHttpServlet.getMarshaller();

				// loop the files
				for (File file : files) {

					// load this workflow
					Workflow workflow = Workflow.load(servletContext, file);
					// add to collection
					workflows.put(workflow.getId(), workflow);

				}

			} // workflows files check

		} // workflows folder check

		// store them in the context
		servletContext.setAttribute("workflows", workflows);

		_logger.info(workflows.size() + " workflows loaded");

		return workflows.size();

	}

	public static int loadProcesses(ServletContext servletContext) throws Exception {

		// get any existing processes
		List<Process> processes = (List<Process>) servletContext.getAttribute("processes");

		// check we got some
		if (processes != null) {
			// log
			_logger.info("Stopping processes");
			// loop the application ids
			for (Process process : processes) {
				// interrupt the process (which will stop it)
				process.interrupt();
			}
		}

		// log
		_logger.info("Loading processes");

		// make a new set of applications
		processes = new ArrayList<Process>();

		// get the directory in which the process xml files are stored
		File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/processes/");

		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	@Override
			public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".process.xml");
	    	}
	    };

	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/") + "/WEB-INF/schemas/" + "/process.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();

		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) {

			// get a scanner to read the file
			Scanner fileScanner = new Scanner(xmlFile).useDelimiter("\\A");

			// read the xml into a string
			String xml = fileScanner.next();

			// close the scanner (and file)
			fileScanner.close();

			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

			// convert the xml into JSON
			JSONObject jsonProcess = org.json.XML.toJSONObject(xml).getJSONObject("process");

			// get the name from the json
			String name = jsonProcess.getString("name");
			// get the class name from the json
			String className = jsonProcess.getString("class");
			// get the class
			Class classClass = Class.forName(className);
			// check the class extends com.rapid.security.SecurityAdapter
			if (!Classes.extendsClass(classClass, com.rapid.core.Process.class)) throw new Exception(name + " process class " + classClass.getCanonicalName() + " must extend com.rapid.core.Process");
			// get a constructor
			Constructor constructor = classClass.getConstructor(ServletContext.class, JSONObject.class);

			// create a process object from the xml
			Process process = (Process) constructor.newInstance(servletContext, jsonProcess);
			// start it
			process.start();
			// add it to our collection
			processes.add(process);

		}

		// store them in the context
		servletContext.setAttribute("processes", processes);

		// log that we've loaded them
		_logger.info(processes.size() + " process" + (processes.size() == 1 ? "" : "es") + " loaded");

		// return the size
		return processes.size();

	}


	@Override
	public void contextInitialized(ServletContextEvent event) {

		// request windows line breaks to make the files easier to edit (in particular the marshalled .xml files)
		System.setProperty("line.separator", "\r\n");

		// this fixes Illegal reflective access by com.sun.xml.bind.v2.runtime.reflect.opt.Injector, see https://github.com/javaee/jaxb-v2/issues/1197
		System.setProperty("com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize","true");

		// get a reference to the servlet context
		ServletContext servletContext = event.getServletContext();

		// set up logging
		try {

			// get a logger
			_logger = LogManager.getLogger(RapidHttpServlet.class);

			// set the logger and store in servletConext
			servletContext.setAttribute("logger", _logger);

			// log!
			_logger.info("Logger created");

		} catch (Exception e) {

			System.err.println("Error initilising logging : " + e.getMessage());

			e.printStackTrace();
		}

		try {

			// assume no encryptionProvider
			EncryptionProvider encryptionProvider = null;
			// look for the rapid.txt file with the saved password and salt
			File secretsFile = new File(servletContext.getRealPath("/") + "/WEB-INF/security/encryption.txt");
			// if it exists
			if (secretsFile.exists()) {
				// get a file reader
				BufferedReader br = new BufferedReader(new FileReader(secretsFile));
				// read the first line
				String className = br.readLine().trim();
				// close the reader
				br.close();

				// if the class name does not start with #
				if (!className.startsWith("#")) {

					// get the class
					Class classClass = Class.forName(className);
					// get the interfaces
					Class[] classInterfaces = classClass.getInterfaces();
					// assume it doesn't have the interface we want
					boolean gotInterface = false;
					// check we got some
					if (classInterfaces != null) {
						for (Class classInterface : classInterfaces) {
							if (com.rapid.utils.Encryption.EncryptionProvider.class.equals(classInterface)) {
								gotInterface = true;
								break;
							}
						}
					}
					// check the class extends com.rapid.Action
					if (gotInterface) {
						// get the constructors
						Constructor[] classConstructors = classClass.getDeclaredConstructors();
						// check we got some
						if (classConstructors != null) {
							// assume we don't get the parameterless one we need
							Constructor constructor = null;
							// loop them
							for (Constructor classConstructor : classConstructors) {
								// check parameters
								if (classConstructor.getParameterTypes().length == 0) {
									constructor = classConstructor;
									break;
								}
							}
							// check we got what we want
							if (constructor == null) {
								_logger.error("Encryption not initialised : Class in security.txt class must have a parameterless constructor");
							} else {
								// construct the class
								encryptionProvider = (EncryptionProvider) constructor.newInstance();
								// log
								_logger.info("Encryption initialised");
							}
						}
					} else {
						_logger.error("Encryption not initialised : Class in security.txt class must extend com.rapid.utils.Encryption.EncryptionProvider");
					}
				}
			} else {
				_logger.info("Encyption not initialised");
			}

			_monitor.setUpMonitor(servletContext);

			// create the encypted xml adapter (if the file above is not found there no encryption will occur)
			RapidHttpServlet.setEncryptedXmlAdapter(new EncryptedXmlAdapter(encryptionProvider));

			// store away the encryption provider
			RapidHttpServlet.setEncryptionProvider(encryptionProvider);

			// initialise the schema factory (we'll reuse it in the various loaders)
			_schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

			// initialise the list of classes we're going to want in the JAXB context (the loaders will start adding to it)
			_jaxbClasses = new ArrayList<Class>();

			// load the logins first
			_logger.info("Loading logins");

			// load the database drivers first
			loadLogins(servletContext);

			_logger.info("Loading database drivers");

			// load the database drivers
			loadDatabaseDrivers(servletContext);

			_logger.info("Loading connection adapters");

			// load the connection adapters
			loadConnectionAdapters(servletContext);

			_logger.info("Loading security adapters");

			// load the security adapters
			loadSecurityAdapters(servletContext);

			_logger.info("Loading form adapters");

			// load the form adapters
			loadFormAdapters(servletContext);

			_logger.info("Loading actions");

			// load the actions
			loadActions(servletContext);

			_logger.info("Loading themes");

			// load themes
			loadThemes(servletContext);

			_logger.info("Loading controls");

			// load the controls
			loadControls(servletContext);

			// add some classes manually
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.NameRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.MinOccursRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.MaxOccursRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.MaxLengthRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.MinLengthRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.EnumerationRestriction.class);
			_jaxbClasses.add(com.rapid.soa.Webservice.class);
			_jaxbClasses.add(com.rapid.soa.SQLWebservice.class);
			_jaxbClasses.add(com.rapid.soa.JavaWebservice.class);
			_jaxbClasses.add(com.rapid.core.Validation.class);
			_jaxbClasses.add(com.rapid.core.Action.class);
			_jaxbClasses.add(com.rapid.core.Event.class);
			_jaxbClasses.add(com.rapid.core.Style.class);
			_jaxbClasses.add(com.rapid.core.Control.class);
			_jaxbClasses.add(com.rapid.core.Page.class);
			_jaxbClasses.add(com.rapid.core.Application.class);
			_jaxbClasses.add(com.rapid.core.Applications.class);
			_jaxbClasses.add(com.rapid.core.Workflow.class);
			_jaxbClasses.add(com.rapid.core.Workflows.class);
			_jaxbClasses.add(com.rapid.core.Device.class);
			_jaxbClasses.add(com.rapid.core.Device.Devices.class);
			_jaxbClasses.add(com.rapid.core.Email.class);

			// convert arraylist to array
			Class[] classes = _jaxbClasses.toArray(new Class[_jaxbClasses.size()]);
			// re-init the JAXB context to include our injectable classes
			JAXBContext jaxbContext = JAXBContext.newInstance(classes);

			// this logs the JAXB classes
			_logger.trace("JAXB  content : " + jaxbContext.toString());

			// store the jaxb context in RapidHttpServlet
			RapidHttpServlet.setJAXBContext(jaxbContext);

			try {
				// get the extras.min.js file - this is a common cause of bugs on upgrades and will be rebuilt by the first reloaded app
				File extrasMin = new File(servletContext.getRealPath("/") + "/scripts_min/extras.min.js");
				// delete the extras.min.js file if present
				if (extrasMin.exists()) extrasMin.delete();
			} catch (Exception ex) {
				// just log
				_logger.info("Failed to delete extras.min.js", ex);
			}

			// load the devices
			Devices.load(servletContext);

			// load the email settings
			Email.load(servletContext);

			// load the applications!
			loadApplications(servletContext);

			// load the workflows!
			loadWorkflows(servletContext);

			// load the processes
			loadProcesses(servletContext);

			// add some useful global objects

			String localDateFormat = servletContext.getInitParameter("localDateFormat");
			if (localDateFormat == null) localDateFormat = "dd/MM/yyyy";
			servletContext.setAttribute("localDateFormat", localDateFormat);

			String localDateTimeFormat = servletContext.getInitParameter("localDateTimeFormat");
			if (localDateTimeFormat == null) localDateTimeFormat = "dd/MM/yyyy HH:mm a";
			servletContext.setAttribute("localDateTimeFormat", localDateTimeFormat);

			boolean actionCache = Boolean.parseBoolean(servletContext.getInitParameter("actionCache"));
			if (actionCache) servletContext.setAttribute("actionCache", new ActionCache(servletContext));

			// allow calling to https without checking certs (for now)
			SSLContext sc = SSLContext.getInstance("SSL");
			TrustManager[] trustAllCerts = new TrustManager[]{ new Https.TrustAllCerts() };
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		} catch (Exception ex) {

			_logger.error("Error initialising Rapid : " + ex.getMessage());

			ex.printStackTrace();
		}

	}

	@Override
	public void contextDestroyed(ServletContextEvent event){

		// log
		_logger.info("Shutting down...");

		// get the servletContext
		ServletContext servletContext = event.getServletContext();

		// get all processes
		List<Process> processes = (List<Process>) servletContext.getAttribute("processes");
		// if we got some
		if (processes != null) {
			// loop them
			for (Process process : processes) {
				// log
				_logger.info("Stopping process ." + process.getName() + "...");
				// interrupt the process (which stops it)
				process.interrupt();
			}
		}

		// get all of the applications
		Applications applications = (Applications) servletContext.getAttribute("applications");
		// if we got some
		if (applications != null) {
			// loop the application ids
			for (String id : applications.getIds()) {
				// get the application
				Versions versions = applications.getVersions(id);
				// loop the versions of each app
				for (String version : versions.keySet()) {
					// log
					_logger.info("Closing application " + id + "/" + version + "...");
					// get the application
					Application application = applications.get(id, version);
					// have it close any sensitive resources
					application.close(servletContext);
				}
			}
		}

		// sleep for 2 seconds to allow any database connection cleanup to complete
		try { Thread.sleep(2000); } catch (Exception ex) {}

		// This manually deregisters JDBC drivers, which prevents Tomcat from complaining about memory leaks from this class
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                _logger.info(String.format("Deregistering jdbc driver: %s", driver));
            } catch (SQLException e) {
            	_logger.error(String.format("Error deregistering driver %s", driver), e);
            }
        }

        // Thanks to http://stackoverflow.com/questions/11872316/tomcat-guice-jdbc-memory-leak
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
        for (Thread t:threadArray) {
            if (t.getName().contains("Abandoned connection cleanup thread")) {
                synchronized (t) {
                	try {
                		_logger.info("Forcing stop of Abandoned connection cleanup thread");
                		t.stop(); //don't complain, it works
                	} catch (Exception ex) {
                		_logger.info("Error forcing stop of Abandoned connection cleanup thread",ex);
                	}
                }
            }
        }

        // sleep for 1 second to allow any database connection cleanup to complete
     	try { Thread.sleep(1000); } catch (Exception ex) {}

        // last log
		_logger.info("Logger shutdown");

		// shutdown logger
		if (_logger != null) LogManager.shutdown();

	}

}
