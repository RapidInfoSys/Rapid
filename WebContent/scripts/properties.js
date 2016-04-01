/*

Copyright (C) 2016 - Gareth Edwards / Rapid Information Systems

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

/*

Functions related to control properties

*/

// this holds all the property listeners by dialogue id so they can be properly detached
var _dialogueListeners = {};
// a global for ensuring more recently shown dialogues are on top of later shown ones
var _dialogueZindex = 10012;
// this holds the cell, propertyObject, property, and details by dialogue id for refeshing child actions
var _dialogueRefeshProperties = {};

// this finds the dialogue each listener is in and stores it so the relevant ones can be detached when the dialogue is closed
function addListener(listener) {
	// assume we're not able to find the listener id
	var listenerId = "unknown";
	// get the nearest dialogue
	var dialogue = listener.closest("div.dialogue");
	// if we got get a dialogue, but not the close link (the close links are attached to the panel then which could cause a mini leak)
	if (dialogue[0] && !listener.is("a.closeDialogue")) {
		// set the listener object to the dialogue id
		listenerId = dialogue.attr("id");
	} else {
		// find the closest div with a data-dialogueid (this will be one of the panels)
		dialogue = listener.closest("div[data-dialogueid]");
		// set the listener id to the dialogue id
		listenerId = dialogue.attr("data-dialogueid");
	}
	
	// instantiate array if need be
	if (!_dialogueListeners[listenerId]) _dialogueListeners[listenerId] = [];
	// add the listener
	_dialogueListeners[listenerId].push(listener);
	
}

function removeListeners(listenerId) {
	
	// if we were given a listenerId
	if (listenerId) {
		// loop all the listeners
		for (var i in _dialogueListeners[listenerId]) {
			// remove all dialogues
			_dialogueListeners[listenerId][i].unbind();
		}
		// remove the  listeners for good
		delete _dialogueListeners[listenerId];		
		// append "Div" if propertiesPanel
		if (listenerId == "propertiesPanel") listenerId += "Div";
		// update "Div" if actionsPanel
		if (listenerId == "actionsPanel") listenerId += "Div";
		// check for any children
		var childDialogues = $("#" + listenerId).find("td[data-dialogueId]");
		// if we got some
		if (childDialogues.length > 0) {
			// loop and remove
			childDialogues.each( function() {
				removeListeners($(this).attr("data-dialogueId"));
			});
		}
	} else {
		// loop all the dialogues
		for (var i in _dialogueListeners) {
			// loop all the listeners
			for (var j in _dialogueListeners[i]) {
				// remove all dialogues
				_dialogueListeners[i][j].unbind();
			}
			// remove the dialogue listeners for good
			delete _dialogueListeners[i];
		}
	}
}

// this renders all the control properties in the properties panel
function showProperties(control) {
		
	// remove all listeners for this panel
	removeListeners("propertiesPanel");
		
	// grab a reference to the properties div
	var propertiesPanel = $(".propertiesPanelDiv");
		
	// if there was a control
	if (control) {
		
		// get the contro class
		var controlClass = _controlTypes[control.type];
	
		// empty the properties
		propertiesPanel.html("");
		// add the help hint
		addHelp("helpProperties",true);
		// add a header toggle
		propertiesPanel.find("h2").click( toggleHeader );

		// append a table
		propertiesPanel.append("<table class='propertiesPanelTable'><tbody></tbody></table>");		
		// get a reference to the table
		var propertiesTable = propertiesPanel.children().last().children().last();
		// add the properties header
		propertiesTable.append("<tr><td colspan='2' class='propertyHeader'><h3>" + controlClass.name + "</h3></td></tr>");
		// add a small break
		propertiesTable.append("<tr><td colspan='2'></td></tr>");
		// show any conflict message
		if (control._conflict) propertiesTable.append("<tr><td colspan='2' class='conflict propertyHeader'>Page \"" + control._conflict + "\" has a control with the same name</td></tr>");
		// show the control id if requested
		if (_version.showControlIds) propertiesTable.append("<tr><td>ID</td><td class='canSelect'>" + control.id + "</td></tr>");
		// check there are class properties
		var properties = controlClass.properties;
		if (properties) {
			// (if a single it's a class not an array due to JSON class conversion from xml)
			if ($.isArray(properties.property)) properties = properties.property; 			
			// loop the class properties
			for (var i in properties) {
				// add a row
				propertiesTable.append("<tr></tr>");
				// get a reference to the row
				var propertiesRow = propertiesTable.children().last();
				// retrieve a property object from the control class
				var property = properties[i];
				// check that visibility is not false
				if (property.visible === undefined || !property.visible === false) {
					// assume no help
					var help = "";
					// if the property has help html
					if (property.helpHtml) {
						// make the helpId
						var helpId = control.id + property.key + "help";
						// create help html
						help = "<img id='" + helpId + "' class='propertyHelp' src='images/help_16x16.png' />"						
					}
					// get the property itself from the control
					propertiesRow.append("<td>" + property.name + help + "</td><td></td>");
					// add the help listener
					if (help) addHelp(helpId,true,true,property.helpHtml);
					// get the cell the property update control is going in
					var cell = propertiesRow.children().last();
					// apply the property function if it starts like a function or look for a known Property_[type] function and call that
					if (property.changeValueJavaScript.trim().indexOf("function(") == 0) {
						try {
							var changeValueFunction = new Function(property.changeValueJavaScript);
							changeValueFunction.apply(this,[cell, control, property]);
						} catch (ex) {
							alert("Error - Couldn't apply changeValueJavaScript for " + control.name + "." + property.name + " " + ex);
						}
					} else {
						if (window["Property_" + property.changeValueJavaScript]) {
							window["Property_" + property.changeValueJavaScript](cell, control, property);
						} else {
							alert("Error - There is no known Property_" + property.changeValueJavaScript + " function");
						}
					}
				}			
			} // visible property
			
		} // got properties
		
	} // got control
	
	// set the parent height to auto
	propertiesPanel.parent().css("height","auto");
		
}

function updateProperty(cell, propertyObject, property, details, value) {
	
	// if the page isn't locked
	if (!_locked) {
		// get the value
		var propertyValue = propertyObject[property.key];
		// get whether the property is complex like an array or object (in which case it's being passed by ref (not by val) and it won't like it has changed)
		var propertyComplex = $.isArray(propertyValue) || $.isPlainObject(propertyValue)
		// only if the property is actually different (or if the value is complext like an array or object, it will have been updated and the reference will not have changed)
		if (propertyValue != value || propertyComplex) {
			// add an undo snapshot (complex properties will have to manage this themselves)
			if (!propertyComplex) addUndo();
			// update the object property value
			propertyObject[property.key] = value;
			// if an html refresh is requested
			if (property.refreshHtml) {
				// in controls.js
				rebuildHtml(propertyObject);			
			}	
			// if a property refresh is requested
			if (property.refreshProperties) {
									
				// if these are events
				if (cell.closest(".actionsPanelDiv")[0]) {
							
					// get the event type
					var eventType = cell.closest("table[data-eventType]").attr("data-eventType");
					
					// get the dialogue id
					var dialogueId = cell.closest("div.propertyDialogue").attr("id");
					
					// if we're in a dialogue
					if (dialogueId) {
						
						// get the refresh properties from the global store
						var refreshProperties = _dialogueRefeshProperties[dialogueId];
						
						// get the parts
						var cell = refreshProperties.cell;
						var propertyObject = refreshProperties.propertyObject;
						var property = refreshProperties.property;
						
						// check for the property function (it wouldn't have made a dialogue unless it was custom)
						if (window["Property_" + property.changeValueJavaScript]) {
							window["Property_" + property.changeValueJavaScript](cell, propertyObject, property);
						} else {
							alert("Error - There is no known Property_" + property.changeValueJavaScript + " function");
						}
						
					} else {
						
						// update this event's actions using the control
						showActions(_selectedControl, eventType);
										
					}
									
				} else {
					
					// update the properties
					showProperties(_selectedControl);
					
					// update the events
					showEvents(_selectedControl);		
										
				}
				
				// resize the page
				windowResize("updateProperty");
										
			}
			
			// if this is the name
			if (property.key == "name") {
				// if the property map is visible
				if ($("#pageMap").is(":visible")) {
					// get the control class
					var controlClass = _controlTypes[propertyObject.type];
					// update the name
					$("#pageMap").find("span[data-id=" + propertyObject.id + "]").html(((controlClass.image) ? "<img src='" + controlClass.image + "'/>" : "") + propertyObject.type + (propertyObject.name ? " - " + propertyObject.name: ""));
				}
			}
			
		} // property value changed check		
	} // page lock check	
}

function setPropertyVisibilty(propertyObject, propertyKey, visibile) {
	// get the class from controls
	var objectClass = _controlTypes[propertyObject.type];
	// try actions if not found
	if (!objectClass) objectClass = _actionTypes[propertyObject.type];
	
	if (propertyObject && objectClass && objectClass.properties) {
		var properties = objectClass.properties;
		// (if a single it's a class not an array due to JSON class conversionf from xml)
		if ($.isArray(properties.property)) properties = properties.property; 
		// loop them
		for (var i in properties) {
			var property = properties[i];
			if (property.key == propertyKey) {
				property.visible = visibile;
				break;
			}
		}
	}
}

// this is a reusable function for creating dialogue boxes
function getDialogue(cell, propertyObject, property, details, width, title, options) {	
		
	// derive the id for this dialogue
	var dialogueId = propertyObject.id + property.key;
	
	// add the data-dialogueId to the cell
	cell.attr("data-dialogueId", dialogueId);
	
	// retrieve the dialogue
	var dialogue = $("#propertiesDialogues").find("#" + dialogueId);
			
	// get the name of the function that requested this dialogue
	var propertyFunction = arguments.callee.caller.name;
			
	// if we couldn't retrieve one, make it now
	if (!dialogue[0]) {		
		// add the div
		dialogue = $("#propertiesDialogues").append("<div id='" + dialogueId + "' class='propertyDialogue'></div>").children().last();
		// check the options
		if (options) {
			// if resizeX
			if (options.sizeX) {
				// add the mouse over div
				var resizeX = dialogue.append("<div class='resizeX'></div>").find("div.resizeX");
				// add the listener
				addListener(resizeX.mousedown( {id: dialogueId}, function(ev) {
					// retain that we'r resizing a dialogue
					_dialogueSize = true;
					// retain it's id
					_dialogueSizeId = ev.data.id;
					// retain the type of resize
					_dialogueSizeType = "X";
					// calculate the offset
					_mouseDownXOffset = ev.pageX - $("#" + _dialogueSizeId).offset().left;
				}));				
			}
			// set min-width to explicit or standard width
			if (options.minWidth) {
				dialogue.css("min-width", options.minWidth);
			} else {
				dialogue.css("min-width", width);
			}
		}
		// add a close link
		var close = dialogue.append("<b class='dialogueTitle' style='float:left;margin-top:-5px;'>" + title + "</b><a href='#' class='dialogueClose' style='float:right;margin-top:-5px;'>close</a></div>").children().last();
	
		// add the close listener (it's put in the listener collection above)
		addListener(close.click({dialogueId: dialogueId}, function(ev) {
			
			// get this dialogue
			var dialogue = $("#" + ev.data.dialogueId);
			
			// look for any child dialogue cells
			var childDialogueCells = dialogue.find("td[data-dialogueId]");
			// loop them
			childDialogueCells.each( function() {
				// get the id
				var childDialogueId = $(this).attr("data-dialogueId");
				// get the child dialogue (if visible)
				var childDialogue = $("#" + childDialogueId + ":visible");
				// if we got one
				if (childDialogue[0]) {
					// find the close link
					var close = childDialogue.find("a.dialogueClose");
					// click it
					close.click();
				}
				
			});
						
			// remove this dialogue
			dialogue.remove();
			
			// call an update on the master property to set the calling cell text
			updateProperty(cell, propertyObject, property, details, propertyObject[property.key]);

		}));			
		// add an options table
		dialogue.append("<br/><table class='dialogueTable'><tbody></tbody></table>");
	}	
	
	// listener to show the dialogue 
	addListener(cell.click({dialogueId: dialogueId, propertyFunction: propertyFunction}, function(ev) {
		// retrieve the dialogue using the id
		var dialogue = $("#propertiesDialogues").find("#" + ev.data.dialogueId);
		// if it doesn't exist 
		if (!dialogue[0]) {
			//call the original property function
			window[ev.data.propertyFunction](cell, propertyObject, property, details);
			// get it again
			dialogue = $("#propertiesDialogues").find("#" + ev.data.dialogueId);
		}		
		// position the dialogue
		dialogue.css({
			"top": cell.offset().top,
			"z-index": _dialogueZindex++
		});
		// show this drop down
		dialogue.slideDown(500, function() {
			windowResize("PropertyDialogue show");
		});			
	}));
	
	// if there is a saved size of the dialogue, set it, otherwise use the given width
	if (_sizes[_version.id + _version.version + dialogueId + "width"]) {
		dialogue.css("width", _sizes[_version.id + _version.version + dialogueId + "width"]);
	} else {
		dialogue.css("width", width);
	}
	
	// return
	return dialogue;	
}

// this function clears down all of the property dialogues
function hideDialogues() {		
	// execute the click on all visible dialogue close links to update the property and even the html
	$("a.dialogueClose:visible").click();
	// remove all listeners
	removeListeners();	
	// grab a reference to any dialogues
	var propertiesDialogues = $("#propertiesDialogues");
	// empty any propertyDialogues that we may have used before
	propertiesDialogues.children().remove();				
}

// this function returns an object with id and name for inputs and outputs, including looking up run-time property details (used by dataCopy, database, and webservice)
function getDataItemDetails(id) {
	
	// if we got an id
	if (id) {
		// get the id parts
		var idParts = id.split(".");
		// derive the id
		var itemId = idParts[0];
		// assume the name is the entire id
		var itemName = id
		// get the control
		var itemControl = getControlById(itemId);
		// if we got one
		if (itemControl) {
			// look for a "other" page name
			if (itemControl._pageName) {
				// use the page name and control name
				itemName = itemControl._pageName + "." + itemControl.name;
			} else {
				// take just the control name
				itemName = itemControl.name;
			}
			// if there's a complex key
			if (idParts.length > 1) {
				// get the class
				var controlClass = _controlTypes[itemControl.type];
				// get any run time properties
				var properties = controlClass.runtimeProperties;
				// if there are runtimeProperties in the class
				if (properties) {
					// promote if array
					if ($.isArray(properties.runtimeProperty)) properties = properties.runtimeProperty;
					// loop them
					for (var i in properties) {
						// get the property
						var property = properties[i];
						// if the key matches
						if (idParts[1] == property.type) {
							// append the property name
							itemName += "." + property.name;
							// we're done
							break;
						}
					}					
				}
			} 
		}
		// return an object with the name and id
		return {id:itemId, name:itemName};
	} else {
		// return an empty object
		return {id:"",name:""};
	}
}

// escape single apostophe's in values
function escapeApos(value) {
	if (value && value.replace) {
		return value.replace("'","&apos;");
	} else {
		return value;
	}
}

// a standard handler for text properties
function Property_text(cell, propertyObject, property, details) {
	var value = "";
	// set the value if it exists
	if (propertyObject[property.key]) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + escapeApos(value) + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) { 
		updateProperty(cell, propertyObject, property, details, ev.target.value); 
	}));
	// if this is the name also add an onchange to rebuild the map
	if (property.key == "name") {
		addListener( input.change( function(ev) { 
			buildPageMap();
		}));
	}
}

// a handler for inputs that must be integer numbers
function Property_integer(cell, propertyObject, property, details) {
	var value = "";
	// set the value if it exists (or is 0)
	if (propertyObject[property.key] || parseInt(propertyObject[property.key]) == 0) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to set the property back if not an integer
	addListener( input.change( function(ev) {
		var input = $(ev.target);
		var val = input.val();    
		// check integer match
		if (val.match(new RegExp("^\\d+$"))) {
			// update value
			updateProperty(cell, propertyObject, property, details, ev.target.value);						
		} else {
			// restore value
			input.val(propertyObject[property.key]);
		}
	}));
}

function Property_bigtext(cell, propertyObject, property, details) {
	var value = "";
	// set the value if it exists
	if (propertyObject[property.key]) value = propertyObject[property.key];
	// add the visible bit
	cell.text(value);
	// append and get a reference to the adjustable form control
	var textarea = $("#propertiesDialogues").append("<textarea style='position:absolute;display:none;height:300px;width:600px;z-index:10012' wrap='off'></textarea>").children().last();
	// add the text
	textarea.text(value);
	// add a listener to update the property
	addListener( cell.click( {textarea: textarea}, function(ev) {
		// assume right is 10
		var right = 10;
		// if we're in a dialogue increase to 20
		if ($(ev.target).closest(".propertyDialogue")) right = 21;
		// set the css
		ev.data.textarea.css({
			"right": right,
			"top": cell.offset().top,
			"z-index" : _dialogueZindex ++
		});
		textarea.slideDown(500);
		// focus it so a click anywhere else fires the unfocus and hides the textbox
		textarea.focus();
	}));	
	// hide the textarea and update the cell on unfocus
	addListener( textarea.blur( function(ev) {
		cell.text(textarea.val());
		textarea.hide(); 
		windowResize("Property_bigtext hide");
	}));
	// listen for key's we don't want to affect behaviour
	addListener( textarea.keydown( textareaOverride ));
	// modify if the text is updated
	addListener( textarea.keyup( function(ev) { 
		updateProperty(cell, propertyObject, property, details, textarea.val());  
	}));
	
}

function Property_select(cell, propertyObject, property, details, changeFunction) {
	// holds the options html
	var options = "";
	var js = property.getValuesFunction;
	try {
		// get and create the getValuesFunctions
		var f = new Function(js);	
		// apply and get what we're after
		var values = f.apply(propertyObject,[]);
		// check if we got an array back, or just use the response
		if ($.isArray(values)) {
			// loop the array and build the options html
			for (var i in values) {
				// if an array of simple types no value attribute
				if ($.type(values[i]) == "string" || $.type(values[i]) == "number" || $.type(values[i]) == "boolean") {
					// if the value is matched add selected
					if (propertyObject[property.key] == values[i]) {
						options += "<option selected='selected'>" + values[i] + "</option>";
					} else {
						options += "<option>" + values[i] + "</option>";
					}	
				} else {
					// this allows value/text pairs either in the form [{"v1":"t1"},{"v2":"t2"}] or [{"value":"v1","text":"t1"},{"value":"v2","text":"t2"}] 
					var value = values[i].value;
					if (!value) value = values[i][0];
					var text = values[i].text;
					if (!text) text = values[i][1];
					if (!text) text = value;
					// if the value is matched add selected
					if (propertyObject[property.key] == value) {
						options += "<option value='" + escapeApos(value) + "' selected='selected'>" + text + "</option>";
					} else {
						options += "<option value='" + escapeApos(value) + "'>" + text + "</option>";
					}
				}
			}
		} else {
			options = values;
		}
	} catch (ex) {
		alert("getValuesFunction failed for " + propertyObject.type + ". " + ex + "\r\r" + js);
	}
	
	// add the select object
	cell.append("<select class='propertiesPanelTable'>" + options + "</select>");
	// get a reference to the object
	var select = cell.children().last();
	// add a listener to update the property
	addListener( select.change({cell: cell, propertyObject: propertyObject, property: property, details: details, changeFunction: changeFunction}, function(ev) {
		// apply the property update
		updateProperty(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details, ev.target.value);
		// invoke any supplied change function
		if (ev.data.changeFunction) ev.data.changeFunction(ev);
	}));
	// read the property
	var val = propertyObject[property.key];
	// update from any primitive values to string values
	switch (val) {
		case (true) : val = "true"; break;
		case (false) : val = "false"; break;
	}
	// set the value
	select.val(val);	
}

function Property_checkbox(cell, propertyObject, property, details) {
	var checked = "";
	// set the value if it exists
	if (propertyObject[property.key] && propertyObject[property.key] != "false") checked = "checked='checked'";
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' type='checkbox' " + checked + " />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.change({cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// update the property
		updateProperty(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details, ev.target.checked);
	}));
}

// adds a gap in the properties
function Property_gap(cell, propertyObject, property, details) {
	cell.prev().html("");
}

function Property_fields(cell, action, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, action, property, details, 200, property.name);		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	// add the borders style
	table.addClass("dialogueTableAllBorders");
	// add the headers
	table.append("<tr><td colspan='2'>Field</td></tr>");
	
	// instantiate if need be
	if (!action[property.key]) action[property.key] = [];
	// get our fields
	var fields = action[property.key];
	
	// assume none yet
	var text = "Click to add...";
	// if we have some
	if (fields.length > 0) {
		// reset the text
		text = "";
		// loop them
		for (var i in fields) {
			// add it to the text
			text += fields[i];
			if (i < fields.length -1) text += ", ";
			// add it to the table
			table.append("<tr><td><input value='" + escapeApos(fields[i]) + "'/></td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		}
				
	}
	// set the cell text
	cell.text(text);
	
	// add change listeners
	addListener( table.find("input").keyup( function (ev) {
		// get the input
		var input = $(ev.target);
		// update the field field at this location
		fields[input.parent().parent().index() - 1] = input.val();
	}));
		
	// add delete listeners
	addListener( table.find("img.delete").click( function (ev) {
		// add undo
		addUndo();
		// get the image
		var img = $(ev.target);
		// remove the field at this location
		fields.splice(img.parent().parent().index() - 1,1);
		// update the dialogue;
		Property_fields(cell, action, property, details);
	}));
	
	// add reorder listeners
	addReorder(fields, table.find("img.reorder"), function() { Property_fields(cell, action, property); });
		
	// append add
	table.append("<tr><td colspan='3'><a href='#'>add...</a></td></tr>");
	
	// add listener
	addListener( table.find("a").click( function (ev) {
		// add a field
		fields.push("");
		// refresh dialogue
		Property_fields(cell, action, property, details);
	}));
	
}

function Property_inputAutoHeight(cell, input, property, details) {
	// check if the input control type is large
	if (input.controlType == "L") {
		// add a checkbox
		Property_checkbox(cell, input, property);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_galleryImages(cell, gallery, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, gallery, property, details, 200, "Images", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
		
	// instantiate if need be
	if (!gallery.images) gallery.images = [];
	// retain variable for them
	var images = gallery.images;
	// assume no text
	var text = "";
	// loop images
	for (var i in images) {
		// add url;
		text += images[i].url;
		// add comma if there are more
		if (i < images.length - 1) text += ",";
	}
	// set the cell
	if (text) {
		cell.html(text);
	} else {
		cell.html("Click to add...");
	}
	
	// append the drop down for existing images
	table.append("<tr><td  style='text-align:center;'>Url</td>" + (gallery.gotCaptions ? "<td  style='text-align:center;'>Caption</td>" : "") + "</tr>");
	
	// loop the images
	for (var i in images) {
		// get this image
		var image = images[i];
		// set caption to empty string if not set
		if (!image.caption) image.caption = "";
		// append
		table.append("<tr><td><input class='url' value='" + escapeApos(image.url) + "' style='max-width:none;width:100%;' /></td>" + (gallery.gotCaptions ? "<td><input class='caption' value='" + escapeApos(image.caption) + "' /></td>" : "") + "<td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
	}
	
	// add the url change listeners
	addListener( table.find("input.url").keyup( {cell:cell, gallery:gallery, property:property, details:details, images:images}, function (ev) {
		// get the input
		var input = $(ev.target);
		// get the url
		var url = input.val();
		// get the image according to the row index, less the header
		var image = ev.data.images[input.closest("tr").index() - 1];
		// if the url is going to change
		if (url != image.url) {
			// add an undo
			addUndo();
			// update the url
			image.url = url;
		}
		// update the reference and rebuild the html (this adds an undo)
		updateProperty(ev.data.cell, ev.data.gallery, ev.data.property, ev.data.details, ev.data.images); 			
	}));
	
	// add the caption change listeners
	addListener( table.find("input.caption").keyup( {cell:cell, gallery:gallery, property:property, details:details, images:images}, function (ev) {
		// get the input
		var input = $(ev.target);
		// get the caption
		var caption = input.val();
		// get the image according to the row index, less the header
		var image = ev.data.images[input.closest("tr").index() - 1];
		// set the caption
		image.caption = caption;
	}));
	
	// add delete listeners
	addListener( table.find("img.delete").click( function (ev) {
		// add undo
		addUndo();
		// get the image
		var img = $(ev.target);
		// remove the image at this location
		images.splice(img.index() - 1);
		// update html 
		rebuildHtml(gallery);
		// update the dialogue;
		Property_galleryImages(cell, gallery, property, details);
	}));
	
	// add reorder listeners
	addReorder(images, table.find("img.reorder"), function() { rebuildHtml(gallery); Property_galleryImages(cell, gallery, property); });
	
	// append add
	table.append("<tr><td colspan='3'><a href='#'>add...</a></td></tr>");
	
	// add listener
	addListener( table.find("a").click( function (ev) {
		// add an image
		images.push({url:""});
		// refresh dialogue
		Property_galleryImages(cell, gallery, property, details);
	}));
			
	// check we don't have a checkbox already
	if (!dialogue.find("input[type=checkbox]")[0]) {
		
		// append caption check box
		dialogue.append("<tr><td colspan='3'><input type='checkbox' " + (gallery.gotCaptions ? "checked='checked'" : "") + " /> captions</td></tr>");
		
		// captions listener
		addListener( dialogue.find("input[type=checkbox]").click( function (ev) {
			// get the checkbox
			var checkbox = $(ev.target);
			// set gotCaptions
			gallery.gotCaptions = checkbox.is(":checked");
			// refresh dialogue
			Property_galleryImages(cell, gallery, property, details);
		}));
		
	}
		
}

function Property_imageFile(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 200, "Image file", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	var value = "";
	// set the value if it exists
	if (propertyObject[property.key]) value = propertyObject[property.key];
	// set the cell
	if (value) {
		cell.html(value);
	} else {
		cell.html("Click to add...");
	}
	
	// check we have some images
	if (_version.images) {
		
		// append the drop down for existing images
		table.append("<tr><td><select><option value=''>Please select...</option></select></td></tr>");
		
		// get a reference to the drop down
		var dropdown = table.find("select");
		
		// loop the images and add to select
		for (var i in _version.images) {
			var selected = "";
			if (_version.images[i] == propertyObject[property.key]) selected = " selected='selected'";
			dropdown.append("<option" + selected + ">" + _version.images[i] + "</option>");
		}
		
		// add change listener
		addListener( dropdown.change( function (ev) {
			// get the file
			var file = $(this).val();
			// update the reference and rebuild the html
			updateProperty(cell, propertyObject, property, details, file);
			// all some time for the page to load in the image before re-establishing the selection border
        	window.setTimeout( function() {
        		// show the dialogue
        		positionAndSizeBorder(_selectedControl);        
        		// resize the window and check for any required scroll bars
        		windowResize("Image file dropdown change");
        	}, 200);
				
		}));
		
	}
	
	// append the  form control and the submit button
	table.append("<tr><td><form id='form_" + propertyObject.id + "' method='post' enctype='multipart/form-data' target='uploadIFrame' action='designer?action=uploadImage&a=" + _version.id + "&v=" + _version.version + "&p=" + _page.id + "&c=" + propertyObject.id + "'><input id='file_" + propertyObject.id + "' name='file' type='file'></input></form></td></tr><tr><td><input type='submit' value='Upload' /></td></tr>");
	
	// get a reference to the submit button
	addListener( table.find("input[type=submit]").click( {id : propertyObject.id}, function (ev) {
		// get the file value
		var file = $("#file_" + ev.data.id).val();
		// submit form if something provided
		if (file) $("#form_" + ev.data.id).submit();
	}));
}

function Property_linkPage(cell, propertyObject, property, details) {
	// if the type is a page
	if (propertyObject.linkType == "P") {
		// generate a select with refreshProperties = true
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove the row
		cell.parent().remove();
	}
}

function Property_linkURL(cell, propertyObject, property, details) {
	// if the type is a url
	if (propertyObject.linkType == "U")	{
		// add a text
		Property_text(cell, propertyObject, property, details);
	} else {
		// remove the row
		cell.parent().remove();
	}
}

function Property_pageName(cell, page, property, details) {
	// get the value from the page name
	var value = page.name;
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + escapeApos(value) + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) {
		// get the input into a jquery object
		var input = $(ev.target);
		// get the current value
		var val = input.val();
		// prepare a string which will hold "safe" values
		var safeVal = "";
		// get the cursor position
		var pos = input.caret();
		// loop the characters in the current value
		for (var i = 0; i < val.length; i++) {
			// retrieve the ascii code for this character
			var c = val.charCodeAt(i);
			// only if the character is in the safe range (0-9, A-Z, a-z, -, _)
			if ((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || c == 45 || c == 95 ) {
				 // add to safe value
				safeVal += val.charAt(i);
			} else {
				// otherwise drop a cursor position
				pos --;
			}
		}
		// if the value isn't safe
		if (val != safeVal) {		
			// update edit box to the safe value
			input.val(safeVal);
			// set the cursor position
			input.caret(pos);
			// retain the safe value
			val = safeVal;
		} 
		// update the property
		updateProperty(cell, page, property, details, val);
	}));

}

function Property_validationControls(cell, propertyObject, property, details) {
		
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 200, "Controls<button class='titleButton' title='Add all page controls with validation'><span>&#xf055;</span></button>", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// add the borders
	table.addClass("dialogueTableAllBorders");
	// make sure table is empty
	table.children().remove();
	
	// retain the controls
	var controls = [];
	// get the value if it exists
	if (propertyObject[property.key]) controls = propertyObject[property.key];	
	// if there are no controls and the current one has validation set it
	if (controls.length == 0 && _selectedControl.validation && _selectedControl.validation.type) { controls.push(_selectedControl.id); propertyObject[property.key] = controls; }
	// make some text
	var text = "";
	for (var i = 0; i < controls.length; i++) {
		var control = getControlById(controls[i]);
		if (control) {
			text += control.name;
		} 		
		if (text && i < controls.length - 1) text += ",";
	}
	
	// add a message if nont
	if (!text) text = "Click to add";
	// append the text into the cell
	cell.append(text);
	
	// add the current options
	for (var i in controls) {
		// see if we can get the control
		var control = getControlById(controls[i]);
		// check we can find the control - we can loose them when pasting
		if (control) {
			// add the row for this value
			table.append("<tr><td>" + control.name + "</td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");			
		} else {
			// remove this control
			controls.splice(i,1);
		}		
	}	
	
	// add listeners to the delete image
	addListener( table.find("img.delete").click( function(ev) {
		// get the row
		var row = $(this).parent().parent();
		// remove the control
		propertyObject.controls.splice(row.index(),1);
		// remove the row
		row.remove();
	}));
	
	// add reorder listeners
	addReorder(controls, table.find("img.reorder"), function() { Property_validationControls(cell, propertyObject, property); });
		
	// add an add dropdown
	var addControl = table.append("<tr><td colspan='2'><select><option value=''>Add control...</option>" + getValidationControlOptions(null, controls) + "</select></td></tr>").children().last().children().last().children().last();
	addListener( addControl.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// get a reference to the dropdown
		var dropdown = $(ev.target);
		// get the controlId
		var controlId = dropdown.val();
		// if we got one
		if (controlId) {
			// initialise the array if need be
			if (!propertyObject.controls) propertyObject.controls = [];
			// add the selected control id to the collection
			propertyObject.controls.push(controlId);
			// set the drop down back to "Please select..."
			dropdown.val("");		
			// re-render the dialogue
			Property_validationControls(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
		}
	}));
	
	// add listeners for all controls add
	addListener( dialogue.find("button").click( {controls:controls,cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// add an undo snapshot
		addUndo();
		// get the list of controls
		var controls = ev.data.controls;
		// initialise if need be
		if (!controls) controls = [];
		// get all page controls
		var pageControls = getControls();
		// prepare a list of controls to insert
		var insertControls = [];
		// loop the page controls
		for (var i in pageControls) {
			// get the pageControl
			var pageControl = pageControls[i];
			// if there is validation
			if (pageControl.validation && pageControl.validation.type) {
				// assume we don't have this control already
				var gotControl = false;
				// loop our controls
				for (var i in controls) {
					if (controls[i] == pageControl.id) {
						gotControl = true;
						break;
					}
				}
				// if not add to insert collection
				if (!gotControl) insertControls.push(pageControl.id);
			} 
		}
		// now loop the insert controls
		for (var i in insertControls) {
			// get the insert control
			var insertControl = insertControls[i];
			// get the insert control position in the page
			var insertPos = getKeyIndexControls(pageControls, insertControl);
			// assume we haven't inserted it
			var inserted = false;
			// now loop the existing validation controls
			for (var j in controls) {
				// get the existing position 
				var existingPos = getKeyIndexControls(pageControls, controls[j]);
				// if the existing pos is after the insert control position
				if (existingPos > insertPos) {
					// insert here
					controls.splice(j, 0, insertControl);
					// retain insert
					inserted = true;
					// we're done
					break;
				} // found a control after this one so insert before the found one
			} // loop dataCopies
			// if we haven't inserted yet do so now
			if (!inserted) controls.push(insertControl);
		} // loop inserts
		// add back the changed controls
		ev.data.propertyObject.controls = controls;
		// update dialogue
		Property_validationControls(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
	}));
	
}

function Property_childActions(cell, propertyObject, property, details) {
		
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 200, property.name, {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// remove the dialogue class so it looks like the properties
	table.parent().removeClass("dialogueTable");
	table.parent().addClass("propertiesPanelTable");
	// make sure its empty
	table.children().remove();
	
	// build what we show in the parent cell
	var actions = [];
	// get the value if it exists
	if (propertyObject[property.key]) actions = propertyObject[property.key];	
	// make some text
	var text = "";
	for (var i = 0; i < actions.length; i++) {
		text += actions[i].type;
		if (i < actions.length - 1) text += ",";
	}
	// if nothing add friendly message
	if (!text) text = "Click to add...";
	// put the text into the cell
	cell.text(text);
	
	// if there are actions
	if (actions.length > 0) {
		// add the copy row and image
		table.append("<tr><td colspan='2'><img class='copyActions' src='images/copy_16x16.png' title='Copy all actions'/></td></tr>");
		// add the listener
		addListener( table.find("img.copyActions").last().click( { actionType:propertyObject.type, propertyName:property.name, actions:actions, cell: cell, propertyObject: propertyObject, property: property, details: details }, function(ev) {
			// copy the actions
			_copyAction = ev.data;
			// rebuild the dialogue so the paste is available immediately
			Property_childActions(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);	
		}));
		// add a small space
		table.append("<tr><td colspan='2'></td></tr>");
	}
	
	// assume we will use the default action options
	var actionOptions = _actionOptions;
	// if there is a a copied item add it in
	if (_copyAction) actionOptions = "<optgroup label='New action'>" + _actionOptions + "</optgroup><optgroup label='Paste action'><option value='pasteActions'>" + getCopyActionName(propertyObject.id) + "</option></optgroup>";
		
	// add an add dropdown
	var addAction = table.append("<tr><td colspan='2' class='propertyAdd propertyHeader'><select><option value=''>Add action...</option>" + actionOptions + "</select></td></tr>").children().last().children().last().children().last();
	
	addListener( addAction.change( { cell: cell, propertyObject: propertyObject, property: property, details: details }, function(ev) {
		// get a reference to the dropdown
		var dropdown = $(ev.target);
		// get the controlId
		var actionType = dropdown.val();
		// if we got one
		if (actionType) {			
		
			// retrieve the propertyObject
			var propertyObject = ev.data.propertyObject;
			// retrieve the property
			var property = ev.data.property;
						
			// initilise the array if need be
			if (!propertyObject[property.key]) propertyObject[property.key] = [];
			// get a reference to the actions
			var actions = propertyObject[property.key];
			
			if (actionType == "pasteActions") {
				// if _copyAction
				if (_copyAction) {
					// reset the paste map
					_pasteMap = {};
					// check for actions collection
					if (_copyAction.actions) {
						// loop them
						for (var j in _copyAction.actions) {
							// get the action
							var action = _copyAction.actions[j];
							// add the action using the paste functionality if it's not going to be it's own parent
							if (ev.data.propertyObject.id != action.id) actions.push( new Action(action.type, action, true) );
						}										
					} else {
						// add the action using the paste functionality
						actions.push( new Action(_copyAction.type, _copyAction, true) );
					}
				}
				
			} else {				
				// add a new action of this type to the event
				actions.push( new Action(actionType) );
			}
						
			// set the drop down back to "Please select..."
			dropdown.val("");
			// rebuild the dialogue
			Property_childActions(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);			
		}		
	}));
	
	// add the current options
	for (var i in actions) {
		// retrieve this action
		var action = actions[i];
		// show the action (in actions.js)
		showAction(table, action, actions, function() { Property_childActions(cell, propertyObject, property); });
	}	
	
	// add reorder listeners
	addReorder(actions, table.find("img.reorder"), function() { Property_childActions(cell, propertyObject, property); });
	
	// get the dialogue id
	var dialogueId = dialogue.attr("id");
	
	// store what we need when refreshing inside this dialogue
	_dialogueRefeshProperties[dialogueId] = {cell: cell, propertyObject: propertyObject, property: property};
					
}

//generic inputs to a server-side action
function Property_inputs(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 400, "Inputs", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	// build what we show in the parent cell
	var inputs = [];
	// get the value if it exists
	if (propertyObject[property.key]) inputs = propertyObject[property.key];	
	// make some text for our cell (we're going to build in in the loop)
	var text = "";
	
	// add a header
	table.append("<tr><td><b>Control</b></td><td><b>Field</b></td><td colspan='2'><b>Input field</b></td></tr>");
		
	// show current choices (with delete and move)
	for (var i = 0; i < inputs.length; i++) {
		// get a single reference
		var input = inputs[i];	
		// if we got one
		if (input) {
			// get a data item object for this
			var dataItem = getDataItemDetails(input.itemId);
			// apend to the text
			text += dataItem.name + ",";
			// add a row
			table.append("<tr><td>" + dataItem.name + "</td><td><input value='" + input.field + "' /></td><td><input value='" + input.inputField + "' /></td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
			// get the field
			var editField = table.find("tr").last().children("td:nth(1)").children("input");
			// add a listener
			addListener( editField.keyup( {inputs: inputs}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update the field
				ev.data.inputs[input.parent().parent().index()-1].field = input.val();
			}));
			// get the inputfield
			var editInputField = table.find("tr").last().children("td:nth(2)").children("input");
			// add a listener
			addListener( editInputField.keyup( {inputs: inputs}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update the field
				ev.data.inputs[input.parent().parent().index()-1].inputField = input.val();				
			}));
			// get the delete image
			var imgDelete = table.find("tr").last().children().last().children("img.delete");
			// add a listener
			addListener( imgDelete.click( {inputs: inputs}, function(ev) {
				// get the input
				var imgDelete = $(ev.target);
				// remove from parameters
				ev.data.inputs.splice(imgDelete.parent().parent().index()-1,1);
				// remove row
				imgDelete.parent().parent().remove();
			}));
		} else {
			// remove this entry from the collection
			inputs.splice(i,1);
			// set i back 1 position
			i--;
		}			
	}
			
	// add reorder listeners
	addReorder(inputs, table.find("img.reorder"), function() { 
		Property_inputs(cell, propertyObject, property, details); 
	});
	
	// add the add
	table.append("<tr><td colspan='4' style='padding:0px;'><select style='margin:0px'><option value=''>Add input...</option>" + getInputOptions() + "</select></td></tr>");
	// find the add
	var inputAdd = table.find("tr").last().children().last().children().last();
	// listener to add output
	addListener( inputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		
		// initialise array if need be
		if (!ev.data.propertyObject[ev.data.property.key]) ev.data.propertyObject[ev.data.property.key] = [];
		// get the parameters (inputs or outputs)
		var inputs = ev.data.propertyObject[ev.data.property.key];
		// add a new one
		inputs.push({itemId: $(ev.target).val(), field: "", inputField: ""});
		// rebuild the dialogue
		Property_inputs(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);	
	}));
	
	// if we got text 
	if (text) {
		// remove the trailing comma
		text = text.substring(0,text.length - 1);
	} else {
		// add friendly message
		text = "Click to add...";
	}
	// put the text into the cell
	cell.text(text);
		
}

function Property_controlsForType(cell, propertyObject, property, details) {
	
	// check we have what we need
	if (details && details.type) {
		
		// find the control class
		var controlClasses = [];
		// if an array
		if ($.isArray(details.type)) {
			// loop it
			for (var i in details.type) {
				// get the class
				var c = _controlTypes[details.type[i]];
				// add this if we got one
				if (c) controlClasses.push(c);
			}
		} else {
			// get the class
			var c = _controlTypes[details.type];
			// add this if we got one
			if (c) controlClasses.push(c);
		}
		
		// check we have one
		if (controlClasses.length > 0) {
			
			// retrieve or create the dialogue
			var dialogue = getDialogue(cell, propertyObject, property, details, 200, details.type + " controls", {sizeX: true});		
			// grab a reference to the table
			var table = dialogue.children().last().children().last();
			// remove the dialogue class so it looks like the properties
			table.parent().removeClass("dialogueTable");
			table.parent().addClass("propertiesPanelTable");
			// make sure its empty
			table.children().remove();			
			
			// build what we show in the parent cell
			var controls = [];
			// get the value if it exists
			if (propertyObject[property.key]) controls = propertyObject[property.key];	
			// make some text
			var text = "";
			// loop the controls
			for (var i = 0; i < controls.length; i++) {
				// get the control
				var control = getControlById(controls[i]);
				// if we got one
				if (control) {
					// add the name to the cell text
					text += control.name;
					// add a comma if not the last one
					if (i < controls.length - 1) text += ",";					
					// add a row with the control name
					table.append("<tr><td>" + control.name + "</td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td><tr>");
				}				
			}
			
			// add listeners to the delete image
			addListener( table.find("img.delete").click( function(ev) {
				// get the row
				var row = $(this).parent().parent();
				// remove the control
				propertyObject[property.key].splice(row.index() - 1,1);
				// remove the row
				row.remove();
			}));
			
			// add reorder listeners
			addReorder(controls, table.find("img.reorder"), function() { Property_controlsForType(cell, propertyObject, property, details); });
			
			// start the options
			var options = "<option>add..</option>";
			// loop the classes
			for (var i in controlClasses) {
				// add to the options
				options += getControlOptions(null, null, controlClasses[i].type);
			}
						
			// have an add row
			table.append("<tr><td colspan='2'><select>" + options + "</select></td></tr>");
			// get a reference to the add
			var add = table.find("select").last();
			// add a listener
			addListener( add.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
				// get the value
				var value = $(ev.target).val();
				// if there was one
				if (value) {
					// get the array
					var controls = ev.data.propertyObject[ev.data.property.key];
					// instatiate if we need to
					if (!controls) {
						controls = [];
						ev.data.propertyObject[ev.data.property.key] = controls;
					}
					// add a blank option
					controls.push(value);
					// refresh
					Property_controlsForType(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);		
				}
			}));
			
			// if no text add friendly message
			if (!text) text = "Click to add...";
			// put the text into the cell
			cell.text(text);
								
		} else {
			
			// put a message into the cell
			cell.text("Control type " + details.type  + " not found");
			
		}
		
	} else {
		
		// put a message into the cell
		cell.text("Control type not provided");
		
	}
	
}

function Property_childActionsForType(cell, propertyObject, property, details) {
	
	// check we have what we need
	if (details && details.type) {
		
		// find the action class
		var actionClass = _actionTypes[details.type];
		
		// check we have one
		if (actionClass) {
			
			// retrieve or create the dialogue
			var dialogue = getDialogue(cell, propertyObject, property, details, 200, "Child " + actionClass.name + " actions", {sizeX: true});		
			// grab a reference to the table
			var table = dialogue.children().last().children().last();
			// remove the dialogue class so it looks like the properties
			table.parent().removeClass("dialogueTable");
			table.parent().addClass("propertiesPanelTable");
			// make sure its empty
			table.children().remove();
			
			// build what we show in the parent cell
			var actions = [];
			// get the value if it exists
			if (propertyObject[property.key]) actions = propertyObject[property.key];	
			// make some text
			var text = "";
			for (var i = 0; i < actions.length; i++) {
				text += actions[i].type;
				if (i < actions.length - 1) text += ",";
			}
			// if nothing add friendly message
			if (!text) text = "Click to add...";
			// put the text into the cell
			cell.text(text);
			
			// add a small space
			if (actions.length > 0) table.append("<tr><td colspan='2'></td></tr>");
			
			// add an add dropdown
			var addAction = table.append("<tr><td colspan='2'><a href='#' style='float:left;'>add...</a></td></tr>").children().last().children().last().children().last();
			
			addListener( addAction.click( { cell: cell, propertyObject : propertyObject, property : property, details: details }, function(ev) {
				// initialise this action
				var action = new Action(ev.data.details.type);
				// if we got one
				if (action) {			
					// retrieve the propertyObject
					var propertyObject = ev.data.propertyObject;
					// retrieve the property
					var property = ev.data.property;
					// initilise the array if need be
					if (!propertyObject[property.key]) propertyObject[property.key] = [];
					// add it to the array
					propertyObject[property.key].push(action);
					// rebuild the dialgue
					Property_childActionsForType(ev.data.cell, propertyObject, property, ev.data.details);			
				}		
			}));
			
			// if there is a _copyAction of the same type
			if (_copyAction && _copyAction.type == details.type) {
				// add a paste link
				var pasteAction = addAction.after("<a href='#' style='float:right;margin-right:5px;'>paste...</a>").next();
				// add a listener
				addListener( pasteAction.click({ cell: cell, propertyObject : propertyObject, property : property, details: details }, function(ev){
					
					// retrieve the propertyObject
					var propertyObject = ev.data.propertyObject;
					
					// check get the type
					if (_copyAction && _copyAction.type == propertyObject.type ) {
												
						// retrieve the property
						var property = ev.data.property;
						
						// initialise array if need be
						if (!propertyObject[property.key]) propertyObject[property.key] = [];
						// get the actions
						var actions = propertyObject[property.key];
						
						// add a new action based on the _copyAction
						actions.push( new Action(_copyAction.type, _copyAction, true) );
						
						// rebuild the dialogue
						Property_childActionsForType(ev.data.cell, propertyObject, property, ev.data.details);	
						
					}
										
				}));
			}
			
			// add the current options
			for (var i in actions) {
				// retrieve this action
				var action = actions[i];
				// show the action (in actions.js)
				showAction(table, action, actions, function() { Property_childActionsForType(cell, propertyObject, property, details); });
			}	
			
			// add reorder listeners
			addReorder(actions, table.find("img.reorder"), function() { Property_childActionsForType(cell, propertyObject, property, details); });
			
			// get the dialogue id
			var dialogueId = dialogue.attr("id");
			
			// store what we need when refreshing inside this dialogue
			_dialogueRefeshProperties[dialogueId] = {cell: cell, propertyObject: propertyObject, property: property};
									
		} else {
			
			// put a message into the cell
			cell.text("Action " + details.type + " not found");
			
		}
		
	} else {
		
		// put a message into the cell
		cell.text("Action type not provided");
		
	}
						
}

// this is a dialogue to specify the inputs, sql, and outputs for the database action
function Property_databaseQuery(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 650, "Query", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// make sure its empty
	table.children().remove();
		
	// initialise the query object if need be
	if (!propertyObject.query) propertyObject.query = {inputs:[], sql:"select field1, field2\nfrom table\nwhere field3 = ?", databaseConnectionIndex: 0, outputs:[]};
	// get the query
	var query = propertyObject.query;
	// get the sql into a variable
	var text = query.SQL;
	// change to message if not provided
	if (!text) text = "Click to define...";
	// put the elipses in the cell
	cell.text(text);
	
	// add inputs table, sql, and outputs table
	table.append("<tr><td colspan='2' style='padding:0px;vertical-align: top;'><table class='dialogueTable inputs'><tr><td><b>Input</b></td><td colspan='2'><b>Field</b></td></tr></table></td><td colspan='3' style='width:50%;padding:2px 10px 0 10px;'><b>SQL</b><br/><textarea style='width:100%;min-width:100%;max-width:100%;min-height:200px;box-sizing:border-box;'></textarea></td><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table  class='dialogueTable outputs'><tr><td><b>Field</b></td><td colspan='2'><b>Output</b></td></tr></table></td></tr>");
	
	// find the inputs table
	var inputsTable = table.find("table.inputs");
	// loop input parameters
	for (var i in query.inputs) {		
		// get the input name
		var itemName = query.inputs[i].itemId;
		// look for a control with an item of this item
		var control = getDataItemDetails(itemName);
		// if we found a control use this as the name
		if (control && control.name) itemName = control.name;		
		// get the field
		var field = query.inputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		inputsTable.append("<tr><td>" + (query.multiRow && i > 0 ? "&nbsp;" : itemName) + "</td><td><input value='" + escapeApos(field) + "' /></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field input
		var fieldInput = inputsTable.find("tr").last().children(":nth(1)").last().children().last();
		// add a listener
		addListener( fieldInput.keyup( {parameters: query.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = inputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		addListener( fieldDelete.click( {parameters: query.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.parameters.splice(input.parent().parent().index()-1,1);
			// remove row
			input.parent().parent().remove();
		}));
	}
	// add reorder listeners
	addReorder(query.inputs, inputsTable.find("img.reorder"), function() { 
		Property_databaseQuery(cell, propertyObject, property); 
	});
	
	// if multi row and at least one input
	if (query.multiRow && query.inputs.length > 0) {
		// add the add input linke
		inputsTable.append("<tr><td style='padding:0px;' colspan='2'><a href='#' style='padding-left:5px;'>add input</a></td><td>&nbsp;</td></tr>");
		// find the input add
		var inputAdd = inputsTable.find("tr").last().children().first().children("a");
		// listener to add input
		addListener( inputAdd.click( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
			// get the input parameters
			var inputs = ev.data.propertyObject.query.inputs;
			// add a new one
			inputs.push({itemId: inputs[0].itemId, field: ""});
			// rebuild the dialgue
			Property_databaseQuery(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
		}));
	} else {
		// add the add input select
		inputsTable.append("<tr><td style='padding:0px;' colspan='2'><select style='margin:0px'><option value=''>add input...</option>" + getInputOptions() + "</select></td><td>&nbsp;</td></tr>");
		// find the input add
		var inputAdd = inputsTable.find("tr").last().children().first().children("select");
		// listener to add input
		addListener( inputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
			// initialise array if need be
			if (!ev.data.propertyObject.query.inputs) ev.data.propertyObject.query.inputs = [];
			// get the parameters (inputs or outputs)
			var parameters = ev.data.propertyObject.query.inputs;
			// add a new one
			parameters.push({itemId: $(ev.target).val(), field: ""});
			// rebuild the dialgue
			Property_databaseQuery(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
		}));
	}
			
	// find the sql textarea
	var sqlControl = table.find("textarea").first();
	sqlControl.text(query.SQL);
	// listener for the sql
	addListener( sqlControl.keyup( {query: query}, function(ev) {
		query.SQL = $(ev.target).val();
	}));
	
	// find the outputs table
	var outputsTable = table.find("table.outputs");
	// loop output parameters
	for (var i in query.outputs) {
		// get the output id
		var itemName = query.outputs[i].itemId;
		// look for a control with an item of this item
		var control = getDataItemDetails(itemName);
		// if we found a control use this as the name
		if (control && control.name) itemName = control.name;
		// get the field
		var field = query.outputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		outputsTable.append("<tr><td><input value='" + escapeApos(field) + "' /></td><td>" + itemName + "</td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field input
		var fieldOutput = outputsTable.find("tr").last().children().first().children().last();
		// add a listener
		addListener( fieldOutput.keyup( {parameters: query.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = outputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		addListener( fieldDelete.click( {parameters: query.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.parameters.splice(input.parent().parent().index()-1,1);
			// remove row
			input.parent().parent().remove();
		}));			
	}
	// add reorder listeners
	addReorder(query.outputs, outputsTable.find("img.reorder"), function() { 
		Property_databaseQuery(cell, propertyObject, property); 
	});
	// add the add
	outputsTable.append("<tr><td style='padding:0px;' colspan='2'><select class='addOutput' style='margin:0px'><option value=''>add output...</option>" + getOutputOptions() + "</select></td><td>&nbsp;</td></tr>");
	// find the output add
	var outputAdd = outputsTable.find("select.addOutput");
	// listener to add output
	addListener( outputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.query.outputs) ev.data.propertyObject.query.outputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.query.outputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_databaseQuery(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);	
	}));
	
	table.append("<tr><td colspan='2'>Multi-row input data?&nbsp;<input class='multi' type='checkbox'" + (query.multiRow ? "checked='checked'" : "" ) + " style='vertical-align: middle;margin-top: -3px;'/></td><td style='text-align: left;overflow:inherit;' colspan='2'>Database connection <select style='width:auto;'>" + getDatabaseConnectionOptions(query.databaseConnectionIndex) + "</select></td><td style='text-align:right;'><button>Test SQL</button></td></tr>");
	
	// get a reference to the multi-data check box
	var multiRow = table.find("tr").last().find("input");
	// add a listener for if it changes
	addListener( multiRow.change( {cell: cell, propertyObject: propertyObject, property: property, details: details, query: query}, function(ev) {
		// set the multiData value
		ev.data.query.multiRow = $(ev.target).is(":checked");
		// refresh the dialogue
		Property_databaseQuery(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
	}));
	
	// get a reference to the db connection
	var dbConnection = table.find("tr").last().find("select");
	// add a listener for the database connection
	addListener( dbConnection.change( {query: query}, function(ev) {
		// set the index value
		ev.data.query.databaseConnectionIndex = ev.target.selectedIndex;
	}));
	
	// get a reference to the test button
	var testSQL = table.find("tr").last().find("button");
	// add a listener for the database connection
	addListener( testSQL.click( {query: query}, function(ev) {
		
		var query = JSON.stringify(ev.data.query);
		
		$.ajax({
	    	url: "designer?a=" + _version.id + "&v=" + _version.version + "&action=testSQL",
	    	type: "POST",
	    	contentType: "application/json",
		    dataType: "json",  
	    	data: query,
	        error: function(server, status, error) { 
	        	alert(error + " : " + server.responseText); 
	        },
	        success: function(response) {
	        	alert(response.message); 		       		        	
	        }
		});
		
	}));
}

// reuse the generic childActionsForType but set the details with type = database
function Property_databaseChildActions(cell, propertyObject, property, details) {
	Property_childActionsForType(cell, propertyObject, property, {type:"database"});
}

//this is a dialogue to specify the inputs, post body, and outputs for the webservice action
function Property_webserviceRequest(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 1000, "Webservice request", {sizeX: true, minWidth: 600});		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// make sure its empty
	table.children().remove();
		
	// initialise the request object if need be
	if (!propertyObject.request) propertyObject.request = {inputs:[], type:"SOAP", url: 'soa', action: 'demo.Samplewebservice', body: '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:soa="http://soa.rapid-is.co.uk">\n  <soapenv:Body>\n    <soa:personSearchRequest>\n      <soa:surname>A</soa:surname>\n    </soa:personSearchRequest>\n  </soapenv:Body>\n</soapenv:Envelope>', outputs:[]};
	// get the request
	var request = propertyObject.request;
	// get the sql into a variable
	var text = request.type;
	// change to message if not provided
	if (!text) text = "Click to define...";
	// put the elipses in the cell
	cell.text(text);
	
	// add inputs table, body, and outputs table
	table.append("<tr><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table class='dialogueTable'><tr><td><b>Input</b></td><td colspan='2'><b>Field</b></td></tr></table></td><td class='normalInputs' colspan='2' style='width:500px;padding:0 6px;'><b style='display:block;'>Request type</b><input type='radio' name='WSType' value='SOAP'/>SOAP<input type='radio' name='WSType' value='JSON'/>JSON<input type='radio' name='WSType' value='XML'/>XML/Restful<b style='display:block;margin-top:5px;margin-bottom:5px;'>URL</b><input class='WSUrl' /></br><b style='display:block;margin-top:5px;margin-bottom:5px;'>Action</b><input class='WSAction' /><b style='display:block;margin-top:5px;margin-bottom:2px;'>Body</b><textarea style='width:100%;min-height:200px;' class='WSBody'></textarea><b style='display:block;'>Response transform</b><textarea style='width:100%;' class='WSTransform'></textarea><b style='display:block;;margin-bottom:5px;'>Response root element</b><input class='WSRoot' style='margin-bottom:5px;' /></td><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table class='dialogueTable'><tr><td><b>Field</b></td><td colspan='2'><b>Output</b></td></tr></table></td></tr>");
	
	// find the inputs table
	var inputsTable = table.children().last().children().first().children().last();
	// loop input parameters
	for (var i in request.inputs) {
		// get the input name
		var itemName = request.inputs[i].itemId;
		// look for the details of this item
		var control = getDataItemDetails(itemName);
		// if we found a control use this as the name
		if (control && control.name) itemName = control.name;
		// get the field
		var field = request.inputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		inputsTable.append("<tr><td>" + itemName + "</td><td><input value='" + escapeApos(field) + "' /></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field input
		var fieldInput = inputsTable.find("tr").last().children(":nth(1)").last().children().last();
		// add a listener
		addListener( fieldInput.keyup( {parameters: request.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = inputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		addListener( fieldDelete.click( {parameters: request.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.parameters.splice(input.parent().parent().index()-1,1);
			// remove row
			input.parent().parent().remove();
		}));
	}
	// add reorder listeners
	addReorder(request.inputs, inputsTable.find("img.reorder"), function() { 
		Property_webserviceRequest(cell, propertyObject, property); 
	});
	// add the add input
	inputsTable.append("<tr><td style='padding:0px;'><select style='margin:0;'><option value=''>Add input...</option>" + getInputOptions() + "</select></td><td>&nbsp;</td><td>&nbsp;</td></tr>");
	// find the input add
	var inputAdd = inputsTable.find("tr").last().children().first().children().first();
	// listener to add input
	addListener( inputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.request.inputs) ev.data.propertyObject.request.inputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.request.inputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_webserviceRequest(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
	}));
	
	// find the type radios
	var typeControls = table.find("input[type=radio]");
	typeControls.filter("[value=" + request.type + "]").prop("checked","true");
	// listener for the action
	addListener( typeControls.click( {request: request}, function(ev) {
		ev.data.request.type = $(ev.target).val();
	}));
	
	// find the url input box
	var actionControl = table.find("input.WSUrl");
	actionControl.val(request.url);
	// listener for the action
	addListener( actionControl.keyup( {request: request}, function(ev) {
		ev.data.request.url = $(ev.target).val();
	}));
	
	// find the action input box
	var actionControl = table.find("input.WSAction");
	actionControl.val(request.action);
	// listener for the action
	addListener( actionControl.keyup( {request: request}, function(ev) {
		ev.data.request.action = $(ev.target).val();
	}));
	
	// find the request body textarea
	var bodyControl = table.find("textarea.WSBody");
	bodyControl.text(request.body);
	// listener for the body
	addListener( bodyControl.keyup( {request: request}, function(ev) {
		ev.data.request.body = $(ev.target).val();
	}));
	
	// find the transform textarea
	var transformControl = table.find("textarea.WSTransform");
	transformControl.text(request.transform);
	// listener for the body
	addListener( transformControl.keyup( {request: request}, function(ev) {
		ev.data.request.transform = $(ev.target).val();
	}));
	
	// find the response root element
	var rootControl = table.find("input.WSRoot");
	rootControl.val(request.root);
	// listener for the root
	addListener( rootControl.keyup( {request: request}, function(ev) {
		ev.data.request.root = $(ev.target).val();
	}));
	
	// find the outputs table
	var outputsTable = table.children().last().children().last().children().last();
	// loop output parameters
	for (var i in request.outputs) {
		// get the output name
		var itemName = request.outputs[i].itemId;
		// look for a control with an item of this item
		var control = getDataItemDetails(itemName);
		// if we found a control use this as the name
		if (control && control.name) itemName = control.name;
		// get the field
		var field = request.outputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		outputsTable.append("<tr><td><input value='" + escapeApos(field) + "' /></td><td>" + itemName + "</td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field input
		var fieldOutput = outputsTable.find("tr").last().children().first().children().last();
		// add a listener
		addListener( fieldOutput.keyup( {parameters: request.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = outputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		addListener( fieldDelete.click( {parameters: request.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.parameters.splice(input.parent().parent().index()-1,1);
			// remove row
			input.parent().parent().remove();
		}));			
	}
	// add reorder listeners
	addReorder(request.outputs, outputsTable.find("img.reorder"), function() { 
		Property_webserviceRequest(cell, propertyObject, property, details); 
	});
	// add the add
	outputsTable.append("<tr><td>&nbsp;</td><td style='padding:0px;'><select style='margin:0px'><option value=''>Add output...</option>" + getOutputOptions() + "</select></td><td>&nbsp;</td></tr>");
	// find the output add
	var outputAdd = outputsTable.find("tr").last().children(":nth(1)").last().children().last();
	// listener to add output
	addListener( outputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.request.outputs) ev.data.propertyObject.request.outputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.request.outputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_webserviceRequest(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);	
	}));
		
}

// this is a special drop down that can make the property below visible
function Property_navigationPage(cell, navigationAction, property, details) {
	
	// add the drop down with it's values
	cell.append("<select><option value=''>Please select...</option>" + getPageOptions(navigationAction[property.key]) + "</select>");
	// get a reference to the drop down
	var pageDropDown = cell.find("select").last();
	// add a listener
	addListener( pageDropDown.change( {cell: cell, navigationAction: navigationAction, property: property, details: details}, function(ev) {
		// get the value
		value = $(ev.target).val();
		// update it
		updateProperty(ev.data.cell, ev.data.navigationAction, ev.data.property, ev.data.details, value);
		// refresh this dialogue
		Property_navigationPage(ev.data.cell, ev.data.navigationAction, ev.data.property, ev.data.details);
	}));
	
}

// this is a dialogue to specify the session variables of the current page
function Property_pageSessionVariables(cell, page, property, details, textOnly) {
	
	// check for simple
	if (_page.simple) {
		
		// remove the row
		cell.parent().remove();
		
	} else {
			
		var variables = [];
		// set the value if it exists
		if (page.sessionVariables) variables = page.sessionVariables;
		// make some text
		var text = "";
		for (var i = 0; i < variables.length; i++) {
			text += variables[i];
			if (i < variables.length - 1) text += ",";
		}
		// add a descrption if nothing yet
		if (!text) text = "Click to add...";
		// append the adjustable form control
		cell.text(text);
		
		// avoid redoing the whole thing 
		if (!textOnly) {
		
			// retrieve or create the dialogue
			var dialogue = getDialogue(cell, page, property, details, 200, "Page variables", {sizeX: true});		
			// grab a reference to the table
			var table = dialogue.find("table").first();
			// add the all grid
			table.addClass("dialogueTableAllBorders");
			// make sure table is empty
			table.children().remove();
			
			// show variables
			for (var i in variables) {
				// add the line
				table.append("<tr><td><input class='variable' value='" + escapeApos(variables[i]) + "' /></td><td style='width:16px;'><img src='images/bin_16x16.png' style='float:right;' /></td></tr>");
				
				// find the text
				var valueEdit = table.find("input.variable").last();
				// add a listener
				addListener( valueEdit.keyup( {cell: cell, page: page, property: property, details: details}, function(ev) {
					// get the input box
					var input = $(ev.target);
					// get the value
					var value = input.val();
					// get the index
					var index = input.closest("tr").index();
					// update value
					ev.data.page.sessionVariables[index] = value;
					// refresh
					Property_pageSessionVariables(ev.data.cell, ev.data.page, ev.data.property, ev.data.details, true);
				}));
						
				// find the delete
				var optionDelete = table.find("tr").last().children().last().children().last();
				// add a listener
				addListener( optionDelete.click( {variables: variables}, function(ev) {
					// add an undo snapshot
					addUndo();
					// get the input
					var input = $(ev.target);
					// remove from parameters
					ev.data.variables.splice(input.parent().parent().index(),1);
					// remove row
					input.parent().parent().remove();
				}));
			}
				
			// have an add row
			table.append("<tr><td colspan='2'><a href='#'>add...</a></td></tr>");
			// get a reference to the add
			var add = table.find("tr").last().children().last().children().last();
			// add a listener
			addListener( add.click( {cell: cell, page: page, property: property, details: details}, function(ev) {
				// add an undo snapshot
				addUndo();
				// initialise if required
				if (!ev.data.page.sessionVariables) ev.data.page.sessionVariables = [];
				// add a blank option
				ev.data.page.sessionVariables.push("");
				// refresh
				Property_pageSessionVariables(ev.data.cell, ev.data.page, ev.data.property, ev.data.details);		
			}));
			
		}
		
	}
	
}

// this is a dialogue which allows for the adding of user roles
function Property_roles(cell, control, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, control, property, details, 200, "User roles", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// add the allgrid class
	table.addClass("dialogueTableAllBorders");
	// make sure table is empty
	table.children().remove();
	
	var roles = [];
	// set the value if it exists
	if (control.roles) roles = control.roles;
	// make some text
	var text = "";
	for (var i = 0; i < roles.length; i++) {
		text += roles[i];
		if (i < roles.length - 1) text += ",";
	}
	// add a descrption if nothing yet
	if (!text) text = "Click to add...";
	// append the adjustable form control
	cell.text(text);
	
	// show roles
	for (var i in roles) {
		// add the line
		table.append("<tr><td>" + roles[i] + "</td><td style='width:16px;'><img src='images/bin_16x16.png' style='float:right;' /></td></tr>");
						
		// find the delete
		var optionDelete = table.find("tr").last().children().last().children().last();
		// add a listener
		addListener( optionDelete.click( {roles: roles, cell: cell, control: control, property: property, details: details}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.roles.splice(input.parent().parent().index(),1);
			// remove row
			input.parent().parent().remove();
			// refresh
			Property_roles(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);
		}));
	}
		
	// have an add dropdown
	table.append("<tr><td colspan='2'><select><option value=''>add...</option>" + getRolesOptions(null, roles) + "</td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.change( {cell: cell, control: control, property: property, details: details}, function(ev) {
		// initialise if required
		if (!ev.data.control.roles) ev.data.control.roles = [];
		// get the role
		var role = ev.target.value;
		// add the selected role if one was chosen
		if (role) ev.data.control.roles.push(role);
		// refresh
		Property_roles(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);		
	}));

}

// this is a dialogue to specify the session variables to set when navigating
function Property_navigationSessionVariables(cell, navigation, property, details) {
	
	// this is some reuse in the link control - if it's type isn't P for page
	if (navigation.linkType && navigation.linkType != "P") {
		// remove this row
		cell.parent().remove();
		// stop going any further
		return false;
	}
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, navigation, property, details, 300, "Set page variables", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// add dialogueTableBorders
	table.addClass("dialogueTableBorders");
	// make sure table is empty
	table.children().remove();
	
	// find the page we're set up to go to
	var page = null;
	for (var i in _pages) {
		if (_pages[i].id == navigation.page) page = _pages[i];
	}
	
	// check a page to go to has been specified
	if (page) {
	
		// initialise the collection if need be
		if (!navigation.sessionVariables || navigation.sessionVariables == "[]") navigation.sessionVariables = [];
		// reset if there are no page session variables
		if (!page.sessionVariables || page.sessionVariables.length == 0) navigation.sessionVariables = [];
		// retrieve the collection
		var sessionVariables = navigation.sessionVariables;
		
		// make some text
		var text = "";
		for (var i = 0; i < sessionVariables.length; i++) {
			text += sessionVariables[i].name;
			if (i < sessionVariables.length - 1) text += ",";
		}
		// add a descrption if nothing yet
		if (!text) text = "Click to add...";
		// append the adjustable form control
		cell.text(text);
		
		// add a header
		table.append("<tr><td><b>Variable</b></td><td><b>Input</b></td><td><b>Field</b></td></tr>");
						
		// show all session parameters in the target page
		for (var i in page.sessionVariables) {
			
			// get the corresponding action variable
			var sessionVariable = sessionVariables[i];
			// if not there or not the right one
			if (!sessionVariable || sessionVariable.name != page.sessionVariables[i]) {
				// create a new one with the right name
				sessionVariable = {name:page.sessionVariables[i], itemId:"", field:""};
				// add to the aray at this position				
				sessionVariables.splice(i,0,sessionVariable);
			}

			// set the input name
			var name = sessionVariable.name;
			// look for a control with this id
			var control = getControlById(name);
			// update name if we found one
			if (control) name = control.name;
			
			// add the line
			table.append("<tr><td>" + name + "</td><td><select><option value=''>Please select...</option>" + getInputOptions(sessionVariable.itemId) + "</select></td><td><input value='" + escapeApos(sessionVariable.field) + "' /></td></tr>");
			
			// find the dropdown
			var itemEdit = table.find("select").last();
			// add a listener
			addListener( itemEdit.change( {sessionVariables: sessionVariables}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update value
				ev.data.sessionVariables[input.parent().parent().index()-1].itemId = input.val();
			}));
			
			// find the input
			var fieldEdit = table.find("input").last();
			// add a listener
			addListener( fieldEdit.keyup( {sessionVariables: sessionVariables}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update value
				ev.data.sessionVariables[input.parent().parent().index()-1].field = input.val();
			}));
					
		}
		// remove any extra entries (in case our insertion made the collection too big)
		if (sessionVariables.length > i) sessionVariables.splice(i*1 + 1, sessionVariables.length);
		
	} else {
		
		// hide this row until a page is selected
		cell.parent().hide();
		
	}
					
}

//whether this page is simple, and no events
function Property_simple(cell, control, property, details) {
	// start with a default check box
	Property_checkbox(cell, control, property, details);
	// get a reference to the checkboxl
	var input = cell.find("input");
	// add a listener to show ot hide the events
	addListener( input.change( function(ev) {
		// rebuild them accordingly
		showEvents(_page);
	}));
}

function Property_navigationStopActions(cell, navigation, property, details) {
	
	if (navigation.dialogue) {
		
		// create a checkbox for the property
		Property_checkbox(cell, navigation, property, details);
		
	} else {
		
		// hide this row 
		cell.parent().hide();
		
	}
	
}

//this is a dialogue to define radio buttons for the radio buttons control
function Property_radiobuttons(cell, control, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, control, property, details, 200, "Radio buttons", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	// if we're using a value list and this version still has them
	if (control.valueList && _version.valueLists && _version.valueLists.length > 0) {
		
		// set the text to the value list
		cell.text(control.valueList);
		
		// remove any current useCodes
		dialogue.find("div.useCodes").remove();
		
	} else {
	
		var buttons = [];
		// set the value if it exists
		if (control.buttons) buttons = control.buttons;
		// make some text
		var text = "";
		for (var i = 0; i < buttons.length; i++) {
			text += buttons[i].label;
			if (control.codes) text += " (" + buttons[i].value + ")";
			if (i < buttons.length - 1) text += ",";
		}
		// add a descrption if nothing yet
		if (!text) text = "Click to add...";
		// append the adjustable form control
		cell.text(text);
		
		// add a heading
		table.append("<tr>" + (control.codes ? "<td><b>Text</b></td><td colspan='2'><b>Code</b></td>" : "<td colspan='2'><b>Text</b></td>") + "</tr>");
		
		// show options
		for (var i in buttons) {
			// add the line
			table.append("<tr><td><input class='label' value='" + escapeApos(buttons[i].label) + "' /></td>" + (control.codes ? "<td><input class='value' value='" + escapeApos(buttons[i].value) + "' /></td>" : "") + "<td style='width:32px;padding:0;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
			
			// find the code
			var valueEdit = table.find("input.value").last();
			// add a listener
			addListener( valueEdit.keyup( {control : control, buttons: buttons}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update value
				ev.data.buttons[input.parent().parent().index()-1].value = input.val();
				// update html 
				rebuildHtml(ev.data.control);
			}));
			
			// find the label
			var textEdit = table.find("input.label").last();
			// add a listener
			addListener( textEdit.keyup( {control : control, buttons: buttons}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update text
				ev.data.buttons[input.parent().parent().index()-1].label = input.val();
				// update html 
				rebuildHtml(ev.data.control);
			}));
			
		}
			
		// have an add row
		table.append("<tr><td colspan='" + (control.codes ? "3" : "2") + "'><a href='#'>add...</a></td></tr>");
		// get a reference to the add
		var add = table.find("tr").last().children().last().children().last();
		// add a listener
		addListener( add.click( {cell: cell, control: control, property: property, details: details}, function(ev) {
			// add a blank option
			ev.data.control.buttons.push({value: "", label: ""});
			// refresh
			Property_radiobuttons(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);		
		}));
				
		// find the deletes
		var buttonDelete = table.find("img.delete");
		// add a listener
		addListener( buttonDelete.click( {cell: cell, control: control, property: property, details: details}, function(ev) {
			// get the del image
			var delImage = $(ev.target);
			// remove from parameters
			ev.data.control.buttons.splice(delImage.parent().parent().index()-1,1);
			// remove row
			delImage.parent().parent().remove();
			// update html if top row
			if (delImage.parent().index() == 1) rebuildHtml(ev.data.control);
			// refresh
			Property_radiobuttons(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);
		}));
		
		// add reorder listeners
		addReorder(buttons, table.find("img.reorder"), function() { 
			// refresh the html and regenerate the mappings
			rebuildHtml(control);
			// refresh the property
			Property_radiobuttons(cell, control, property, details); 
		});
		
		// check we don't have a checkbox already
		if (!dialogue.find("div.useCodes")[0]) {
			// add checkbox
			table.after("<div class='useCodes'>Use codes <input type='checkbox' " + (control.codes ? "checked='checked'" : "") + " /></span>");
			// get a reference
			var optionsCodes = dialogue.children().last();
			// add a listener
			addListener( optionsCodes.change( {cell: cell, control: control, buttons: buttons, property: property, details: details}, function(ev) {
				// get the value
				ev.data.control.codes = ev.target.checked;
				// refresh
				Property_radiobuttons(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);
			
			}));
			
		}
		
	}
	
	// only if this version has value lists
	if (_version.valueLists && _version.valueLists.length > 0) {
		// check we don't have a value list select already
		if (!dialogue.find("select")[0]) {
			// add the select
			dialogue.append("Value list <select><option value=''>None</option>" + getValueListsOptions(control.valueList) + "</select>");
			// get a reference
			var valueListSelect = dialogue.find("select");
			// add a listener
			addListener( valueListSelect.change( {cell: cell, control: control, property: property, details: details}, function(ev) {
				// get the value
				ev.data.control.valueList = $(ev.target).val();
				// refresh
				Property_radiobuttons(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);
				// rebuild
				rebuildHtml(ev.data.control);
			}));				
		}
		// add margin to checkbox if present
		$("div.useCodes").css("margin-bottom","10px");
	}
	
}

//possible system values used by the Logic property
var _logicOperations = [["==","= equals"],["!=","!= doesn't equal"],[">","> greater than"],[">=",">= greater than or equal to"],["<","< less than"],["<=","<= less than or equal to"]];

function logicConditionText(condition) {
	// make some text
	var text = "Not set";	
	// check the type
	switch (condition.type) {
		case "CTL" :
			// assume there is no control
			var control = null;
			// get the id parts
			var idParts = condition.id.split(".");
			// get the control id
			var controlId = idParts;
			// if  there was more than one part, take the first
			if (idParts.length > 1) controlId = idParts[0];
			// look for the control
			if (controlId) control = getControlById(controlId);
			// if we don't find one just show id (could be page variable)
			text = (control ? control.name : condition.id);
			// add the property if present
			if (idParts.length > 1 && text != condition.id) text += "." + idParts[1];
			// add the field if present
			if (condition.field) text += "." + condition.field;
		break;
		case "SYS" : case "SES" :
			// the second part of the id
			text = condition.id.split(".")[1];
			// if this is the field use the field property instead
			if (text == "field") text = condition.field;
		break;		
	}
	// clean up any old style refences from session to page variables
	if (text) text = text.replace("Session.","Variable.");
	// return
	return text;
}

function logicConditionValue(cell, action, key, conditionIndex, valueId) {
	
	// get a reference to the condition
	var condition = action[key][conditionIndex];
	// instantiate the value if need be
	if (!condition[valueId]) condition[valueId] = {type:"CTL"};
	// get a reference for the value
	var value = condition[valueId];
		
	// clear and add a table into the cell for this value
	cell.html("<table class='propertiesPanelTable'></table>")
	// get a reference to it
	var table = cell.find("table").last();
	
	var options = "";
	if (key == "visibilityConditions") {
		options = getPageVisibilityOptions(value.id);
	} else {
		options = getInputOptions(value.id);
	}
	
	table.append("<tr><td>" + (valueId == "value1" ? "Item 1" : "Item 2") + "</td><td><select>" + options + "</select></td></tr>");
	// get a reference to the select
	var select = table.find("select");
	// retain the value if we don't have one yet
	if (!value.id) value.id = select.val();			
	// add listers
	addListener( table.find("select").change( function(ev) {		
		// get the id
		var id = $(ev.target).val();
		// derive the new type
		var type = "CTL";				
		// check for system value
		if (id.substr(0,7) == "System.") type = "SYS";
		// check for session value
		if (id.substr(0,8) == "Session.") type = "SES";
		// set the new type 
		value.type = type;
		// set the id
		value.id = id;
		// refresh the property
		logicConditionValue(cell, action, key, conditionIndex, valueId); 
	}));
		
	switch (value.type) {
		case "CTL" :
			// set the html			
			table.append("<tr><td>Field</td><td><input /></td></tr>");			
			// get the field
			var input = table.find("input").last();
			// set any current value
			if (value.field) input.val(value.field);
			// add the listener
			addListener( input.keyup( function(ev) {		
				// set the new value 
				value.field = $(ev.target).val();
			}));
		break;
		case "SYS" :
			if (value.id == "System.field") {
				// set the html
				table.append("<tr><td>Value</td><td><input /></td></tr>");
				// get the input
				var input = table.find("input").last();
				// set any current value
				if (value.field) input.val(value.field);
				// add the listeners
				addListener( input.keyup( function(ev) {		
					// set the new value into the field
					value.field = $(ev.target).val();
				}));
			}
		break;		
	}
	
}

function Property_logicConditions(cell, action, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, action, property, details, 500, "Conditions", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	// remove the dialogueTable class
	table.removeClass("dialogueTable");
	// add the dialogueTable class
	table.addClass("propertiesPanelTable");
	// get the conditions
	var conditions = action[property.key];
	// instantiate if required
	if (!conditions) conditions = [];
	// if the type is not specified make it an and
	if (!action.conditionsType) action.conditionsType = "and";
	// assume there is no text
	var text = "";
		
	// loop the conditions
	for (var i in conditions) {
		
		// get the condition
		var condition = conditions[i];
		
		// add cells
		table.append("<tr><td></td><td style='width:150px;'></td><td></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		
		// get last row
		var lastrow = table.find("tr").last();
		
		// get cell references
		var value1Cell = lastrow.children("td:nth(0)");		
		var value2Cell = lastrow.children("td:nth(2)");
		var operationCell = lastrow.children("td:nth(1)");
		
		// add (sub)properties
		logicConditionValue(value1Cell, action, property.key, i, "value1");
		logicConditionValue(value2Cell, action, property.key, i, "value2");
		
		var operationHtml = "<select style='margin-top:1px;'>"
		for (var j in _logicOperations) {
			operationHtml += "<option value='" + _logicOperations[j][0] + "'" + (condition.operation == _logicOperations[j][0] ? " selected='selected'" : "") + ">" + _logicOperations[j][1] + "</option>";
		}
		operationHtml += "</select>";
		operationCell.append(operationHtml);
		
		// add a listener for the operation
		operationCell.find("select").last().change({condition: condition}, function(ev){
			ev.data.condition.operation = $(ev.target).val();
		});
		
		// build the text from the conditions, operation (== is mapped to =)
		text += logicConditionText(condition.value1) + " " + (condition.operation == "==" ? "=" : condition.operation)  + " " + logicConditionText(condition.value2);
		// add the type to seperate conditions
		if (i < conditions.length - 1) text += " " + action.conditionsType + " ";
		
	}
	
	// update text if not set
	if (!text) text = "Click to add...";
	// add in the text
	cell.text(text);
	
	// find the deletes
	var deleteImages = table.find("img.delete");
	// add a listener
	addListener( deleteImages.click( {conditions: conditions}, function(ev) {
		// get the del image
		var delImage = $(ev.target);
		// remove from conditions
		ev.data.conditions.splice(delImage.parent().parent().index(),1);
		// if there are now less than 2 conditions remove and/or row too
		if (ev.data.conditions.length < 2) $(ev.target).closest("table").find("tr:last").prev().remove();
		// remove row
		delImage.parent().parent().remove();		
	}));
		
	// add reorder listeners
	addReorder(conditions, table.find("img.reorder"), function() { 		
		// refresh the property
		Property_logicConditions(cell, action, property, details); 
	});
	
	// only if there are 2 or more conditions
	if (conditions.length > 1) {		
		// add type
		table.append("<tr><td colspan='4' style='padding-left:12px;'><input type='radio' name='" + action.id + "type' value='and'" + (action.conditionsType == "and" ? " checked='checked'" : "") + "/>all conditions must be true (And) <input type='radio' name='" + action.id + "type' value='or'" + (action.conditionsType == "or" ? " checked='checked'" : "") + "/>any condition can be true (Or) </td></tr>");
		// add change listeners
		table.find("input[name=" + action.id + "type]").change( function(ev){
			// set the condition type to the new val
			action.conditionsType = $(ev.target).val();
		});
	}
				
	// add add
	table.append("<tr><td colspan='4'><a href='#'>add...</a></td></tr>");
	// add listener
	table.find("a").last().click( function(ev) {
		// instatiate if need be
		if (!action[property.key]) action[property.key] = [];
		// add new condition
		action[property.key].push({value1:{type:"CTL"}, operation: "==", value2: {type:"CTL"}});
		// update this table
		Property_logicConditions(cell, action, property, details);
	});
		
}

// this very similar to the above but hidden if no form adapter
function Property_visibilityConditions(cell, control, property, details) {
	// if this is not a simple page
	if (_version.formAdapter && !_page.simple) {
		Property_logicConditions(cell, control, property, details)
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// this is a dialogue to refine the options available in dropdown and list controls
function Property_options(cell, control, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, control, property, details, 200, "Options", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	// check for a value list and that this version has some
	if (control.valueList && _version.valueLists && _version.valueLists.length > 0) {
		
		// set the text to the value list
		cell.text(control.valueList);
		
		// remove any prior use codes 
		dialogue.find("div.useCodes").remove();
		
	} else {
		
		var options = [];
		// set the value if it exists
		if (control.options) options = control.options;
		// make some text
		var text = "";
		for (var i = 0; i < options.length; i++) {
			text += options[i].text;
			if (control.codes) text += " (" + options[i].value + ")";	
			if (i < options.length - 1) text += ",";
		}
		// add a descrption if nothing yet
		if (!text) text = "Click to add...";
		// append the adjustable form control
		cell.text(text);
					
		// add a heading
		table.append("<tr><td><b>Text</b></td>" + (control.codes ? "<td colspan='2'><b>Code</b></td>" : "") + "</tr>");
		
		// show options
		for (var i in options) {
			// add the line
			table.append("<tr><td><input class='text' value='" + escapeApos(options[i].text) + "' /></td>" + (control.codes ? "<td><input class='value' value='" + escapeApos(options[i].value) + "' /></td>" : "") + "<td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
							
			// find the text
			var textEdit = table.find("input.text").last();
			// add a listener
			addListener( textEdit.keyup( {control : control, options: options}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update text
				ev.data.options[input.parent().parent().index()-1].text = input.val();
				// update html if top row
				if (input.parent().parent().index() == 1 || control.type != "dropdown") rebuildHtml(control);
			}));
			
			// find the code
			var valueEdit = table.find("input.value").last();
			// add a listener
			addListener( valueEdit.keyup( {options: options}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update value
				ev.data.options[input.parent().parent().index()-1].value = input.val();
			}));
					
		}
		
		// find the deletes
		var deleteImages = table.find("img.delete");
		// add a listener
		addListener( deleteImages.click( {options: options}, function(ev) {
			// get the del image
			var delImage = $(ev.target);
			// remove from parameters
			ev.data.options.splice(delImage.parent().parent().index()-1,1);
			// remove row
			delImage.parent().parent().remove();
			// update html if top row
			if (delImage.parent().index() == 1) rebuildHtml(control);
		}));
			
		// add reorder listeners
		addReorder(options, table.find("img.reorder"), function() { 
			// refresh the html and regenerate the mappings
			rebuildHtml(control);
			// refresh the property
			Property_options(cell, control, property, details); 
		});
			
		// have an add row
		table.append("<tr><td colspan='" + (control.codes ? "3" : "2") + "'><a href='#'>add...</a></td></tr>");
		// get a reference to the add
		var add = table.find("tr").last().children().last().children().last();
		// add a listener
		addListener( add.click( {cell: cell, control: control, property: property, details: details}, function(ev) {
			// add a blank option
			ev.data.control.options.push({value: "", text: ""});
			// refresh
			Property_options(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);		
		}));
		
		// check we don't have a checkbox already
		if (!dialogue.find("div.useCodes")[0]) {
			// add checkbox
			table.after("<div class='useCodes'>Use codes <input type='checkbox' " + (control.codes ? "checked='checked'" : "") + " /></div>");
			// get a reference
			var optionsCodes = dialogue.find("input[type=checkbox]");
			// add a listener
			addListener( optionsCodes.change( {cell: cell, control: control, options: options, property: property, details: details}, function(ev) {
				// get the value
				control.codes = ev.target.checked;
				// refresh
				Property_options(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);		
			}));		
		}
		
	} 
	
	// if this version has value lists
	if (_version.valueLists && _version.valueLists.length > 0) {
		// check we don't have a value list select already
		if (!dialogue.find("select")[0]) {
			// add the select
			dialogue.append("Value list <select><option value=''>None</option>" + getValueListsOptions(control.valueList) + "</select>");
			// get a reference
			var valueListSelect = dialogue.find("select");
			// add a listener
			addListener( valueListSelect.change( {cell: cell, control: control, property: property, details: details}, function(ev) {
				// get the value
				ev.data.control.valueList = $(ev.target).val();
				// refresh
				Property_options(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);
				// rebuild
				rebuildHtml(ev.data.control);
			}));				
		}
		dialogue.find("div.useCodes").css("margin-bottom","10px");
	}
	
}

// the different sort options
var _gridColumnSorts = {
		"" : { text: "none"},
		"t" : { text: "text"},
		"n" : { text: "number"},
		"d1" : { text: "date (dd/mm/yyyy)"},
		"d2" : { text: "date (dd-mon-yyyy)"},
		"d3" : { text: "date (mm/dd/yyyy)"},
		"d4" : { text: "date (yyyy-mm-dd)"},
		"c" : { text: "custom"}
}

// the sort function help text
var _gridSortFunctionHelpText = "// enter JavaScript here that returns a number to reflect\n// the order by comparing two objects, \"item1\" and \"item2\"\n// each has a \"value\" and \"index\" property";
// the cell function help text
var _gridCellFunctionHelpText = "// enter JavaScript here that can alter the contents when this\n// cell is populated. The value is available in the \"value\"\n// variable, and the cell in \"this\"";

// this is a dialogue to refine the options available in a grid control
function Property_gridColumns(cell, grid, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, grid, property, details, 650, "Columns",{sizeX: true, minWidth: 300});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	// append inputTableWithTitles class
	table.addClass("inputTableWithTitles");
	// get the grid columns
	var columns = grid.columns;
	// if they don't exist or an empty array string make them an empty array
	if (!columns || columns == "[]") columns = [];
	// make some text
	var text = "";
	for (var i = 0; i < columns.length; i++) {
		text += columns[i].title;
		if (i < columns.length - 1) text += ",";
	}
	if (!text) text = "Click to add...";
	// append the adjustable form control
	cell.text(text);
	
	// add a header
	table.append("<tr><td style='width:20px;'><b>Visible</b></td><td><b>Title</b></td><td><b>Title style</b></td><td><b>Field</b></td><td><b>Field style</b></td><td><b>Sort</b></td><td colspan='2'><b>Cell function</b></td></tr>");
		
	// show columns
	for (var i in columns) {
		
		// set the sort select (show the ellipses if custom)
		var sortSelect = "<select " + (columns[i].sort == "c" ? "style='width:60px;'" : "" ) + ">";
		// loop the values and add
		for (var j in _gridColumnSorts) {
			sortSelect += "<option value='" + j + "'"+ (columns[i].sort == j ? " selected='selected'" : "") + ">" + _gridColumnSorts[j].text + "</option>";
		}
		// close it
		sortSelect += "</select>";
			
		// set the cellFunction text to ellipses
		var cellFunctionText = "...";
		// update to function if present
		if (columns[i].cellFunction) cellFunctionText = columns[i].cellFunction;
		
		// add the line
		table.append("<tr><td class='center'><input type='checkbox' " + (columns[i].visible ? "checked='checked'" : "")  + " /></td><td><input value='" + escapeApos(columns[i].title) + "' /></td><td><input value='" + escapeApos(columns[i].titleStyle) + "' /></td><td><input value='" + escapeApos(columns[i].field) + "' /></td><td><input value='" + escapeApos(columns[i].fieldStyle) + "' /></td><td style='max-width:20px;'>" + sortSelect + "<span>...</span></td><td class='paddingLeft5' style='max-width:20px;'>" + cellFunctionText.replaceAll("<","&lt;") + "</td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		
		// find the checkbox
		var visibleEdit = table.find("tr").last().children(":nth(0)").first().children().first();
		// add a listener
		addListener( visibleEdit.change( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].visible = ev.target.checked;
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the title
		var titleEdit = table.find("tr").last().children(":nth(1)").first().children().first();
		// add a listener
		addListener( titleEdit.keyup( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].title = input.val();
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the titleStyle
		var titleStyleEdit = table.find("tr").last().children(":nth(2)").first().children().first();
		// add a listener
		addListener( titleStyleEdit.keyup( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].titleStyle = input.val();
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the field
		var fieldEdit = table.find("tr").last().children(":nth(3)").first().children().first();
		// add a listener
		addListener( fieldEdit.keyup( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].field = input.val();
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the fieldStyle
		var fieldStyleEdit = table.find("tr").last().children(":nth(4)").first().children().first();
		// add a listener
		addListener( fieldStyleEdit.keyup( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].fieldStyle = input.val();
			// refresh the html and regenerate the mappings
			rebuildHtml(ev.data.grid);
		}));
		
		// find the sort drop down
		var fieldStyleEdit = table.find("tr").last().children(":nth(5)").first().children().first();
		// add a listener
		addListener( fieldStyleEdit.change( {grid: grid, cell: cell, grid: grid, property: property, details: details}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].sort = input.val();
			// refresh the grid
			Property_gridColumns(ev.data.cell, ev.data.grid, ev.data.property, ev.data.details)
			// refresh the html 
			rebuildHtml(ev.data.grid);
		}));
		
		// find the sort custom function
		var fieldStyleEdit = table.find("tr").last().children(":nth(5)").first().children().last();
		// add a listener
		addListener( fieldStyleEdit.click( {grid: grid}, function(ev) {
			// get the span
			var span = $(ev.target);
			// get the index
			var index = span.parent().parent().index()-1;
			// set the index
			textArea.attr("data-index",index);
			// set the type
			textArea.attr("data-type","s");
			// get the function text
			var sortFunctionText = ev.data.grid.columns[index].sortFunction;
			// check the text
			if (sortFunctionText) {
				// show if exists
				textArea.val(sortFunctionText);
			} else {
				// show help if not
				textArea.val(_gridSortFunctionHelpText);
			}
			// show and focus the textarea
			textArea.show().focus();
		}));
		
		// find the cellFunction
		var fieldStyleEdit = table.find("tr").last().children(":nth(6)").first();
		// add a listener
		addListener( fieldStyleEdit.click( {grid: grid}, function(ev) {
			// get the td
			var td = $(ev.target);
			// get the index
			var index = td.parent().index()-1;
			// set the index
			textArea.attr("data-index",index);
			// set the type
			textArea.attr("data-type","f");
			// get the function text
			var cellFunctionText = ev.data.grid.columns[index].cellFunction;
			// check the text
			if (cellFunctionText) {
				textArea.val(cellFunctionText);
			} else {
				textArea.val(_gridCellFunctionHelpText);
			}
			// show and focus the textarea
			textArea.show().focus();
		}));
						
	}
	
	// add the cell function text area
	var textArea = dialogue.append("<textarea data-index='-1' style='position:absolute;display:none;width:500px;height:300px;top:26px;right:10px;' wrap='off'></textarea>").find("textarea:first");
	// hide it on unfocus
	addListener( textArea.blur( function(ev) {		
		// get the value
		var value = textArea.val();
		// update to elipses if nothing or only help text
		if (!value || value == _gridCellFunctionHelpText) value = "...";
		// get the index
		var index = textArea.attr("data-index")*1;		
		// get the type
		var type = textArea.attr("data-type");	
		// update the td text if known cell function
		if (index >= 0 && type == "f") table.find("tr:nth(" + (index + 1) + ")").last().children("td:nth(6)").html(value);
		// empty the value
		textArea.val("");
		// hide it
		textArea.hide();		
	}));
	
	// update the applicable property on text area key up
	addListener( textArea.keyup( {grid: grid}, function(ev) {
		// get the value
		var value = textArea.val();
		// get the index
		var index = textArea.attr("data-index")*1;
		// get the type
		var type = textArea.attr("data-type");
		// update the object value
		if (index >= 0) {			
			// update the custom sort
			if (type == "s") {
				// check the value different from help
				if (value && value != _gridSortFunctionHelpText) {
					ev.data.grid.columns[index].sortFunction = textArea.val();
				} else {
					ev.data.grid.columns[index].sortFunction = null;
				}				
			}
			// update the cell function
			if (type == "f") {
				// check value different from help
				if (value && value != _gridCellFunctionHelpText) {
					ev.data.grid.columns[index].cellFunction = value;
				} else {
					ev.data.grid.columns[index].cellFunction = null;
				}
			}
		}
	}));
	
	// add delete listeners
	var deleteImages = table.find("img.delete");
	// add a listener
	addListener( deleteImages.click( {columns: columns}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.columns.splice(input.parent().parent().index()-1,1);
		// remove row
		input.parent().parent().remove();
		// refresh the html and regenerate the mappings
		rebuildHtml(grid);
	}));
	
	// add reorder listeners
	addReorder(columns, table.find("img.reorder"), function() { 
		// refresh the html and regenerate the mappings
		rebuildHtml(grid);
		// refresh the property
		Property_gridColumns(cell, grid, property, details); 
	});
	
	// have an add row
	table.append("<tr><td colspan='5'><a href='#'>add...</a></td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.click( {cell: cell, grid: grid, property: property, details: details}, function(ev) {
		// add a blank option
		ev.data.grid.columns.push({visible: true, title: "", titleStyle: "", field: "", fieldStyle: "", cellFunction: ""});
		// refresh
		Property_gridColumns(ev.data.cell, ev.data.grid, ev.data.property, ev.data.details);		
	}));

}

// the grid scroll width property, only appears if horizontal scrolling is on
function Property_gridScrollWidth(cell, grid, property, details) {
	// only if horizontal scrolling
	if (grid.scrollH) {
		// add the bigtext
		Property_text(cell, grid, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

//the grid scroll width property, only appears if horizontal scrolling is on
function Property_gridScrollHeight(cell, grid, property, details) {
	// only if vertical scrolling
	if (grid.scrollV) {
		// add the bigtext
		Property_text(cell, grid, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}	
}

//the grid scroll width property, only appears if horizontal scrolling is on
function Property_gridScrollFixedHeader(cell, grid, property, details) {
	// only if vertical scrolling
	if (grid.scrollV) {
		// add the bigtext
		Property_checkbox(cell, grid, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}	
}

// this is a dialogue to choose controls and specify their hints
function Property_controlHints(cell, hints, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, hints, property, details, 500, "Control hints", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	var text = "";
	// get the hint controls
	var controlHints = hints.controlHints;
	// if they don't exist or an empty array string make them an empty array
	if (!controlHints || controlHints == "[]") controlHints = [];
	
	// add a header
	table.append("<tr><td><b>Control</b></td><td><b>Action</b></td><td style='min-width:150px;max-width:150px;'><b>Hint text</b></td><td colspan='2'><b>Style</b></td></td></tr>");
		
	// loop the controls
	for (var i in controlHints) {
		
		// find the control hint
		var controlHint = controlHints[i];
		
		// find the control
		var control = getControlById(controlHint.controlId);

		// append the control name to the hints text
		if (control) text += control.name;
		
		// create the type options
		var typeOptions = "<option value='hover'" + ((controlHint.type == 'hover') ? " selected": "") + ">hover</option><option value='click'" + ((controlHint.type == 'click') ? " selected": "") + ">click</option>";
		
		// add the row
		table.append("<tr class='nopadding'><td><select class='control'><option value=''>Please select...</option>" + getControlOptions(controlHint.controlId) + "</select></td><td><select class='type'>" + typeOptions + "</select></td><td style='max-width:150px;'><span>" + controlHint.text + "</span></td><td><input value='" + escapeApos(controlHint.style) + "'/></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
	
		// add a seperating comma to the text if not the last hint
		if (i < controlHints.length - 1) text += ",";
					
	}
	
	// if the hints text is empty
	if (!text) text = "Click to add...";
	// append the text into the hints property
	cell.text(text);
	
	// add control listeners
	var controlSelects = table.find("select.control");
	// add a listener
	addListener( controlSelects.change( {controlHints: controlHints}, function(ev) {
		// get the select
		var select = $(ev.target);
		// update the control id
		ev.data.controlHints[select.parent().parent().index()-1].controlId = select.val();
	}));
	
	// add type listeners
	var typeSelects = table.find("select.type");
	// add a listener
	addListener( typeSelects.change( {controlHints: controlHints}, function(ev) {
		// get the select
		var select = $(ev.target);
		// update the control id
		ev.data.controlHints[select.parent().parent().index()-1].type = select.val();
	}));
	
	// add text listeners
	var texts = table.find("span");
	// loop them
	texts.each(function() {		
		// get a reference to the span
		var span = $(this);
		//function Property_bigtext(cell, propertyObject, property, refreshHtml) {
		Property_bigtext(span.parent(), controlHints[span.parent().parent().index()-1], {key: "text"});		
	});
	
	// add style listeners
	var styles = table.find("input");
	// add a listener
	addListener( styles.change( {controlHints: controlHints}, function(ev) {
		// get the input
		var input = $(ev.target);
		// update the control id
		ev.data.controlHints[input.parent().parent().index()-1].style = input.val();
	}));
				
	// add delete listeners
	var deleteImages = table.find("img.delete");
	// add a listener
	addListener( deleteImages.click( {controlHints: controlHints}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.controlHints.splice(input.parent().parent().index()-1,1);
		// remove row
		input.parent().parent().remove();
		// refresh the html and regenerate the mappings
		rebuildHtml(controlHints);
	}));
	
	// add reorder listeners
	addReorder(controlHints, table.find("img.reorder"), function() { 
		// refresh the html and regenerate the mappings
		rebuildHtml(hints);
		// refresh the property
		Property_controlHints(cell, hints, property, details); 
	});
		
	// have an add row
	table.append("<tr><td colspan='4'><a href='#'>add...</a></td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.click( {cell: cell, hints: hints, property: property, details: details}, function(ev) {
		// instantiate array if need be
		if (!ev.data.hints.controlHints) ev.data.hints.controlHints = [];
		// add a blank hint
		ev.data.hints.controlHints.push({controlId: "", type: "hover", text: "", style: ""});
		// refresh
		Property_controlHints(ev.data.cell, ev.data.hints, ev.data.property, ev.data.details);		
	}));

}

function Property_slidePanelVisibility(cell, propertyObject, property, details) {
	// if we're holding a P (this defaulted in designerer.js)
	cell.text(propertyObject.visible);
	// add the listener to the cell
	addListener( cell.click( function(ev) {
		// add an undo snapshot
		addUndo();
		// get a reference to the slidePanel
		var slidePanel = propertyObject;
		// toggle the value
		slidePanel.visible = !slidePanel.visible;		
		// add/remove classes
		if (slidePanel.visible) {
			slidePanel.object.addClass("slidePanelOpen");
			slidePanel.object.removeClass("slidePanelClosed");
		} else {
			slidePanel.object.addClass("slidePanelClosed");
			slidePanel.object.removeClass("slidePanelOpen");
		}
		// refresh the html
		rebuildHtml(propertyObject);
		// update text
		$(ev.target).text(slidePanel.visible);
	}));
}


function Property_flowLayoutCellWidth(cell, flowLayout, property, details) {
	var value = "";
	// set the value if it exists
	if (flowLayout[property.key]) value = flowLayout[property.key];
	// append the adjustable form control
	cell.append("<input value='" + escapeApos(value) + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) {
		// update the property
		updateProperty(cell, flowLayout, property, details, ev.target.value);
		// update the iFrame control details
		var pageWindow =  _pageIframe[0].contentWindow || _pageIframe[0];
		// add the design-time details object into the page
		pageWindow[flowLayout.id + "details"] = {};
		// set the cell width
		pageWindow[flowLayout.id + "details"].cellWidth = ev.target.value;
		// iframe resize
		_pageIframe.resize();		
	}));
}

function Property_datacopySource(cell, datacopyAction, property, details) {
	// only if datacopyAction type is not bulk
	if (datacopyAction.copyType == "bulk") {
		// remove this row
		cell.closest("tr").remove();		
	} else {
		// show the source drop down		
		Property_select(cell, datacopyAction, property, details);
	}
}

function Property_datacopySourceField(cell, datacopyAction, property, details) {
	// only if datacopyAction type is not bulk
	if (datacopyAction.copyType == "bulk") {
		// remove this row
		cell.closest("tr").remove();
	} else {
		// show the source field text		
		Property_text(cell, datacopyAction, property, details);
	}
}

function Property_datacopyChildField(cell, datacopyAction, property, details) {
	// only if datacopyAction type is child
	if (datacopyAction.copyType == "child") {
		// show the duration
		Property_text(cell, datacopyAction, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_datacopySearchField(cell, datacopyAction, property, details) {
	// only if datacopyAction is search
	if (datacopyAction.copyType == "search") {
		// show the duration
		Property_text(cell, datacopyAction, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_datacopySearchSource(cell, datacopyAction, property, details) {
	// only if datacopyAction is search
	if (datacopyAction.copyType == "search") {
		// show the duration
		Property_select(cell, datacopyAction, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_datacopyMaxRows(cell, datacopyAction, property, details) {
	// only if datacopyAction is search
	if (datacopyAction.copyType == "search") {
		// show the duration
		Property_integer(cell, datacopyAction, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_datacopyFields(cell, datacopyAction, property, details) {
	// only if datacopyAction is search
	if (datacopyAction.copyType == "trans") {
		// show the reusable  fields dialigue
		Property_fields(cell, datacopyAction, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_datacopyDestinations(cell, propertyObject, property, details) {
	
	// only if datacopyAction type is not bulk
	if (propertyObject.copyType == "bulk") {
		// remove this row
		cell.closest("tr").remove();
	} else {
		// retrieve or create the dialogue
		var dialogue = getDialogue(cell, propertyObject, property, details, 300, "Destinations", {sizeX: true});		
		// grab a reference to the table
		var table = dialogue.find("table").first();
		// make sure table is empty
		table.children().remove();
		
		// build what we show in the parent cell
		var dataDestinations = [];
		// get the value if it exists
		if (propertyObject[property.key]) dataDestinations = propertyObject[property.key];	
		// make some text for our cell (we're going to build in in the loop)
		var text = "";
		
		// add a header
		table.append("<tr><td><b>Control</b></td><td colspan='2'><b>Field</b></td></tr>");
			
		// show current choices (with delete and move)
		for (var i = 0; i < dataDestinations.length; i++) {
			// get a single reference
			var dataDestination = dataDestinations[i];	
			// if we got one
			if (dataDestination) {
				// get a data item object for this
				var dataItem = getDataItemDetails(dataDestination.itemId);
				// apend to the text
				text += dataItem.name + ",";
				// add a row
				table.append("<tr><td>" + dataItem.name + "</td><td><input value='" + dataDestination.field + "' /></td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
				// get the field
				var editField = table.find("tr").last().children("td:nth(1)").children("input");
				// add a listener
				addListener( editField.keyup( {dataDestinations: dataDestinations}, function(ev) {
					// get the input
					var editField = $(ev.target);
					// update the field
					ev.data.dataDestinations[editField.parent().parent().index()-1].field = editField.val();
				}));
				// get the delete image
				var imgDelete = table.find("tr").last().children().last().children("img.delete");
				// add a listener
				addListener( imgDelete.click( {dataDestinations: dataDestinations}, function(ev) {
					// get the input
					var imgDelete = $(ev.target);
					// remove from parameters
					ev.data.dataDestinations.splice(imgDelete.parent().parent().index()-1,1);
					// remove row
					imgDelete.parent().parent().remove();
				}));
			} else {
				// remove this entry from the collection
				dataDestinations.splice(i,1);
				// set i back 1 position
				i--;
			}			
		}
				
		// add reorder listeners
		addReorder(dataDestinations, table.find("img.reorder"), function() { 
			Property_datacopyDestinations(cell, propertyObject, property, details); 
		});
		
		// add the add
		table.append("<tr><td colspan='3' style='padding:0px;'><select style='margin:0px'><option value=''>Add destination...</option>" + getOutputOptions() + "</select></td></tr>");
		// find the add
		var destinationAdd = table.find("tr").last().children().last().children().last();
		// listener to add output
		addListener( destinationAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
			
			// initialise array if need be
			if (!ev.data.propertyObject[ev.data.property.key]) ev.data.propertyObject[ev.data.property.key] = [];
			// get the parameters (inputs or outputs)
			var dataDestinations = ev.data.propertyObject[ev.data.property.key];
			// add a new one
			dataDestinations.push({itemId: $(ev.target).val(), field: ""});
			// rebuild the dialogue
			Property_datacopyDestinations(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);	
		}));
		
		// if we got text 
		if (text) {
			// remove the trailing comma
			text = text.substring(0,text.length - 1);
		} else {
			// add friendly message
			text = "Click to add...";
		}
		// put the text into the cell
		cell.text(text);
	}
	
}

var _dataCopyTypes = [[false,"replace"],["append","append"],["row","row merge"]];

function getCopyTypeOptions(type) {
	var options = "";
	for (var i in _dataCopyTypes) {
		options += "<option value='" + _dataCopyTypes[i][0] + "'" + (type == _dataCopyTypes[i][0] ? " selected='selected'" : "") + ">" + _dataCopyTypes[i][1] + "</option>";
	}
	return options;
}

// this function returns the position of a key in datacopies
function getKeyIndexBulkCopies(dataCopies, key, input) {
	for (var i in dataCopies) {
		if ((input && dataCopies[i].source == key) || (!input && dataCopies[i].destination == key)) return i*1;
	}
	return -1;
}

// this function returns the position of a key in the page controls 
function getKeyIndexControls(controls, key) {
	// get the control id (properties will have some stuff after the .)
	key = key.split("/.")[0];
	// loop all the controls
	for (var i in controls) {
		// return position on match
		if (controls[i].id == key) return i*1;
	}
	return -1;
}

// this function ammends the dataCopies collection to have all get or set data controls depending on whether input is true or false
function getPageControlsBulkCopies(datacopyAction, input) {
	
	// create array if need be
	if (!datacopyAction.dataCopies) datacopyAction.dataCopies = [];
	// retain a reference to it
	var dataCopies = datacopyAction.dataCopies;	
	// get all controls
	var controls = getControls();
	// store bulk copy inserts as we discover them
	var bulkCopyInserts = [];
	// loop them
	for (var i in controls) {
		// get the control
		var control = controls[i];
		// we'll set the key if we find the get/set data method we need
		var key = null;
		// get the control class
		var controlClass = _controlTypes[control.type];
		// if we got one  and the control is named
		if (controlClass && control.name) {			
			// if there is a getdata method
			if ((input && controlClass.getDataFunction) || (!input && controlClass.setDataJavaScript)) {
				// set the key to the control id
				key = control.id;
			} else {
				// get any run time properties
				var properties = controlClass.runtimeProperties;
				// if there are runtimeProperties in the class
				if (properties) {
					// promote if array
					if ($.isArray(properties.runtimeProperty)) properties = properties.runtimeProperty;
					// loop them
					for (var i in properties) {
						// get the property
						var property = properties[i];
						// if we want inputs and there's is a get function, or outputs and there's set javascript
						if ((input && property.getPropertyFunction) || (!input && property.setPropertyJavaScript)) {
							// use this as the key
							key = control.id + "." + property.type;
							// we only want the first one
							break;
						} // property check
						
					} // properties loop
					
				} // properties check
				
			} // get / set check

		} // control class check
		
		// if there's a key it should be in dataCopies
		if (key) {
			// get it's position
			var index = getKeyIndexBulkCopies(dataCopies, key, input);
			// if it's not there
			if (index < 0) {
				// make a new object for it
				var dataCopy = {source: null, sourceField: "", destination: null, destinationField: ""};
				// if for source / destination
				if (input) {
					// set the source
					dataCopy.source = key;					
					// also most likely to be a row merge
					dataCopy.type = "row";
				} else {
					// set the destination
					dataCopy.destination = key;
				}				
				// rememeber that we don't have this control
				bulkCopyInserts.push(dataCopy);
			} // index check
			
		} // key check
		
	} // controls loop
	
	// now loop the inserts finding where they should go
	for (var i in bulkCopyInserts) {
		// get the copy to insert
		var copy = bulkCopyInserts[i];
		// set an added property
		copy.added = true;
		// assume no copy above
		var copyAbove = null;
		// get the key
		var key = copy.source || copy.destination;		
		// get the control postion
		var controlPos = getKeyIndexControls(controls, key);
		// assume we haven't inserted it
		var inserted = false;
		// now loop the existing bulkCopies
		for (var j in dataCopies) {
			// get the existing position 
			var existingPos = getKeyIndexControls(controls, input ? dataCopies[j].source : dataCopies[j].destination);
			// if the existing pos is after the insert control position
			if (existingPos > controlPos) {
				// set the copyAbove
				copyAbove = dataCopies[j];				
				// insert here
				dataCopies.splice(j, 0, copy);
				// retain insert
				inserted = true;
				// we're done
				break;
			} // found a control after this one so insert before the found one
		} // loop dataCopies
		// if we haven't inserted yet do so now
		if (!inserted) {
			// check there is an existing data copy above and set if so
			if (dataCopies.length > 0) copyAbove = dataCopies[dataCopies.length - 1];							
			// insert it now
			dataCopies.push(copy);
		}
		// if there was a copy above
		if (copyAbove) {
			// check if source / destination
			if (input) {
				// if the data copy we've just moved past has a destination field, we can assume this copy will have the same destination
				if (copyAbove.destinationField || (copyAbove.destination && copyAbove.added)) copy.destination = copyAbove.destination;
			} else {
				// if the data copy we've just moved past has a source field, we can assume this copy will have the same source
				if (copyAbove.sourceField || (copyAbove.source && copyAbove.added)) copy.source = copyAbove.source;
			}
		}		
	} // loop inserts
	
}

function Property_datacopyCopies(cell, datacopyAction, property, details) {

	// only if datacopyAction type is bulk
	if (datacopyAction.copyType == "bulk") {	
		
		// retrieve or create the dialogue
		var dialogue = getDialogue(cell, datacopyAction, property, details, 700, "Bulk data copies", {sizeX: true});		
		// grab a reference to the table
		var table = dialogue.find("table").first();
		// make sure table is empty
		table.children().remove();
		
		// build what we show in the parent cell
		var dataCopies = [];
		// get the value if it exists
		if (datacopyAction[property.key]) dataCopies = datacopyAction[property.key];	
		// make some text for our cell (we're going to build in in the loop)
		var text = "";
		
		// add a header
		table.append("<tr><td><b>Source</b><button class='titleButton sources' title='Add all page controls as sources'><span>&#xf055;</span></button></td><td><b>Source field</b></td><td><b>Destination</b><button class='titleButton destinations' title='Add all page controls as destinations'><span>&#xf055;</span></button></td><td><b>Destination field</b></td><td colspan='2'><b>Copy type</b></td></tr>");
			
		// add sources listener
		addListener( table.find("button.sources").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {
			// add an undo snapshot
			addUndo();
			// bring in all source controls
			getPageControlsBulkCopies(ev.data.datacopyAction, true);
			// refresh
			Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
		}));	
		
		// add destinations listener
		addListener( table.find("button.destinations").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {
			// add an undo snapshot
			addUndo();
			// bring in all destination controls
			getPageControlsBulkCopies(ev.data.datacopyAction, false);
			// refresh
			Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
		}));	
		
		// show current choices (with delete and move)
		for (var i = 0; i < dataCopies.length; i++) {
			
			// get this data copy
			var dataCopy = dataCopies[i];
						
			// add a row
			table.append("<tr><td><select class='source'><option value=''>Please select...</option>" + getInputOptions(dataCopy.source) + "</select></td><td><input  class='source' value='" + escapeApos(dataCopy.sourceField) + "' /></td><td><select class='destination'><option value=''>Please select...</option>" + getOutputOptions(dataCopy.destination) + "</select></td><td><input class='destination' value='" + escapeApos(dataCopy.destinationField) + "' /></td><td><select class='type' style='min-width:60px;'>" + getCopyTypeOptions(dataCopy.type) + "</select></td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
			
			// get the source data item
			var source = getDataItemDetails(dataCopy.source);
			// get the destination data item
			var destination = getDataItemDetails(dataCopy.destination);
			// apend to the text
			text += source.name + " to " + destination.name + ",";
			
		}
		
		// source listeners
		addListener( table.find("select.source").change( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].source = target.val();
		}));		
		// source field listeners
		addListener( table.find("input.source").keyup( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].sourceField = target.val();
		}));
		// destination listeners
		addListener( table.find("select.destination").change( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].destination = target.val();
		}));
		// destination field listeners
		addListener( table.find("input.destination").keyup( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].destinationField = target.val();
		}));
		// source listeners
		addListener( table.find("select.type").change( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].type = target.val();
		}));
		
		// get the delete images
		var imgDelete = table.find("img.delete");
		// add a listener
		addListener( imgDelete.click( {dataCopies: dataCopies}, function(ev) {
			// get the input
			var imgDelete = $(ev.target);
			// remove from parameters
			ev.data.dataCopies.splice(imgDelete.parent().parent().index()-1,1);
			// remove row
			imgDelete.parent().parent().remove();
		}));
			
		// add reorder listeners
		addReorder(dataCopies, table.find("img.reorder"), function() { 
			Property_datacopyCopies(cell, datacopyAction, property); 
		});
		
		// add the add
		table.append("<tr><td colspan='8'><a href='#' style='margin-left:5px;'>add...</a></td></tr>");
		// find the add
		var destinationAdd = table.find("a").last();
		// listener to add output
		addListener( destinationAdd.click( {cell: cell, datacopyAction: datacopyAction, property: property, details: details}, function(ev) {
			// initialise array if need be
			if (!ev.data.datacopyAction.dataCopies) ev.data.datacopyAction.dataCopies = [];
			// get the parameters (inputs or outputs)
			var dataCopies = ev.data.datacopyAction.dataCopies;
			// add a new one
			dataCopies.push({source:"",sourceField:"",destination:"",destinationField:""});
			// rebuild the dialogue
			Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property, ev.data.details);	
		}));
		
		// if we got text 
		if (text) {
			// remove the trailing comma
			text = text.substring(0,text.length - 1);
		} else {
			// add friendly message
			text = "Click to add...";
		}
		// put the text into the cell
		cell.text(text);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}

}

function Property_controlActionType(cell, controlAction, property, details) {
	// if this property has not been set yet
	if (!controlAction.actionType) {
		// update to custom if there is a command property (this is for backwards compatibility)
		if (controlAction.command && controlAction.command != "// Enter JQuery command here. The event object is passed in as \"ev\"") {
			controlAction.actionType = "custom";
		} else {
			controlAction.actionType = "hide";
		}
	}
	// build the select
	Property_select(cell, controlAction, property);
}

function Property_controlActionDuration(cell, controlAction, property, details) {
	// only if controlAction is slide or fade
	if (controlAction.actionType.indexOf("slide") == 0 || controlAction.actionType.indexOf("fade") == 0) {
		// show the duration
		Property_integer(cell, controlAction, property);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_controlActionClasses(cell, controlAction, property, details) {
	// only if controlAction is custom
	if (controlAction.actionType == "addClass" || controlAction.actionType == "removeClass" || controlAction.actionType == "toggleClass" || controlAction.actionType == "removeChildClasses") {
		// add thegtext
		Property_select(cell, controlAction, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_controlActionCommand(cell, controlAction, property, details) {
	// only if controlAction is custom
	if (controlAction.actionType == "custom") {
		// add the bigtext
		Property_bigtext(cell, controlAction, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

var _fontawesomeGlyphs = [["","none"],["&#xf042;","adjust"],["&#xf170;","adn"],["&#xf037;","align-center"],["&#xf039;","align-justify"],["&#xf036;","align-left"],["&#xf038;","align-right"],["&#xf0f9;","ambulance"],["&#xf13d;","anchor"],["&#xf17b;","android"],["&#xf209;","angellist"],["&#xf103;","angle-double-down"],["&#xf100;","angle-double-left"],["&#xf101;","angle-double-right"],["&#xf102;","angle-double-up"],["&#xf107;","angle-down"],["&#xf104;","angle-left"],["&#xf105;","angle-right"],["&#xf106;","angle-up"],["&#xf179;","apple"],["&#xf187;","archive"],["&#xf1fe;","area-chart"],["&#xf0ab;","arrow-circle-down"],["&#xf0a8;","arrow-circle-left"],["&#xf01a;","arrow-circle-o-down"],["&#xf190;","arrow-circle-o-left"],["&#xf18e;","arrow-circle-o-right"],["&#xf01b;","arrow-circle-o-up"],["&#xf0a9;","arrow-circle-right"],["&#xf0aa;","arrow-circle-up"],["&#xf063;","arrow-down"],["&#xf060;","arrow-left"],["&#xf061;","arrow-right"],["&#xf047;","arrows"],["&#xf0b2;","arrows-alt"],["&#xf07e;","arrows-h"],["&#xf07d;","arrows-v"],["&#xf062;","arrow-up"],["&#xf069;","asterisk"],["&#xf1fa;","at"],["&#xf04a;","backward"],["&#xf05e;","ban"],["&#xf080;","bar-chart"],["&#xf02a;","barcode"],["&#xf0c9;","bars"],["&#xf0fc;","beer"],["&#xf1b4;","behance"],["&#xf1b5;","behance-square"],["&#xf0f3;","bell"],["&#xf0a2;","bell-o"],["&#xf1f6;","bell-slash"],["&#xf1f7;","bell-slash-o"],["&#xf206;","bicycle"],["&#xf1e5;","binoculars"],["&#xf1fd;","birthday-cake"],["&#xf171;","bitbucket"],["&#xf172;","bitbucket-square"],["&#xf032;","bold"],["&#xf0e7;","bolt"],["&#xf1e2;","bomb"],["&#xf02d;","book"],["&#xf02e;","bookmark"],["&#xf097;","bookmark-o"],["&#xf0b1;","briefcase"],["&#xf15a;","btc"],["&#xf188;","bug"],["&#xf1ad;","building"],["&#xf0f7;","building-o"],["&#xf0a1;","bullhorn"],["&#xf140;","bullseye"],["&#xf207;","bus"],["&#xf1ec;","calculator"],["&#xf073;","calendar"],["&#xf133;","calendar-o"],["&#xf030;","camera"],["&#xf083;","camera-retro"],["&#xf1b9;","car"],["&#xf0d7;","caret-down"],["&#xf0d9;","caret-left"],["&#xf0da;","caret-right"],["&#xf150;","caret-square-o-down"],["&#xf191;","caret-square-o-left"],["&#xf152;","caret-square-o-right"],["&#xf151;","caret-square-o-up"],["&#xf0d8;","caret-up"],["&#xf20a;","cc"],["&#xf1f3;","cc-amex"],["&#xf1f2;","cc-discover"],["&#xf1f1;","cc-mastercard"],["&#xf1f4;","cc-paypal"],["&#xf1f5;","cc-stripe"],["&#xf1f0;","cc-visa"],["&#xf0a3;","certificate"],["&#xf127;","chain-broken"],["&#xf00c;","check"],["&#xf058;","check-circle"],["&#xf05d;","check-circle-o"],["&#xf14a;","check-square"],["&#xf046;","check-square-o"],["&#xf13a;","chevron-circle-down"],["&#xf137;","chevron-circle-left"],["&#xf138;","chevron-circle-right"],["&#xf139;","chevron-circle-up"],["&#xf078;","chevron-down"],["&#xf053;","chevron-left"],["&#xf054;","chevron-right"],["&#xf077;","chevron-up"],["&#xf1ae;","child"],["&#xf111;","circle"],["&#xf10c;","circle-o"],["&#xf1ce;","circle-o-notch"],["&#xf1db;","circle-thin"],["&#xf0ea;","clipboard"],["&#xf017;","clock-o"],["&#xf0c2;","cloud"],["&#xf0ed;","cloud-download"],["&#xf0ee;","cloud-upload"],["&#xf121;","code"],["&#xf126;","code-fork"],["&#xf1cb;","codepen"],["&#xf0f4;","coffee"],["&#xf013;","cog"],["&#xf085;","cogs"],["&#xf0db;","columns"],["&#xf075;","comment"],["&#xf0e5;","comment-o"],["&#xf086;","comments"],["&#xf0e6;","comments-o"],["&#xf14e;","compass"],["&#xf066;","compress"],["&#xf1f9;","copyright"],["&#xf09d;","credit-card"],["&#xf125;","crop"],["&#xf05b;","crosshairs"],["&#xf13c;","css3"],["&#xf1b2;","cube"],["&#xf1b3;","cubes"],["&#xf0f5;","cutlery"],["&#xf1c0;","database"],["&#xf1a5;","delicious"],["&#xf108;","desktop"],["&#xf1bd;","deviantart"],["&#xf1a6;","digg"],["&#xf192;","dot-circle-o"],["&#xf019;","download"],["&#xf17d;","dribbble"],["&#xf16b;","dropbox"],["&#xf1a9;","drupal"],["&#xf052;","eject"],["&#xf141;","ellipsis-h"],["&#xf142;","ellipsis-v"],["&#xf1d1;","empire"],["&#xf0e0;","envelope"],["&#xf003;","envelope-o"],["&#xf199;","envelope-square"],["&#xf12d;","eraser"],["&#xf153;","eur"],["&#xf0ec;","exchange"],["&#xf12a;","exclamation"],["&#xf06a;","exclamation-circle"],["&#xf071;","exclamation-triangle"],["&#xf065;","expand"],["&#xf08e;","external-link"],["&#xf14c;","external-link-square"],["&#xf06e;","eye"],["&#xf1fb;","eyedropper"],["&#xf070;","eye-slash"],["&#xf09a;","facebook"],["&#xf082;","facebook-square"],["&#xf049;","fast-backward"],["&#xf050;","fast-forward"],["&#xf1ac;","fax"],["&#xf182;","female"],["&#xf0fb;","fighter-jet"],["&#xf15b;","file"],["&#xf1c6;","file-archive-o"],["&#xf1c7;","file-audio-o"],["&#xf1c9;","file-code-o"],["&#xf1c3;","file-excel-o"],["&#xf1c5;","file-image-o"],["&#xf016;","file-o"],["&#xf1c1;","file-pdf-o"],["&#xf1c4;","file-powerpoint-o"],["&#xf0c5;","files-o"],["&#xf15c;","file-text"],["&#xf0f6;","file-text-o"],["&#xf1c8;","file-video-o"],["&#xf1c2;","file-word-o"],["&#xf008;","film"],["&#xf0b0;","filter"],["&#xf06d;","fire"],["&#xf134;","fire-extinguisher"],["&#xf024;","flag"],["&#xf11e;","flag-checkered"],["&#xf11d;","flag-o"],["&#xf0c3;","flask"],["&#xf16e;","flickr"],["&#xf0c7;","floppy-o"],["&#xf07b;","folder"],["&#xf114;","folder-o"],["&#xf07c;","folder-open"],["&#xf115;","folder-open-o"],["&#xf031;","font"],["&#xf04e;","forward"],["&#xf180;","foursquare"],["&#xf119;","frown-o"],["&#xf1e3;","futbol-o"],["&#xf11b;","gamepad"],["&#xf0e3;","gavel"],["&#xf154;","gbp"],["&#xf06b;","gift"],["&#xf1d3;","git"],["&#xf09b;","github"],["&#xf113;","github-alt"],["&#xf092;","github-square"],["&#xf1d2;","git-square"],["&#xf184;","gittip"],["&#xf000;","glass"],["&#xf0ac;","globe"],["&#xf1a0;","google"],["&#xf0d5;","google-plus"],["&#xf0d4;","google-plus-square"],["&#xf1ee;","google-wallet"],["&#xf19d;","graduation-cap"],["&#xf1d4;","hacker-news"],["&#xf0a7;","hand-o-down"],["&#xf0a5;","hand-o-left"],["&#xf0a4;","hand-o-right"],["&#xf0a6;","hand-o-up"],["&#xf0a0;","hdd-o"],["&#xf1dc;","header"],["&#xf025;","headphones"],["&#xf004;","heart"],["&#xf08a;","heart-o"],["&#xf1da;","history"],["&#xf015;","home"],["&#xf0f8;","hospital-o"],["&#xf0fd;","h-square"],["&#xf13b;","html5"],["&#xf20b;","ils"],["&#xf01c;","inbox"],["&#xf03c;","indent"],["&#xf129;","info"],["&#xf05a;","info-circle"],["&#xf156;","inr"],["&#xf16d;","instagram"],["&#xf208;","ioxhost"],["&#xf033;","italic"],["&#xf1aa;","joomla"],["&#xf157;","jpy"],["&#xf1cc;","jsfiddle"],["&#xf084;","key"],["&#xf11c;","keyboard-o"],["&#xf159;","krw"],["&#xf1ab;","language"],["&#xf109;","laptop"],["&#xf202;","lastfm"],["&#xf203;","lastfm-square"],["&#xf06c;","leaf"],["&#xf094;","lemon-o"],["&#xf149;","level-down"],["&#xf148;","level-up"],["&#xf1cd;","life-ring"],["&#xf0eb;","lightbulb-o"],["&#xf201;","line-chart"],["&#xf0c1;","link"],["&#xf0e1;","linkedin"],["&#xf08c;","linkedin-square"],["&#xf17c;","linux"],["&#xf03a;","list"],["&#xf022;","list-alt"],["&#xf0cb;","list-ol"],["&#xf0ca;","list-ul"],["&#xf124;","location-arrow"],["&#xf023;","lock"],["&#xf175;","long-arrow-down"],["&#xf177;","long-arrow-left"],["&#xf178;","long-arrow-right"],["&#xf176;","long-arrow-up"],["&#xf0d0;","magic"],["&#xf076;","magnet"],["&#xf183;","male"],["&#xf041;","map-marker"],["&#xf136;","maxcdn"],["&#xf20c;","meanpath"],["&#xf0fa;","medkit"],["&#xf11a;","meh-o"],["&#xf130;","microphone"],["&#xf131;","microphone-slash"],["&#xf068;","minus"],["&#xf056;","minus-circle"],["&#xf146;","minus-square"],["&#xf147;","minus-square-o"],["&#xf10b;","mobile"],["&#xf0d6;","money"],["&#xf186;","moon-o"],["&#xf001;","music"],["&#xf1ea;","newspaper-o"],["&#xf19b;","openid"],["&#xf03b;","outdent"],["&#xf18c;","pagelines"],["&#xf1fc;","paint-brush"],["&#xf0c6;","paperclip"],["&#xf1d8;","paper-plane"],["&#xf1d9;","paper-plane-o"],["&#xf1dd;","paragraph"],["&#xf04c;","pause"],["&#xf1b0;","paw"],["&#xf1ed;","paypal"],["&#xf040;","pencil"],["&#xf14b;","pencil-square"],["&#xf044;","pencil-square-o"],["&#xf095;","phone"],["&#xf098;","phone-square"],["&#xf03e;","picture-o"],["&#xf200;","pie-chart"],["&#xf1a7;","pied-piper"],["&#xf1a8;","pied-piper-alt"],["&#xf0d2;","pinterest"],["&#xf0d3;","pinterest-square"],["&#xf072;","plane"],["&#xf04b;","play"],["&#xf144;","play-circle"],["&#xf01d;","play-circle-o"],["&#xf1e6;","plug"],["&#xf067;","plus"],["&#xf055;","plus-circle"],["&#xf0fe;","plus-square"],["&#xf196;","plus-square-o"],["&#xf011;","power-off"],["&#xf02f;","print"],["&#xf12e;","puzzle-piece"],["&#xf1d6;","qq"],["&#xf029;","qrcode"],["&#xf128;","question"],["&#xf059;","question-circle"],["&#xf10d;","quote-left"],["&#xf10e;","quote-right"],["&#xf074;","random"],["&#xf1d0;","rebel"],["&#xf1b8;","recycle"],["&#xf1a1;","reddit"],["&#xf1a2;","reddit-square"],["&#xf021;","refresh"],["&#xf18b;","renren"],["&#xf01e;","repeat"],["&#xf112;","reply"],["&#xf122;","reply-all"],["&#xf079;","retweet"],["&#xf018;","road"],["&#xf135;","rocket"],["&#xf09e;","rss"],["&#xf143;","rss-square"],["&#xf158;","rub"],["&#xf0c4;","scissors"],["&#xf002;","search"],["&#xf010;","search-minus"],["&#xf00e;","search-plus"],["&#xf064;","share"],["&#xf1e0;","share-alt"],["&#xf1e1;","share-alt-square"],["&#xf14d;","share-square"],["&#xf045;","share-square-o"],["&#xf132;","shield"],["&#xf07a;","shopping-cart"],["&#xf012;","signal"],["&#xf090;","sign-in"],["&#xf08b;","sign-out"],["&#xf0e8;","sitemap"],["&#xf17e;","skype"],["&#xf198;","slack"],["&#xf1de;","sliders"],["&#xf1e7;","slideshare"],["&#xf118;","smile-o"],["&#xf0dc;","sort"],["&#xf15d;","sort-alpha-asc"],["&#xf15e;","sort-alpha-desc"],["&#xf160;","sort-amount-asc"],["&#xf161;","sort-amount-desc"],["&#xf0de;","sort-asc"],["&#xf0dd;","sort-desc"],["&#xf162;","sort-numeric-asc"],["&#xf163;","sort-numeric-desc"],["&#xf1be;","soundcloud"],["&#xf197;","space-shuttle"],["&#xf110;","spinner"],["&#xf1b1;","spoon"],["&#xf1bc;","spotify"],["&#xf0c8;","square"],["&#xf096;","square-o"],["&#xf18d;","stack-exchange"],["&#xf16c;","stack-overflow"],["&#xf005;","star"],["&#xf089;","star-half"],["&#xf123;","star-half-o"],["&#xf006;","star-o"],["&#xf1b6;","steam"],["&#xf1b7;","steam-square"],["&#xf048;","step-backward"],["&#xf051;","step-forward"],["&#xf0f1;","stethoscope"],["&#xf04d;","stop"],["&#xf0cc;","strikethrough"],["&#xf1a4;","stumbleupon"],["&#xf1a3;","stumbleupon-circle"],["&#xf12c;","subscript"],["&#xf0f2;","suitcase"],["&#xf185;","sun-o"],["&#xf12b;","superscript"],["&#xf0ce;","table"],["&#xf10a;","tablet"],["&#xf0e4;","tachometer"],["&#xf02b;","tag"],["&#xf02c;","tags"],["&#xf0ae;","tasks"],["&#xf1ba;","taxi"],["&#xf1d5;","tencent-weibo"],["&#xf120;","terminal"],["&#xf034;","text-height"],["&#xf035;","text-width"],["&#xf00a;","th"],["&#xf009;","th-large"],["&#xf00b;","th-list"],["&#xf165;","thumbs-down"],["&#xf088;","thumbs-o-down"],["&#xf087;","thumbs-o-up"],["&#xf164;","thumbs-up"],["&#xf08d;","thumb-tack"],["&#xf145;","ticket"],["&#xf00d;","times"],["&#xf057;","times-circle"],["&#xf05c;","times-circle-o"],["&#xf043;","tint"],["&#xf204;","toggle-off"],["&#xf205;","toggle-on"],["&#xf1f8;","trash"],["&#xf014;","trash-o"],["&#xf1bb;","tree"],["&#xf181;","trello"],["&#xf091;","trophy"],["&#xf0d1;","truck"],["&#xf195;","try"],["&#xf1e4;","tty"],["&#xf173;","tumblr"],["&#xf174;","tumblr-square"],["&#xf1e8;","twitch"],["&#xf099;","twitter"],["&#xf081;","twitter-square"],["&#xf0e9;","umbrella"],["&#xf0cd;","underline"],["&#xf0e2;","undo"],["&#xf19c;","university"],["&#xf09c;","unlock"],["&#xf13e;","unlock-alt"],["&#xf093;","upload"],["&#xf155;","usd"],["&#xf007;","user"],["&#xf0f0;","user-md"],["&#xf0c0;","users"],["&#xf03d;","video-camera"],["&#xf194;","vimeo-square"],["&#xf1ca;","vine"],["&#xf189;","vk"],["&#xf027;","volume-down"],["&#xf026;","volume-off"],["&#xf028;","volume-up"],["&#xf18a;","weibo"],["&#xf1d7;","weixin"],["&#xf193;","wheelchair"],["&#xf1eb;","wifi"],["&#xf17a;","windows"],["&#xf19a;","wordpress"],["&#xf0ad;","wrench"],["&#xf168;","xing"],["&#xf169;","xing-square"],["&#xf19e;","yahoo"],["&#xf1e9;","yelp"],["&#xf167;","youtube"],["&#xf16a;","youtube-play"],["&#xf166;","youtube-square"]];

function getGlyphNameByCode(code) {
	for (var i in _fontawesomeGlyphs) {
		if (_fontawesomeGlyphs[i][0] == code) return _fontawesomeGlyphs[i][1];
	}
}

function Property_glyphCode(cell, controlAction, property, details) {
	
	// retieve the glyph code
	var code = controlAction[property.key];
	// if we got one
	if (code) {
		// add it and it's name into the cell
		cell.append("<span class='fa'>" + code + "</span>&nbsp;" + getGlyphNameByCode(code));
	} else {
		// add message
		cell.append("Please select...");
	}
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, controlAction, property, details, 200, "Glyphs");		
	// remove the standard table
	dialogue.find("table").first().remove();
	// add a scrolling div with the table inside
	dialogue.append("<div style='overflow-y:scroll;max-height:400px;'><table></table></div>");
	// get the new table
	table = dialogue.find("table").first();
	// add all of the glyphs, with the current one highlighted
	for (var i in _fontawesomeGlyphs) {
		table.append("<tr><td data-code='" + _fontawesomeGlyphs[i][0].replace("&","&amp;") + "' class='hover" + (code && code == _fontawesomeGlyphs[i][0] ? " selected" : "") + "'><span class='fa'>" + _fontawesomeGlyphs[i][0] + "</span><span class='fa_name'>&nbsp;" + _fontawesomeGlyphs[i][1] + "</span></td></tr>");
	}
	// if a position was set go back to it
	if (dialogue.attr("data-scroll")) table.parent().scrollTop(dialogue.attr("data-scroll"));
		
	addListener( table.find("td").click({cell: cell, controlAction: controlAction, property: property, details: details}, function(ev) {
		// get the cell
		var cell = $(ev.target).closest("td");
		// remove selected from others
		cell.closest("table").find("td").removeClass("selected");
		// apply selected to this
		cell.addClass("selected");
		// get the code
		var code = cell.attr("data-code");
		// get the table
		var table = cell.closest("table");
		// add the scroll position
		dialogue.attr("data-scroll",table.parent().scrollTop());
		// update the property
		updateProperty(ev.data.cell, ev.data.controlAction, ev.data.property, ev.data.details, code);
	}));
	
}

function Property_buttonGlyphPosition(cell, propertyObject, property, details) {
	// only if glyph code is specified
	if (propertyObject.glyphCode) {
		// add the bigtext
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_buttonGlyphBackground(cell, propertyObject, property, details) {
	// only if glyph code is specified
	if (propertyObject.glyphCode) {
		// add the bigtext
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// this is used by the maps for changing the lat/lng
function Property_mapLatLng(cell, propertyObject, property, details) {
	var value = "";
	// set the value if it exists
	if (propertyObject[property.key] || parseInt(propertyObject[property.key]) == 0) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input value='" + escapeApos(value) + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to return the property value if not a number
	addListener( input.change( function(ev) {
		var input = $(ev.target);
		var val = input.val();    
		// check decimal match
		if (val.match(new RegExp("^-?((\\d+(\\.\\d*)?)|(\\.\\d+))$"))) {
			// update value (but don't update the html)
			updateProperty(cell, propertyObject, property, details, ev.target.value);
			// get a reference to the iFrame window
			var w = _pageIframe[0].contentWindow;  
			// get the map
			var map = w._maps[propertyObject.id];
			// move the centre
			map.setCenter(new w.google.maps.LatLng(propertyObject.lat, propertyObject.lng));
		} else {
			// restore value
			input.val(propertyObject[property.key]);
		}
	}));
}

function Property_mapZoom(cell, propertyObject, property, details) {
	var value = "";
	// set the value if it exists (or is 0)
	if (propertyObject[property.key] || parseInt(propertyObject[property.key]) == 0) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input value='" + escapeApos(value) + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.change( function(ev) {
		var input = $(ev.target);
		var val = input.val();    
		// check integer match
		if (val.match(new RegExp("^\\d+$"))) {
			// make a number
			val = parseInt(val);
			// update value but not the html
			updateProperty(cell, propertyObject, property, details, val);
			// update the zoom
			// get a reference to the iFrame window
			var w = _pageIframe[0].contentWindow;  
			// get the map
			var map = w._maps[propertyObject.id];
			// move the centre
			map.setZoom(val);
		} else {
			// restore value
			input.val(propertyObject[property.key]);
		}
	}));
}

// this is displayed as a page property but is actually held in local storage
function Property_device(cell, propertyObject, property, details) {
	// holds the options html
	var options = "";
	// loop the array and build the options html
	for (var i in _devices) {
		// if the value is matched add selected
		if (i*1 == _device) {
			options += "<option value='" + i + "' selected='selected'>" + _devices[i].name + "</option>";
		} else {
			options += "<option value='" + i + "'>" + _devices[i].name + "</option>";
		}
	}
		
	// add the select object
	cell.append("<select>" + options + "</select>");
	// get a reference to the object
	var select = cell.children().last();
	// add a listener to update the property
	addListener( select.change( function(ev) {
		// retain the new value
		_device = $(ev.target).val() * 1;
		// store it
		if (typeof(localStorage) !== "undefined") localStorage.setItem("_device" ,_device);
		// recalculate scale
		_scale = _ppi / _devices[_device].PPI * _devices[_device].scale * _zoom;
		// hide the scroll bars to avoid artifacts during resizing
		$("#scrollV").hide();
		$("#scrollH").hide();
		// windowResize
		windowResize("_device");		
		// iframe resize
		_pageIframe.resize();
	}));
	// if value is not set, set the top value
	if (!propertyObject[property.key]) propertyObject[property.key] = select.val()*1;
	
}

// this is displayed as a page property but is actually held in local storage
function Property_orientation(cell, propertyObject, property, details) {
	// if we're holding a P (this defaulted in designerer.js)
	if (_orientation == "P") {
		cell.text("Portrait");
	} else {
		cell.text("Landscape");
	}
	// add the listener to the cell
	addListener( cell.click(function(ev) {
		// toggle the value
		if (_orientation == "P") {
			_orientation = "L";
			$(ev.target).text("Landscape");
		} else {
			_orientation = "P";
			$(ev.target).text("Portrait");
		}
		// store it
		if (typeof(localStorage) !== "undefined") localStorage.setItem("_orientation" ,_orientation);
		// hide the scroll bars to avoid artifacts during resizing
		$("#scrollV").hide();
		$("#scrollH").hide();
		// windowResize
		windowResize("_orientation");
	}));
}

//this is displayed as a page property but is actually held in local storage
function Property_zoom(cell, propertyObject, property, details) {
	// holds the options html
	var options = "";
	var values = [[0.5,"50%"],[1,"100%"],[1.5,"150%"],[2,"200%"],[3,"300%"],[4,"400%"]];
	// loop the array and build the options html
	for (var i in values) {
		// if the value is matched add selected
		if (values[i][0] == _zoom) {
			options += "<option value='" + values[i][0] + "' selected='selected'>" + values[i][1] + "</option>";
		} else {
			options += "<option value='" + values[i][0] + "'>" + values[i][1] + "</option>";
		}
	}
			
	// add the select object
	cell.append("<select class='propertiesPanelTable'>" + options + "</select>");
	// get a reference to the object
	var select = cell.children().last();
	// add a listener to update the property
	addListener( select.change( function(ev) {
		// retain the new value
		_zoom = $(ev.target).val() * 1;
		// store it
		if (typeof(localStorage) !== "undefined") localStorage.setItem("_zoom" ,_zoom);
		// recalculate scale
		_scale = _ppi / _devices[_device].PPI * _devices[_device].scale * _zoom;
		// hide the scroll bars to avoid artifacts during resizing
		$("#scrollV").hide();
		$("#scrollH").hide();
		// windowResize
		windowResize("_zoom");
	}));	
}

// this function controls whether guidline table borders are visible
function Property_guidelines(cell, propertyObject, property, details) {
	// assume we want them
	var showGuidelines = true;
	// if we have local storage
	if (typeof(localStorage) !== "undefined") {
		// if we have a local storage item for this
		if (localStorage.getItem("_guidelines")) {
			// parse it to a boolean
			showGuidelines = JSON.parse(localStorage.getItem("_guidelines"));
		} 
	}
	// add the checkbox
	cell.html("<input type='checkbox' />");
	// get the check box
	var checkbox =  cell.find("input");
	// set the checked value
	checkbox.prop("checked",showGuidelines);
	// add a listener for it to update local storage
	addListener(checkbox.change( function(ev) {
		// if we have local storage
		if (typeof(localStorage) !== "undefined") {
			// retain the new value
			localStorage.setItem("_guidelines", JSON.stringify($(ev.target).prop("checked"))); 
			// update
			Property_guidelines(cell, propertyObject, property, details);
		}
	}));
	// update the guidlines (this function is in desginer.js)
	updateGuidelines();	
}

// possible mobileActionType values used by the mobileActionType property
var _mobileActionTypes = [["dial","Dial number"],["sms","Send text/sms message"],["email","Send email"],["url","Open url"],["addImage","Get image"],["uploadImages","Upload images"],["addBarcode","Scan barcode"],["navigate","Navigate to"],["sendGPS","Send GPS position"],["stopGPS","Stop GPS updates"],["message","Status bar message"],["disableBackButton","Disable back button"],["online","Online actions"]];

// this property changes the visibility of other properties according to the chosen type
function Property_mobileActionType(cell, mobileAction, property, details) {
	// the selectHtml
	var selectHtml = "<select>";
	// loop the mobile action types
	for (var i in _mobileActionTypes) {
		selectHtml += "<option value='" + _mobileActionTypes[i][0] + "'" + (mobileAction.actionType == _mobileActionTypes[i][0]? " selected='selected'" : "") + ">" + _mobileActionTypes[i][1] + "</option>";
	}
	selectHtml += "</select>";
	// add the available types and retrieve dropdown
	var actionTypeSelect = cell.append(selectHtml).find("select");	
	// assume all other properties invisible
	setPropertyVisibilty(mobileAction, "numberControlId", false);
	setPropertyVisibilty(mobileAction, "numberField", false);
	setPropertyVisibilty(mobileAction, "emailControlId", false);
	setPropertyVisibilty(mobileAction, "emailField", false);
	setPropertyVisibilty(mobileAction, "subjectControlId", false);
	setPropertyVisibilty(mobileAction, "subjectField", false);
	setPropertyVisibilty(mobileAction, "messageControlId", false);
	setPropertyVisibilty(mobileAction, "messageField", false);
	setPropertyVisibilty(mobileAction, "urlControlId", false);
	setPropertyVisibilty(mobileAction, "urlField", false);
	setPropertyVisibilty(mobileAction, "galleryControlId", false);
	setPropertyVisibilty(mobileAction, "imageMaxSize", false);
	setPropertyVisibilty(mobileAction, "imageQuality", false);
	setPropertyVisibilty(mobileAction, "galleryControlIds", false);
	setPropertyVisibilty(mobileAction, "barcodeDestinations", false);
	setPropertyVisibilty(mobileAction, "successActions", false);
	setPropertyVisibilty(mobileAction, "errorActions", false);
	setPropertyVisibilty(mobileAction, "navigateControlId", false);
	setPropertyVisibilty(mobileAction, "navigateField", false);
	setPropertyVisibilty(mobileAction, "navigateSearchFields", false);
	setPropertyVisibilty(mobileAction, "navigateMode", false);
	setPropertyVisibilty(mobileAction, "gpsDestinations", false);
	setPropertyVisibilty(mobileAction, "gpsFrequency", false);	
	setPropertyVisibilty(mobileAction, "gpsCheck", false);
	setPropertyVisibilty(mobileAction, "message", false);
	setPropertyVisibilty(mobileAction, "onlineActions", false);
	setPropertyVisibilty(mobileAction, "onlineWorking", false);
	setPropertyVisibilty(mobileAction, "onlineFail", false);
	// adjust required property visibility accordingly
	switch (mobileAction.actionType) {
		case "dial" :
			setPropertyVisibilty(mobileAction, "numberControlId", true);
			setPropertyVisibilty(mobileAction, "numberField", true);
		break;
		case "sms" :
			setPropertyVisibilty(mobileAction, "numberControlId", true);
			setPropertyVisibilty(mobileAction, "numberField", true);
			setPropertyVisibilty(mobileAction, "messageControlId", true);
			setPropertyVisibilty(mobileAction, "messageField", true);
		break;
		case "email" :
			setPropertyVisibilty(mobileAction, "emailControlId", true);
			setPropertyVisibilty(mobileAction, "emailField", true);
			setPropertyVisibilty(mobileAction, "subjectControlId", true);
			setPropertyVisibilty(mobileAction, "subjectField", true);
			setPropertyVisibilty(mobileAction, "messageControlId", true);
			setPropertyVisibilty(mobileAction, "messageField", true);
		break;
		case "url" :
			setPropertyVisibilty(mobileAction, "urlControlId", true);
			setPropertyVisibilty(mobileAction, "urlField", true);
		break;
		case "addImage" :
			setPropertyVisibilty(mobileAction, "galleryControlId", true);
			setPropertyVisibilty(mobileAction, "imageMaxSize", true);
			setPropertyVisibilty(mobileAction, "imageQuality", true);
		break;
		case "uploadImages" :
			setPropertyVisibilty(mobileAction, "galleryControlIds", true);
			setPropertyVisibilty(mobileAction, "successActions", true);
			setPropertyVisibilty(mobileAction, "errorActions", true);
		break;
		case "addBarcode" :
			setPropertyVisibilty(mobileAction, "barcodeDestinations", true);
		break;
		case "navigate" :
			setPropertyVisibilty(mobileAction, "navigateMode", true);
			setPropertyVisibilty(mobileAction, "navigateControlId", true);
			setPropertyVisibilty(mobileAction, "navigateField", true);			
			setPropertyVisibilty(mobileAction, "navigateSearchFields", true);
		break;
		case "sendGPS" :						
			setPropertyVisibilty(mobileAction, "gpsDestinations", true);
			setPropertyVisibilty(mobileAction, "gpsFrequency", true);
			setPropertyVisibilty(mobileAction, "gpsCheck", true);
		break;
		case "message" :
			setPropertyVisibilty(mobileAction, "message", true);
		break;
		case "online" :
			setPropertyVisibilty(mobileAction, "onlineActions", true);
			setPropertyVisibilty(mobileAction, "onlineWorking", true);
			setPropertyVisibilty(mobileAction, "onlineFail", true);
		break;
	}
	// listener for changing the type
	addListener( actionTypeSelect.change({cell: cell, mobileAction: mobileAction, property: property, details: details}, function(ev) {
		// get the new value
		value = $(ev.target).val();
		// update the property (which will update the required visibilities)
		updateProperty(ev.data.cell, ev.data.mobileAction, ev.data.property, ev.data.details, value);	
	}));
}

//reuse the generic childActionsForType but set the details with type = database
function Property_galleryControls(cell, propertyObject, property, details) {
	// get any old school single property value
	var galleryControlId = propertyObject.galleryControlId;
	// if there was one
	if (galleryControlId) {
		// get the new array kind
		var galleryControlIds = propertyObject.galleryControlIds;
		// if null
		if (!galleryControlIds) {
			// instantiate
			galleryControlIds = [];
			// assign
			propertyObject.galleryControlIds = galleryControlIds;
		}
		// splice the old value to the top of the new property array
		galleryControlIds.splice(0,0,galleryControlId);
		// remove the old property
		propertyObject.galleryControlId = null;		
	}
	// run the controls for type
	Property_controlsForType(cell, propertyObject, property, {type:["gallery","signature"]});
}

// helper function for rebuilding the main control panel page select drop down
function rebuildPageSelect(cell, propertyObject, property) {		
	// get the dropdown
	var pageSelect = $("#pageSelect");
	pageSelect.children().remove();
	// rebuild the dropdown
	for (var i in _pages) {
		var page = _pages[i];
		pageSelect.append("<option value='" + page.id + "'" + (page.id == _page.id ? " selected='true'" : "") + ">" + page.name + " - " + page.title + "</option>");
	}
	// update the dialogue
	Property_pageOrder(cell, propertyObject, property); 
}

// page order
function Property_pageOrder(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 250, "Page order", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// add the all border class
	table.addClass("dialogueTableAllBorders");
	// make sure table is empty
	table.children().remove();
	
	// get the dialogue title
	var title = dialogue.find(".dialogueTitle").first();
	if (!title.next().is("button")) {
		title.after("<button style='float:left;margin-top:-7px;' class='titleButton sources' title='Reset page order'><span>&#xf0e2;</span></button>");
		// listener for resetting the page order
		addListener( dialogue.find("button.titleButton").first().click( function(ev) {
			// sort the pages
			_pages.sort( function(p1, p2) {
				var s1 = p1.name + " - " + p1.title;
				var s2 = p2.name + " - " + p2.title;
				return s1.localeCompare(s2);
			});
			// retain that the page order has been manually changed
			_pageOrderChanged = false;
			// retain that the page order has been reset
			_pageOrderReset = true;
			// rebuild the list and dialogue
			rebuildPageSelect(cell, propertyObject, property);
			// add an undo snapshot
			addUndo();
		}));
	}
		
	// assume the text of the cell is empty
	var text = "";
	
	for (var i in _pages) {
		// get the page
		var page = _pages[i];
		
		// append to the text
		text += page.name
		// add a comma if need be
		if (i < _pages.length - 1) text += ", ";
			
		// add a page name row
		table.append("<tr><td>" + page.name + " - " + page.title + "</td><td style='width:16px;padding-left:0;'><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		
	}
		
	// add reorder listeners
	addReorder(_pages, table.find("img.reorder"), function() {		
		// retain that the page order has been changed
		_pageOrderChanged = true;
		// retain that the page order has not been reset
		_pageOrderReset = false;
		// rebuild the list and dialogue
		rebuildPageSelect(cell, propertyObject, property);
		// add an undo snapshot (this will also prompt users to save the page)
		addUndo();
	});
	
	// put the text into the cell
	cell.text(text);
	
}

// a handler for text properties where there is a form adapter
function Property_formPageType(cell, propertyObject, property, details) {
	// only if there is a form adapter
	if (_version.formAdapter) {
		// add the select handler for this property
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

//a handler for text properties where there is a form adapter
function Property_formActionType(cell, propertyObject, property, details) {
	// only if there is a form adapter
	if (_version.formAdapter) {
		// start the main get value function with all basic types
		var getValuesFunction = "return [[\"\",\"Please select...\"],[\"next\",\"next page\"],[\"prev\",\"previous page\"],[\"id\",\"copy form id\"],[\"val\",\"copy form value\"]";
		// check the page type
		switch (_page.formPageType * 1) {
		case 1:
			// submitted
			getValuesFunction += ",[\"sub\",\"copy form submit message\"],[\"pdf\",\"copy form pdf url\"]";
			break;
		case 2:
			// error
			getValuesFunction += ",[\"err\",\"copy form error message\"]";
			break;
		case 3:
			// save
			getValuesFunction += ",[\"res\",\"copy form resume url\"]";
			break;
		}
		// close the array and add the final semi colon
		getValuesFunction += "];";		
		// add to property object
		property.getValuesFunction = getValuesFunction;
		// add the select handler for this property
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// a handler for text properties where there is a form adapter
function Property_formText(cell, propertyObject, property, details) {
	// only if there is a form adapter
	if (_version.formAdapter && (!_page.formPageType || _page.formPageType == 0)) {
		// add the text handler for this property
		Property_text(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// this is for the form action dataDestination
function Property_formDataSource(cell, propertyObject, property, details) {
	// only if the type is to copy values
	if (propertyObject.actionType == "val") {
		// add the Select
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
} 

// this is for the form action dataDestination
function Property_formDataDestination(cell, propertyObject, property, details) {
	// only if the type is one that requires a destination
	if (propertyObject.actionType == "id" || propertyObject.actionType == "val" || propertyObject.actionType == "sub" || propertyObject.actionType == "err" || propertyObject.actionType == "res" || propertyObject.actionType == "pdf" ) {
		// add the select
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
} 

// chart properties
function Property_chartType(cell, propertyObject, property, details) {
	Property_select(cell, propertyObject, property, details, function change(ev) {
		// get the control class
		var controlClass = _controlTypes[propertyObject.type];		
		// clean up the JavaScript by triming and removing line breaks and tabs
		var js = controlClass.initDesignJavaScript.trim();
		// try and apply it
		try {				
			// get the js into a new function variable
			var f = new Function(js);
			// run it
			f.apply(propertyObject, []);
		} catch (ex) {
			alert("initDesignJavaScript failed for " + this.type + ". " + ex + "\r\r" + js);
		}
	});
}
