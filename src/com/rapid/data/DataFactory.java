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

import java.io.PrintStream;
import java.io.PrintWriter;
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

	// this class is useful for rethrowing Exceptions, say after cleaning up database connections
	public static class RethrownSQLException extends SQLException {

		private Exception _ex;

		public RethrownSQLException(Exception ex) {
			_ex = ex;
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.fillInStackTrace();
			}
		}

		@Override
		public synchronized Throwable getCause() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.getCause();
			}
		}

		@Override
		public String getLocalizedMessage() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.getLocalizedMessage();
			}
		}

		@Override
		public String getMessage() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.getMessage();
			}
		}

		@Override
		public StackTraceElement[] getStackTrace() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.getStackTrace();
			}
		}

		@Override
		public synchronized Throwable initCause(Throwable arg0) {
			if (_ex == null) {
				return null;
			} else {
				return _ex.initCause(arg0);
			}
		}

		@Override
		public void printStackTrace() {
			if (_ex != null) _ex.printStackTrace();
		}

		@Override
		public void printStackTrace(PrintStream arg0) {
			if (_ex != null) _ex.printStackTrace(arg0);
		}

		@Override
		public void printStackTrace(PrintWriter arg0) {
			if (_ex != null) _ex.printStackTrace(arg0);
		}

		@Override
		public void setStackTrace(StackTraceElement[] arg0) {
			if (_ex != null) _ex.setStackTrace(arg0);
		}

		@Override
		public String toString() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.toString();
			}
		}

	}

	// print an exception stack trace
	public static String getStringStackTrace(Exception ex) {

		String stackTrace = "\n\n";

		stackTrace += ex.getClass().getName() + "\n\n";

		if (ex.getStackTrace() != null) for (StackTraceElement element : ex.getStackTrace()) stackTrace += element + "\n";

		return stackTrace;

	}

	// protected instance variables

	protected ConnectionAdapter _connectionAdapter;
	protected String _sql;
	protected boolean _autoCommit, _readOnly;
	protected Connection _connection;
	protected PreparedStatement _preparedStatement;
	protected ResultSet _resultset;

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

	// protected methods

	protected void populateStatement(RapidRequest rapidRequest, PreparedStatement statement, ArrayList<Parameter> parameters, int startColumn, boolean checkParameters) throws SQLException {

		// get the parameter metadata - some jdbc drivers will return null, especially for more complex things like insert/update, or stored procedures
		ParameterMetaData parameterMetaData = null;

		// identify sql server
		boolean isSQLServer =  _connectionAdapter.getDriverClass().contains("sqlserver");

		// sql server has problems getting parameter meta data for non-exec scripts, especially select statements with joins falsely reporting "The multi-part identifier could not be bound"
		if (!isSQLServer || !checkParameters) parameterMetaData = statement.getParameterMetaData();

		// if we're supposed the check the parameters but didn't get any -
		if (checkParameters && parameters == null) {

			// if we have meta data to check, and it expects some
			if (parameterMetaData != null && parameterMetaData.getParameterCount() > 0) throw new SQLException("SQL has " + parameterMetaData.getParameterCount() + " parameters, none provided");

		} else {

			// if we're checking parameters and got parameter meta data from the jdbc
			if (parameterMetaData != null) {

				// if we're checking parameters
				if (checkParameters) {
					// we need exactly the same number of input and meta data parameters
					if (parameterMetaData.getParameterCount() - startColumn != parameters.size()) throw new SQLException("SQL has " + parameterMetaData.getParameterCount() + " parameters, " + (parameters.size() - startColumn) + " provided");
				} else {
					// if there are inputs parameters and no metadata parameters this is most likely due to the procedure not being found
					if (parameters.size() > 0 && parameterMetaData.getParameterCount() == 0) throw new SQLException("SQL object could not be found");
					// if there are more inputs then metadata parameters
					if (parameters.size() > parameterMetaData.getParameterCount()) throw new SQLException("SQL requires " + parameterMetaData.getParameterCount() + " parameters, " + (parameters.size() - startColumn) + " provided");
				}

			}

			// start at start column - this is for Oracle callable statements as one parameter is already used
			int i = startColumn;

			// loop the parameters
			for (Parameter parameter : parameters) {

				// parameters are 1 based
				i++;

				// check the parameter type and populate accordingly
				switch (parameter.getType()) {
				case Parameter.NULL :
					statement.setNull(i, Types.NULL);
					break;
				case Parameter.STRING :
					// get the value
					String value = parameter.getString();
					// if null
					if (value == null) {
						statement.setNull(i, Types.NULL);
					} else {
						/*
						// This was turned off as too many customers already convert their own dates and this might create problems with Oracle, also all sql server exec parameters were nvarchar, even if date
						// assume we will not alter the value
						boolean override = false;
						// check we have meta data
						if (parameterMetaData != null) {
							// get the type
							int type = parameterMetaData.getParameterType(i);
							// if the parameter at this position is actually a date
							if (value.length() > 0 && type == java.sql.Types.DATE) {
								try {
									// parse the string to a Java date
									java.util.Date date = rapidRequest.getRapidServlet().getLocalDateFormatter().parse(value);
									// set a new SQL date
									statement.setDate(i, new Date(date.getTime()));
									// remember we overrode the value
									override = true;
								} catch (ParseException e) {}
							}
						}
						// set the value if not overridden
						if (!override) statement.setString(i, value);
						*/
						statement.setString(i, value);
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

	protected ResultSet getFirstResultSet(PreparedStatement preparedStatement) throws SQLException {

		preparedStatement.execute();

		_resultset = preparedStatement.getResultSet();

		while (_resultset == null && (preparedStatement.getMoreResults() || preparedStatement.getUpdateCount() > -1)) {
			_resultset = preparedStatement.getResultSet();
		}

		return _resultset;

	}

	// public methods

	public PreparedStatement getPreparedStatement(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException  {

		// some jdbc drivers need various modifications to the sql
		if (_connectionAdapter.getDriverClass().contains("sqlserver")) {
			// line breaks in the sql replacing - here's looking at you MS SQL!
			_sql = sql.trim().replace("\n", " ");
		} else {
			// otherwise just trim and retain
			_sql = sql.trim();
		}

		if (_connection == null) _connection = getConnection(rapidRequest);

		if (_preparedStatement != null) _preparedStatement.close();

		try {

			_preparedStatement = _connection.prepareStatement(_sql);

			// don't check parameter numbers for exec queries
			populateStatement(rapidRequest, _preparedStatement, parameters, 0, !_sql.startsWith("exec"));

		} catch (SQLException ex) {

			close();

			throw new RethrownSQLException(ex);

		}

		return _preparedStatement;

	}

	public ResultSet getPreparedResultSet(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		return getFirstResultSet(getPreparedStatement(rapidRequest, sql, parameters));

	}

	public ResultSet getPreparedResultSet(RapidRequest rapidRequest, String sql, Object... parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		Parameters params = new Parameters(parameters);

		return getFirstResultSet(getPreparedStatement(rapidRequest, sql, params));

	}

	public int getPreparedUpdate(RapidRequest rapidRequest, String sql, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {

		int rows = -1;

		if (sql.trim().toLowerCase().startsWith("begin")) {

			_sql = sql;

			CallableStatement cs = getConnection(rapidRequest).prepareCall(sql);

			populateStatement(rapidRequest, cs, parameters, 0, false);

			try {

				cs.execute();

				rows = cs.getUpdateCount();

				cs.close();

			} catch (SQLException ex) {

				cs.close();

				close();

				throw new RethrownSQLException(ex);

			}

		} else {

			PreparedStatement ps = getPreparedStatement(rapidRequest, sql, parameters);

			try {

				rows = ps.executeUpdate();

				ps.close();

			} catch (SQLException ex) {

				ps.close();

				close();

				throw new RethrownSQLException(ex);

			}

		}

		return rows;

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

				_resultset.close();

			} else if (sqlCheck.startsWith("insert") || sqlCheck.startsWith("update") || sqlCheck.startsWith("delete"))  {

				result = Integer.toString(getPreparedUpdate(rapidRequest, sql, parameters));

			} else {

				_sql = sql;

				if (_connection == null) _connection = getConnection(rapidRequest);

				CallableStatement st = _connection.prepareCall("{? = call " + sql + "}");

				_preparedStatement = st;

				populateStatement(rapidRequest, st, parameters, 1, false);

				st.registerOutParameter(1, Types.NVARCHAR);

				try {

					st.execute();

					result = st.getString(1);

					st.close();

				} catch (Exception ex) {

					st.close();

					close();

					throw new RethrownSQLException(ex);

				}

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

		// close any statement that may still be open if we returned a resultset
		if (_preparedStatement != null) _preparedStatement.close();

		// if we have a connection adapter and a connection
		if (_connectionAdapter != null && _connection != null) {
			// have the adapter close the connection
			_connectionAdapter.closeConnection(_connection);
		} else if (_connection != null) {
			// just close the connection if that's all we have
			_connection.close();
		}

	}

}
