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

package com.rapid.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.utils.Files;

public class ActionCache {

	// private static variables
	private static Logger _logger = LogManager.getLogger(ActionCache.class);
	private static Marshaller _marshaller;
	private static Unmarshaller _unmarshaller;

	@XmlRootElement
	public static class Cache {

		// private variables

		private File _cacheFile;
		private Map<String, String> _cache;

		// constructors

		// JAXB
		public Cache() {}
		// ActionCache
		public Cache(ServletContext servletContext, String appId) throws JAXBException {

			// create a file object for the cache.xml file
			_cacheFile = new File(servletContext.getRealPath("/") + "/WEB-INF/applications/" + Files.safeName(appId + "/cache.xml"));

			if (_cacheFile.exists()) {
				// unmarshall the file into an object
				Cache actionCache = (Cache) _unmarshaller.unmarshal(_cacheFile);
				// bring that objects cache into this one
				_cache = actionCache.getCache();
			} else {
				_cache = new HashMap<String, String>();
			}

			_logger.info("Action cache initialised for " + appId);

		}

		// properties (these are required to be marshalled)

		public Map<String, String> getCache() { return _cache; }
		public void setCache(Map<String, String> cache) { _cache = cache; }

		// public methods

		public void save() throws IOException, JAXBException  {

			// create a temp file for saving the application to
			File tempFile = new File(_cacheFile.getParentFile().getAbsolutePath() + "/cache-saving.xml");

			// get a file output stream to write the data to
			FileOutputStream fos = new FileOutputStream(tempFile.getAbsolutePath());

			// marshal the security object to the temp file
			_marshaller.marshal(this, fos);

			// close the stream
		    fos.close();

		    // copy / overwrite the app file with the temp file
		    Files.copyFile(tempFile, _cacheFile);

		    // delete the temp file
		    tempFile.delete();

		}

	}

	// private variables

	private ServletContext _servletContext;
	private Map<String,Cache> _applicationCaches;

	// constructors

	public ActionCache(ServletContext servletContext) throws JAXBException {

		// retain servletContext
		_servletContext = servletContext;

		// create the JAXB context and marshalers for this
		JAXBContext jaxb = JAXBContext.newInstance(Cache.class);
		_marshaller = jaxb.createMarshaller();
		_marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		_unmarshaller = jaxb.createUnmarshaller();

		// initialise the map of all application caches
		_applicationCaches = new HashMap<String,Cache>();

	}


	// public methods

	// converts the marshalled string into a JSONObject and stores it in the appropriate application cache
	public JSONObject get(String appId, String actionId, String key) throws JSONException, JAXBException {
		// get the appropriate application cache
		Cache appCache = _applicationCaches.get(appId);
		// create if null
		if (appCache == null) appCache = new Cache(_servletContext, appId);
		// fetch any data from the cache
		String value = appCache.getCache().get(actionId + key);
		// check for null
		if (value == null) {
			return null;
		} else {
			// convert to JSON before returning
			return new JSONObject(value);
		}
	}

	// converts the marshalled string into a JSONObject and stores it in the appropriate application cache
	public void put(String appId, String actionId, String key, JSONObject jsonObject) throws JAXBException {
		// get the appropriate application cache
		Cache appCache = _applicationCaches.get(appId);
		// create if null
		if (appCache == null) appCache = new Cache(_servletContext, appId);
		// add data to the cache
		appCache.getCache().put(actionId + key, jsonObject.toString());
		// attempt to save the cache (one day this might be on it's own queue thread to avoid file locking)
		try {
			appCache.save();
		} catch (Exception ex) {
			_logger.error(ex);
		}
	}

}
