/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;

import com.rapid.server.RapidHttpServlet;

public class Device {

	@XmlRootElement
	public static class Devices extends ArrayList<Device> {

		// properties (for marshalling / unmarshalling of this object)
		public Devices getDevices() { return this; }
		public void setDevices(Devices devices) {
			// jaxb on some platforms sends the unmarshalled object (itself) into this method
			if (this != devices) {
				this.clear();
				this.addAll(devices);
			}
		}

		// static methods
		public static Devices load(ServletContext servletContext) throws JAXBException, IOException {

			// create the list
			Devices devices = null;

			// get the file in which the device xml file is stored
			File file = new File(servletContext.getRealPath("/") + "/WEB-INF/devices/devices.xml");

			// if it exists
			if (file.exists()) {
				try {
					// get the unmarshaller from the context
					Unmarshaller unmarshaller = RapidHttpServlet.getUnmarshaller();
					// unmarshall the devices
					devices = (Devices) unmarshaller.unmarshal(file);
				} catch (Exception ex) {
					// log
					LogManager.getLogger(Device.class).error("Error loading devices", ex);
				}
			}

			// create a new list if we haven't got one yet
			if (devices == null) devices = new Devices();

			// assume the top device is not a normal monitor (called desktop)
			boolean gotScreen = false;
			// check device count
			if (devices.size() > 0) {
				// get top device
				Device device = devices.get(0);
				// if this is the screen
				if ("Desktop".equals(device.getName()) && device.getPPI() == 96 && device.getScale() == 1d) gotScreen = true;
			}

			// if we don't have a screen
			if (!gotScreen) {
				// add it to the top of the collection
				devices.add(0, new Device("Desktop", 0, 0, 96, 1d));
				// save the file
				devices.save(servletContext);
			}

			// add them to the servlet context
			servletContext.setAttribute("devices", devices);

			// return the devices
			return devices;

		}

		// instance methods
		public void save(ServletContext servletContext) throws JAXBException, IOException {

			// get the file in which the control xml files are stored
			File file = new File(servletContext.getRealPath("/") + "/WEB-INF/devices/devices.xml");
			// make dirs if need be
			if (!file.exists()) file.getParentFile().mkdirs();

			// get the marshaller from the context
			Marshaller marshaller = RapidHttpServlet.getMarshaller();
			// marshall the devices to the file
			marshaller.marshal(this, file);

		}

	}

	// private instance variables
	private String _name;
	private int _width, _height, _ppi;
	private double _scale;

	// properties
	public String getName() { return _name; }
	public void setName(String name) { _name = name; }

	public int getWidth() { return _width; }
	public void setWidth(int width) { _width = width; }

	public int getHeight() { return _height; }
	public void setHeight(int height) { _height = height; }

	public int getPPI() { return _ppi; }
	public void setPPI(int ppi) { _ppi = ppi; }

	public double getScale() { return _scale; }
	public void setScale(double scale) { _scale = scale; }

	// constructors
	public Device() {}
	public Device(String name, int width, int height, int ppi, double scale) {
		_name = name;
		_width = width;
		_height = height;
		_ppi = ppi;
		_scale = scale;
	}

}
