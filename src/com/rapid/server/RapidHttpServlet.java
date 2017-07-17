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

package com.rapid.server;

/*

This class wraps HttpServlet and provides a number of useful functions
Mostly getters that retrieve from the servlet context

 */

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

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
import com.rapid.utils.JAXB.EncryptedXmlAdapter;

@SuppressWarnings({"serial", "unchecked", "rawtypes"})
public class RapidHttpServlet extends HttpServlet {

	// private static variables

	private static Logger _logger = LogManager.getLogger(RapidHttpServlet.class);
	private static JAXBContext _jaxbContext;
	private static EncryptedXmlAdapter _encryptedXmlAdapter;

	// properties

	public static JAXBContext getJAXBContext() { return _jaxbContext; }
	public static void setJAXBContext(JAXBContext jaxbContext) { _jaxbContext = jaxbContext; }

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
		return getInitParameter(name);
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
			return (String) getServletContext().getAttribute("localDateFormat");
		}

		// this is used to format between Java Date and local dateTime format (used by backups, page lock, and database action) - not threadsafe so new instance each time
		public String getLocalDateTimeFormat() {
			return (String) getServletContext().getAttribute("localDateTimeFormat");
		}

	// this is used to format between Java Date and Local date format - not threadsafe so new instance each time
	public SimpleDateFormat getLocalDateFormatter() {
		return new SimpleDateFormat((String) getServletContext().getAttribute("localDateFormat"));
	}

	// this is used to format between Java Date and local dateTime format (used by backups, page lock, and database action) - not threadsafe so new instance each time
	public SimpleDateFormat getLocalDateTimeFormatter() {
		return new SimpleDateFormat((String) getServletContext().getAttribute("localDateTimeFormat"));
	}

	// this is used is actions such as database and webservice to cache results for off-line demos
	public ActionCache getActionCache() {
		return (ActionCache) getServletContext().getAttribute("actionCache");
	}

	public void sendException(HttpServletResponse response, Exception ex) throws IOException {
		sendException(null, response, ex);
	}

	public void sendException(RapidRequest rapidRequest, HttpServletResponse response, Exception ex) throws IOException {

		response.setStatus(500);

		PrintWriter out = response.getWriter();

		out.print( ex.getLocalizedMessage());

		boolean showStackTrace = Boolean.parseBoolean(getServletContext().getInitParameter("showStackTrace"));

		if (showStackTrace) {

			String stackTrace = "\n\n";

			if (rapidRequest != null) stackTrace += rapidRequest.getDetails() + "\n\n";

			stackTrace += ex.getClass().getName() + "\n\n";

			if (ex.getStackTrace() != null) for (StackTraceElement element : ex.getStackTrace()) stackTrace += element + "\n";

			out.print(stackTrace);

		}

		if (rapidRequest == null) {

			_logger.error(ex);

		} else {

			_logger.error(ex.getLocalizedMessage() + "\n" + rapidRequest.getDetails(), ex);

		}

		out.close();

	}

	public void sendMessage(HttpServletResponse response, int status, String title, String message ) throws IOException {

		response.setStatus(status);

		response.setContentType("text/html");

		PrintWriter out = response.getWriter();

		// write a header
		out.write("<html>\n  <head>\n    <title>Rapid - " + title + "</title>\n    <meta charset=\"utf-8\"/>\n    <link rel='stylesheet' type='text/css' href='styles/index.css'></link>\n  </head>\n");

		// write no permission
		out.write("  <body>\n    <div class=\"image\"><a href=\"http://www.rapid-is.co.uk\"><img src=\"images/RapidLogo_60x40.png\" /></a></div>\n    <div class=\"title\"><span>Rapid - " + title + "</span><span class=\"link\"><a href=\"logout.jsp\">log out</a></span></div>\n    <div class=\"info\"><p>" + message + "</p></div>\n  </body>\n</html>");

		out.println();

		out.close();

	}


}
