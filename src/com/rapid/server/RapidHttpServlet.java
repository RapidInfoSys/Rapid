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

/*

This class wraps HttpServlet and provides a number of useful functions
Mostly getters that retrieve from the servlet context

 */

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Applications;
import com.rapid.core.Device.Devices;
import com.rapid.core.Theme;
import com.rapid.core.Workflows;
import com.rapid.server.filter.RapidAuthenticationAdapter;
import com.rapid.utils.Bytes;
import com.rapid.utils.Encryption.EncryptionProvider;
import com.rapid.utils.Exceptions;
import com.rapid.utils.JAXB.EncryptedXmlAdapter;

@SuppressWarnings({"serial", "unchecked", "rawtypes"})
public class RapidHttpServlet extends HttpServlet {

	// private static variables

	private static Logger _logger = LogManager.getLogger(RapidHttpServlet.class);
	private static JAXBContext _jaxbContext;
	private static EncryptedXmlAdapter _encryptedXmlAdapter;
	private static EncryptionProvider _encryptionProvider;

	// private instance variables
	private List<String> _uploadMimeTypes;
	private Map<String, List<byte[]>> _uploadMimeTypeBytes;

	// enterprise monitor
	protected Monitor _monitor = new Monitor();

	// properties

	public static JAXBContext getJAXBContext() { return _jaxbContext; }
	public static void setJAXBContext(JAXBContext jaxbContext) { _jaxbContext = jaxbContext; }

	public static EncryptionProvider getEncryptionProvider() { return _encryptionProvider; }
	public static void setEncryptionProvider(EncryptionProvider encryptionProvider) { _encryptionProvider = encryptionProvider; }

	public static EncryptedXmlAdapter getEncryptedXmlAdapter() { return _encryptedXmlAdapter; }
	public static void setEncryptedXmlAdapter(EncryptedXmlAdapter encryptedXmlAdapter) { _encryptedXmlAdapter = encryptedXmlAdapter; }

	// public methods

	public static Marshaller getMarshaller() throws JAXBException, IOException {
		// marshaller is not thread safe so we need to create a new one each time
		Marshaller marshaller = _jaxbContext.createMarshaller();
		// add the encrypted xml adapter
		marshaller.setAdapter(_encryptedXmlAdapter);
		// return
		return marshaller;
	}

	public static Unmarshaller getUnmarshaller() throws JAXBException, IOException {

		// initialise the unmarshaller
		Unmarshaller unmarshaller = _jaxbContext.createUnmarshaller();
		// add the encrypted xml adapter
		unmarshaller.setAdapter(_encryptedXmlAdapter);

		// add a validation listener (this makes for better error messages)
		unmarshaller.setEventHandler( new ValidationEventHandler() {
			@Override
			public boolean handleEvent(ValidationEvent event) {

				// get the location
				ValidationEventLocator location = event.getLocator();

				// log
				_logger.debug("JAXB validation event - " + event.getMessage() + (location == null ? "" : " at line " + location.getLineNumber() + ", column " + location.getColumnNumber() + ", node " + location.getNode()));

				// messages with "unrecognized type name" are very useful they're not sever themselves must almost always followed by a severe with a less meaningful message
				if (event.getMessage().contains("unrecognized type name") || event.getSeverity() == ValidationEvent.FATAL_ERROR) {
					return false;
				} else {
					return true;
				}

			}
		});

		// return
		return unmarshaller;
	}

	public Logger getLogger() {	return (Logger) getServletContext().getAttribute("logger");	}

	public Constructor getSecurityConstructor(String type) {
		HashMap<String,Constructor> constructors = (HashMap<String, Constructor>) getServletContext().getAttribute("securityConstructors");
		return constructors.get(type);
	}

	public Constructor getActionConstructor(String type) {
		HashMap<String,Constructor> constructors = (HashMap<String, Constructor>) getServletContext().getAttribute("actionConstructors");
		return constructors.get(type);
	}

	public JSONArray getJsonDatabaseDrivers() {
		return (JSONArray) getServletContext().getAttribute("jsonDatabaseDrivers");
	}

	public JSONArray getJsonConnectionAdapters() {
		return (JSONArray) getServletContext().getAttribute("jsonConnectionAdapters");
	}

	public JSONArray getJsonSecurityAdapters() {
		return (JSONArray) getServletContext().getAttribute("jsonSecurityAdapters");
	}

	public JSONArray getJsonFormAdapters() {
		return (JSONArray) getServletContext().getAttribute("jsonFormAdapters");
	}

	public JSONArray getJsonControls() {
		return (JSONArray) getServletContext().getAttribute("jsonControls");
	}

	public JSONObject getJsonControl(String type) throws JSONException {
		JSONArray jsonControls = getJsonControls();
		if (jsonControls != null) {
			for (int i = 0; i < jsonControls.length(); i++) {
				if (type.equals(jsonControls.getJSONObject(i).getString("type"))) return jsonControls.getJSONObject(i);
			}
		}
		return null;
	}

	public JSONArray getJsonActions() {
		return (JSONArray) getServletContext().getAttribute("jsonActions");
	}

	public JSONObject getJsonAction(String type) throws JSONException {
		JSONArray jsonActions = getJsonActions();
		if (jsonActions != null) {
			for (int i = 0; i < jsonActions.length(); i++) {
				if (type.equals(jsonActions.getJSONObject(i).getString("type"))) return jsonActions.getJSONObject(i);
			}
		}
		return null;
	}

	public Applications getApplications() {
		return (Applications) getServletContext().getAttribute("applications");
	}

	public Workflows getWorkflows() {
		return (Workflows) getServletContext().getAttribute("workflows");
	}

	public Devices getDevices() {
		return (Devices) getServletContext().getAttribute("devices");
	}

	public List<Theme> getThemes() {
		return (List<Theme>) getServletContext().getAttribute("themes");
	}

	public String getSecureInitParameter(String name) {
		String value = getInitParameter(name);
		try {
			if(value!=null && _encryptionProvider!=null)
				return _encryptionProvider.decrypt(value);
		} catch (GeneralSecurityException | IOException e) {
			_logger.debug("Did not decrypt parameter value: "+value);
		}
		return value;
	}

	// this is used to format between Java Date and XML date - not threadsafe so new instance each time
	public SimpleDateFormat getXMLDateFormatter() {
		return new SimpleDateFormat("yyyy-MM-dd");
	}

	// this is used to format between Java Date and XML dateTime - not threadsafe so new instance each time
	public SimpleDateFormat getXMLDateTimeFormatter() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	}

	// this is used to format between Java Date and Local date format - not threadsafe so new instance each time
	public String getLocalDateFormat() {
		String localDateFormat = (String) getServletContext().getAttribute("localDateFormat");
		if (localDateFormat == null) localDateFormat = "dd/MM/yyyy";
		return localDateFormat;
	}

	// this is used to format between Java Date and local dateTime format (used by backups, page lock, and database action) - not threadsafe so new instance each time
	public String getLocalDateTimeFormat() {
		String localDateTimeFormat = (String) getServletContext().getAttribute("localDateTimeFormat");
		if (localDateTimeFormat == null) localDateTimeFormat = "dd/MM/yyyy HH:mm a";
		return localDateTimeFormat;
	}

	// this is used to format between Java Date and Local date format - not threadsafe so new instance each time
	public SimpleDateFormat getLocalDateFormatter() {
		return new SimpleDateFormat(getLocalDateFormat());
	}

	// this is used to format between Java Date and local dateTime format (used by backups, page lock, and database action) - not threadsafe so new instance each time
	public SimpleDateFormat getLocalDateTimeFormatter() {
		return new SimpleDateFormat(getLocalDateTimeFormat());
	}

	// any control and action suffix
	public String getControlAndActionSuffix() {
		// look for a controlAndActionPrefix
		String controlAndActionSuffix = getServletContext().getInitParameter("controlAndActionSuffix");
		// update to empty string if not present - this is the default and expected for older versions of the web.xml
		if (controlAndActionSuffix == null) controlAndActionSuffix = "";
		// return
		return controlAndActionSuffix;
	}

	// whether the connection adapter allows the public unauthenticated access
	public boolean isPublic() {
		// return the  value of the parameter - this is set in the RapidAuthenticationAdapter
		return (Boolean) getServletContext().getAttribute(RapidAuthenticationAdapter.INIT_PARAM_PUBLIC_ACCESS);
	}

	// allowed upload mimetypes - set in web.xml with uploadMimeTypes, must correspond with uploadMimeTypeBytes
	public List<String> getUploadMimeTypes() {
		// if we don't have one yet
		if (_uploadMimeTypes == null) {
			// make new one
			_uploadMimeTypes = new ArrayList<String>();
			// get the allowed upload mimetypes from the web.xml file
			String uploadMimeTypes = getServletContext().getInitParameter("uploadMimeTypes");
			// default if null
			if (uploadMimeTypes == null) uploadMimeTypes = "image/bmp,image/gif,image/jpeg,image/png,application/pdf";
			// get list and loop
			for (String mimeType : uploadMimeTypes.split(",")) {
				// add to list
				_uploadMimeTypes.add(mimeType);
			}
		}
		// return
		return _uploadMimeTypes;
	}

	// allowed upload mimetypes - set in web.xml with uploadMimeTypeBytes, must correspond with uploadMimeTypes
	public Map<String, List<byte[]>> getUploadMimeTypeBytes() {
		// if we don't have one yet
		if (_uploadMimeTypeBytes == null) {
			// make new one
			_uploadMimeTypeBytes = new HashMap<String, List<byte[]>>();
			// get the allowed upload mimetype bytes from the web.xml file
			String uploadMimeTypeBytes = getServletContext().getInitParameter("uploadMimeTypeBytes");
			// default if null
			if (uploadMimeTypeBytes == null) uploadMimeTypeBytes = "424D,47494638,FFD8FF,89504E47,25504446";
			// get string bytes
			String[] signatureBytes = uploadMimeTypeBytes.split(",");

			// get list and loop
			for (int i = 0; i < signatureBytes.length; i++ ) {
				// get the ith mimetype from the list
				String mimeType = _uploadMimeTypes.get(i);
				// get the mimetypebytes array the list - returns null if its byte array doesn't exist
				List<byte[]> bytesList = _uploadMimeTypeBytes.get(mimeType);
				// initialise an array list if it doesnt exist
				if (bytesList == null) bytesList = new ArrayList<byte[]>();
				// add bytes to list
				bytesList.add(Bytes.fromHexString(signatureBytes[i]));
				// add list to map
				_uploadMimeTypeBytes.put(mimeType, bytesList);
			}
		}
		// return
		return _uploadMimeTypeBytes;
	}

	// force the uploadMimeTypes to refresh
	public void resetMimeTypes() {
		_uploadMimeTypes = null;
		_uploadMimeTypeBytes = null;
	}

	// this is used is actions such as database and webservice to cache results for off-line demos
	public ActionCache getActionCache() {
		return (ActionCache) getServletContext().getAttribute("actionCache");
	}

	// encrypt a value with the encryption adapter, if there is one
	public String encryptValue(String value) throws GeneralSecurityException, IOException {
		if (value != null && _encryptionProvider != null) return _encryptionProvider.encrypt(value);
		return value;
	}

	// decrypt a value  with the encryption adapter, if there is one
	public String decryptValue(String value) throws GeneralSecurityException, IOException {
		if (value != null && _encryptionProvider != null) return _encryptionProvider.decrypt(value);
		return value;
	}

	// send the user an exception in a formatted page
	public void sendException(RapidRequest rapidRequest, HttpServletResponse response, Exception ex) throws IOException {

		// set the status
		response.setStatus(500);

		// get a writer to put the content in
		PrintWriter out = response.getWriter();

		// print the message
		out.print( ex.getLocalizedMessage());

		// if showStackTrace is set in web.xml
		boolean showStackTrace = Boolean.parseBoolean(getServletContext().getInitParameter("showStackTrace"));

		// print the stack trace if requested
		if (showStackTrace) out.print(Exceptions.getStringStackTrace(ex));

		// check for rapid request
		if (rapidRequest == null) {

			// simple log if none
			_logger.error(ex);

		} else {

			// include rapid request details if we have one
			_logger.error(ex.getLocalizedMessage() + "\n" + rapidRequest.getDetails(), ex);

		}

		// close the writer
		out.close();

	}

	// override to the above
	public void sendException(HttpServletResponse response, Exception ex) throws IOException {
		sendException(null, response, ex);
	}

	// send the user a general message in a formatted page
	public void sendMessage(HttpServletResponse response, int status, String title, String message ) throws IOException {

		// set the status
		response.setStatus(status);

		// set the content type
		response.setContentType("text/html");

		// get a writer to put the content in
		PrintWriter out = response.getWriter();

		// write a header
		out.write("<html>\n  <head>\n    <title>Rapid - " + title + "</title>\n    <meta charset=\"utf-8\"/>\n    <link rel='stylesheet' type='text/css' href='styles/index.css'></link>\n  </head>\n");

		// write body
		out.write("  <body>\n    <div class=\"image\"><a href=\"http://www.rapid-is.co.uk\"><img title=\"Rapid Information Systems\" src=\"images/RapidLogo.svg\" /></a></div>\n    <div class=\"midTitle\"><span>Rapid</span></div>\n    <div class=\"subBar\"><span class=\"link\"><a href=\"logout.jsp\">LOG OUT</a></span><span class=\"versionColumn\">" + Rapid.VERSION + "</span></div>\n    <div class=\"body\">\n      <div class=\"columnMiddle\">\n          <div class=\"info\">\n            <h1>" + title + "</h1>\n            <p>" + message + "</p>\n          </div>\n        </div>\n      </div>\n    </div>\n  </body>\n</html>");

		// close the writer
		out.close();

	}

}
