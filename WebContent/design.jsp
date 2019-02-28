<!DOCTYPE html>
<%@ page language="java" contentType="text/html;charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="org.apache.logging.log4j.LogManager"%>
<%@ page import="org.apache.logging.log4j.Logger" %>
<%@ page import="com.rapid.core.*" %>
<%@ page import="com.rapid.server.Rapid" %>
<%@ page import="com.rapid.server.RapidRequest" %>
<%@ page import="com.rapid.server.filter.*" %>
<%@ page import="com.rapid.security.SecurityAdapter" %>
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

//log that this is loading
LogManager.getLogger(this.getClass()).debug("design.jsp request : " + request.getQueryString());
//get the applications
Applications applications = (Applications) getServletContext().getAttribute("applications");
// retain a ref to rapid app
Application rapid = applications.get("rapid");
// get a rapid request
RapidRequest rapidRequest = new RapidRequest(request, rapid); 
// get the rapid application security
SecurityAdapter security = rapid.getSecurityAdapter();
// assume no permission
boolean designerPermission = false;
// check the user password
if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
	// check design permission
	designerPermission = security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE);	
}

%>
<html>
<head>
	
	<title>Rapid Design - <%=com.rapid.server.Rapid.VERSION %></title>
	<meta charset="utf-8">
	<link rel="icon" href="favicon.ico"></link>
<%
	if (designerPermission) {
%>		
	<script type='text/javascript' src='scripts/<%=com.rapid.server.Rapid.JQUERY%>'></script>
	<script type='text/javascript' src='scripts/<%=com.rapid.server.Rapid.JQUERYUI%>'></script>
	<script type="text/javascript" src="scripts/extras.js"></script>
	<script type="text/javascript" src="scripts/designer.js"></script>
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
	<script type="text/javascript" src="<%=Application.getWebFolder(rapid)%>/rapid.js"></script>
	<script type="text/javascript">
	
	var _userName = "<%=rapidRequest.getUserName() %>";
	
	</script>	
	<link rel="stylesheet" type="text/css" href="styles/fonts/fontawesome/css/font-awesome.css"></link>
	<link rel="stylesheet" type="text/css" href="styles/fonts/rapid/font-rapid.css"></link>
	<link rel="stylesheet" type="text/css" href="styles/designer.css"></link>
	<link rel="stylesheet" type="text/css" href="styles/properties.css"></link>
	
	<!-- CodeMirror files -->
	<link rel="stylesheet" href="scripts/controls/codeMirror/lib/codemirror.css">
	<script src="scripts/controls/codeMirror/lib/codemirror.js"></script>
	<!-- CodeMirror addons -->
	<script src="scripts/controls/codeMirror/addon/display/placeholder.js"></script>
	<script src="scripts/controls/codeMirror/mode/javascript/javascript.js"></script>
	<script src="scripts/controls/codeMirror/mode/css/css.js"></script>
	<script src="scripts/controls/codeMirror/mode/sql/sql.js"></script>
	<script src="scripts/controls/codeMirror/mode/xml/xml.js"></script>
	<link rel="stylesheet" href="scripts/controls/codeMirror/theme/cobalt.css">
	<link rel="stylesheet" href="scripts/controls/codeMirror/theme/dracula.css">
	<script src="scripts/controls/codeMirror/addon/edit/matchbrackets.js"></script>
	<script src="scripts/controls/codeMirror/addon/edit/closebrackets.js"></script>
	<script src="scripts/controls/codeMirror/addon/edit/closetag.js"></script>
	<script src="scripts/controls/codeMirror/addon/selection/active-line.js"></script>
	<script src="scripts/controls/codeMirror/addon/display/autorefresh.js"></script>
	<script src="scripts/controls/codeMirror/addon/hint/show-hint.js"></script>
	<link rel="stylesheet" href="scripts/controls/codeMirror/addon/hint/show-hint.css">
	<script src="scripts/controls/codeMirror/addon/hint/javascript-hint.js"></script>
	<script src="scripts/controls/codeMirror/addon/hint/css-hint.js"></script>
	<script src="scripts/controls/codeMirror/addon/hint/sql-hint.js"></script>
	<script src="scripts/controls/codeMirror/addon/hint/xml-hint.js"></script>
	
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
	if (designerPermission) {
%>	
	<div id="loading">
		<div id="loadingPanel">
			<div>
				<div><img style="width: 200px; height:135px; margin-right: 25px;" src="images/RapidLogo.svg" /></div>
				<div style="position:relative; bottom:30px;"><b>Rapid <%=com.rapid.server.Rapid.VERSION %></b></div>
			</div>
			<div>		
				<div style="height:77px; position:relative; bottom:15px;">
					<i style="margin-top: 5px; margin-bottom: 5px; font-size:40px;" class="glyph fa fa-spin"></i>
					<div>loading...</div>
				</div>		
			</div>
			<div class="subBar" style="background:#ff0004; height:30px;"></div>
		</div>
	</div>
	
	<iframe id="page" scrolling="no"></iframe>
	
	<div id="scrollV" style="position:absolute;top:0;left:622px;height:618px;width:17px;overflow:scroll;z-index:10006;display:none;">
		<p id="scrollVInner" style="margin:0;width:100%;height:0px;">&nbsp;</p>
	</div>
	
	<div id="scrollH" style="position:absolute;top:601;left:221px;height:17px;width:418px;overflow:scroll;z-index:10006;display:none;">
		<p id="scrollHInner" style="margin:0;width:0;height:100%;">&nbsp;</p>
	</div>
		
	<div id="designerTools">	 
			
		<div id="controlPanelShow" style="z-index:10010"></div>
		
		<div id="controlPanel" style="z-index:10011; width: 205px; border: none;">
		
			<div id="controlPanelSize" ></div>
															
			<div id="controlPanelInner">
			
				<div id="controlPanelPin"><i class="fa fa-caret-left" title="unpin panel" style="color:white;"></i></div>
																
				<div class="buttons">					
					<button id="appAdmin" class="buttonLeft" title="Open the Rapid Admin screen">Rapid Admin</button>
					<button id="appAdminNewTab" class="buttonRight buttonImage" title="Open the Rapid Admin screen in a new tab"><i class="fa fa-caret-right"></i></button>
					
				</div>
				
				<h2 style="">Application<i id="helpApplication" class="headerHelp glyph fa hintIcon"></i></h2>
				<select id="appSelect">
					<!-- Applications are added here as options the designer loads -->
				</select>
				
				<h2 style="border:none;">Version<i id="helpVersion" class="headerHelp glyph fa hintIcon"></i></h2>
				<select id="versionSelect">
					<!-- Application versions are added here as options the designer loads -->
				</select>					
				
				<h2 style="border:none;"><img id="pagePrev" class="pageNav pageNavDisabled" src="images/triangleLeftWhite_8x8.png" title="previous page" />Page<img id="pageNext" class="pageNav pageNavDisabled" src="images/triangleRightWhite_8x8.png" title="next page" /><i id="helpPage" class="headerHelp glyph fa hintIcon"></i></h2>
				<select id="pageSelect">
					<!-- Pages are added here as options the designer loads -->
				</select>
				
				<div id="pageLock">
					<h3>This page is locked for editing</h3>
				</div>
				
				<div class="buttons">				
					<button id="pageEdit" class="buttonLeft buttonRight" title="View and edit the page properties">properties</button>
				</div>		
								
				<div class="buttons">
					<button id="pageNew" class="buttonLeft" title="Create a new page for this application">new</button>
					<button id="pageSave" class="" title="Save this page">save</button>
					<button id="pageView" class="" title="View this page in the application">view</button><button id="pageViewNewTab" class="buttonRight buttonImage"  title="View this page in a new tab"><i class="fa fa-caret-right"></i></button>
				</div>	
						
				<div class="buttons">
					<button id="undo" class="buttonLeft" disabled="disabled" title="Undo changes">undo</button>
					<button id="redo" class="buttonRight" disabled="disabled" title="Redo changes">redo</button>
				</div>	
								
				<div id="controlControls" style="margin-top:0;margin-bottom:-3px;">
					<h2 id="controlsHeader">Controls
						<i class="headerToggle fa fa-caret-up" title="Hide controls" style="color:white;"></i>
					</h2>
					
					<div id="controlsList">
						<!-- Controls are added here as list items when the designer loads -->
					</div>					
				</div>	
				
				<h2 id="controlsMap" style="margin-top:5px; border:none;">Page controls
					<i class="headerToggle fa fa-caret-up" title="Hide page controls" style="color:white;"></i>
					<i id="helpMap" class="headerHelp glyph fa hintIcon"></i>
				</h2>
				
				<div id="pageMap" class="design-map" >
					<ul id="pageMapList"></ul>
					<button id="pageMapHighlight" class="fa" title="Locate selected control">&#xf140;</button>		
					<input id="pageMapSearch" placeholder="search"></input>				
				</div>	

				<div class="controlPanelVersion" >
					<img src="images/RapidLogo.svg"/>
					<div id="controlPanelVersion">Rapid<br/><%=com.rapid.server.Rapid.VERSION %></div>
				</div>					
			
			</div>
																					
		</div>
		
		<div id="propertiesPanel" style="z-index:10011">
		
			<div id="propertiesPanelSize" ></div>
																					
			<div id="propertiesPanelInner">
			
				<div id="propertiesPanelPin"><i class="fa fa-caret-right" title="hide panel" style="color:white;"></i></div>
										
				<div class="untilsPanelDiv">
							
					<i id="helpPropertiesPanel" class="headerHelp glyph fa hintIcon" style="color:white; font-size:15px;"></i>
					<div class="buttons">		
						<button id="selectPeerLeft" class="buttonLeft">
							<i class="fa" title="Select the control before this one"></i>
						</button>
						<button id="selectParent">
							<i class="fa" title="Select the parent of this control"></i>
						</button>
						<button id="selectChild">
							<i class="fa" title="Select the first child of this control"></i>
						</button>
						<button id="selectPeerRight" class="buttonRight">
							<i class="fa" title="Select the control after this one"></i>
						</button>
					</div>					
											
					<div class="buttons">
						<button id="swapPeerLeft" class="buttonLeft" style="line-height:20px;">
							<i class="fa" aria-hidden="true" title="Swap position with control before this one" style="font-size:15px; vertical-align:middle;"></i>
						</button>
						<button id="addPeerLeft">
							<div class="fa fa-lg" title="Add a new control before this one" style="height:20px;"></div>
						</button>
						<button id="deleteControl" style="line-height:20px;">&nbsp;<i class="delete fa fa-trash" title="Delete this control"></i>&nbsp;</button>
						<button id="addPeerRight">
						<div class="fa fa-lg" title="Add a new control after this one" style="height:20px;">
						</div>
						</button>
						<button id="swapPeerRight" class="buttonRight" style="line-height:20px;">
							<i class="fa" aria-hidden="true" title="Swap position with control after this one" style="font-size:15px; vertical-align:middle;"></i>
						</button>
					</div>						
					
					<div class="buttons">
						<button id="copy" class="buttonLeft" title="copy this control">&nbsp;copy</button>
						<button id="paste" class="buttonRight" title="paste this control">paste</button>
					</div>								
				</div>		
								
				<h2 id="propertiesHeader">Properties  <i id="helpProperties" class="headerHelp glyph fa hintIcon"></i><i class="headerToggle fa fa-caret-up"></i></h2>
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
		
		<div id="designCover"></div>
				
		<div id="desktopCoverBottom" class="desktopCover"></div>
		<div id="desktopCoverRight" class="desktopCover"></div>
											
	</div>	
	
	<div id="selectionBorder">
		<div id="selectionBorderLeft" class="selectionBorderInner"></div>
		<div id="selectionBorderTop" class="selectionBorderInner"></div>
		<div id="selectionBorderRight" class="selectionBorderInner"></div>
		<div id="selectionBorderBottom" class="selectionBorderInner"></div>
	</div>
	
	<div id="selectionCover">
		<div></div>
	</div>
	
	<div id="selectionInsertCover">
		<div></div>
	</div>
					
	<img id="selectionMoveLeft" class="selectionCursor" src="images/move_left.svg" />
	<img id="selectionMoveRight" class="selectionCursor" src="images/move_right.svg" />
	<img id="selectionInsert" class="selectionCursor" src="images/insert.svg" />
	
	<iframe id="uploadIFrame" name="uploadIFrame" width="0" height="0" style="width:0;height:0;border:0px hidden #fff;" onload="fileuploaded(this);"></iframe>
	
	<div id="dialogues"></div>
	
	<div id="save">
		<span id="saveAnimation" class="fa fa-cog fa-spin"></span>
		<span id="saveMessage">saving...</span>
	</div>

<%
	} else {
%>		
		<div class="image">
			<a href="http://www.rapid-is.co.uk"><img title="Rapid Information Systems" src="images/RapidLogo.svg" /></a>	
		</div>
		
		<div class="midTitle" style="">
			<span style="">Rapid</span>
		</div>
		<div class="subBar" style=""><div style="float:right; width:95px; text-align:center; padding:1px 0; font-size:12px; font-weight: bold; color: white;"><%=com.rapid.server.Rapid.VERSION %></div></div>
		<div class="body">
			<div class="columnMiddle">
				<h3 style="margin:0; padding:40px;">You do not have permission to use the Rapid Designer</h3>
			</div>
		</div>
		
<%		
	}
%>			
</body>
</html>