<!DOCTYPE html>
<%@ page language="java" contentType="text/html;charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="org.apache.logging.log4j.LogManager" %>
<%@ page import="com.rapid.core.*" %>
<%@ page import="com.rapid.server.Rapid" %>
<%@ page import="com.rapid.server.RapidRequest" %>
<%@ page import="com.rapid.server.filter.*" %>
<%@ page import="com.rapid.security.SecurityAdapter" %>
<%

/*

Copyright (C) 2017 - Gareth Edwards / Rapid Information Systems

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

//log that this is loading
LogManager.getLogger(this.getClass()).debug("workflow.jsp request : " + request.getQueryString());
//get the applications
Applications applications = (Applications) getServletContext().getAttribute("applications");
// retain a ref to rapid app
Application rapid = applications.get("rapid");
// get a rapid request
RapidRequest rapidRequest = new RapidRequest(request, rapid); 
// get the rapid application security
SecurityAdapter security = rapid.getSecurityAdapter();
// assume no permission
boolean permission = false;
// check the user password
if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
	// check design permission
	permission = security.checkUserRole(rapidRequest, Rapid.WORKFLOW_ROLE);
}

%>
<html>
<head>
	
	<title>Rapid Workflow - <%=com.rapid.server.Rapid.VERSION %></title>
	<meta charset="utf-8">
	<link rel="icon" href="favicon.ico"></link>
<%
	if (permission) {
%>		
	<script type="text/javascript" src="scripts/jquery-1.10.2.js"></script>
	<script type="text/javascript" src="scripts/jquery-ui-1.10.3.js"></script>
	<script type="text/javascript" src="scripts/jsPlumb-2.2.8.js"></script>
	<script type="text/javascript" src="scripts/extras.js"></script>
	<script type="text/javascript" src="scripts/workflow.js"></script>
	<script type="text/javascript" src="scripts/reorder.js"></script>
	<script type="text/javascript" src="scripts/properties.js"></script>
	<script type="text/javascript" src="scripts/propertiescustom.js"></script>	
	<script type="text/javascript" src="scripts/controls.js"></script>
	<script type="text/javascript" src="scripts/validation.js"></script>
	<script type="text/javascript" src="scripts/actions.js"></script>
	<script type="text/javascript" src="scripts/styles.js"></script>
	<script type="text/javascript" src="scripts/dialogue.js"></script>
	<script type="text/javascript" src="scripts/map.js"></script>
	<script type="text/javascript" src="scripts/help.js"></script>			
	<script type="text/javascript">
	
	var _userName = "<%=rapidRequest.getUserName() %>";
	
	</script>	
	<link rel="stylesheet" type="text/css" href="styles/designer.css"></link>
	<link rel="stylesheet" type="text/css" href="styles/properties.css"></link>
<%
	} else {
%>
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
<%
	}
%>
</head>
<body>
<%
	if (permission) {
%>	
	<div id="loading">
		<div id="loadingPanel">
			<div><img style="padding: 10px;width: 200px; height:134px; margin-left:-50px;" src="images/RapidLogo_200x134.png" /></div>
			<div><b>Rapid <%=com.rapid.server.Rapid.VERSION %></b></div>		
			<div><img style="margin-top: 5px; margin-bottom: 5px;" src="images/wait_220x19.gif"></div>		
			<div>loading...</div>
		</div>
	</div>
	
	<div id="flowCanvas" class="flow jtk-demo-canvas canvas-wide jtk-surface jtk-surface-nopan" scrolling="no"></div>
	
	<div id="scrollV" style="position:absolute;top:0;left:622px;height:618px;width:17px;overflow:scroll;z-index:10006;display:none;">
		<p id="scrollVInner" style="margin:0;width:100%;height:0px;">&nbsp;</p>
	</div>
	
	<div id="scrollH" style="position:absolute;top:601;left:221px;height:17px;width:418px;overflow:scroll;z-index:10006;display:none;">
		<p id="scrollHInner" style="margin:0;width:0;height:100%;">&nbsp;</p>
	</div>
		
	<div id="designerTools">	 
			
		<div id="controlPanelShow" style="z-index:10010"></div>
		
		<div id="controlPanel" style="z-index:10011">
		
			<div id="controlPanelSize" ></div>
															
			<div id="controlPanelInner">
			
				<div id="controlPanelPin"><img src="images/triangleLeftWhite_8x8.png" title="unpin panel" /></div>
																
				<div class="buttons">					
					<button id="appAdmin" class="buttonLeft" title="Open the Rapid Admin screen">Rapid Admin</button>
					<button id="appAdminNewTab" class="buttonRight buttonImage" title="Open the Rapid Admin screen in a new tab"><img src="images/triangleRightWhite_8x8.png" /></button>
				</div>
				
				<h2>Workflow<img id="helpApplication" class="headerHelp" src="images/help_16x16.png" /></h2>
				<select id="flowSelect">
					<!-- Workflows are added here as options the designer loads -->
				</select>
				
				<h2>Version<img id="helpVersion" class="headerHelp" src="images/help_16x16.png" /></h2>
				<select id="versionSelect">
					<!-- Workflow versions are added here as options the designer loads -->
				</select>					
								
				<div id="flowLock">
					<h3>This workflow is locked for editing</h3>
				</div>
				
				<div class="buttons">				
					<button id="flowEdit" class="buttonLeft buttonRight" title="View and edit the workflow properties">properties</button>
				</div>		
								
				<div class="buttons">
					<button id="flowNew" class="buttonLeft" title="Create a new workflow for this application">new</button>
					<button id="flowSave" class="buttonRight" title="Save this workflow">save</button>
				</div>	
						
				<div class="buttons">
					<button id="undo" class="buttonLeft" disabled="disabled" title="Undo changes">undo</button>
					<button id="redo" class="buttonRight" disabled="disabled" title="Redo changes">redo</button>
				</div>	
								
				<div id="actionActions" style="margin-top:0;margin-bottom:-3px;">
					<h2 id="actionsHeader">Actions
						<img class="headerToggle" src="images/triangleUpWhite_8x8.png" />
						<img id="helpActions" class="headerHelp" src="images/help_16x16.png" />
					</h2>
					
					<div id="actionsList">
						<!-- Actions are added here as list items when the designer loads -->
					</div>					
				</div>	

			</div>
																					
		</div>
		
		<div id="propertiesPanel" style="z-index:10011">
		
			<div id="propertiesPanelSize" ></div>
																					
			<div id="propertiesPanelInner">
			
				<div id="propertiesPanelPin"><img src="images/triangleRightWhite_8x8.png" title="hide panel" /></div>
										
				<div class="untilsPanelDiv">
							
					<img id="helpPropertiesPanel" class="headerHelp" src="images/help_16x16.png" />
					<div class="buttons">					
						<button id="selectPeerLeft" class="buttonLeft"><img src="images/moveLeft_16x16.png" title="Select the control before this one"/></button>
						<button id="selectParent"><img src="images/moveUp_16x16.png" title="Select the parent of this control"/></button>
						<button id="selectChild"><img src="images/moveDown_16x16.png" title="Select the first child of this control"/></button>
						<button id="selectPeerRight" class="buttonRight"><img src="images/moveRight_16x16.png" title="Select the control after this one"/></button>
					</div>							
											
					<div class="buttons">
						<button id="addPeerLeft" class="buttonLeft"><img src="images/addLeft_16x16.png" title="Add a new action before this one"/></button>
						<button id="deleteAction" class="">&nbsp;<img src="images/bin_16x16.png" title="Delete this action"/>&nbsp;</button>
						<button id="addPeerRight"class="buttonRight"><img src="images/addRight_16x16.png" title="Add a new control after this one"/></button>
					</div>						
					
					<div class="buttons">
						<button id="copy" class="buttonLeft" title="copy this control">&nbsp;copy</button>
						<button id="paste" class="buttonRight" title="paste this control">paste</button>
					</div>								
				</div>		
								
				<h2 id="propertiesHeader">Properties  <img id='helpProperties' class='headerHelp' src='images/help_16x16.png' /><img class='headerToggle' src='images/triangleUpWhite_8x8.png' /></h2>
				<div>
					<div class="propertiesPanelDiv" data-dialogueId="propertiesPanel"></div>			
					<div class="validationPanelDiv" data-dialogueId="validationPanel"></div>
					<div id="actionsPanelDiv" class="actionsPanelDiv" data-dialogueId="actionsPanel"></div>
				</div>				
				<div id="stylesPanelDiv" data-dialogueId="stylesPanel"></div>		
			
			</div>
																			
		</div>
		
		<div id="propertiesDialogues" data-dialogueId="propertiesDialogues"></div>
		
		<span id="styleInput" contenteditable="true"></span>
		<span id="styleHint"></span>
		<ul id="styleList"></ul>
												
	</div>	
	
	<div id="dialogues"></div>

<%
	} else {
%>

	<div class="image">
		<a href="http://www.rapid-is.co.uk"><img src="images/RapidLogo_60x40.png" /></a>
	</div>
	
	<div class="title">
		<span>Rapid - No permission</span>
		<span class="link"><a href="logout.jsp">log out</a></span>
	</div>

	<div class="info"><p>You do not have permission to use Rapid Workflow</p></div>
		
<%		
	}
%>			
</body>
</html>