<!DOCTYPE html encoding="utf-8">
<%@ page language="java" contentType="text/html;charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="com.rapid.core.*" %>
<%@ page import="com.rapid.server.Rapid" %>
<%@ page import="com.rapid.server.RapidRequest" %>
<%@ page import="com.rapid.server.filter.*" %>
<%

/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

// log that this is loading
Logger.getLogger(this.getClass()).debug("designpage.jsp request : " + request.getQueryString());
// retain a ref to the app
Application rapidApp = null;
// retain a ref to the page
Page rapidPage = null;
// retain whether we got an app and a pge
boolean gotAppAndPage = false;
// retain whether we have permission
boolean designerPermission = false;
// get the app parameter
String appId = request.getParameter("a");
//get the version parameter
String version = request.getParameter("v");
//get the page parameter
String pageId = request.getParameter("p");

// check we have both an app and a page
if (appId != null && pageId != null) {
	
	//get the applications
	Applications applications = (Applications) getServletContext().getAttribute("applications");
	// get the app version
	rapidApp = applications.get(appId, version);
	// check we got an app
	if (rapidApp != null) {
		// get the page
		rapidPage = rapidApp.getPage(pageId);
		// check we got the page
		if (rapidPage != null) {
			// we got an app and a page
			gotAppAndPage = true;	
			// get a rapid request
			RapidRequest rapidRequest = new RapidRequest(request);
			// check we have the RapidDesign permission in the security provider for this app
			designerPermission = rapidApp.getSecurity().checkUserRole(rapidRequest, Rapid.DESIGN_ROLE);
			// if this is the rapid app the super permission is required too
			if ("rapid".equals(rapidApp.getId())) designerPermission = designerPermission && rapidApp.getSecurity().checkUserRole(rapidRequest, Rapid.SUPER_ROLE);	
		}							
	}
}
%>

<html>
<head>	
	<title>Rapid Desktop - Design Page</title>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8">	
<%

if (gotAppAndPage && designerPermission) {

	// add the required resource links, but not the css
	out.print(rapidPage.getResourcesHtml(rapidApp, false));	
	
%>
	<style type="text/css">
	
	html {
		height: 100%;
	}
	
	body {
		transform-origin: 0 0;
	}
	
	.pageLoading {
		text-align: center;	
		margin: 100px auto;
		font-size: 20px;		
		padding: 20px;
		border: 4px solid black;
		border-radius: 10px;
		width: 300px;
		background: white;
		box-shadow: 5px 5px 5px #888;
		font-family: Arial;
	}
	
	.pageLoading p {	
		margin: 5px;
	}
			
	</style>
<%
} else {
%>
	<link rel="stylesheet" type="text/css" href="styles/designer.css"></link>
<%
}
%>
</head>
<body>
<%
if (!gotAppAndPage) {
%>
	<div><h3>Application and page cannot be found</h3></div>
<%
} else if (!designerPermission) {
%>
	<div><h3>You do not have permission to load this page in Rapid Design - contact your administrator</h3></div>	
<%
} else {
%>
	<div><div class="pageLoading"><p><img src="images/wait_220x19.gif" /></p><p style='margin-left:20px;'>loading...</p></div></div>
<%
} 
%>
</body>
</html>