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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;

import com.rapid.data.ConnectionAdapter.ConnectionAdapterException;
import com.rapid.server.RapidRequest;

public class DataFactory {

	public static class Parameter {

		public static final int NULL = 1;
		public static final int STRING = 2;
		public static final int DATE = 3;
		public static final int INTEGER = 4;
		public static final int FLOAT = 5;
		public static final int DOUBLE = 6;
		public static final int LONG = 7;

		private int _type;
		private String _string;
		private Date _date;
		private int _int;
		private float _float;
		private double _double;
		private long _long;

		public Parameter() {
			_type = NULL;
		}

		public Parameter(String value) {
			_type = STRING;
			_string = value;
		}

		public Parameter(Date value) {
			_type = DATE;
			_date = value;
		}

		public Parameter(java.util.Date value) {
			Date date = new Date(value.getTime());
			_type = DATE;
			_date = date;
		}

		public Parameter(int value) {
			_type = INTEGER;
			_int = value;
		}

		public Parameter(float value) {
			_type = FLOAT;
			_float = value;
		}

		public Parameter(double value) {
			_type = DOUBLE;
			_double = value;
		}

		public Parameter(long value) {
			_type = LONG;
			_long = value;
		}

		public int getType() { return _type; }
		public String getString() { return _string; }
		public Date getDate() { return _date; }
		public int getInteger() { return _int; }
		public float getFloat() { return _float; }
		public double getDouble() { return _double; }
		public long getLong() { return _long; }

		@Override
		public String toString() {
			switch (_type) {
			case NULL : return "null";
			case STRING : return _string;
			case DATE : return _date.toString();
			case INTEGER : return Integer.toString(_int);
			case FLOAT : return Float.toString(_float);
			case DOUBLE : return Double.toString(_double);
			case LONG : return Long.toString(_long);
			}
			return "unknown type";
		}

	}

	@SuppressWarnings("serial")
	public static class Parameters extends ArrayList<Parameter> {

		public void addNull() { this.add(new Parameter()); }
		public void addString(String value) { this.add(new Parameter(value)); }
		public void addInt(int value) { this.add(new Parameter(value)); }
		public void addDate(Date value) { this.add(new Parameter(value)); }
		public void addFloat(float value) { this.add(new Parameter(value)); }
		public void addDouble(double value) { this.add(new Parameter(value)); }
		public void addLong(long value) { this.add(new Parameter(value)); }
		public void add() { this.add(new Parameter()); }
		public void add(String value) { this.add(new Parameter(value)); }
		public void add(int value) { this.add(new Parameter(value)); }
		public void add(Date value) { this.add(new Parameter(value)); }
		public void add(java.util.Date value) { this.add(new Parameter(value)); }
		public void add(float value) { this.add(new Parameter(value)); }
		public void add(double value) { this.add(new Parameter(value)); }
		public void add(long value) { this.add(new Parameter(value)); }

		public Parameters() {}
		public Parameters(Object...parameters) {
			if (parameters != null) {
				for (Object object : parameters) {
					if (object== null) {
						this.add(new Parameter());
					} else if (object instanceof String) {
						String v = (String) object;
						this.add(new Parameter(v));
					} else if (object instanceof Integer) {
						Integer v = (Integer) object;
						this.add(new Parameter(v));
					} else if (object instanceof Date) {
						Date v = (Date) object;
						this.add(new Parameter(v));
					} else if (object instanceof java.util.Date) {
						java.util.Date v = (java.util.Date) object;
						this.add(new Parameter(v));
					} else if (object instanceof Float) {
						Float v = (Float) object;
						this.add(new Parameter(v));
					} else if (object instanceof Double) {
						Double v = (Double) object;
						this.add(new Parameter(v));
					} else if (object instanceof Long) {
						Long v = (Long) object;
						this.add(new Parameter(v));
					}
				}
			}
		}

		@Override
		public String toString() {
			String parametersString = "";
			for (int i = 0; i < this.size(); i++) {
				Parameter parameter = this.get(i);
				if (parameter.getString() == null) {
					parametersString += parameter.toString();
				} else {
					parametersString += "'" + parameter.toString() + "'";
				}
				if (i < this.size() - 1) parametersString += ", ";
			}
			return parametersString;
		}

	}

	// private instance variables

	private ConnectionAdapter _connectionAdapter;
	private String _sql;
	private boolean _autoCommit, _readOnly;
	private Connection _connection;
	private PreparedStatement _preparedStatement;
	private ResultSet _resultset;

	// constructors

	public DataFactory(ConnectionAdapter connectionAdapter) {
		_connectionAdapter = connectionAdapter;
		_autoCommit = true;
	}

	public DataFactory(ConnectionAdapter connectionAdapter, boolean autoCommit) {
		_connectionAdapter = connectionAdapter;
		_autoCommit = autoCommit;
	}

	// public methods

	public ConnectionAdapter getConnectionAdapter() {
		return _connectionAdapter;
	}

	public Connection getConnection(RapidRequest rapidRequest) throws SQLException, ClassNotFoundException, ConnectionAdapterException {
		_connection = _connectionAdapter.getConnection(rapidRequest);
		_connection.setAutoCommit(_autoCommit);
		_connection.setReadOnly(_readOnly);
		return _connection;
	}

	public boolean getAutoCommit() { return _autoCommit; }
	public void setAutoCommit(boolean autoCommit) {	_autoCommit = autoCommit; }
	
	public boolean getReadOnly() { return _readOnly; }
	public void setReadOnly(boolean readOnly) {	_readOnly = readOnly; }		

	private void populateStatement(PreparedStatement statement, ArrayList<Parameter> parameters, int startColumn, boolean checkParameters) throws SQLException {

		// some jdbc drivers can't get parameter metadata for insert / update
		boolean isUpdateOrInsert = _sql.trim().toLowerCase().startsWith("insert") || _sql.trim().toLowerCase().startsWith("update");
		boolean isSqlServer = _connectionAdapter._connectionString.contains("sqlserver");

		ParameterMetaData parameterMetaData = null;
		if(!isUpdateOrInsert && !isSqlServer)
			parameterMetaData = statement.getParameterMetaData();

		if (checkParameters && parameters == null) {

			if (parameterMetaData!=null && parameterMetaData.getParameterCount() > 0) throw new SQLException("SQL has " + parameterMetaData.getParameterCount() + " parameters, none provided");

		} else {

			if (parameterMetaData!=null && checkParameters && parameterMetaData.getParameterCount() - startColumn != parameters.size()) throw new SQLException("SQL has " + parameterMetaData.getParameterCount() + " parameters, " + (parameters.size() - startColumn) + " provided");

			int i = startColumn;

			for (Parameter parameter : parameters) {

				i++;

				switch (parameter.getType()) {
				case Parameter.NULL :
					statement.setNull(i, Types.NULL);
					break;
				case Parameter.STRING :
					if (parameter.getString() == null) {
						statement.setNull(i, Types.NULL);
					} else {
						statement.setString(i, parameter.getString());
					}
					break;
				case Parameter.DATE :
					if (parameter.getDate() == null) {
						statement.setNull(i, Types.NULL);
					} else {
						statement.setTimestamp(i, new Timestamp(parameter.getDate().getTime()));
					}
					break;
				case Parameter.INTEGER :
					statement.setInt(i, parameter.getInteger());
					break;
				case Parameter.FLOAT :
					statement.setFloat(i, parameter.getFloat());
					break;
				case Parameter.DOUBLE :
					statement.setDouble(i, parameter.getDouble());
					break;
				case Parameter.LONG :
					statement.setLong(i, parameter.getLong());
					break;
				}
			}

		}

	}

	public PreparedStatement getPreparedStatement(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException  {

		// some jdbc drivers need various modifications to the sql
		if (_connectionAdapter.getDriverClass().contains("sqlserver")) {
			// line breaks in the sql replacing - here's looking at you MS SQL!
			_sql = sql.replace("\n", " ");
		} else {
			// otherwise just retain
			_sql = sql;
		}

		if (_connection == null) _connection = getConnection(rapidRequest);

		if (_preparedStatement != null) _preparedStatement.close();

		_preparedStatement = _connection.prepareStatement(_sql);

		// don't check parameters for execute queries
		populateStatement(_preparedStatement, parameters, 0, !sql.trim().toLowerCase().startsWith("exec"));

		return _preparedStatement;

	}
	
	private ResultSet getFirstResultSet(PreparedStatement preparedStatement) throws SQLException {
		
		preparedStatement.execute();
	
		_resultset = preparedStatement.getResultSet();
	
		while (_resultset == null && (preparedStatement.getMoreResults() || preparedStatement.getUpdateCount() > -1)) {
			_resultset = preparedStatement.getResultSet();
		}
			
		return _resultset;
	
	}
	public ResultSet getPreparedResultSet(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		return getFirstResultSet(getPreparedStatement(rapidRequest, sql, parameters));

	}

	public ResultSet getPreparedResultSet(RapidRequest rapidRequest, String sql, Object... parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		Parameters params = new Parameters(parameters);

		return getFirstResultSet(getPreparedStatement(rapidRequest, sql, params));

	}

	public int getPreparedUpdate(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		if (sql.trim().toLowerCase().startsWith("begin")) {
			
			_sql = sql;

			CallableStatement cs = getConnection(rapidRequest).prepareCall(sql);

			populateStatement(cs, parameters, 0, false);

			cs.execute();

			return cs.getUpdateCount();

		} else {

			return getPreparedStatement(rapidRequest, sql, parameters).executeUpdate();

		}

	}

	public int getPreparedUpdate(RapidRequest rapidRequest, String SQL, Object... parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		Parameters params = new Parameters(parameters);

		return getPreparedUpdate(rapidRequest, SQL, params);

	}

	public String getPreparedScalar(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		String result = null;

		if (sql != null) {

			String sqlCheck = sql.trim().toLowerCase();

			if (sqlCheck.startsWith("select")) {

				_resultset = getFirstResultSet(getPreparedStatement(rapidRequest, sql, parameters));

				if (_resultset.next()) result = _resultset.getString(1);

			} else if (sqlCheck.startsWith("insert") || sqlCheck.startsWith("update") || sqlCheck.startsWith("delete"))  {

				result = Integer.toString(getPreparedUpdate(rapidRequest, sql, parameters));

			} else {
				
				_sql = sql;

				if (_connection == null) _connection = getConnection(rapidRequest);

				CallableStatement st = _connection.prepareCall("{? = call " + sql + "}");

				_preparedStatement = st;

				populateStatement(st, parameters, 1, false);

				st.registerOutParameter(1, Types.NVARCHAR);

				st.execute();

				result = st.getString(1);

			}

		}

		return result;

	}

	public String getPreparedScalar(RapidRequest rapidRequest, String SQL, Object... parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		Parameters params = new Parameters(parameters);

		return getPreparedScalar(rapidRequest, SQL, params);

	}

	public void commit() throws SQLException {

		if (_connection != null) _connection.commit();

	}

	public void rollback() throws SQLException {

		if (_connection != null) _connection.rollback();

	}

	public void close() throws SQLException {

		if (_preparedStatement != null) _preparedStatement.close();

		if (_connectionAdapter != null && _connection != null) {
			_connectionAdapter.closeConnection(_connection);
		} else if (_connection != null) {
			_connection.close();
		}

	}

}
