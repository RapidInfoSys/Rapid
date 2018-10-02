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

package com.rapid.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class Http {

	public static class Header {

		// private instance variables
		String _name, _value;

		// properties
		public String getName() { return _name; }
		public String getValue() { return _value; }

		// constructor
		public Header(String name, String value) {
			_name = name;
			_value = value;
		}

	}

	// helper method
	public static List<Header> getHeaders(Header... headers) {
		List<Header> headersList = new ArrayList<Header>();
		if (headers != null) {
			for (Header header : headers) {
				headersList.add(header);
			}
		}
		return headersList;
	}

	// main method

	public static String request(boolean post, String url, List<Header> headers, String body) throws IOException {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// if POST
		if (post) {
			//add request header
			con.setRequestMethod("POST");

			// Set up for post request
			con.setDoOutput(true);
		}

		// look for any headers
		if (headers != null) {
			// loop them
			for (Header header : headers) {
				con.setRequestProperty(header.getName(), header.getValue());
			}
		}

		if (post) {
			// get the writer
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			// write the request
			wr.writeBytes(body);
			// flush
			wr.flush();
			// close
			wr.close();
		}

		// get the response code
		int responseCode = con.getResponseCode();
		// placeholder for response body
		String response = null;

		// check if all ok
		if (responseCode == 200) {
			// all ok get response from input stream
			response = Strings.getString(con.getInputStream());
		} else {
			// not ok get response from error stream
			response = Strings.getString(con.getErrorStream());
		}

		// return response
		return response;

	}

	// overloads

	public static String post(String url, List<Header> headers, String body) throws IOException {
		return request(true, url, headers, body);
	}

	public static String post(String url, String body) throws IOException {
		return request(true, url, null, body);
	}

	public static String get(String url, List<Header> headers) throws IOException {
		return request(false, url, headers, null);
	}

	public static String get(String url) throws IOException {
		return request(false, url, null, null);
	}

	public static String postSOAP(String url, String soapAction, String body) throws IOException {
		// create the headers using our speedy helper method
		List<Header> headers = getHeaders(new Header("SOAPAction",soapAction), new Header("Content-Type","text/xml"));
		return request(true, url, headers, body);
	}

	// return the base url of the server as seen by the client, from the request
	public static String getBaseUrl(HttpServletRequest request) {
	    String scheme = request.getScheme() + "://";
	    String serverName = request.getServerName();
	    String serverPort = (request.getServerPort() == 80) ? "" : ":" + request.getServerPort();
	    String contextPath = request.getContextPath();
	    return scheme + serverName + serverPort + contextPath;
	}

}
