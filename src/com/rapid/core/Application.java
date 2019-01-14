/*

Copyright (C) 2017 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.rapid.core.Page.Lock;
import com.rapid.core.Pages.PageHeader;
import com.rapid.data.DatabaseConnection;
import com.rapid.forms.FormAdapter;
import com.rapid.forms.RapidFormAdapter;
import com.rapid.security.RapidSecurityAdapter;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.server.Rapid;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.soa.Webservice;
import com.rapid.utils.Files;
import com.rapid.utils.Minify;
import com.rapid.utils.Strings;
import com.rapid.utils.XML;
import com.rapid.utils.ZipFile;
import com.rapid.utils.ZipFile.ZipSource;
import com.rapid.utils.ZipFile.ZipSources;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Application {

	// the version of this class's xml structure when marshalled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)
	public static final int XML_VERSION = 1;

	// application version statuses
	public static final int STATUS_DEVELOPMENT = 0;
	public static final int STATUS_LIVE = 1;
	public static final int STATUS_MAINTENANCE = 2;

	// the name of the folder in which to store backups
	public static final String BACKUP_FOLDER = "_backups";

	// public static classes

	// an exception class when loading
	public static class RapidLoadingException extends Exception {

		private static final long serialVersionUID = 5010L;

		private String _message;
		private Exception _exception;
		private Throwable _cause;

		public RapidLoadingException(String message, Exception ex) {
			_message = message;
			_cause = ex.getCause();
			if (ex.getMessage() != null) _message += " - " + ex.getMessage();
			if (ex.getCause() != null) _message += " - " + ex.getCause().getMessage();
			// clean up the jaxb suggestion
			if (_message.contains(". Did you mean")) {
				_message = _message.substring(0, _message.indexOf(". Did you mean"));
			}
		}

		@Override
		public String getMessage() {
			return _message;
		}

		@Override
		public StackTraceElement[] getStackTrace() {
			if (_exception == null) {
				return null;
			} else {
				return _exception.getStackTrace();
			}
		}

		@Override
		public Throwable getCause() {
			return _cause;
		}

	}

	// application parameters which will can access in some of our actions
	@XmlType(namespace="http://rapid-is.co.uk/core")
	public static class Value {

		// private instance variables

		private String _text, _value;

		// properties

		public String getText() { return _text; }
		public void setText(String text) { _text = text; }

		public String getValue() { return _value; }
		public void setValue(String value) { _value = value; }

		// constructors

		public Value() {}
		public Value(String text) { _text = text;}
		public Value(String text, String value) {
			_text = text;
			_value = value;
		}

		// overrides

		@Override
		public String toString() {
			if (_value == null) {
				return _text;
			} else {
				return _text + " (" + _value + ")";
			}
		}

	}

	// a value list of which there will be many - we can't extend list as jaxb does not handle list of list
	public static class ValueList  {

		// private instance variables

		private String _name;
		private boolean _usesCodes;
		private List<Value> _values;

		// properties

		public String getName() { return _name; }
		public void setName(String name) { _name = name; }

		public boolean getUsesCodes() { return _usesCodes; }
		public void setUsesCodes(boolean usesCodes) { _usesCodes = usesCodes; }

		public List<Value> getValues() { return _values; }
		public void setValues(List<Value> values) { _values = values; }

		// constructors

		public ValueList() {}
		public ValueList(String name, boolean usesCodes) {
			_name = name;
			_usesCodes = usesCodes;
		}

		// overrides

		@Override
		public String toString() {
			String s = _name;
			if (_values == null) {
				s += "  values is null";
			} else {
				s += "  " + _values.size() + " values";
			}
			return s  + " " + (_usesCodes ? "uses codes" : "no codes");
		}

	}

	// application parameters which will can access in some of our actions
	@XmlType(namespace="http://rapid-is.co.uk/core")
	public static class Parameter {

		// private instance variables

		private String _name, _description, _value;

		// properties

		public String getName() { return _name; }
		public void setName(String name) { _name = name; }

		public String getDescription() { return _description; }
		public void setDescription(String description) { _description = description; }

		public String getValue() { return _value; }
		public void setValue(String value) { _value = value; }

		// constructors
		public Parameter() {
			_name = "";
			_value = "";
		}
		public Parameter(String name, String value) {
			_name = name;
			_value = value;
		}
		public Parameter(String name, String description, String value) {
			_name = name;
			_description = description;
			_value = value;
		}

	}

	// application and page backups
	public static class Backup {

		private String _id, _name, _user, _size;
		private Date _date;

		public String getId() {	return _id;	}

		public String getName() { return _name; }

		public Date getDate() { return _date; }

		public String getUser() { return _user; }

		public String getSize() { return _size;	}

		public Backup(String id, Date date, String user, String size) {
			_id = id;
			_date = date;
			_user = user;
			_size = size;
		}

		public Backup(String id, String name, Date date, String user, String size) {
			_id = id;
			_name = name;
			_date = date;
			_user = user;
			_size = size;
		}

	}

	// the resource dependency is the control or action dependent on the resource
	public static class ResourceDependency {

		// the types that require resources
		public static final int RAPID = 0;
		public static final int ACTION = 1;
		public static final int CONTROL = 2;
		public static final int THEME = 3;

		// private instance variables
		private int _typeClass;
		private String _type;

		// properties
		public int getTypeClass() { return _typeClass; }
		public void setTypeClass(int typeClass) { _typeClass = typeClass; }

		public String getType() { return _type; }
		public void setType(String type) { _type = type; }

		// constructors
		public ResourceDependency() {};

		public ResourceDependency(int typeClass) {
			_typeClass = typeClass;
		}

		public ResourceDependency(int typeClass, String type) {
			_typeClass = typeClass;
			_type = type;
		}

	}

	// the resource is specified in the action, control, or theme xml files
	public static class Resource {

		// these are the types defined in the control and action .xsd files
		public static final int JAVASCRIPT = 1;
		public static final int CSS = 2;
		public static final int JAVASCRIPTFILE = 3;
		public static final int CSSFILE = 4;
		public static final int JAVASCRIPTLINK = 5;  // links are not minified
		public static final int CSSLINK = 6; // links are not minified
		public static final int FILE = 7;

		// source types
		public static final int ACTION = 101;
		public static final int CONTROL = 102;

		// private instance variables
		private int _type;
		private String _name, _content;
		private List<ResourceDependency> _dependancies;

		// properties
		public String getName() { return _name; }
		public void setName(String name) { _name = name; }

		public int getType() { return _type; }
		public void setType(int type) { _type = type; }

		public String getContent() { return _content; }
		public void setContent(String content) { _content = content; }

		public List<ResourceDependency> getDependencies() { return _dependancies; }
		public void setDependencies(List<ResourceDependency> dependancies) {  _dependancies = dependancies; }

		// constructors
		public Resource() {};

		public Resource(int type, String content, int dependencyTypeClass) {
			_type = type;
			_content = content;
			_dependancies = new ArrayList<ResourceDependency>();
			_dependancies.add(new ResourceDependency(dependencyTypeClass));
		}

		public Resource(int type, String content, int dependencyTypeClass, String dependencyType) {
			_type = type;
			_content = content;
			_dependancies = new ArrayList<ResourceDependency>();
			_dependancies.add(new ResourceDependency(dependencyTypeClass, dependencyType));
		}

		public Resource(String name, int type, String content) {
			_name = name;
			_type = type;
			_content = content;
		}

		// methods
		public void addDependency(ResourceDependency dependency) {
			if (_dependancies == null) _dependancies = new ArrayList<ResourceDependency>();
			_dependancies.add(dependency);
		}

		// check for dependencies on a single type (usually Rapid)
		public boolean hasDependency(int typeClass) {
			// assume no dependency
			boolean hasDependency = false;
			// if there are some to check
			if (_dependancies != null) {
				// loop them
				for (ResourceDependency dependency : _dependancies) {
					// check and return immediately
					if (typeClass == dependency.getTypeClass()) return true;
				}
			}
			return hasDependency;
		}

		// check for dependencies for a type class and list of types
		public boolean hasDependency(int typeClass, List<String> types) {
			// assume no dependency
			boolean hasDependency = false;
			// if there are some to check
			if (types != null && _dependancies != null) {
				// loop them
				for (ResourceDependency dependency : _dependancies) {
					// check and return immediately
					if (typeClass == dependency.getTypeClass() && types.contains(dependency.getType())) return true;
				}
			}
			return hasDependency;
		}

	}

	// some overridden methods for the Resource collection
	public static class Resources extends ArrayList<Resource> {

		private static final long serialVersionUID = 1025L;

		@Override
		public boolean contains(Object o) {
			if (o.getClass() == Resource.class) {
				Resource r = (Resource) o;
				for (Resource resource : this) {
					if (r.getType() == resource.getType() && r.getContent() == resource.getContent()) return true;
				}
			}
			return false;
		}

		@Override
		public boolean add(Resource resource) {
			if (contains(resource)) {
				return false;
			} else {
				return super.add(resource);
			}
		}

		@Override
		public void add(int index, Resource resource) {
			if (!contains(resource)) {
				super.add(index, resource);
			}
		}

		public void add(int type, String content, int dependencyTypeClass, String dependencyType) {
			// assume we can't find the resource
			Resource resource = null;
			// loop all resources
			for (Resource r : this) {
				// if we can match the type and content
				if (r.getType() == type && r.getContent() == content) {
					// retain this resource
					resource = r;
					// we're done with this loop
					break;
				}
			}
			// check for an existing resource
			if (resource == null) {
				// didn't find one so create
				resource = new Resource(type, content, dependencyTypeClass, dependencyType);
				// add to this collection
				this.add(resource);
				// if this is a theme resource
				if (dependencyTypeClass == ResourceDependency.THEME) {
					// add rapid as a dependency so it appears in all pages
					resource.addDependency(new ResourceDependency(ResourceDependency.RAPID, "rapid"));
				}
			} else {
				// add the dependency to the resource
				resource.addDependency(new ResourceDependency(dependencyTypeClass, dependencyType));
			}

		}

	}

	// instance variables
	private int _xmlVersion, _status, _applicationBackupsMaxSize, _pageBackupsMaxSize;
	private String _id, _version, _name, _title, _description, _startPageId, _themeType, _styles, _statusBarColour, _statusBarHighlightColour, _statusBarTextColour, _statusBarIconColour, _functions, _securityAdapterType, _storePasswordDuration, _formAdapterType, _createdBy, _modifiedBy, _formEmailFrom, _formEmailTo, _formEmailAttachmentType, _formFileType, _formFilePath, _formFileUserName, _formFilePassword, _formWebserviceURL, _formWebserviceType, _formWebserviceSOAPAction;
	private boolean _isForm, _pageNameIds, _showConrolIds, _showActionIds, _deviceSecurity, _formEmail, _formFile, _formWebservice;
	private Date _createdDate, _modifiedDate;
	private Map<String,Integer> _pageOrders;
	private SecurityAdapter _securityAdapter;
	private FormAdapter _formAdapter;
	private List<DatabaseConnection> _databaseConnections;
	private List<Webservice> _webservices;
	private List<ValueList> _valueLists;
	private List<Parameter> _parameters;
	private List<String> _controlTypes, _actionTypes;
	private Pages _pages;
	private Resources _appResources, _resources;
	private List<String> _styleClasses;
	private List<String> _pageVariables;

	// properties

	// the XML version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }

	// the id uniquely identifies the page (it is produced by taking all unsafe characters out of the name)
	public String getId() { return _id; }
	public void setId(String id) { _id = id; }

	// the version is used for Rapid Mobile's offline files to work with different published versions of the app
	public String getVersion() { return _version; }
	public void setVersion(String version) { _version = version; }

	// the status is used for Rapid Mobile's offline files to work with different published versions of the app
	public int getStatus() { return _status; }
	public void setStatus(int status) { _status = status; }

	// this is expected to be short name, probably even a code that is used by users to simply identify pages (also becomes the file name)
	public String getName() { return _name; }
	public void setName(String name) { _name = name; }

	// this is a user-friendly, long title
	public String getTitle() { return _title; }
	public void setTitle(String title) { _title = title; }

	// an even longer description of what this page does
	public String getDescription() { return _description; }
	public void setDescription(String description) { _description = description; }

	// the user that created this application
	public String getCreatedBy() { return _createdBy; }
	public void setCreatedBy(String createdBy) { _createdBy = createdBy; }

	// the date this application was created
	public Date getCreatedDate() { return _createdDate; }
	public void setCreatedDate(Date createdDate) { _createdDate = createdDate; }

	// the last user to save this application
	public String getModifiedBy() { return _modifiedBy; }
	public void setModifiedBy(String modifiedBy) { _modifiedBy = modifiedBy; }

	// the date this application was last saved
	public Date getModifiedDate() { return _modifiedDate; }
	public void setModifiedDate(Date modifiedDate) { _modifiedDate = modifiedDate; }

	// the page orders if they are overridden
	public Map<String,Integer> getPageOrders() { return _pageOrders; }
	public void setPageOrders(Map<String,Integer> pageOrders) { _pageOrders = pageOrders; _pages.clearCachedOrder(); }

	// whether form settings checkbox has been ticked or not
	public boolean getIsForm() { return _isForm; }
	public void setIsForm(boolean isForm) { _isForm = isForm; }

	// whether page id's are sequential numbers (the traditional way) or the page name (for more complex apps with page naming conventions)
	public boolean getPageNameIds() { return _pageNameIds; }
	public void setPageNameIds(boolean pageNameIds) { _pageNameIds = pageNameIds; }

	// whether control ids should be shown when designing this app
	public boolean getShowControlIds() { return _showConrolIds; }
	public void setShowControlIds(boolean showConrolIds) { _showConrolIds = showConrolIds; }

	// whether action ids should be shown when designing this app
	public boolean getShowActionIds() { return _showActionIds; }
	public void setShowActionIds(boolean showActionIds) { _showActionIds = showActionIds; }

	// the application start page which will be supplied if no page is explicitly provided
	public String getStartPageId() { return _startPageId; }
	public void setStartPageId(String startPageId) { _startPageId = startPageId; }

	// whether email form checkbox has been ticked or not
	public boolean getFormEmail() { return _formEmail; }
	public void setFormEmail(boolean formEmail) { _formEmail = formEmail; }

	// who the email is sent from
	public String getFormEmailFrom() { return _formEmailFrom; }
	public void setFormEmailFrom(String formEmailFrom) { _formEmailFrom = formEmailFrom; }

	// email address to send to
	public String getFormEmailTo() { return _formEmailTo; }
	public void setFormEmailTo(String formEmailTo) { _formEmailTo = formEmailTo; }

	// whether to set the email attachment type to none, CSV or PDF
	public String getFormEmailAttachmentType() { return _formEmailAttachmentType; }
	public void setFormEmailAttachmentType(String formEmailAttachmentType) { _formEmailAttachmentType = formEmailAttachmentType; }

	// whether the form details file checkbox has been ticked or not
	public boolean getFormFile() { return _formFile; }
	public void setFormFile(boolean formFile) { _formFile = formFile; }

	// whether to send the form file as a CSV or PDF
	public String getFormFileType() { return _formFileType; }
	public void setFormFileType(String formFileType) { _formFileType = formFileType; }

	// file path of the form
	public String getFormFilePath() { return _formFilePath; }
	public void setFormFilePath(String formFilePath) { _formFilePath = formFilePath; }

	// username of the form
	public String getFormFileUserName() { return _formFileUserName; }
	public void setFormFileUserName(String formFileUserName) { _formFileUserName = formFileUserName; }

	// password of the form
	public String getFormFilePassword() { return _formFilePassword; }
	public void setFormFilePassword(String formFilePassword) { _formFilePassword = formFilePassword; }

	// whether the checkbox for called a webservice is ticked or not
	public boolean getFormWebservice() { return _formWebservice; }
	public void setFormWebservice(boolean formWebservice) { _formWebservice = formWebservice; }

	// webservice url
	public String getFormWebserviceURL() { return _formWebserviceURL; }
	public void setFormWebserviceURL(String formWebserviceURL) { _formWebserviceURL = formWebserviceURL; }

	// webservice type of either SOAP, JSON or Restful XML
	public String getFormWebserviceType() { return _formWebserviceType; }
	public void setFormWebserviceType(String formWebserviceType) { _formWebserviceType = formWebserviceType; }

	// what the SOAP action is set to
	public String getFormWebserviceSOAPAction() { return _formWebserviceSOAPAction; }
	public void setFormWebserviceSOAPAction(String formWebserviceSOAPAction) { _formWebserviceSOAPAction = formWebserviceSOAPAction; }

	// the CSS theme type which we'll look up and add to the rapid.css file
	public String getThemeType() { return _themeType; }
	public void setThemeType(String themeType) { _themeType = themeType; }

	// the CSS styles added to the generated application rapid.css file
	public String getStyles() { return _styles; }
	public void setStyles(String styles) { _styles = styles; }

	// colour of the status bar in Rapid Mobile
	public String getStatusBarColour() { return _statusBarColour; }
	public void setStatusBarColour(String statusBarColour) { _statusBarColour =  statusBarColour; }

	// colour of the status bar highlight in Rapid Mobile
	public String getStatusBarHighlightColour() { return _statusBarHighlightColour; }
	public void setStatusBarHighlightColour(String statusBarHighlightColour) { _statusBarHighlightColour =  statusBarHighlightColour; }

	// colour of the status bar text in Rapid Mobile
	public String getStatusBarTextColour() { return _statusBarTextColour; }
	public void setStatusBarTextColour(String statusBarTextColour) { _statusBarTextColour =  statusBarTextColour; }

	// colour of icons in Rapid Mobile
	public String getStatusBarIconColour() { return _statusBarIconColour; }
	public void setStatusBarIconColour(String statusBarIconColour) { _statusBarIconColour =  statusBarIconColour; }

	// the JavaScript functions added to the generated application rapid.js file (this has been replaced by application resources)
	public String getFunctions() { return _functions; }
	public void setFunctions(String functions) { _functions = functions; }

	// a collection of database connections used via the connection adapter class to produce database connections
	public List<DatabaseConnection> getDatabaseConnections() { return _databaseConnections; }
	public void setDatabaseConnections(List<DatabaseConnection> databaseConnections) { _databaseConnections = databaseConnections; }

	// a collection of webservices for this application
	public List<Webservice> getWebservices() { return _webservices; }
	public void setWebservices(List<Webservice> webservices) { _webservices = webservices; }

	// the type name of the security adapter this application uses
	public String getSecurityAdapterType() { return _securityAdapterType; }
	public void setSecurityAdapterType(String securityAdapterType) { _securityAdapterType = securityAdapterType; }

	// whether to apply device security to this application
	public boolean getDeviceSecurity() { return _deviceSecurity; }
	public void setDeviceSecurity(boolean deviceSecurity) { _deviceSecurity = deviceSecurity; }

	// whether to not retain the password in Rapid Mobile - note that it's in the negative
	public String getStorePasswordDuration() { return _storePasswordDuration; }
	public void setStorePasswordDuration(String storePasswordDuration) { _storePasswordDuration = storePasswordDuration; }

	// the type name of the form adapter this application uses (if any)
	public String getFormAdapterType() { return _formAdapterType; }
	public void setFormAdapterType(String formAdapterType) { _formAdapterType = formAdapterType; }

	// a collection of parameters for this application
	public List<ValueList> getValueLists() { return _valueLists; }
	public void setValueLists(List<ValueList> valueLists) { _valueLists = valueLists; }

	// a collection of parameters for this application
	public List<Parameter> getParameters() { return _parameters; }
	public void setParameters(List<Parameter> parameters) { _parameters = parameters; }

	// control types used in this application
	public List<String> getControlTypes() { return _controlTypes; }
	public void setControlTypes(List<String> controlTypes) { _controlTypes = controlTypes; }

	// action types used in this application
	public List<String> getActionTypes() { return _actionTypes; }
	public void setActionTypes(List<String> actionTypes) { _actionTypes = actionTypes; }

	// number of application backups to keep
	public int getApplicationBackupsMaxSize() { return _applicationBackupsMaxSize; }
	public void setApplicationBackupMaxSize(int applicationBackupsMaxSize) { _applicationBackupsMaxSize = applicationBackupsMaxSize; }

	// number of page backups to keep
	public int getPageBackupsMaxSize() { return _pageBackupsMaxSize; }
	public void setPageBackupsMaxSize(int pageBackupsMaxSize) { _pageBackupsMaxSize = pageBackupsMaxSize; }

	// these are app resources which are marshalled to the application.xml file and add to all pages
	public Resources getAppResources() { return _appResources; }
	public void setAppResources(Resources appResources) { _appResources = appResources; }

	// constructors

	public Application() throws ParserConfigurationException, XPathExpressionException, RapidLoadingException, SAXException, IOException {
		_xmlVersion = XML_VERSION;
		_pages = new Pages(this);
		_pageOrders = new HashMap<String,Integer>();
		_statusBarColour = "#aaaaaa";
		_statusBarHighlightColour = "#999999";
		_statusBarTextColour = "#ffffff";
		_statusBarIconColour = "white";
		_databaseConnections = new ArrayList<DatabaseConnection>();
		_webservices = new ArrayList<Webservice>();
		_parameters = new ArrayList<Parameter>();
		_applicationBackupsMaxSize = 3;
		_pageBackupsMaxSize = 3;
	};

	// instance methods

	// this is where the application configuration will be stored
	public String getConfigFolder(ServletContext servletContext) {
		return getConfigFolder(servletContext, _id, _version);
	}

	// this is the web folder with the full system path
	public String getWebFolder(ServletContext servletContext) {
		return getWebFolder(servletContext, _id, _version);
	}

	// this is the backup folder
	public String getBackupFolder(ServletContext servletContext, boolean allVersions) {
		return getBackupFolder(servletContext, _id, _version, allVersions);
	}

	// this replaces [[xxx]] in a string where xxx is a known system or application parameter
	public String insertParameters(ServletContext servletContext, String string) {
		// check for non-null
		if (string != null) {
			// get pos of [[
			int pos = string.indexOf("[[");
			// check string contains [[
			if (pos > -1) {
				// if it has ]] thereafter
				if (string.indexOf("]]") > pos) {
					// webfolder is the client web facing resources
					if (string.contains("[[webfolder]]")) string = string.replace("[[webfolder]]", getWebFolder(this));
					// appfolder and configfolder are the hidden server app resources
					if (string.contains("[[appfolder]]")) string = string.replace("[[appfolder]]", getConfigFolder(servletContext, _id, _version));
					if (string.contains("[[configfolder]]")) string = string.replace("[[configfolder]]", getConfigFolder(servletContext, _id, _version));
					// root folder is WEB-INF
					if (string.contains("[[rootfolder]]")) string = string.replace("[[rootfolder]]", servletContext.getRealPath("/") + "/WEB-INF/");
					// if we have parameters
					if (_parameters != null) {
						// loop them
						for (Parameter parameter : _parameters) {
							// define the match string
							String matchString = "[[" + parameter.getName() + "]]";
							// if the match string is present replace it with the value
							if (string.contains(matchString)) string = string.replace(matchString, parameter.getValue());
						}
					}
				}
			}
		}
		// return it
		return string;
	}

	// used by the function below if no specified start page or if the specified one can't be found
	private Page getStartPageUsingOrder(ServletContext servletContext) throws RapidLoadingException {
		if (_pages.size() > 0) {
			// get the id of the first page alphabetically
			String firstPageId = _pages.getSortedPages().get(0).getId();
			// get this page
			return _pages.getPage(servletContext, firstPageId);
		} else {
			return null;
		}
	}

	// get the first page the users want to see (set in Rapid Admin on first save)
	public Page getStartPage(ServletContext servletContext) throws RapidLoadingException {
		// retain an instance to the page we are about to return
		Page startPage = null;
		// check whether we have a _startPageId set
		if (_startPageId == null) {
			// get the first page by order
			startPage = getStartPageUsingOrder(servletContext);
		} else {
			// get the start page from the id
			startPage = _pages.getPage(servletContext, _startPageId);
			// if it's null the start page has probably been deleted without updating the application object
			if (startPage == null) {
				// set the start page
				startPage = getStartPageUsingOrder(servletContext);
				// try and update the _startPageId again if we got one
				if (startPage != null) _startPageId = startPage.getId();
			}
		}
		// return
		return startPage;
	}

	// loop the collection of database connections looking for a named one
	public DatabaseConnection getDatabaseConnection(String name) {
		if (_databaseConnections != null) {
			for (DatabaseConnection databaseConnection : _databaseConnections) if (name.equals(databaseConnection.getName())) return databaseConnection;
		}
		return null;
	}
	// add a single database connection
	public void addDatabaseConnection(DatabaseConnection databaseConnection) { _databaseConnections.add(databaseConnection); }
	// remove a single database connection
	public void removeDatabaseConnection(String name) {
		DatabaseConnection databaseConnection = getDatabaseConnection(name);
		if (databaseConnection != null) _databaseConnections.remove(databaseConnection);
	}

	// get a parameter value by name
	public String getParameterValue(String parameterName) {
		// if there are parameters
		if (_parameters != null) {
			// loop them
			for (Parameter parameter : _parameters) {
				// check the name and return if match
				if (parameterName.equals(parameter.getName())) return parameter.getValue();
			}
		}
		// return null if nothing found
		return null;
	}

	// get pages
	public Pages getPages() { return _pages; }

	// get a control by it's id
	public Control getControl(ServletContext servletContext, String id) {
		Control control = null;
		// check we have pages and an id
		if (_pages != null && id != null) {
			// if the id is not a zero length string
			if (id.length() > 0) {
				// split the id parts on the underscore
				String[] idParts = id.split("_");
				// get the first part into a page id
				String pageId = idParts[0];
				try {
					// get the specified page
					Page page = _pages.getPage(servletContext, pageId);
					// check we got a page
					if (page == null) {
						// no page matching this control id prefix so just loop all pages
						for (String loopPageId : _pages.getPageIds()) {
							// fetch this page
							page = _pages.getPage(servletContext, loopPageId);
							// look for the control
							control = page.getControl(id);
							// if we found it return it!
							if (control != null) return control;
						}
					} else {
						// look for the control in the page according to its prefix
						control = page.getControl(id);
						// return it if we found it!
						if (control != null) return control;
					}
				} catch (Exception ex) {
					// get the logger
					Logger logger = (Logger) servletContext.getAttribute("logger");
					// log this exception
					logger.error("Error getting control from application", ex);
				}
			} // id length > 0 check
		} // id and page non-null check
		// couldn't find it either in specified page, or all pages
		return null;
	}

	// get an action by it's id
	public Action getAction(ServletContext servletContext, String id) {
		Action action = null;
		// check we have pages and an id
		if (_pages != null && id != null) {
			// if the id is not a zero length string
			if (id.length() > 0) {
				// split the id parts on the underscore
				String[] idParts = id.split("_");
				// get the first part into a page id
				String pageId = idParts[0];
				try {
					// get the specified page
					Page page = _pages.getPage(servletContext, pageId);
					// check we got a page
					if (page == null) {
						// no page matching this control id prefix so just loop all pages
						for (String loopPageId : _pages.getPageIds()) {
							// fetch this page
							page = _pages.getPage(servletContext, loopPageId);
							// look for the control
							action = page.getAction(id);
							// if we found it return it!
							if (action != null) return action;
						}
					} else {
						// look for the control in the page according to its prefix
						action = page.getAction(id);
						// return it if we found it!
						if (action != null) return action;
					}
				} catch (Exception ex) {
					// get the logger
					Logger logger = (Logger) servletContext.getAttribute("logger");
					// log this exception
					logger.error("Error getting action from application", ex);
				}
			} // id length > 0 check
		} // id and page non-null check
		// couldn't find it either in specified page, or all pages
		return null;
	}

	// get a webservice by it's id
	public Webservice getWebserviceById(String id) {
		if (_webservices != null) {
			for (Webservice webservice : _webservices) {
				if (id.equals(webservice.getId())) return webservice;
			}
		}
		return null;
	}

	// get a webservice by it's name
	public Webservice getWebserviceByName(String name) {
		if (_webservices != null) {
			for (Webservice webservice : _webservices) {
				if (name.equals(webservice.getName())) return webservice;
			}
		}
		return null;
	}

	// return the list of style classes
	public List<String> getStyleClasses() {
		return _styleClasses;
	}

	// return the list of page variables used in the application
	public List<String> getPageVariables(ServletContext servletContext) throws RapidLoadingException {
		// if not set yet
		if (_pageVariables == null) {
			// make the collection of pages
			_pageVariables = new ArrayList<String>();
			// loop the pages
			for (String pageId : _pages.getPageIds()) {
				// get the page
				Page page = _pages.getPage(servletContext, pageId);
				// get any variables
				List<String> pageVariables = page.getSessionVariables();
				// if we got some
				if (pageVariables != null) {
					// loop them
					for (String pageVariable : pageVariables) {
						// add if we don't have already
						if (!_pageVariables.contains(pageVariable)) _pageVariables.add(pageVariable);
					}
				}
			}
		}
		return _pageVariables;
	}
	// different name from above to stop jaxb writing it to xml
	public void emptyPageVariables() {
		_pageVariables = null;
	}

	// an instance of the security adapter used by this object
	public SecurityAdapter getSecurityAdapter() { return _securityAdapter; }
	// set the security to a given type
	public void setSecurityAdapter(ServletContext servletContext, String securityAdapterType) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException {
		// set the security adaper type from the incoming parameter
		_securityAdapterType = securityAdapterType;
		// if it was null update to rapid
		if (_securityAdapterType == null) _securityAdapterType = "rapid";
		// get a map of the security adapter constructors
		HashMap<String,Constructor> constructors = (HashMap<String, Constructor>) servletContext.getAttribute("securityConstructors");
		// get the constructor for our type
		Constructor<SecurityAdapter> constructor = constructors.get(_securityAdapterType);
		// if we couldn't find a constructor for the specified type
		if (constructor == null) {
			// set the type to rapid
			_securityAdapterType = "rapid";
			// instantiate a rapid security adapter
			_securityAdapter = new RapidSecurityAdapter(servletContext, this);
		} else {
			// instantiate the specified security adapter
			_securityAdapter = constructor.newInstance(servletContext, this);
		}
	}

	// an instance of the form adapter used by this object
	public FormAdapter getFormAdapter() { return _formAdapter; }
	// set the security to a given type
	public void setFormAdapter(ServletContext servletContext, String formAdapterType) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException {
		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");
		// if there is a current one
		if (_formAdapter != null) {
			try {
				// close it
				_formAdapter.close();
			} catch (Exception ex) {
				// log any error
				logger.error("Error closing form adapter for " + _id + "/" + _version, ex);
			}
		}
		// set the security adaper type from the incoming parameter
		_formAdapterType = formAdapterType;
		// if it was null
		if (_formAdapterType == null || "".equals(_formAdapterType)) {
			// clear the current one
			_formAdapter = null;
		} else {
			// get a map of the form adapter constructors
			HashMap<String,Constructor> constructors = (HashMap<String, Constructor>) servletContext.getAttribute("formConstructors");
			// get the constructor for our type
			Constructor<FormAdapter> constructor = constructors.get(_formAdapterType);
			// if we got this constructor
			if (constructor == null) {
				// log
				logger.trace("Instantiating Rapid form adapter for " + _id + "/" + _version);
				// revert to rapid form adapter
				_formAdapter = new RapidFormAdapter(servletContext, this, "rapid");
			} else {
				// log
				logger.trace("Instantiating form adapter " + _formAdapterType + " for " + _id + "/" + _version);
				// instantiate the specified form adapter
				_formAdapter = constructor.newInstance(servletContext, this, _formAdapterType);
			}
		}
	}

	// this is a list of elements to go in the head section of the page for any resources the applications controls or actions may require
	public List<Resource> getResources() { return _resources; }

	public Theme getTheme(ServletContext servletContext) {
		// check the theme type
    	if (_themeType != null) {
    		// get the themes
    		List<Theme> themes =  (List<Theme>) servletContext.getAttribute("themes");
    		// check we got some
    		if (themes != null) {
    			// loop them
    			for (Theme theme : themes) {
    				// check type
    				if (_themeType.equals(theme.getType())) return theme;
    			}
    		}
    	}
    	// not found
    	return null;
	}

	// scan the css for classes
	private List<String> scanStyleClasses(String css, List<String> classes) {

		// only if we got something we can use
		if (css != null) {

			// find the first .
			int startPos = css.indexOf(".");

			// if we got one
			while (startPos >= 0) {

				// find the start of the next style
				int styleStartPos = css.indexOf("{", startPos);

				// find the end of the next style
				int styleEndPos = css.indexOf("}", startPos);

				// only if we are in front of a completed style and there is a . starting before the style
				if (styleStartPos < styleEndPos && startPos < styleStartPos) {

					// find the end of the class style target by the first brace
					int endPos = styleStartPos;
					// if it works out
					if (endPos > startPos) {
						// fetch the class without the . and any trailing space
						String styleClass = css.substring(startPos + 1, endPos).trim();
						// remove any closing brackets
						if (styleClass.indexOf(")") > 0) styleClass = styleClass.substring(0, styleClass.indexOf(")"));
						// remove any colons
						if (styleClass.indexOf(":") > 0) styleClass = styleClass.substring(0, styleClass.indexOf(":"));
						// remove anything after a space
						if (styleClass.indexOf(" ") > 0) styleClass = styleClass.substring(0, styleClass.indexOf(" "));
						// check we don't have it already and add it if ok - i.e. contains no other .'s (this is common for urls getting picked up here)
						if (!classes.contains(styleClass) && styleClass.indexOf(".") < 0) classes.add(styleClass);
					}

				}

				// exit here if styleEndPos is going to cause problems
				if (styleEndPos == -1) break;

				// find the next .
				startPos = css.indexOf(".", styleEndPos);

			}

		}

		// sort the classes into alphabetical order
		Collections.sort(classes);

		return classes;

	}

	// this adds resources from either a control or action, they are added to the resources collection for printing in the top of each page if they are files, or amended to the application .js or .css files
	private void addResources(JSONObject jsonObject, String jsonObjectType, StringBuilder js, StringBuilder css) throws JSONException {

		// look for a resources object
		JSONObject jsonResourcesObject = jsonObject.optJSONObject("resources");

		// if we got one
		if (jsonResourcesObject != null) {

			// get a name for the jsonObject
			String name = jsonObject.optString("name");

			// get the dependency type
			String dependencyType = jsonObject.getString("type");

			// use the override below
			addResources(jsonResourcesObject, jsonObjectType, name, dependencyType, js, css);

		}

	}

	// this adds resources from either a control or action, they are added to the resources collection for printing in the top of each page if they are files, or amended to the application .js or .css files
	private void addResources(JSONObject jsonResourcesObject, String jsonObjectType, String name, String dependencyType, StringBuilder js, StringBuilder css) throws JSONException {

		// if we got one
		if (jsonResourcesObject != null) {

			// get the resource into an array (which is how the jaxb is passed the json)
			JSONArray jsonResources = jsonResourcesObject.optJSONArray("resource");

			// if we didn't get an array this is probably a single item collection stored as an object
			if (jsonResources == null) {
				// get the resource as an object
				JSONObject jsonResource = jsonResourcesObject.optJSONObject("resource");
				// if we got something
				if (jsonResource != null) {
					// make a proper array
					jsonResources = new JSONArray();
					// add the item
					jsonResources.put(jsonResource);
				}
			}

			// check we have something
			if (jsonResources != null) {

				// assume this is a rapid resource
				int dependencyTypeClass = ResourceDependency.RAPID;
				// update if action
				if ("action".equals(jsonObjectType)) dependencyTypeClass = ResourceDependency.ACTION;
				// update if control
				if ("control".equals(jsonObjectType)) dependencyTypeClass = ResourceDependency.CONTROL;
				// update if theme
				if ("theme".equals(jsonObjectType)) dependencyTypeClass = ResourceDependency.THEME;

				// loop them
				for (int j = 0; j < jsonResources.length(); j++) {

					// get a reference to this resource
					JSONObject jsonResource = jsonResources.getJSONObject(j);
					// get the type
					String resourceType = jsonResource.getString("type");
					// get the contens which is either a path, or the real stuff
					String resourceContents = jsonResource.getString("contents").trim();
					// define the comments name as the name
					String commentsName = name;
					// safety check
					if (commentsName == null) commentsName = "";
					// add the json object type and resource type
					commentsName += " " + jsonObjectType + " " + resourceType;

					// add as resources if they're files, or append the string builders (the app .js and .css are added as resources at the end)
					if ("javascript".equals(resourceType)) {
						js.append("\n/* " + commentsName + " resource JavaScript */\n\n" + resourceContents + "\n");
					} else if ("css".equals(resourceType)) {
						css.append("\n/* " + commentsName + " resource styles */\n\n" + resourceContents + "\n");
					} else if ("javascriptFile".equals(resourceType)) {
						_resources.add(Resource.JAVASCRIPTFILE, resourceContents, dependencyTypeClass, dependencyType);
					} else if ("cssFile".equals(resourceType)) {
						_resources.add(Resource.CSSFILE, resourceContents, dependencyTypeClass, dependencyType);
					} else if ("javascriptLink".equals(resourceType)) {
						_resources.add(Resource.JAVASCRIPTLINK, resourceContents, dependencyTypeClass, dependencyType);
					} else if ("cssLink".equals(resourceType)) {
						_resources.add(Resource.CSSLINK, resourceContents, dependencyTypeClass, dependencyType);
					} else if ("file".equals(resourceType)) {
						_resources.add(Resource.FILE, resourceContents, dependencyTypeClass, dependencyType);
					}

				} // resource loop

			} // json resource check

		} // json resources check

	}

	// this function initialises the application when its first loaded, initialises the security adapter and builds the rapid.js and rapid.css files
	public void initialise(ServletContext servletContext, boolean createResources) throws JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException, IOException {

		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");

		// trace log that we're initialising
		logger.trace("Initialising application " + _name + "/" + _version);

		// initialise the security adapter
		setSecurityAdapter(servletContext, _securityAdapterType);

		// initialise the form adapter
		setFormAdapter(servletContext, _formAdapterType);

		// initialise the resource includes collection
		_resources = new Resources();

		// if there is any app JavaScript functions - this is for backwards compatibility as _functions have been moved to JavaScript resources
		if (_functions != null) {
			// initialise app resources if need be
			if (_appResources == null) _appResources = new Resources();
			// add _functions as JavaScript resource to top of list
			_appResources.add(0, new Resource("Application functions", 1, _functions));
			// remove the _functions
			_functions = null;
		}

		// if the created date is null set to today
		if (_createdDate == null) _createdDate = new Date();

		// when importing an application we need to initialise but don't want the resource folders made in the old applications name
		if (createResources) {

			// get the jsonControls
			JSONArray jsonControls = (JSONArray) servletContext.getAttribute("jsonControls");

			// get the jsonActions
			JSONArray jsonActions = (JSONArray) servletContext.getAttribute("jsonActions");

			// string builders for the different sections in our rapid.js file
			StringBuilder resourceJS = new StringBuilder();
			StringBuilder initJS = new StringBuilder();
			StringBuilder dataJS = new StringBuilder();
			StringBuilder actionJS = new StringBuilder();

			// string builder for our rapid.css file
			StringBuilder resourceCSS = new StringBuilder();

			// check controls
			if (jsonControls != null) {

				// check control types
				if (_controlTypes != null) {

					// remove the page control (if it's there)
					_controlTypes.remove("page");
					// add it to the top of the list
					_controlTypes.add(0, "page");

					// collection of dependent controls that need adding
					ArrayList<String> dependentControls = new ArrayList<String>();

					// loop control types used by this application
					for (String controlType : _controlTypes) {

						// loop all available controls
			    		for (int i = 0; i < jsonControls.length(); i++) {

			    			// get the control
			    			JSONObject jsonControl = jsonControls.getJSONObject(i);

			    			// check if we're on the type we need
			    			if (controlType.equals(jsonControl.optString("type"))) {

			    				// look for any dependent control types
			    				JSONObject dependantTypes = jsonControl.optJSONObject("dependentTypes");
			    				// if we got some
			    				if (dependantTypes != null) {
			    					// look for an array
			    					JSONArray dependantTypesArray = dependantTypes.optJSONArray("dependentType");
			    					// if we got one
			    					if (dependantTypesArray != null) {
			    						// loop the array
			    						for (int j = 0; j < dependantTypesArray.length(); j++) {
			    							String dependantType = dependantTypesArray.getString(j);
				    						if (!_controlTypes.contains(dependantType) && !dependentControls.contains(dependantType)) dependentControls.add(dependantType);
			    						}
			    					} else {
			    						// just use the object
			    						String dependantType = dependantTypes.getString("dependentType");
			    						if (!_controlTypes.contains(dependantType) && !dependentControls.contains(dependantType)) dependentControls.add(dependantType);
			    					}
			    				}

			    				// look for any dependent action types
			    				JSONObject dependantActionTypes = jsonControl.optJSONObject("dependentActionTypes");
			    				// if we got some
			    				if (dependantActionTypes != null) {
			    					// look for an array
			    					JSONArray dependantActionTypesArray = dependantActionTypes.optJSONArray("dependentActionType");
			    					// if we got one
			    					if (dependantActionTypesArray == null) {
			    						// just use the object
			    						String dependantType = dependantActionTypes.getString("dependentActionType");
			    						if (!_actionTypes.contains(dependantType) && !dependentControls.contains(dependantType)) dependentControls.add(dependantType);
			    					} else {
			    						// loop the array
			    						for (int j = 0; j < dependantActionTypesArray.length(); j++) {
			    							String dependantType = dependantActionTypesArray.getString(j);
				    						if (!_actionTypes.contains(dependantType) && !dependentControls.contains(dependantType)) dependentControls.add(dependantType);
			    						}
			    					}
			    				}

			    				// we're done
			    				break;
			    			} // available control type check

			    		} // available control types loop

					} // application control types loop

					// now add all of the dependent controls
					_controlTypes.addAll(dependentControls);

					// loop control types used by this application
					for (String controlType : _controlTypes) {

						// loop all available controls
			    		for (int i = 0; i < jsonControls.length(); i++) {

			    			// get the control
			    			JSONObject jsonControl = jsonControls.getJSONObject(i);

			    			// check if we're on the type we need
			    			if (controlType.equals(jsonControl.optString("type"))) {

			    				// add any resources (actions can have them too)
			    				addResources(jsonControl, "control", resourceJS, resourceCSS);

			    				// get any initJavaScript
				    			String js = jsonControl.optString("initJavaScript", "");
				    			// check we got some
				    			if (js.length() > 0) {
			    					initJS.append("\nfunction Init_" + jsonControl.getString("type") + "(id, details) {\n");
			    					initJS.append("  " + js.trim().replace("\n", "\n  "));
			    					initJS.append("\n}\n");
				    			}

				    			// check for a getData method
				    			String getDataFunction = jsonControl.optString("getDataFunction");
				    			// if there was something
				    			if (getDataFunction != null) {
				        			// clean and print! (if not an empty string)
				        			if (getDataFunction.trim().length() > 0) dataJS.append("\nfunction getData_" + controlType + "(ev, id, field, details) {\n  " + getDataFunction.trim().replace("\n", "\n  ") + "\n}\n");

				    			}

				    			// check for a setData method
				    			String setDataFunction = jsonControl.optString("setDataJavaScript");
				    			// if there was something
				    			if (setDataFunction != null) {
				        			// clean and print! (if not an empty string)
				        			if (setDataFunction.trim().length() > 0) dataJS.append("\nfunction setData_" + controlType + "(ev, id, field, details, data, changeEvents) {\n  " + setDataFunction.trim().replace("\n", "\n  ") + "\n}\n");
				    			}

				    			// retrieve any runtimeProperties
				    			JSONObject jsonRuntimePropertyCollection = jsonControl.optJSONObject("runtimeProperties");
				    			// check we got some
				    			if (jsonRuntimePropertyCollection != null) {

				    				// get the first one
				    				JSONObject jsonRuntimeProperty = jsonRuntimePropertyCollection.optJSONObject("runtimeProperty");
				    				// get an array
				    				JSONArray jsonRunTimeProperties = jsonRuntimePropertyCollection.optJSONArray("runtimeProperty");

				    				// initialise counters
				    				int index = 0;
					    			int count = 0;

					    			// if we got an array
					    			if (jsonRunTimeProperties != null) {
					    				// retain the first entry in the object
					    				jsonRuntimeProperty = jsonRunTimeProperties.getJSONObject(0);
					    				// retain the size
					    				count = jsonRunTimeProperties.length();
					    			}

					    			do {

					    				// get the type
				    					String type = jsonRuntimeProperty.getString("type");

				    					// get the get function
				    					String getFunction = jsonRuntimeProperty.optString("getPropertyFunction", null);
				    					// print the get function if there was one
					    				if (getFunction != null) dataJS.append("\nfunction getProperty_" + controlType + "_" + type + "(ev, id, field, details) {\n  " + getFunction.trim().replace("\n", "\n  ") + "\n}\n");

					    				// get the set function
				    					String setFunction = jsonRuntimeProperty.optString("setPropertyJavaScript", null);
				    					// print the get function if there was one
					    				if (setFunction != null) dataJS.append("\nfunction setProperty_" + controlType + "_" + type + "(ev, id, field, details, data, changeEvents) {\n  " + setFunction.trim().replace("\n", "\n  ") + "\n}\n");

					    				// increment index
					    				index++;

					    				// get the next one
					    				if (index < count) jsonRuntimeProperty = jsonRunTimeProperties.getJSONObject(index);

					    			} while (index < count);

				    			}

			    				// we're done with this jsonControl
			    				break;
			    			}

			    		} // jsonControls loop

					} // control types loop

				} // control types check

			} // jsonControls check

			// check  actions
	    	if (jsonActions != null) {

	    		// check action types
	    		if (_actionTypes != null) {

	    			// collection of dependent controls that need adding
					ArrayList<String> dependentActions = new ArrayList<String>();

					// loop control types used by this application
					for (String actionType : _actionTypes) {

						// loop all available controls
			    		for (int i = 0; i < jsonActions.length(); i++) {

			    			// get the action
			    			JSONObject jsonAction = jsonActions.getJSONObject(i);

			    			// check if we're on the type we need
			    			if (actionType.equals(jsonAction.optString("type"))) {

			    				// look for any dependant control types
			    				JSONObject dependantTypes = jsonAction.optJSONObject("dependentTypes");
			    				// if we got some
			    				if (dependantTypes != null) {
			    					// look for an array
			    					JSONArray dependantTypesArray = dependantTypes.optJSONArray("dependentType");
			    					// if we got one
			    					if (dependantTypesArray != null) {
			    						// loop the array
			    						for (int j = 0; j < dependantTypesArray.length(); j++) {
			    							String dependantType = dependantTypesArray.getString(j);
				    						if (!_actionTypes.contains(dependantType) && !dependentActions.contains(dependantType)) dependentActions.add(dependantType);
			    						}
			    					} else {
			    						// just use the object
			    						String dependantType = dependantTypes.getString("dependentType");
			    						if (!_actionTypes.contains(dependantType) && !dependentActions.contains(dependantType)) dependentActions.add(dependantType);
			    					}
			    				}

			    				// we're done
			    				break;
			    			}

			    		}

					}

					// now add all of the dependent controls
					_controlTypes.addAll(dependentActions);

	    			// loop action types used by this application
					for (String actionType : _actionTypes) {

						// loop jsonActions
			    		for (int i = 0; i < jsonActions.length(); i++) {

			    			// get action
			    			JSONObject jsonAction = jsonActions.getJSONObject(i);

			    			// check the action is the one we want
			    			if (actionType.equals(jsonAction.optString("type"))) {

			    				// add any resources (controls can have them too)
			    				addResources(jsonAction, "action", resourceJS, resourceCSS);

				    			// get action JavaScript
				    			String js = jsonAction.optString("actionJavaScript");
				    			// only produce rapid action is this is rapid app
				    			if (js != null && ("rapid".equals(_id) || !"rapid".equals(actionType))) {
				        			// clean and print! (if not an empty string)
				        			if (js.trim().length() > 0) actionJS.append("\n" + js.trim() + "\n");
				    			}

			    				// move onto the next action type
			    				break;
			    			}

			    		} // jsonActions loop

					} // action types loop

	    		} // action types check

	    	} // jsonAction check

	    	// assume no theme css
	    	String themeCSS = "";
	    	// assume no theme name
	    	String themeName = "No theme";
	    	// get the theme
	    	Theme theme = getTheme(servletContext);
	    	// if the was one
	    	if (theme != null) {
	    		// retain the theme CSS
				themeCSS = theme.getCSS();
				// retain the name
				themeName = theme.getName();
				// get any resources
				addResources(theme.getResources(), "theme", themeName, null, resourceJS, resourceCSS);
	    	}

	    	// put the appResources at the end so they can be overrides
    		if (_appResources != null) {
    			for (Resource resource : _appResources) {
    				// create new resource based on this one (so that the dependancy doesn't get written back to the application.xml file)
    				Resource appResource = new Resource(resource.getType(), resource.getContent(), ResourceDependency.RAPID);
    				// if the type is a file or link prefix with the application folder
    				switch (resource.getType()) {
    					case Resource.JAVASCRIPTFILE : case Resource.CSSFILE :
    						// files are available on the local file system so we prefix with the webfolder
    						appResource.setContent(getWebFolder(this) + (resource.getContent().startsWith("/") ? "" : "/") + resource.getContent());
    					break;
    					case  Resource.JAVASCRIPTLINK: case Resource.CSSLINK:
    						// links are not so go in as-is
    						appResource.setContent(resource.getContent());
    					break;
    				}
    				// add new resource based on this one but with Rapid dependency
    				_resources.add(appResource);
    			}
    		}

	    	// create folders to write the rapid.js file
			String applicationPath = getWebFolder(servletContext);
			File applicationFolder = new File(applicationPath);
			if (!applicationFolder.exists()) applicationFolder.mkdirs();

			// write the rapid.js file
			FileOutputStream fos = new FileOutputStream (applicationPath + "/rapid.js");
			PrintStream ps = new PrintStream(fos);

			// write the rapid.min.js file
			FileOutputStream fosMin = new FileOutputStream (applicationPath + "/rapid.min.js");
			PrintWriter pw = new PrintWriter(fosMin);

			// file header
			ps.print("\n/* This file is auto-generated on application load and save - it is minified when the application status is live */\n");
			// check functions
			if (_functions != null) {
				if (_functions.length() > 0) {
					// header (this is removed by minify)
					ps.print("\n\n/* Application functions JavaScript */\n\n");
					// insert params
					String functionsParamsInserted = insertParameters(servletContext, _functions);
					// print
					ps.print(functionsParamsInserted);
					// print minify
					Minify.toWriter(functionsParamsInserted, pw, Minify.JAVASCRIPT, "Application functions");
				}
			}
			// check resource js
			if (resourceJS.length() > 0) {
				// header
				ps.print("\n\n/* Control and Action resource JavaScript */\n\n");
				// insert params
				String resourceJSParamsInserted = insertParameters(servletContext, resourceJS.toString());
				// print
				ps.print(resourceJS.toString());
				// print minify
				Minify.toWriter(resourceJSParamsInserted, pw, Minify.JAVASCRIPT, "Control and Action resources");
			}
			// check init js
			if (initJS.length() > 0) {
				// header
				ps.print("\n\n/* Control initialisation methods */\n\n");
				// insert params
				String initJSParamsInserted = insertParameters(servletContext, initJS.toString());
				// print
				ps.print(initJS.toString());
				// print minify
				Minify.toWriter(initJSParamsInserted, pw, Minify.JAVASCRIPT, "Control initialisation methods");
			}
			// check datajs
			if (dataJS.length() > 0) {
				// header
				ps.print("\n\n/* Control getData and setData methods */\n\n");
				// insert params
				String dataJSParamsInserted = insertParameters(servletContext, dataJS.toString());
				// print
				ps.print(dataJS.toString());
				// print minify
				Minify.toWriter(dataJSParamsInserted, pw, Minify.JAVASCRIPT, "Control getData and setData methods");
			}
			// check action js
			if (actionJS.length() > 0) {
				// header
				ps.print("\n\n/* Action methods */\n\n");
				// insert params
				String actionParamsInserted = insertParameters(servletContext, actionJS.toString());
				// print
				ps.print(actionJS.toString());
				// print minify
				Minify.toWriter(actionParamsInserted, pw, Minify.JAVASCRIPT, "Action methods");
			}

			// close debug writer and stream
			ps.close();
			fos.close();
			// close min writer and stream
			pw.close();
			fosMin.close();

			// get the rapid CSS into a string and insert parameters
			String resourceCSSWithParams = insertParameters(servletContext, resourceCSS.toString());
			String appThemeCSSWithParams = insertParameters(servletContext, themeCSS);
			String appCSSWithParams = insertParameters(servletContext, _styles);

			// write the rapid.css file
			fos = new FileOutputStream (applicationPath + "/rapid.css");
			ps = new PrintStream(fos);
			ps.print("\n/* This file is auto-generated on application load and save - it is minified when the application status is live */\n\n");
			if (resourceCSSWithParams != null) {
				ps.print(resourceCSSWithParams.trim());
			}
			if (appThemeCSSWithParams != null) {
				ps.print("\n\n/* " + themeName + " theme styles */\n\n");
				ps.print(appThemeCSSWithParams.trim());
			}
			if (appCSSWithParams != null) {
				ps.print("\n\n/* Application styles */\n\n");
				ps.print(appCSSWithParams.trim());
			}
			ps.close();
			fos.close();

			// minify it to a rapid.min.css file
			Minify.toFile(resourceCSSWithParams + "\n" + appThemeCSSWithParams + "\n" + appCSSWithParams, applicationPath + "/rapid.min.css", Minify.CSS, "Resource, theme, and app CSS");

			// check the status
	    	if (_status == STATUS_LIVE) {
	    		// add the application js min file as a resource
	    		_resources.add(new Resource(Resource.JAVASCRIPTFILE, getWebFolder(this) + "/rapid.min.js", ResourceDependency.RAPID));
	    		// add the application css min file as a resource
	    		_resources.add(new Resource(Resource.CSSFILE, getWebFolder(this) + "/rapid.min.css", ResourceDependency.RAPID));
	    	} else {
	    		// add the application js file as a resource
	    		_resources.add(new Resource(Resource.JAVASCRIPTFILE, getWebFolder(this) + "/rapid.js", ResourceDependency.RAPID));
	    		// add the application css file as a resource
	    		_resources.add(new Resource(Resource.CSSFILE, getWebFolder(this) + "/rapid.css", ResourceDependency.RAPID));
	    	}

	    	// loop all resources and minify js and css files
			for (Resource resource : _resources) {
				// get the content (which is the filename)
				String fileName = resource.getContent();
				// only interested in js and css files
				switch (resource.getType()) {
					case Resource.JAVASCRIPTFILE :
						// get a file for this
						File jsFile = new File(servletContext.getRealPath("/") + (fileName.startsWith("/") ? "" : "/")  + fileName);
						// if the file exists, and it's in the scripts folder and ends with .js
						if (jsFile.exists() && fileName.startsWith("scripts/") && fileName.endsWith(".js")) {
							// derive the min file name by modifying the start and end
							String fileNameMin = "scripts_min/" + fileName.substring(8, fileName.length() - 3) + ".min.js";
							// get a file for minifying
							File jsFileMin = new File(servletContext.getRealPath("/") + "/" + fileNameMin);
							// if this file does not exist
							if (!jsFileMin.exists()) {
								// make any dirs it may need
								jsFileMin.getParentFile().mkdirs();
								// minify to it
								Minify.toFile(jsFile, jsFileMin, Minify.JAVASCRIPT, "JavaScript file " + fileName);
							}
							// if this application is live, update the resource to the min file
							if (_status == STATUS_LIVE) resource.setContent(fileNameMin);
						}
					break;
					case Resource.CSSFILE :
						// get a file for this
						File cssFile = new File(servletContext.getRealPath("/") + (fileName.startsWith("/") ? "" : "/")  + fileName);
						// if the file exists, and it's in the scripts folder and ends with .js
						if (cssFile.exists() && fileName.startsWith("styles/") && fileName.endsWith(".css")) {
							// derive the min file name by modifying the start and end
							String fileNameMin = "styles_min/" + fileName.substring(7, fileName.length() - 4) + ".min.css";
							// get a file for minifying
							File cssFileMin = new File(servletContext.getRealPath("/") + "/" + fileNameMin);
							// if this file does not exist
							if (!cssFileMin.exists()) {
								// make any dirs it may need
								cssFileMin.getParentFile().mkdirs();
								// minify to it
								Minify.toFile(cssFile, cssFileMin, Minify.CSS, "CSS file " + fileName);
							}
							// if this application is live, update the resource to the min file
							if (_status == STATUS_LIVE) resource.setContent(fileNameMin);
						}
					break;
				}

	    	} // loop resources

			// a list for all of the style classes we're going to send up with
			_styleClasses = new ArrayList<String>();

			// populate the list of style classes by scanning the global styles
			scanStyleClasses(_styles, _styleClasses);
			// and any theme
			scanStyleClasses(appThemeCSSWithParams, _styleClasses);

			// remove any that have the same name as controls
			if (jsonControls != null) {
				// loop them
				for (int i = 0;  i < jsonControls.length(); i++) {
					// remove any classes with this controls type
					_styleClasses.remove(jsonControls.getJSONObject(i).getString("type"));
				}
			}


		} // create resources

		// empty the list of page variables so it's regenerated
		_pageVariables = null;

		// debug log that we initialised
		logger.debug("Initialised application " + _name + "/" + _version + (createResources ? "" : " (no resources)"));

	}

	// remove any page locks for a given user
	public int removeUserPageLocks(ServletContext servletContext, String userName) throws RapidLoadingException {
		// assume no locks removed
		int locksRemoved = 0;
		// check there are pages
		if (_pages != null) {
			// loop them
			for (String pageId : _pages.getPageIds()) {
				// get the page
				Page page = _pages.getPage(pageId);
				// if the page is still in memory
				if (page != null) {
					// get the page lock
					Lock pageLock = page.getLock();
					// if there was one
					if (pageLock != null) {
						// if it matches the user name remove the lock
						if (userName.equals(pageLock.getUserName())) {
							page.setLock(null);
							locksRemoved ++;
						}
					}
				}
			}
		}
		return locksRemoved;
	}

	public List<Backup> getApplicationBackups(RapidHttpServlet rapidServlet) throws JSONException {

		List<Backup> backups = new ArrayList<Backup>();

		File backupFolder = new File(getBackupFolder(rapidServlet.getServletContext(), false));

		if (backupFolder.exists()) {

			for (File backup : backupFolder.listFiles()) {

				if (backup.isDirectory()) {

					String id = backup.getName();

					String[] nameParts = id.split("_");

					if (id.startsWith(_id + _version) && nameParts.length >= 3) {

						String name = "";
						Date date = new Date();
						String size = Files.getSizeName(backup);
						SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");
						String user = "";

						//loop through the parts
						for (int i = 0; i < nameParts.length; i++) {

							//if this part is a date
							if (nameParts[i].matches("^\\d{8}$")) {
								//remove the last underscore from the name
								name = name.substring(0, name.length() - 1);
								try {
									//parse the date and time (adjacent index of date will always be time)
									date = df.parse(nameParts[i] + " " + nameParts[i+1]);
									String datetime = nameParts[i] +"_"+nameParts[i+1]+"_";
									//get the index of the datetime string
									int datetimeIndex = id.indexOf(datetime);
									//the rest is the username part - until the beginning of .page.xml
									user = id.substring(datetimeIndex + datetime.length(), id.length());
								} catch (ParseException ex) {
									throw new JSONException(ex);
								}

								break;
							}

							//otherwise just concatenate the file names
							name += nameParts[i] + "_";
						}

						backups.add(new Backup(id, date, user, size));

					} // name parts > 3

				} // directory check

			} // file loop

			// sort the list by date
			Collections.sort(backups, new Comparator<Backup>() {
				@Override
				public int compare(Backup obj1, Backup obj2) {
					if (obj1.getDate().before(obj2.getDate())) {
						return -1;
					} else {
						return 1;
					}
				}
			});

			// check if we have too many
			while (backups.size() > _applicationBackupsMaxSize) {
				// get the top backup folder into a file object
				backupFolder = new File(getBackupFolder(rapidServlet.getServletContext(), false) + "/" + backups.get(0).getId());
				// delete it
				Files.deleteRecurring(backupFolder);
				// remove it
				backups.remove(0);
			}

		}

		return backups;

	}

	public List<Backup> getPageBackups(RapidHttpServlet rapidServlet) throws JSONException {

		List<Backup> backups = new ArrayList<Backup>();

		File backupFolder = new File(getBackupFolder(rapidServlet.getServletContext(), false));

		if (backupFolder.exists()) {

			for (File backup : backupFolder.listFiles()) {

				String fileName = backup.getName();

				if (fileName.endsWith(".page.xml")) {

					String[] nameParts = fileName.split("_");

					if (nameParts.length >= 3) {

						String name = "";
						Date date = new Date();
						String size = Files.getSizeName(backup);
						SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");
						String user = "";

						//loop through the parts
						for (int i = 0; i < nameParts.length; i++) {

							//if this part is a date
							if (nameParts[i].matches("^\\d{8}$")) {
								//remove the last underscore from the name
								name = name.substring(0, name.length() - 1);

								try {
									//parse the date and time (adjacent index of date will always be time)
									date = df.parse(nameParts[i] + " " + nameParts[i+1]);
									String datetime = nameParts[i] +"_"+nameParts[i+1]+"_";
									//get the index of the datetime string
									int datetimeIndex = fileName.indexOf(datetime);
									//the rest is the username part - until the beginning of .page.xml
									user = fileName.substring(datetimeIndex + datetime.length(), fileName.indexOf(".page.xml"));
								} catch (ParseException ex) {
									throw new JSONException(ex);
								}

								break;
							}

							//otherwise just concatenate the file names
							name += nameParts[i] + "_";
						}

						backups.add(new Backup(fileName, name, date, user, size));

					} // name parts > 3

				} // ends .page.xml

			} // file loop

			// sort the list by date
			Collections.sort(backups, new Comparator<Backup>() {
				@Override
				public int compare(Backup obj1, Backup obj2) {
					if (obj1.getDate().before(obj2.getDate())) {
						return -1;
					} else {
						return 1;
					}
				}
			});

			// create a map to count backups of each page
			Map<String,Integer> pageBackupCounts = new HashMap<String,Integer>();
			// loop all of the backups in reverse order
			for (int i = backups.size() - 1; i >= 0; i--) {
				// get the back up
				Backup pageBackup = backups.get(i);
				// assume no backups so far for this page
				int pageBackupCount = 0;
				// set the backup count if we have one
				if (pageBackupCounts.get(pageBackup.getName()) != null) pageBackupCount = pageBackupCounts.get(pageBackup.getName());
				// increment the count
				pageBackupCount ++;
				// check the size
				if (pageBackupCount > _pageBackupsMaxSize) {
					// get the backup into a file object
					File backupFile = new File(backupFolder.getAbsolutePath() + "/" + backups.get(i).getId());
					// delete it
					backupFile.delete();
					// remove this backup
					backups.remove(i);
				}
				// store the page backup count
				pageBackupCounts.put(pageBackup.getName(), pageBackupCount);
			}

		}

		return backups;

	}

	public void backup(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, boolean allVersions) throws IOException {

		// get the username
		String userName = rapidRequest.getUserName();
		if (userName == null) userName = "unknown";

		// get the current date and time in a string
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateString = formatter.format(new Date());

		// create a fileName for the archive
		String fileName = Files.safeName(_id + _version + "_" + dateString + "_" + userName);

		// create folders to backup the app
		String backupPath = getBackupFolder(rapidServlet.getServletContext(), allVersions) + "/" + fileName;
		File backupFolder = new File(backupPath);
		if (!backupFolder.exists()) backupFolder.mkdirs();

		// create a file object for the application data folder
		File appFolder = new File(getConfigFolder(rapidServlet.getServletContext()));

		// create a list of files to ignore
		List<String> ignoreFiles = new ArrayList<String>();
		ignoreFiles.add(BACKUP_FOLDER);

	 	// copy the existing files and folders to the backup folder
	    Files.copyFolder(appFolder, backupFolder, ignoreFiles);

	    // create a file object and folders for the web folder archive
	    backupFolder = new File(backupPath + "/WebContent");
	    if (!backupFolder.exists()) backupFolder.mkdirs();

	    // create a file object for the application web folder
	    appFolder = new File(getWebFolder(rapidServlet.getServletContext()));

	 	// copy the existing web content files and folders to the webcontent backup folder
	    Files.copyFolder(appFolder, backupFolder, ignoreFiles);

	}

	public Application copy(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, String newId, String newVersion, boolean backups, boolean delete) throws Exception {

		// retain the ServletContext
		ServletContext servletContext = rapidServlet.getServletContext();

		// load the app into a copy without initialising
		Application appCopy = Application.load(servletContext, new File(getConfigFolder(servletContext) + "/application.xml"), false);

		// update the copy id
		appCopy.setId(newId);
		// update the copy version
		appCopy.setVersion(newVersion);
		// update the copy status to in developement
		appCopy.setStatus(Application.STATUS_DEVELOPMENT);
		// update the created date
		appCopy.setCreatedDate(new Date());

		// get the app copy folder
		File appCopyFolder = new File(appCopy.getConfigFolder(servletContext));
		// get a app web copy folder location
		File appWebCopyFolder = new File(appCopy.getWebFolder(servletContext));

		try {

			// save the copy to create the folder and, application.xml and page.xml files
			appCopy.save(rapidServlet, rapidRequest, false);

			// get the copy of the application.xml file
			File appCopyFile = new File(appCopyFolder + "/application.xml");
			// read the copy to a string
			String appCopyXML = Strings.getString(appCopyFile);
			// replace all app/version references
			appCopyXML = appCopyXML.replace("/" + _id + "/" + _version + "/", "/" + newId + "/" + newVersion + "/");
			// save it back to it's new location
			Strings.saveString(appCopyXML, appCopyFile);

			// look for a security.xml file
			File appSecurityFile = new File(getConfigFolder(servletContext) + "/security.xml");
			// if we have one, copy it
			if (appSecurityFile.exists()) Files.copyFile(appSecurityFile, new File(appCopyFolder + "/security.xml"));

			// get the pages config folder
			File appPagesFolder = new File(getConfigFolder(servletContext) + "/pages");
			// check it exists
			if (appPagesFolder.exists()) {
				// the folder we are copying to
				File appPagesCopyFolder = new File(appCopyFolder + "/pages");
				// make the dirs
				appPagesCopyFolder.mkdirs();
				// loop the files
				for (File appCopyPageFile : appPagesFolder.listFiles()) {
					// if this is a page.xml file
					if (appCopyPageFile.getName().endsWith(".page.xml")) {
						// read the copy to a string
						String pageCopyXML = Strings.getString(appCopyPageFile);
						// replace all app/version references
						pageCopyXML = pageCopyXML
								.replace("/" + _id + "/" + _version + "/", "/" + newId + "/" + newVersion + "/")
								.replace("~?a=" + _id + "&amp;" + _version + "&amp;", "~?a=" + newId + "&amp;" + newVersion + "&amp;");
						// get the page file
						File pageFile = new File(appPagesCopyFolder + "/" + appCopyPageFile.getName());
						// save it back to it's new location
						Strings.saveString(pageCopyXML, pageFile);
					}
				}
			}

			// get the web folder
			File appWebFolder = new File(getWebFolder(servletContext));
			// if it exists
			if (appWebFolder.exists()) {
				// copy everything
				Files.copyFolder(appWebFolder, appWebCopyFolder);
			}

			// if we want to copy the backups too
			if (backups) {
				// get the backups folder
				File appBackupFolder = new File(getBackupFolder(servletContext, false));
				// check it exists
				if (appBackupFolder.exists()) {
					// create a folder to copy to
					File appBackupCopyFolder = new File(appCopy.getBackupFolder(servletContext, false));
					// make the dirs
					appBackupCopyFolder.mkdirs();
					// copy the folder
					Files.copyFolder(appBackupFolder, appBackupCopyFolder);
				}
			}

			// reload the application with the new app and page references
			appCopy = Application.load(servletContext, appCopyFile);

			// add this one to the applications collection
			rapidServlet.getApplications().put(appCopy);

			// delete this one
			if (delete) delete(rapidServlet, rapidRequest, false);

			// return the copy
			return appCopy;

		} catch (Exception ex) {

			// remove the failed copy from the applications collection
		    rapidServlet.getApplications().remove(appCopy);

			// delete copy folder
			Files.deleteRecurring(appCopyFolder);

			// delete copy web folder
			Files.deleteRecurring(appWebCopyFolder);

			// rethrow
			throw ex;

		}

	}

	public long save(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, boolean backup) throws JAXBException, IOException, IllegalArgumentException, SecurityException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {

		// create folders to save the app
		String folderPath = getConfigFolder(rapidServlet.getServletContext());
		File folder = new File(folderPath);
		if (!folder.exists()) folder.mkdirs();

		// create a file object for the application
		File appFile = new File(folderPath + "/application.xml");
		// backup the app if it already exists
		if (appFile.exists() && backup) backup(rapidServlet, rapidRequest, false);

		// create a temp file for saving the application to
		File tempFile = new File(folderPath + "/application-saving.xml");

		// update the modified by and date
		_modifiedBy = rapidRequest.getUserName();
		_modifiedDate = new Date();

		// marshal the application object to the temp file
		FileOutputStream fos = new FileOutputStream(tempFile.getAbsolutePath());
		RapidHttpServlet.getMarshaller().marshal(this, fos);
	    fos.close();

	    // copy / overwrite the app file with the temp file
	    Files.copyFile(tempFile, appFile);

	    // store the size of the file writter
	    long fileSize = tempFile.length();

	    // delete the temp file
	    tempFile.delete();

	    // put this application in the collection
	    rapidServlet.getApplications().put(this);

	    // initialise the application, rebuilding the resources
	    initialise(rapidServlet.getServletContext(), true);

	    return fileSize;
	}

	public void delete(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, boolean allVersions) throws JAXBException, IOException {

		// get the servlet context
		ServletContext servletContext = rapidServlet.getServletContext();

		// create a file object for the config folder
		File appFolder = new File(getConfigFolder(servletContext));
		// if this is all versions promote from version to app
		if (allVersions) appFolder = appFolder.getParentFile();

		// create a file object for the webcontent folder
		File webFolder = new File (getWebFolder(servletContext));
		// if this is all versions promote from version to app
		if (allVersions) webFolder = webFolder.getParentFile();

		// if the app folder exists
		if (appFolder.exists()) {
			// backup the application
			backup(rapidServlet, rapidRequest, allVersions);
			// delete the version app folder
			Files.deleteRecurring(appFolder);
			// if the parent is empty now delete too
			if (appFolder.getParentFile().list().length == 0) appFolder.getParentFile().delete();
			// delete the version web folder
			Files.deleteRecurring(webFolder);
			// if the parent is empty now delete too
			if (webFolder.getParentFile().list().length == 0) webFolder.getParentFile().delete();
		}

		// call delete on the form adapter if there is one
		if (getFormAdapter() != null) getFormAdapter().delete(rapidRequest);

		// close the application
		close(servletContext);

		// remove this application from the collection
		rapidServlet.getApplications().remove(this);

	}

	// create a named .zip file for the app in the /temp folder
	public void zip(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, User user, String fileName, boolean offlineUse) throws JAXBException, IOException, JSONException, RapidLoadingException {

		// create folders to save locate app file
		String folderPath = getConfigFolder(rapidServlet.getServletContext());

		// create a file object for the application
		File appFile = new File(folderPath + "/application.xml");

		// if the app file exists
		if (appFile.exists()) {

			// create a list of sources for our zip
			ZipSources zipSources = new ZipSources();

			// deleteable folder
			File deleteFolder = null;

			// create a file object for the webcontent folder
			File webFolder = new File(getWebFolder(rapidServlet.getServletContext()));

			// if for offlineUse
			if (offlineUse) {

				// loop the contents of the webFolder
				for (File file : webFolder.listFiles()) {
					// add this file to the zip using the web folder path
					zipSources.add(file, getWebFolder(this));
				}

				// set the delete folder
				deleteFolder = new File(rapidServlet.getServletContext().getRealPath("/") + "/WEB-INF/temp/" + _id + "_" + rapidRequest.getUserName());
				// create it
				deleteFolder.mkdirs();

				// make a details.txt file
				File detailsFile = new File(deleteFolder + "/details.txt");
				// get a file writer
				Writer fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(detailsFile), "UTF-8"));
				// write the details
				fw.write(_id + "\r\n" +  Rapid.MOBILE_VERSION + " - " + _version + "\r\n" + _title + "\r\n");
				// lines 4 to 7 are for the status bar colours
				if (_statusBarColour != null) fw.write(_statusBarColour);
				fw.write("\r\n");
				if (_statusBarHighlightColour != null) fw.write(_statusBarHighlightColour);
				fw.write("\r\n");
				if (_statusBarTextColour != null) fw.write(_statusBarTextColour);
				fw.write("\r\n");
				if (_statusBarIconColour != null) fw.write(_statusBarIconColour);
				fw.write("\r\n");
				// line 8 is the password retention
				fw.write(_storePasswordDuration + "\r\n");
				// close the file writer
				fw.close();
				// add the file to the zip with a root path
				zipSources.add(detailsFile, "");

				// check we have pages
				if (_pages != null) {
					// loop them
					for (PageHeader pageHeader : _pages.getSortedPages()) {
						// get the page id
						String pageId = pageHeader.getId();
						// get a reference to the page
						Page page = _pages.getPage(rapidServlet.getServletContext(), pageId);
						// create a file for it in the delete folder
						File pageFile = new File(deleteFolder + "/" + pageId + ".htm");
						// create a file writer for it for now
						fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pageFile), "UTF-8"));
						// for now get a printWriter to write the page html
						page.writeHtml(rapidServlet, null, rapidRequest, this, user, fw, false, true);
						// close it
						fw.close();
						// add the file to the zip with a root path
						zipSources.add(pageFile, "");
					}
					// get the start page
					Page page = getStartPage(rapidServlet.getServletContext());
					// if we got one add it as index.htm
					if (page != null) {
						// create a file for it for it in the delete folder
						File pageFile = new File(deleteFolder + "/" + "index.htm");
						// create a file writer for it for now
						fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pageFile), "UTF-8"));
						// for now get a printWriter to write the page html
						page.writeHtml(rapidServlet, null, rapidRequest, this, user, fw, false, true);
						// close it
						fw.close();
						// add the file to the zip with a root path
						zipSources.add(pageFile, "");
					}

				}

				// check we have resources
				if (_resources != null) {
					// loop them
					for (Resource resource : _resources) {
						// check they're any of our file types
						if (resource.getType() == Resource.JAVASCRIPTFILE || resource.getType() == Resource.CSSFILE || resource.getType() == Resource.JAVASCRIPTLINK || resource.getType() == Resource.CSSLINK || resource.getType() == Resource.FILE) {
							// get a file object for them
							File resourceFile = new File(rapidServlet.getServletContext().getRealPath("/") + "/" + resource.getContent());
							// if file exists
							if (resourceFile.exists()) {
								// get the path from the file name
								String path = Files.getPath(resource.getContent());
								// add as zip source
								zipSources.add(resourceFile, path);
							}
						}
					}
				}

			} else {

				// loop the contents of the webFolder and place in WebContent subfolder
				for (File file : webFolder.listFiles()) {
					// add this file to the WEB-INF path
					zipSources.add(new ZipSource(file,"WebContent"));
				}

				// create a file object for the application folder
				File appFolder = new File(folderPath);

				// loop the contents of the appFolder and place in WEB-INF subfolder
				for (File file : appFolder.listFiles()) {
					// add this file to the WebContent path
					zipSources.add(new ZipSource(file,"WEB-INF"));
				}

			}

			// get a file for the temp directory
			File tempDir = new File(rapidServlet.getServletContext().getRealPath("/") + "/WEB-INF/temp");
			// create it if not there
			if (!tempDir.exists()) tempDir.mkdir();

			// create the zip file object with our destination, always in the temp folder
			ZipFile zipFile = new ZipFile(new File(rapidServlet.getServletContext().getRealPath("/") + "/WEB-INF/temp/" + fileName));

			// create a list of files to ignore
			ArrayList<String> ignoreFiles = new ArrayList<String>();
			// don't include any files or folders from the back in the .zip
			ignoreFiles.add(BACKUP_FOLDER);

			// zip the sources into the file
			zipFile.zipFiles(zipSources, ignoreFiles);

			// loop the deleteable files
			if (deleteFolder != null) {
				try {
					// delete the folder and all contents
					Files.deleteRecurring(deleteFolder);
				} catch (Exception ex) {
					// log exception
					LogManager.getLogger(this.getClass()).error("Error deleting temp file " + deleteFolder, ex);
				}

			}

		}

	}

	// an overload for the above which will include the for export rather than offlineUse files
	public void zip(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, User user, String fileName) throws JAXBException, IOException, JSONException, RapidLoadingException {
		zip(rapidServlet, rapidRequest, user, fileName, false);
	}

	// close the database connections and form adapters before reload
	public void close(ServletContext servletContext) {
		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");
		// closing
		logger.debug("Closing application " + _id + "/" + _version + "...");
		// if we got some
		if (_databaseConnections != null) {
			// loop them
			for (DatabaseConnection databaseConnection : _databaseConnections) {
				// close database connection
				try {
					// call the close method
					databaseConnection.close();
					// log
					logger.debug("Closed " + databaseConnection.getName());
				} catch (SQLException ex) {
					logger.error("Error closing database connection " + databaseConnection.getName() + " for application " + _id + "/" + _version, ex);
				}
			}
		}
		// close form adapter
		if (_formAdapter != null) {
			try {
				// call the close method
				_formAdapter.close();
				// log
				logger.debug("Closed form adapter");
			} catch (Exception ex) {
				logger.error("Error closing form adapter for application " + _id + "/" + _version, ex);
			}
		}
	}

	// static methods

	// this is where the application configuration will be stored
	public static String getConfigFolder(ServletContext servletContext, String id, String version) {
		// this use of getRealPath was required under Jetty 9.4
		return servletContext.getRealPath("/") + "/WEB-INF/applications/" + id + "/" + version;
	}

	// this is the web folder with the full system path
	public static String getWebFolder(ServletContext servletContext, String id, String version) {
		// this use of getRealPath was required under Jetty 9.4
		return servletContext.getRealPath("/") + "/applications/" + id + "/" + version;
	}

	// this is the web folder as seen externally
	public static String getWebFolder(Application application) {
		// this use of getRealPath was required under Jetty 9.4
		return "applications/" + application.getId() + "/" + application.getVersion();
	}

	// this is the backup folder
	public static String getBackupFolder(ServletContext servletContext, String id, String version, boolean allVersions) {
		if (allVersions) {
			return servletContext.getRealPath("/") + "/WEB-INF/applications/" +  BACKUP_FOLDER + "/" + id + "/" + version;
		} else {
			return servletContext.getRealPath("/") + "/WEB-INF/applications/" + id + "/" + version + "/" + BACKUP_FOLDER;
		}
	}

	// this is a simple overload for default loading of applications where the resources are all regenerated
	public static Application load(ServletContext servletContext, File file) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException, RapidLoadingException, XPathExpressionException {
		return load(servletContext, file, true);
	}

	// this method loads the application by ummarshelling the xml, and then doing the same for all page .xmls, before calling the initialise method
	public static Application load(ServletContext servletContext, File file, boolean initialise) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException, RapidLoadingException, XPathExpressionException {

		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");

		// trace log that we're about to load a page
		logger.trace("Loading application from " + file);

		// open the xml file into a document
		Document appDocument = XML.openDocument(file);

		// specify the version as -1
		int xmlVersion = -1;

		// look for a version node
		Node xmlVersionNode = XML.getChildElement(appDocument.getFirstChild(), "XMLVersion");

		// if we got one update the version
		if (xmlVersionNode != null) xmlVersion = Integer.parseInt(xmlVersionNode.getTextContent());

		// if the version of this xml isn't the same as this class we have some work to do!
		if (xmlVersion != XML_VERSION) {

			// get the page name
			String name = XML.getChildElementValue(appDocument.getFirstChild(), "name");

			// log the difference
			logger.debug("Application " + name + " with xml version " + xmlVersion + ", current xml version is " + XML_VERSION);

			//
			// Here we would have code to update from known versions of the file to the current version
			//

			// check whether there was a version node in the file to start with
			if (xmlVersionNode == null) {
				// create the version node
				xmlVersionNode = appDocument.createElement("XMLVersion");
				// add it to the root of the document
				appDocument.getFirstChild().appendChild(xmlVersionNode);
			}

			// set the xml to the latest version
			xmlVersionNode.setTextContent(Integer.toString(XML_VERSION));

			// save it
			XML.saveDocument(appDocument, file);

			logger.debug("Updated " + name + " application xml version to " + XML_VERSION);

		}

		// get the unmarshaller
		Unmarshaller unmarshaller = RapidHttpServlet.getUnmarshaller();

		try {

			// unmarshall the application
			Application application = (Application) unmarshaller.unmarshal(file);

			// if we don't want pages loaded or resource generation skip this
			if (initialise) {

				// load the pages (actually clears down the pages collection and reloads the headers)
				application.getPages().loadpages(servletContext);

				// initialise the application and create the resources
				application.initialise(servletContext, true);

			}

			// log that the application was loaded
			logger.info("Loaded application " + application.getName() + "/" + application.getVersion() + (initialise ? "" : " (no initialisation)"));

			// this can be memory intensive so garbage collect
			if(appDocument != null){
				appDocument = null;
	            System.runFinalization();
	            System.gc();
	        }

			return application;

		} catch (JAXBException ex) {

			throw new RapidLoadingException("Error loading application file at " + file, ex);

		}

	}

}
