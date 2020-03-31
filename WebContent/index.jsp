<!DOCTYPE html><%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%@ page import="java.util.Map" %><%@ page import="com.rapid.core.*" %><%@ page import="com.rapid.server.filter.*" %><%@ page import="com.rapid.server.RapidRequest" %><%@ page import="com.rapid.security.SecurityAdapter" %><%

/*

Copyright (C) 2020 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
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

//get the applications
Applications applications = (Applications) getServletContext().getAttribute("applications");
// get the rapid app
Application rapid = applications.get("rapid");
//get a rapid request
RapidRequest rapidRequest = new RapidRequest(request, rapid); 
%>
<html>
	<head>
	
		<title>Rapid - Welcome</title>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">	
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
		<link rel="icon" href="favicon.ico"></link>
		<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
		<link rel='stylesheet' type='text/css' href='styles/fonts/fontawesome/css/font-awesome.css'></link>
		<script type='text/javascript' src='scripts/<%=com.rapid.server.Rapid.JQUERY%>'></script>
		<script type="text/javascript">

if ('serviceWorker' in navigator) {
  console.log('CLIENT: service worker registration in progress.');
  navigator.serviceWorker.register('sw.js')
  .then(function() {
    console.log('CLIENT: service worker registration complete.');
  }).catch(function(error) {
    console.log('CLIENT: service worker registration failure : ' + error);
  });
} else {
  console.log('CLIENT: service worker is not supported.');
}

// JQuery is ready! 
$(document).ready( function() {	
	
	// find the div we're going to put the apps in
	var appsDiv = $("#apps");
	
	// load the apps
	$.ajax({
    	url: "~?action=getApps",
    	type: "POST",          
    	dataType: "json",    
    	data: "{}",
        error: function(server, status, error) { 
        	appsDiv.html(error); 
        },
        success: function(data) {
        	if (data && data.length > 0) {
        		
        		// build a list with links that the user has access to
        		var appHtml = "<ul>";
        		for (var i in data) {
        			var app = data[i];
        			appHtml += "<a href='" + app.id + "' style='font-size:18px; font-weight:normal;'><li>" + app.title + "</li></a>";
        		}
        		appHtml += "</ul>";
        		appsDiv.html(appHtml);

        	} else {
        		appsDiv.html("<b>You do not have permission to use any applications</b>"); 
        	}   		       	
        }
	});
	
	if (!navigator.onLine) $(document.body).addClass("offline");
});

$(window).on("offline", function() {
	$(document.body).addClass("offline");
});

$(window).on("online", function() {
	$(document.body).removeClass("offline");
});
		</script>
	</head>
	<body>
		<div class="image">  <!-- RapidLogo_60x40.png -->
			<a href="http://www.rapid-is.co.uk">
				<img title="Rapid Information Systems" src="images/RapidLogo.svg" />
			</a>	
		</div>
		
		<div class="midTitle">
			<span style="">Rapid</span>
		</div>
		<div class="subBar">
			<span class="link requiresOnline"><a href="logout.jsp">LOG OUT</a></span><% if (com.rapid.security.SecurityAdapter.hasPasswordUpdate(getServletContext())) { %>			
			<span class="link requiresOnline"><a href="update.jsp">CHANGE PASSWORD</a></span><%} %>
			<span class="versionColumn"><%=com.rapid.server.Rapid.VERSION %></span>
		</div>

		<div class="body">
			<div class="columnMiddle" style="">
<% 
			// get the rapid application security
			SecurityAdapter securityAdapter = rapid.getSecurityAdapter();
			
			// check the user password in the rapid application
			if (securityAdapter.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
		
				// check for any of the offical roles
				if (securityAdapter.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE) || securityAdapter.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE) || securityAdapter.checkUserRole(rapidRequest, com.rapid.server.Rapid.USERS_ROLE) || securityAdapter.checkUserRole(rapidRequest, com.rapid.server.Rapid.SUPER_ROLE)) {
%>
				<section class="requiresOnline">
					<a href="~?a=rapid">
					 	<div class="fa-stack fa-lg">
							<i class="fa fa-circle fa-stack-2x"></i>
							<i class="fa fa-cogs fa-stack-1x"></i>
						</div>
					 	<div id="admin">Admin</div>
					 </a>
				</section>
<% 
				}
				
				// check for the design role
				if (securityAdapter.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE)) {
%>
				<section class="requiresOnline">
					<a href="design.jsp">
						<div class="fa-stack fa-lg">
							<i class="fa fa-circle fa-stack-2x"></i>
							<i class="fa fa-wrench fa-stack-1x"></i>
						</div>
						<div id="design">Design</div>
					</a>
				</section>
<% 
				}
			}
%>
				<section style="padding-bottom:0;">
					<a href="#" onclick="loadApps();">
						<div class="fa-stack fa-lg">
							<i class="fa fa-circle fa-stack-2x"></i>
							<i class="fa fa-rocket fa-stack-1x"></i>
						</div>
						<div>Applications</div>
					</a>
					<div class="apps" id="apps"><b>loading...</b></div>
				</section>
		
			</div>
		
		</div>
	</body>
</html>