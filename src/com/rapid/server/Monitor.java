package com.rapid.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rapid.server.RapidRequest;

public class Monitor {
	private static Logger _logger = LogManager.getLogger(Monitor.class);

	// monitor status and mode
	private boolean _isAlive = false;
	private boolean _hasStarted = false;
	private boolean _isLoggingExceptions;
	private boolean _isLoggingAll;

	// database
	private Connection _connection;
	private PreparedStatement _preparedInsertStatement;

	// entry data
	private java.sql.Timestamp _startTime;
	private String _details;
	private String _actionName;
	private String _appId;
	private String _appVersion;

	public Monitor() {
	}

	public void setUpMonitor(ServletContext servletContect) throws SQLException  {
		String connectionString = servletContect.getInitParameter("monitor.jdbc");
		String username = servletContect.getInitParameter("monitor.user");
		String password = servletContect.getInitParameter("monitor.password");
		
		if(connectionString==null || connectionString.length()==0 || username==null || username.length()==0 || password==null || password.length()==0) {
			_logger.debug("Monitoring not initialised");
			_isAlive = false;
			_hasStarted = true;
			return;
		}

		_connection = DriverManager.getConnection(connectionString, username, password);
		
		_isAlive = isDatabaseConnectionActive();
		if(_isAlive)
			_preparedInsertStatement = _connection.prepareStatement("insert monitor(url, serverName, context, username, appId, appVersion, pageId, actionId, actionType, actionName, details, requestSize, responseSize, component, ipAddress, requestDate, responseDate, respondeCode, exception) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		else
			_logger.debug("Monitoring not initialised");
		
		String loggingMode = servletContect.getInitParameter("monitor.mode");
		if("all".equalsIgnoreCase(loggingMode)) {
			_isLoggingExceptions = true;
			_isLoggingAll = true;
		}
		if("exception".equalsIgnoreCase(loggingMode)) {
			_isLoggingExceptions = true;
			_isLoggingAll = false;
		}
	}

	public boolean isAlive(ServletContext servletContect) {
		if(_isAlive)
			return true;

		if(!_isAlive && _hasStarted)
			return false;

		_hasStarted = true;

		try {
			setUpMonitor(servletContect);
		} catch(Exception e) {
			e.printStackTrace();
		}

		return _isAlive;
	}

	public boolean isLoggingAll() {
		return _isLoggingAll;
	}

	public boolean isLoggingExceptions() {
		return _isLoggingExceptions;
	}

	public void setDetails(String details) {
		this._details = details;
	}

	public void setActionName(String actionName) {
		this._actionName = actionName;
	}

	public void setAppId(String appName) {
		this._appId = appName;
	}

	public void setAppVersion(String appVersion) {
		this._appVersion = appVersion;
	}

	public void openEntry() {
		_startTime = new java.sql.Timestamp(new java.util.Date().getTime());
		_details = null;
	}
	
	public void commitEntry(RapidRequest rapidRequest, HttpServletResponse response, long responseSize) {
		commitEntry(rapidRequest, response, responseSize, null);
	}

	public void commitEntry(RapidRequest rapidRequest, HttpServletResponse response, long responseSize, String exceptionMessage) {
		try {

			if(_connection==null)
				setUpMonitor(rapidRequest.getRapidServlet().getServletContext());
			
			String url = rapidRequest.getRequest().getRequestURL().toString();
			String query = rapidRequest.getRequest().getQueryString();
		
			String wholeUrl = url;
			if(wholeUrl.endsWith("~") && query!=null && query.length()>0)
				wholeUrl = wholeUrl.substring(0, wholeUrl.length()-1) + "?" + query;
		
			ServletRequest request = rapidRequest.getRequest();

			String appId = request.getParameter("a");
			if(_appId!=null)
				appId = _appId;
			
			String appVersion = request.getParameter("v");
			if(_appVersion!=null)
				appVersion = _appVersion;
			
			String pageId = request.getParameter("p");
			if(appId==null) {
				appVersion = null;
				pageId = null;
			}

			String server = request.getServerName();
			String context = rapidRequest.getRequest().getContextPath();
			context = context.substring(1, context.length());
			
			String uri = rapidRequest.getRequest().getRequestURI();
			int lastSlash = uri.lastIndexOf("/");
			String component = uri.substring(lastSlash+1);
			if("~".equals(component))
				component = "rapid";
			
			String userName = rapidRequest.getUserName();

			String actionId = request.getParameter("act");
			
			String actionType = null;
			if(actionId!=null)
				actionType = rapidRequest.getAction().getType();
				
			String actionName = request.getParameter("action");
			if(actionName==null) {
				actionName = _actionName;
			}
			
			if("imageUpload".equalsIgnoreCase(actionName)) {
				_details = request.getParameter("name");
			}
			
			int requestSize = request.getContentLength();
			if(requestSize<0)
				requestSize = 0;

			_preparedInsertStatement.setString(1, wholeUrl);
			_preparedInsertStatement.setString(2, server);
			_preparedInsertStatement.setString(3, context);
			_preparedInsertStatement.setString(4, userName);
			_preparedInsertStatement.setString(5, appId);
			_preparedInsertStatement.setString(6, appVersion);
			_preparedInsertStatement.setString(7, pageId);
			_preparedInsertStatement.setString(8, actionId);
			_preparedInsertStatement.setString(9, actionType);
			_preparedInsertStatement.setString(10, actionName);
			_preparedInsertStatement.setString(11, _details);
			_preparedInsertStatement.setInt(12, requestSize);
			_preparedInsertStatement.setLong(13, responseSize);
			_preparedInsertStatement.setString(14, component);
			_preparedInsertStatement.setString(15, request.getRemoteAddr());
			_preparedInsertStatement.setObject(16, _startTime);
			_preparedInsertStatement.setObject(17, new java.sql.Timestamp(new java.util.Date().getTime()));
			if(response!=null)
				_preparedInsertStatement.setInt(18, ((HttpServletResponse) response).getStatus());
			else
				_preparedInsertStatement.setInt(18, -1);
				
			_preparedInsertStatement.setString(19, exceptionMessage);
			_preparedInsertStatement.executeUpdate();
			_connection.commit();
			
			_details = null;
			_actionName = null;
			_appId = null;
			_appVersion = null;
		} catch (SQLException e) {
			_logger.debug("Monitor database open entry error: "+e.getMessage());
		}
	}

	public void createEntry(ServletContext servletContext, String appId, String appVersion, String actionName, long requestSize, long response)  {
		createEntry(servletContext, appId, appVersion, actionName, requestSize, response, null);
	}

	public void createEntry(ServletContext servletContext, String appId, String appVersion, String actionName, long requestSize, long response, String exceptionMessage) {
		try {

			if(_connection==null)
				setUpMonitor(servletContext);
			
			_preparedInsertStatement.setString(1, "SERVER SIDE");
			_preparedInsertStatement.setString(2, null);
			_preparedInsertStatement.setString(3, null);
			_preparedInsertStatement.setString(4, null);
			_preparedInsertStatement.setString(5, appId);
			_preparedInsertStatement.setString(6, appVersion);
			_preparedInsertStatement.setString(7, null);
			_preparedInsertStatement.setString(8, null);
			_preparedInsertStatement.setString(9, null);
			_preparedInsertStatement.setString(10, actionName);
			_preparedInsertStatement.setString(11, _details);
			_preparedInsertStatement.setLong(12, requestSize);
			_preparedInsertStatement.setLong(13, response);
			_preparedInsertStatement.setString(14, null);
			_preparedInsertStatement.setString(15, null);
			_preparedInsertStatement.setObject(16, new java.sql.Timestamp(new java.util.Date().getTime()));
			_preparedInsertStatement.setObject(17, new java.sql.Timestamp(new java.util.Date().getTime()));
			_preparedInsertStatement.setInt(18, -1);
			_preparedInsertStatement.setString(19, exceptionMessage);
			_preparedInsertStatement.executeUpdate();
			_connection.commit();
			
			_details = null;
			_actionName = null;
			_appId = null;
			_appVersion = null;
		} catch (SQLException e) {
			_logger.debug("Monitor database open entry error: "+e.getMessage());
		}
	}
	
	private boolean isDatabaseConnectionActive() {
		try {
			Statement statement = _connection.createStatement();  
			ResultSet resultSet = statement.executeQuery("select count(*) count from monitor");
			if(resultSet!=null) {
				resultSet.close();
				return true;
			}
			return false;
		} catch(SQLException ex) {
			return false;
		}
	}

	public void close() {
		try {
			if(_connection!=null)
				_connection.close();
			if(_preparedInsertStatement!=null)
				_preparedInsertStatement.close();
		} catch (SQLException e) {
			_logger.debug("Monitor database connection close error: "+e.getMessage());
		}
	}
}
