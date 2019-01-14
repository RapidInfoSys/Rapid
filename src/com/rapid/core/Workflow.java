package com.rapid.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.rapid.core.Application.RapidLoadingException;
import com.rapid.data.DatabaseConnection;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Files;
import com.rapid.utils.XML;

@XmlRootElement
public class Workflow {

	// the version of this class's xml structure when marshalled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)
	public static final int XML_VERSION = 1;

	// instance variables
	private int _xmlVersion;
	private String _id,  _name, _title, _description, _createdBy, _modifiedBy;
	private Date _createdDate, _modifiedDate;
	private List<DatabaseConnection> _databaseConnections;
	private List<String> _actionTypes;

	// properties

	// the XML version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }

	// the id uniquely identifies the workflow (it is produced by taking all unsafe characters out of the name)
	public String getId() { return _id; }
	public void setId(String id) { _id = id; }

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

	// a collection of database connections used via the connection adapter class to produce database connections
	public List<DatabaseConnection> getDatabaseConnections() { return _databaseConnections; }
	public void setDatabaseConnections(List<DatabaseConnection> databaseConnections) { _databaseConnections = databaseConnections; }

	// control types used in this application
	public List<String> getActionTypes() { return _actionTypes; }
	public void setActionTypes(List<String> actionTypes) { _actionTypes = actionTypes; }

	// constructors

	public Workflow() {
		_xmlVersion = XML_VERSION;
		_databaseConnections = new ArrayList<DatabaseConnection>();
	};

	// public methods

	// close the database connections and form adapters before reload
	public void close(ServletContext servletContext) {
		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");
		// closing
		logger.debug("Closing workflow " + _id  + "...");
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
					logger.error("Error closing database connection " + databaseConnection.getName() + " for workflow " + _id, ex);
				}
			}
		}
		/*
		// close form adapter
		if (_formAdapter != null) {
			try {
				// call the close method
				_formAdapter.close();
				// log
				logger.debug("Closed form adapter");
			} catch (Exception ex) {
				logger.error("Error closing form adapter for " + _id + "/" + _version, ex);
			}
		}
		*/
	}

	public void save(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, boolean backup) throws JAXBException, IOException, IllegalArgumentException, SecurityException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {

		// create folders to save the workflow
		String folderPath = getConfigFolder(rapidServlet.getServletContext());
		File folder = new File(folderPath);
		if (!folder.exists()) folder.mkdirs();

		// create a file object for the application
		File file = new File(folderPath + "/" + _id + "-workflow.xml");
		// backup the app if it already exists
		//if (file.exists() && backup) backup(rapidServlet, rapidRequest, false);

		// create a temp file for saving the application to
		File tempFile = new File(folderPath + "/" + _id + "-workflow-saving.xml");

		// update the modified by and date
		_modifiedBy = rapidRequest.getUserName();
		_modifiedDate = new Date();

		// marshal the application object to the temp file
		FileOutputStream fos = new FileOutputStream(tempFile.getAbsolutePath());
		RapidHttpServlet.getMarshaller().marshal(this, fos);
	    fos.close();

	    // copy / overwrite the app file with the temp file
	    Files.copyFile(tempFile, file);

	    // delete the temp file
	    tempFile.delete();

	    // put this application in the collection
	    rapidServlet.getWorkflows().put(_id, this);

	}

	public void delete(RapidHttpServlet rapidServlet, RapidRequest rapidRequest) throws JAXBException, IOException {

		// get the servlet context
		ServletContext servletContext = rapidServlet.getServletContext();

		// create a file object for the config folder
		File workflowFile = new File(getConfigFolder(servletContext) + "/" + _id + ".workflow.xml");

		// if the app folder exists
		if (workflowFile.exists()) {
			// backup the workflow
			//backup(rapidServlet, rapidRequest, allVersions);
			// delete the workflow file
			workflowFile.delete();
		}

		// close the workflow
		close(servletContext);

		// remove this application from the collection
		rapidServlet.getWorkflows().remove(_id);

	}

	// public static methods

	public static String getConfigFolder(ServletContext servletContext) {
		return servletContext.getRealPath("/") + "/WEB-INF/workflows";
	}

	public static Workflow load(ServletContext servletContext, File file) throws ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, JAXBException, RapidLoadingException {

		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");

		// trace log that we're about to load a page
		logger.trace("Loading application from " + file);

		// open the xml file into a document
		Document document = XML.openDocument(file);

		// specify the version as -1
		int xmlVersion = -1;

		// look for a version node
		Node xmlVersionNode = XML.getChildElement(document.getFirstChild(), "XMLVersion");

		// if we got one update the version
		if (xmlVersionNode != null) xmlVersion = Integer.parseInt(xmlVersionNode.getTextContent());

		// if the version of this xml isn't the same as this class we have some work to do!
		if (xmlVersion != XML_VERSION) {

			// get the page name
			String name = XML.getChildElementValue(document.getFirstChild(), "name");

			// log the difference
			logger.debug("Workflow " + name + " with xml version " + xmlVersion + ", current xml version is " + XML_VERSION);

			//
			// Here we would have code to update from known versions of the file to the current version
			//

			// check whether there was a version node in the file to start with
			if (xmlVersionNode == null) {
				// create the version node
				xmlVersionNode = document.createElement("XMLVersion");
				// add it to the root of the document
				document.getFirstChild().appendChild(xmlVersionNode);
			}

			// set the xml to the latest version
			xmlVersionNode.setTextContent(Integer.toString(XML_VERSION));

			// save it
			XML.saveDocument(document, file);

			logger.debug("Updated " + name + " workflow xml version to " + XML_VERSION);

		}

		// get the unmarshaller
		Unmarshaller unmarshaller = RapidHttpServlet.getUnmarshaller();

		try {

			// unmarshall the workflow
			Workflow workflow = (Workflow) unmarshaller.unmarshal(file);

			// log that the application was loaded
			logger.info("Loaded workflow " + workflow.getName());

			// this can be memory intensive so garbage collect
			if(document != null){
				document = null;
	            System.runFinalization();
	            System.gc();
	        }

			return workflow;

		} catch (JAXBException ex) {

			throw new RapidLoadingException("Error loading workflow file at " + file, ex);

		}

	}

}
