<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
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

String message = (String) session.getAttribute("message");

%>
<html>
<head>
	<title>Rapid - Update password</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
	<link rel="icon" href="favicon.ico"></link>
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
	<link rel='stylesheet' type='text/css' href='styles/fonts/fontawesome/css/font-awesome.css'></link>
	<script type="text/javascript" src="scripts/<%=com.rapid.server.Rapid.JQUERY %>"></script>
	
	<script type="text/javascript">
	
		function validatePassword() {
			
			var passwordNew = $("input[name='passwordNew']").val();
			var passwordConfirm = $("input[name='passwordConfirm']").val();

			if (passwordNew != passwordConfirm) {
				alert("New and confirmation passwords must match.");	
				return false;
			}
			
		}
			
	</script>
	
</head>

<body onload="document.login.password.focus();">

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

			<form name="login" id="RapidLogin" method="post" onsubmit="return validatePassword()">
			
				<div class="row">
					<div class="columnUserInput">
						<div class="columnUserIcon" style="">
							<span class="fa fa-unlock-alt" style=""></span>
						</div>
						<input type="password" placeholder="Current password" name="password" required>
					</div>
				</div>
				
				<div class="row">
					<div class="columnUserInput">
						<div class="columnUserIcon" style="">
							<span class="fa fa-lock" style=""></span>
						</div>
						<input type="password" placeholder="New password" name="passwordNew" required>
					</div>
				</div>
				
				<div class="row">
					<div class="columnUserInput">
						<div class="columnUserIcon" style="">
							<span class="fa fa-question-circle" style=""></span>
						</div>
						<input type="password" placeholder="Confirm new password" name="passwordConfirm" required>
					</div>
				</div>
			
				<button class="resetButton" type="submit"><i class="fa fa-sign-out"></i>  Update password</button>

			</form>
				
			<% 
			if (message != null) {
			%>
				<p class="message"><%=message %></p>
			<%	
				session.setAttribute("message", null);
			}
			%>
		</div>	
	
</div>

</body>
</html>