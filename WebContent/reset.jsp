<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rapid.core.Email" %>
<%@ page import="com.rapid.security.SecurityAdapter" %>
<%

/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

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

// get any message from the session
String message = (String) session.getAttribute("message");

// assume password reset is not supported
Boolean hasPasswordReset = false;

//if email is configured
if (Email.getEmailSettings() != null) {
	// if any app has password reset
	hasPasswordReset = SecurityAdapter.hasPasswordReset(getServletContext());
}

%>
<html>
<head>
	<title>Rapid - Reset password</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
	<link rel="icon" href="favicon.ico"></link>
	<link rel='stylesheet' type='text/css' href='styles/fonts/fontawesome/css/font-awesome.css'></link>
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
	<script type="text/javascript" src="scripts/<%=com.rapid.server.Rapid.JQUERY %>"></script>
	
	<script type="text/javascript">
	
		function validateEmail() {
			
			var emailString = $("input[name='email']").val();
			var regex = new RegExp("^[_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{2,4})$");
			var isValid = regex.test(emailString);

			if(!isValid){
				alert("Email NOT valid. Please enter a valid email.");	
				return false;
			}
			
		}
			
	</script>
	
</head>

<body onload="document.reset.email.focus();">

	<div class="image">
		<a href="http://www.rapid-is.co.uk"><img title="Rapid Information Systems" src="images/RapidLogo.svg" /></a>	
	</div>
	<div class="midTitle" style="">
		<span style="">Rapid</span>
	</div>
	<div class="subBar">
		<div class="versionColumn"><%=com.rapid.server.Rapid.VERSION %></div>
	</div>

<div class="body">
	<div class="columnMiddle">
		<%
		if (hasPasswordReset) {
		%>
			<form name="reset" id="RapidReset" method="post" onsubmit="return validateEmail()">
				<div class="row">
					<div class="columnUserInput">
						<div class="columnUserIcon" style="">
							<span class="fa fa-at" style=""></span>
						</div>
						<input type="text" placeholder="Email" name="email" required>
					</div>
				</div>
			
				<button class="resetButton" type="submit"><i class="fa fa-sign-in"></i>  Reset password</button>
			</form>
			
		<% 
			// if there is a message
			if (message != null) {
					// print the message into the page
		%>
					<p class="message"><%=message %></p>
		<%
				// empty the message
				session.setAttribute("message", null);
			}
		} else {
		%>
			<p>Password reset is not currently enabled</p>
		<%
		}
		%>
	</div>
</div>

</body>
</html>