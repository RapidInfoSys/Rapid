<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%

/*

Copyright (C) 2018 - Gareth Edwards / Rapid Information Systems

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

session.invalidate();

%>
<html>
<head>	
	<title>Rapid - Log out</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
	<link rel="icon" href="favicon.ico"></link>
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
</head>

<body>
<div class="image"> 
	<a href="http://www.rapid-is.co.uk">
		<img title="Rapid Information Systems" src="images/RapidLogo.svg" />
	</a>	
</div>

<div class="midTitle">
	<span style="">Rapid</span>
</div>
<div class="subBar">
	<span class="versionColumn"><%=com.rapid.server.Rapid.VERSION %></span>
</div>

<div class="body">
	<div class="columnMiddle">
		<div class="info" style="">
			<p style="">You have been logged out</p>
			<a href="." style="font-size:unset;"><button>Log in again</button></a>
		</div>
	</div>
</div>
</body>
</html>