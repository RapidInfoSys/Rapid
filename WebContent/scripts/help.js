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

// a global array to hold the help text
var _helpText = [];
// various help messages - these could be defined elsewhere for multiple language support
_helpText["helpApplication"] = "These are the applications you have permission to design. They are sorted by name and description. To create or manage applications go to Rapid Admin by clicking the \"administration\" button above.";
_helpText["helpVersion"] = "These are the version of the application you have permission to design. They are sorted by creation date. To create or manange application versions go to Rapid Admin by clicking the \"administration\" button above.";
_helpText["helpPage"] = "The pages in the application sorted by name and title. Click the \"properties\" button to edit the page properties. To create a new page click \"new\", \"save\" to save, \"view\" will leave the designer and take you to the working page. Undo or redo any changes with \"undo\" and \"redo\".";
_helpText["helpControls"] = "To add controls drag them from the panel below to the position you want in your page. You will see different left or right arrows for placing the control to the left or right of an existing control, or a down arrow to add the new control within an existing control. To move controls already in the page select them and drag to the new position. Not all controls can be moved so you might need to select their parent control. Click on the controls header to hide the controls and show the page controls. Use Rapid Admin / Actions and controls to add or remove from the list.";
_helpText["helpMap"] = "A map of all controls on the page. Click on the page controls header to show. Controls can be selected from the page controls list.";
_helpText["helpPropertiesPanel"] = "Select the control before this one with <img src='images/moveLeft_16x16.png' />. To select the parent control, or the control that this one is inside, use <img src='images/moveUp_16x16.png' />. To select the first child control, or first control inside this one use <img src='images/moveDown_16x16.png' />. Use <img src='images/moveRight_16x16.png' /> to select the control after this one. <img src='images/swapLeft_16x16.png' /> will move this control before the one to its left or above it, <img src='images/addLeft_16x16.png' /> will add a new control before this one, of the same type. <img src='images/bin_16x16.png' /> will delete the control or the page. Use <img src='images/addRight_16x16.png' /> to add a new control of the same type after this one, and <img src='images/swapRight_16x16.png' /> to move this control after the one to its right or below it.  Copy this control for pasting elsewhere in this page, or other pages. The page can be copied and pasted too.";
_helpText["helpProperties"] = "Edit the properties of the control by clicking on the contents of the second column. Some controls will let you specify the validation rules for the data you expect. Add actions to control events by selecting from the drop down.";
_helpText["helpStyles"] = "Specify the CSS rules for this control. Valid css rules will appear as you type.";
_helpText["helpStyleClasses"] = "Choose a CSS class from your global styling to apply to this control. Classes can be added by editing Application styles in Rapid Admin.";

function addHelp(id, property, right) {
	
	// check if we have a hint element already, create one if not
	if (!$("#" + id + "hint")[0]) {
		// add the hint span
		$("body").append("<span class='hint' id='" + id + "hint'>" + _helpText[id] + "</span>");
		// hide it
		$("#" + id + "hint").hide();		
	}
		
	// hide hint on mouseout
	var outListener = $("#" + id).mouseout({id: id}, function(ev) {
		$("#" + id + "hint").hide();
	});
	
	// show the hint on mouseover
	var overListener = $("#" + id).mouseover({id: id}, function(ev) { 
		$("#" + ev.data.id + "hint").css({
			left: ev.pageX + 5 - (property || right ? $("#" + id + "hint").outerWidth() + 10 : 0),
			top: ev.pageY + 5
		}).show();  
	});
	
	// if this is a hint for a property add the listeners to the array for deregistration
	if (property) {
		addListener(outListener);
		addListener(overListener);
	}
}

//JQuery is ready! 
$(document).ready( function() {
	
	addHelp("helpApplication");
	addHelp("helpVersion");
	addHelp("helpPage");
	addHelp("helpControls");
	addHelp("helpMap");
	addHelp("helpPropertiesPanel", null, true);	
	
});	
