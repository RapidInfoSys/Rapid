/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.sqlite.SQLiteConnection;

import com.rapid.data.ConnectionAdapter.ConnectionAdapterException;
import com.rapid.server.RapidRequest;

// this class extends the data factory for SQLite providing a static connection for updates and synchronisation to avoid file locks
public class SQLiteDataFactory extends DataFactory {

	// the static connection used by all overrides
	protected static SQLiteConnection _staticConnection;
	// a lock object for synchronising the update calls which we make in the constructor
	protected static Object _lock;

	// constructors
	public SQLiteDataFactory(ConnectionAdapter connectionAdapter) {
		// call the super
		super(connectionAdapter);
		// create the object we will use for locking
		if (_lock == null) _lock = new Object();
	}

	public SQLiteDataFactory(ConnectionAdapter connectionAdapter, boolean autoCommit) {
		// call the super
		super(connectionAdapter, autoCommit);
		// create the object we will use for locking
		if (_lock == null) _lock = new Object();
	}


	// we change the process in this override ever so slightly to allow non-update calls to have their own connection
	@Override
	public Connection getConnection(RapidRequest rapidRequest) throws SQLException, ClassNotFoundException, ConnectionAdapterException {
		SQLiteConnection sqliteConnection = (SQLiteConnection) _connectionAdapter.getConnection(rapidRequest);
		sqliteConnection.setTransactionIsolation(SQLiteConnection.TRANSACTION_READ_UNCOMMITTED); // allow dirty read - it doesn't seem to make much difference, committing the updates was the big thing
		sqliteConnection.setBusyTimeout(20000); // a 20 second busy timeout - updates must be COMMITED!
		_connection = sqliteConnection;
		return _connection;
	}

	// this new method ensures the connection used on update calls is the static one and is used in a synchronised way
	public PreparedStatement getPreparedUpdateStatement(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException  {

		// trim and retain sql
		_sql = sql.trim();

		// close any previous statement
		if (_preparedStatement != null) _preparedStatement.close();

		// synchronise the execution of prepared statements on the static connection to avoid file lock errors
		synchronized(_lock) {

			// make a new static connection if we need one
			if (_staticConnection == null) {
				_staticConnection = (SQLiteConnection) _connectionAdapter.getConnection(rapidRequest);
				_staticConnection.setAutoCommit(true);
				_staticConnection.setReadOnly(false);
				_staticConnection.setBusyTimeout(1000);
			}

			// get our prepared statement
			_preparedStatement = _staticConnection.prepareStatement(_sql);

		}

		// don't check parameter numbers for exec queries
		populateStatement(rapidRequest, _preparedStatement, parameters, 0, !_sql.startsWith("exec"));

		return _preparedStatement;

	}

	// this override uses the new getPreparedUpdateStatement
	@Override
	public int getPreparedUpdate(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		// get a prepared statement (also synchronised)
		PreparedStatement ps = getPreparedUpdateStatement(rapidRequest, sql, parameters);

		// assume an issue with the rows updated
		int rows = -1;

		// synchronise the execution of prepared statements on the static connection to avoid file lock errors
		synchronized(_lock) {

			// get the updated rows
			rows = ps.executeUpdate();

		}

		// close the statement
		ps.close();

		// return the rows
		return rows;

	}

	// must commit to clear any locks on the update connections so the select ones can proceed
	@Override
	public void commit() throws SQLException {
		// only the static connection is doing updates so it's the one to commit - disabled for now as autocommit is true
		// if (_staticConnection != null) _staticConnection.commit();
	}

	@Override
	public void rollback() throws SQLException {
		// only the static connection is doing updates so it's the one to rollback
		// if (_staticConnection != null) _staticConnection.rollback();
	}

	// a close all method which also closes the static connection and should be used for cleanup/shutdown - disable for now as autocommit is true
	public void closeAll() throws SQLException {

		// close the non-static connection
		close();

		// if we have a connection adapter and a static connection
		if (_connectionAdapter != null && _staticConnection != null) {
			// have the adapter close the connection
			_connectionAdapter.closeConnection(_staticConnection);
		} else if (_staticConnection != null) {
			// just close the static connection if that's all we have
			_staticConnection.close();
		}

	}

}
