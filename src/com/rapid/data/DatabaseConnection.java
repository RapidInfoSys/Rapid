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

package com.rapid.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.rapid.core.Application;
import com.rapid.utils.JAXB.EncryptedXmlAdapter;

// the details of a database connection (WebService is defined in its own class as its extendable)
public class DatabaseConnection {

	// instance variables

	String _name, _driverClass, _connectionString, _connectionAdapterClass, _userName, _password;
	ConnectionAdapter _connectionAdapter;

	// properties

	public String getName() { return _name; }
	public void setName(String name) { _name = name; }

	public String getDriverClass() { return _driverClass; }
	public void setDriverClass(String driverClass) { _driverClass = driverClass; }

	public String getConnectionString() { return _connectionString; }
	public void setConnectionString(String connectionString) { _connectionString = connectionString; }

	public String getConnectionAdapterClass() { return _connectionAdapterClass; }
	public void setConnectionAdapterClass(String connectionAdapterClass) { _connectionAdapterClass = connectionAdapterClass; }

	public String getUserName() { return _userName; }
	public void setUserName(String userName) { _userName = userName; }

	@XmlJavaTypeAdapter( EncryptedXmlAdapter.class )
	public String getPassword() { return _password; }
	public void setPassword(String password) { _password = password; }

	// constructors
	public DatabaseConnection() {};
	public DatabaseConnection(ServletContext servletContext, Application application, String name, String driverClass, String connectionString, String connectionAdapterClass, String userName, String password) {
		_name = name;
		_driverClass = driverClass;
		_connectionString = connectionString;
		_connectionAdapterClass = connectionAdapterClass;
		_userName = userName;
		_password = password;
	}

	// instance methods

	// get the connection adapter, instantiating only if null as this is quite expensive
	public synchronized ConnectionAdapter getConnectionAdapter(ServletContext servletContext, Application application) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {

		// only if the connection adapter has not already been initialised
		if (_connectionAdapter == null) {
			// get our class
			Class classClass = Class.forName(_connectionAdapterClass);
			// initialise a constructor
			Constructor constructor = classClass.getConstructor(ServletContext.class, String.class, String.class, String.class, String.class);
			// initialise the class
			_connectionAdapter = (ConnectionAdapter) constructor.newInstance(
					servletContext,
					_driverClass,
					application.insertParameters(servletContext, _connectionString),
					_userName,
					_password) ;
		}

		return _connectionAdapter;

	}

	// set the connection adapter to null to for it to be re-initialised
	public synchronized void reset() throws SQLException {
		// close it first
		close();
		// set it to null
		_connectionAdapter = null;
	}

	// set the connection adapter to null to for it to be re-initialised
	public synchronized void close() throws SQLException  {
		if (_connectionAdapter != null) _connectionAdapter.close();
	}

}