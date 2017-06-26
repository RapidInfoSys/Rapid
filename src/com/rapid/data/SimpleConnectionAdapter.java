/*

Copyright (C) 2017 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletContext;

import com.rapid.server.RapidRequest;

/*

The simplest adapter there is. Does nothing special.

*/

public class SimpleConnectionAdapter extends ConnectionAdapter {

	public SimpleConnectionAdapter(ServletContext servletContext, String driverClassName, String connectionString, String userName, String password) {
		super(
			servletContext,
			driverClassName,
			connectionString,
			userName,
			password
		);
	}

	public SimpleConnectionAdapter(ServletContext servletContext, DatabaseConnection databaseConnection) {
		super(
			servletContext,
			databaseConnection.getDriverClass(),
			databaseConnection.getConnectionString(),
			databaseConnection.getUserName(),
			databaseConnection.getPassword()
		);
	}

	@Override
	public Connection getConnection(RapidRequest rapidRequest) throws ClassNotFoundException, SQLException {
		// ok a small bit of complexity which works out whether a user has been specified as getConnection does not like to be handed one if not
		// get username from superclass
		String userName = getUserName();
		// convert to empty string if null
		if (userName == null) userName = "";
		// get password from superclass
		String password = getPassword();
		// convert to empty string if null
		if (password == null) password = "";
		// get the connections string
		String connectionString = getConnectionString();
		// if user null or not set don't pass username/password
		if ("".equals(userName)) {
			return DriverManager.getConnection(connectionString);
		} else {
			return DriverManager.getConnection(connectionString, userName , password);
		}
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		// just close the connection
		connection.close();
	}

	@Override
	public void close() throws SQLException {
		// nothing to clean up
	}

}
