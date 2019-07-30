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

/*

Functions related to control properties

*/

// this holds all the property listeners by dialogue id so they can be properly detached
var _dialogueListeners = {};
// a global for ensuring more recently shown dialogues are on top of later shown ones
var _dialogueZindex = 10012;
// this holds the cell, propertyObject, property, and details by dialogue id for refreshing child actions
var _dialogueRefeshProperties = {};
// the control name is important for checking conflicts and the change event is not picked up if a new control is selected so we track it specially
var _controlName;
// track whether the mouse is on the codeEditor
var _onCodeEditor = false;

// this function returns a set of options for a dropdown using the current set of pages
function getPageOptions(selectId, ignoreId) {
	var options = "";
	for (var i in _pages) {
		var page = _pages[i];
		if (page.id != ignoreId) options += "<option value='" + page.id + "' " + (page.id == selectId ? "selected='selected'" : "") + ">" + page.name + " - "  +page.title + "</option>"; 
	}
	return options;
}

// this function returns a set of options for a dropdown of controls
function getControlOptions(selectId, ignoreId, type, noGroup) {
	var controls = getControls();
	var options = "";
	for (var i in controls) {
		var control = controls[i];
		// note how only controls with names are included, and type is only matched if included
		if (control.id != ignoreId && control.name && (!type || type == control.type)) options += "<option value='" + control.id + "' " + (control.id == selectId ? "selected='selected'" : "") + ">" + control.name + "</option>"; 
	}
	// wrap if we had some and we're allowing groups
	if (options && !noGroup) options = "<optgroup label='Page controls'>" + options + "</optgroup>";
	// assume no other page controls added
	var otherPageControls = false;
	// other page controls can be used for input
	if (_page && _pages) {
		for (var i in _pages) {			
			if (_pages[i].id != _page.id && _pages[i].controls) {				
				var pageControlOptions = "";												
				for (var j in _pages[i].controls) {
					var otherPageControl = _pages[i].controls[j];					
					if (otherPageControl.otherPages && (!type || type == otherPageControl.type)) {
						pageControlOptions +=  "<option value='" + otherPageControl.id + "' " + (otherPageControl.id == selectId ? "selected='selected'" : "") + ">" + otherPageControl.name + "</option>"; 
					}
				}
				// if we got some wrap and add to options 
				if (pageControlOptions) options += "<optgroup label='" + _pages[i].name + " - " + _pages[i].title + "'>" + pageControlOptions + "</optgroup>";
			}
		}		
	}
	return options;
}

// this function returns a set of options for a dropdown of security roles
function getRolesOptions(selectRole, ignoreRoles) {
	var options = "";
	var roles = _version.roles;
	if (roles) {		
		for (var i in roles) {
			// retrieve this role			
			var role = roles[i];
			// assume we're not going to ignore it
			var ignore = false;
			// loop ignore roles
			if (ignoreRoles) {
				for (var j in ignoreRoles) {
					if (role == ignoreRoles[j]) {
						ignore = true;
						break;
					}
				}
			}			
			// if we're not going to ignore it
			if (!ignore) options += "<option " + (role == selectRole ? "selected='selected'" : "") + ">" + role + "</option>"; 
		}
	}
	return options;
}

//different date time values for datacopy source
var _datetimeValues = ["current date","current time","current date and time"];

// this function returns system values
function getDateTimeValueOptions(selectId) {
	var options = "";
	// system values
	if (_datetimeValues) {
		options += "<optgroup label='Date and time values'>";
		for (var i in _datetimeValues) {
			var val = "Datetime." + _datetimeValues[i];
			options += "<option value='" + val + "'" + (val == selectId ? " selected='selected'" : "") + ">" + _datetimeValues[i] + "</option>";
		}
		options += "</optgroup>";
	}
	return options;
}

// different system properties for inputs
var _systemValues = ["app id","app version","parameter", "page id","page name","page title","user name","device","online","mobile","mobile version","true","false","null","empty","field"];

// this function returns system values
function getSystemValueOptions(selectId) {
	var options = "";
	// system values
	if (_systemValues) {
		options += "<optgroup label='System values'>";
		for (var i in _systemValues) {
			var val = "System." + _systemValues[i];
			options += "<option value='" + val + "'" + (val == selectId ? " selected='selected'" : "") + ">" + _systemValues[i] + "</option>";
		}
		options += "</optgroup>";
	}
	return options;
}

// this function returns a set of options for a dropdown for inputs or outputs (depending on input true/false), can be controls, control properties (input only), other page controls, page variables (input only), system values (input only)
function getDataOptions(selectId, ignoreId, input, hasDatetime) {
	var options = "";	
	var controls = getControls();
	var gotSelected = false;
	if (controls) {
		options += "<optgroup label='Page controls'>";
		for (var i in controls) {	
			// retrieve the control
			var control = controls[i];
			// get the control class
			var controlClass = _controlTypes[control.type];
			// if we're not ignoring the control and it has a name
			if (controlClass && control.id != ignoreId && control.name) {				
				// if it has a get data function (for input), or a setDataJavaScript
				if ((input && controlClass.getDataFunction) || (!input && controlClass.setDataJavaScript)) {
					if (control.id == selectId && !gotSelected) {
						options += "<option value='" + control.id + "' selected='selected'>" + control.name + "</option>";
						gotSelected = true;
					} else {
						options += "<option value='" + control.id + "' >" + control.name + "</option>";
					}				
				}				
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
							// derive the key
							var key = control.id + "." + property.type;
							// add the option
							options += "<option value='" + key  +  "' " + (key == selectId ? "selected='selected'" : "") + ">" + control.name + "." + property.name + "</option>";
						}	
					}
				}
			}												
		}
		options += "</optgroup>";
	}
	
	// page variables are for input only
	if (input && _page && _page.sessionVariables) {
		options += "<optgroup label='Page variables'>";
		for (var i in _page.sessionVariables) {
			if (selectId == _page.sessionVariables[i] && !gotSelected) {
				options += "<option value='" + escapeApos(_page.sessionVariables[i]) + "' selected='selected' >" + _page.sessionVariables[i] + "</option>";
				gotSelected = true;
			} else {
				options += "<option value='" + escapeApos(_page.sessionVariables[i]) + "' >" + _page.sessionVariables[i] + "</option>";
			}			
		}
		options += "</optgroup>";
	}
	
	// other page controls can be used for input
	if (_page && _pages) {
		// loop the other pages
		for (var i in _pages) {
			// if the loop item is no the current page and it has some controls
			if (_pages[i].id != _page.id && _pages[i].controls) {
				// start blank options for loop page
				var pageControlOptions = "";
				// loop controls
				for (var j in _pages[i].controls) {
					// get this control
					var control = _pages[i].controls[j];
					// if it can be used from other pages
					if (control.otherPages) {
						// if we're looking for inputs and this is one, or we're not looking for inputs (outputs) and this isn't
						if ((input && control.input) || (!input && control.output)) {
							// if this is the control we're looking to select
							if (selectId == control.id && !gotSelected) {
								// add the option for the control input/output with it selected
								pageControlOptions += "<option value='" + control.id + "' selected='selected' >" +  control.name + "</option>";
								gotSelected = true;
							} else {
								// just add an option for the input/output
								pageControlOptions += "<option value='" + control.id + "' >" + control.name + "</option>";
							}
						}
						// if the control has runtime properties - also a source of inputs and outputs
						if (control.runtimeProperties) {
							// loop them
							for (var k in control.runtimeProperties) {
								// if we're looking for inputs and this is one, or we're not looking for inputs (outputs) and this isn't
								if ((input && control.runtimeProperties[k].input) || (!input && control.runtimeProperties[k].output)) {
									// if this is the property we're looking to select
									if (selectId == control.id + "." + control.runtimeProperties[k].type && !gotSelected) {
										// add the option for the property with it selected
										pageControlOptions += "<option value='" + control.id + "." + control.runtimeProperties[k].type + "' selected='selected' >" + control.name + "." + control.runtimeProperties[k].name + "</option>";
									} else {
										// just add an option for the property
										pageControlOptions += "<option value='" + control.id + "." + control.runtimeProperties[k].type + "' >" + control.name + "." + control.runtimeProperties[k].name + "</option>";
									}
								}
							}
						}
					}
				}
				// if we got some options for the page we're looping wrap it into a group
				if (pageControlOptions) options += "<optgroup label='" + escapeApos(_pages[i].name + " - " + _pages[i].title) + "'>" + pageControlOptions + "</optgroup>";			
			}
		}
	}
	// date time value, only for the datacopy inputs, set with dateTime = true
	if (input && hasDatetime) options += getDateTimeValueOptions(selectId);
	// system values, only for inputs - these are defined in an array above this function
	if (input && _systemValues) options += getSystemValueOptions(selectId);
	// return
	return options;
}

// this function returns a set of options for form values from previous pages
function getFormValueOptions(selectId) {
	// we want this pages session variables and all prior pages session variables and controls with canBeUsedForFormPageVisibilty
	var options = "";
	var gotSelected = false;
	// page variables 
	if (_page && _page.sessionVariables) {
		options += "<optgroup label='Page variables'>";
		for (var i in _page.sessionVariables) {
			var val = "Session." + _page.sessionVariables[i];
			if (selectId == val && !gotSelected) {
				options += "<option value='" + val + "' selected='selected' >" + _page.sessionVariables[i] + "</option>";
				gotSelected = true;
			} else {
				options += "<option value='" + val + "' >" + _page.sessionVariables[i] + "</option>";
			}			
		}
		options += "</optgroup>";
	}
	// other pages
	if (_page && _pages) {
		// loop the pages
		for (var i in _pages) {
			// stop when the current page is reached (we only want the prior ones)
			if (_pages[i].id == _page.id) break;
			if (_pages[i].controls) {
				var pageControlOptions = "";
				// page session variables
				for (var j in _pages[i].sessionVariables) {
					var val = "Session." + _pages[i].sessionVariables[j];
					if (selectId == val) {
						pageControlOptions += "<option value='" + val + "' selected='selected' >" +  _pages[i].sessionVariables[j] + "</option>";
						gotSelected = true;
					} else {
						pageControlOptions += "<option value='" + val + "' >" + _pages[i].sessionVariables[j] + "</option>";						
					}
				}
				// page controls
				for (var j in _pages[i].controls) {					
					var control = _pages[i].controls[j];					
					if (control.pageVisibility && control.name) {																	
						if (selectId == control.id && !gotSelected) {
							pageControlOptions += "<option value='" + control.id + "' selected='selected' >" +  control.name + "</option>";
							gotSelected = true;
						} else {
							pageControlOptions += "<option value='" + control.id + "' >" + control.name + "</option>";
						}												
					}					
				}
				// if we got options for the page we are looping wrap into a group
				if (pageControlOptions) options += "<optgroup label='" + escapeApos(_pages[i].name + " - " + _pages[i].title) + "'>" + pageControlOptions + "</optgroup>";
			}			
		}
	}
	return options;
}



// this function returns a set of options for a dropdown of sessionVariables and controls with a getData method
function getInputOptions(selectId, ignoreId, hasDatetime) {
	return getDataOptions(selectId, ignoreId, true, hasDatetime);
}

// this function returns a set of options for a dropdown of sessionVariables and controls with a setData method
function getOutputOptions(selectId, ignoreId) {
	return getDataOptions(selectId, ignoreId, false);
}

// this function returns a set of options for use in page visibility logic
function getPageVisibilityOptions(selectId) {
	return getFormValueOptions(selectId) + getSystemValueOptions(selectId);
}

// this function returns a set of options for a dropdown of existing events from current controls 
function getEventOptions(selectId, controls) {
	// assume no options
	var options = "";	
	// assume not for the page
	var forPage = false;
	// if controls not provided
	if (!controls) {
		// remember this is for the page
		forPage = true;
		// loop this page
		for (var i in _page.events) {
			var event = _page.events[i];
			var id = "page." + event.type;
			options += "<option value='" + id + "' " + (selectId  == id ? "selected='selected'" : "") + ">" + id + "</option>";			
		}
		// get controls from this page
		controls = getControls();
	}
	// loop the controls
	for (var i in controls) {
		// get this one
		var control = controls[i];
		// if it exists and has an id and name and events
		if (control && control.id && control.name && control.events) {
			// loop the events
			for (var j in control.events) {
				// get this events
				var event = control.events[j];
				// only if this event has some actions
				if (event.actions && event.actions.length > 0) {
					// set the if for this event
					var id = control.id + "." + event.type;
					// set the text to display
					var text = control.name + "." + event.type;
					// append as an option
					options += "<option value='" + id + "' " + (selectId  == id ? "selected='selected'" : "") + ">" + text + "</option>";
				}
			}
		}
	}
	// other page events and action can be used for input
	if (forPage && _pages) {
		for (var i in _pages) {
			// if different from this page and there is a controls collection
			if (_pages[i].id != _page.id && _pages[i].controls) {
				// get any options for this page
				var otherPageOptions = getEventOptions(selectId, _pages[i].controls);
				// if we got some
				if (otherPageOptions) options += "<optgroup label='" + escapeApos(_pages[i].name + " - " + _pages[i].title) + "'>" + otherPageOptions + "</optgroup>";
			}
		}
	}
	return options;
}

// this function creates the option for an existing action option
function getExistingActionOption(action, index, selectId, ignoreId, forPage) {
	var option = "";
	var text =  (index*1+1) + " - "  + action.type.substr(0,1).toUpperCase() + action.type.substr(1) + " action";
	if (action.comments) text += " : " + action.comments;
	if (action.id != ignoreId) option = "<option value='" + action.id + "' " + (action.id == selectId ? "selected='selected'" : "") + ">" + (forPage?"":"&nbsp;&nbsp;") + text + "</option>";
	return option;
}

function getExistingActionEventOptionGroup(control, event, eventJS, forPage) {
	var name = _page.name;
	if (control) name = control.name;
	return "<optgroup label='" + (forPage?"":"&nbsp;&nbsp;") + name + " - " + event.type.substr(0,1).toUpperCase() + event.type.substr(1) + " event'>" + eventJS + "</optgroup>";
}

// this function returns a set of options for a dropdown of existing actions from current controls 
function getExistingActionOptions(selectId, ignoreId, controls) {
	// assume no options
	var options = "";
	// assume not for the page
	var forPage = false;
	// if controls are not provided
	if (!controls) {
		// remember this is for the page
		forPage = true;
		// add the page events
		for (var i in _page.events) {
			var eventJS = "";
			var event = _page.events[i];
			for (var j in event.actions) {
				eventJS += getExistingActionOption(event.actions[j], j, selectId, ignoreId, forPage);
			}			
			if (eventJS) options += getExistingActionEventOptionGroup(null, event, eventJS, forPage);
		}
		// now get the page controls
		controls = getControls();
	}
	// loop the controls
	for (var i in controls) {
		if (controls[i].name) {
			for (var j in controls[i].events) {
				var eventJS = "";
				var event = controls[i].events[j];
				for (var k in event.actions) {					
					eventJS += getExistingActionOption(event.actions[k], k, selectId, ignoreId, forPage);
				}
				if (eventJS) options += getExistingActionEventOptionGroup(controls[i], event, eventJS, forPage);
			}
		}
	}
	// other page events and action can be used for input
	if (forPage && _pages) {
		for (var i in _pages) {
			// if different from this page and there is a controls collection
			if (_pages[i].id != _page.id && _pages[i].controls) {
				// get any options for this page
				var otherPageExistingActionOptions = getExistingActionOptions(selectId, ignoreId, _pages[i].controls);
				// if we got some
				if (otherPageExistingActionOptions) options += "<optgroup label='" + escapeApos(_pages[i].name + " - " + _pages[i].title) + "'>" + otherPageExistingActionOptions + "</optgroup>";
			}
		}
	}
	return options;
}

function getDatabaseConnectionOptions(selectIndex) {
	var options = "";
	if (_version.databaseConnections) {
		for (var i in _version.databaseConnections) {
			options += "<option value='" + i + "' " + (i == selectIndex ? "selected='selected'" : "") + ">" + _version.databaseConnections[i] + "</option>";
		}
	}
	return options;
}

function getStyleClassesOptions(selected) {
	var classOptions = "";
	// loop any style classes
	for (var i in _styleClasses) {
		classOptions += "<option" + (_styleClasses[i] == selected ? " selected='selected'" : "") + ">" + _styleClasses[i] + "</option>";
	}
	// return
	return classOptions;
}

function getValueListsOptions(selected) {
	var valueLists = "";
	if (_version.valueLists) {
		for (var i in _version.valueLists) {
			valueLists += "<option" + (_version.valueLists[i].name == selected ? " selected='selected'" : "") + ">" + _version.valueLists[i].name + "</option>";
		}
	}
	return valueLists;
}

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
		// if there is helpHtml
		if (controlClass.helpHtml) {
			// add a help icon after the title
			propertiesTable.find("h3").after("<i id='" + control.id + "help' class='controlHelp glyph fa hintIcon'></i>");
			//<i id="helpApplication" class="headerHelp glyph fa hintIcon"></i>
			// add the help listener
			addHelp(control.id + "help",true,true,controlClass.helpHtml);
		}
		// show any conflict message
		if (control._conflict) propertiesTable.append("<tr><td colspan='2' class='conflict propertyHeader'>Page \"" + control._conflict + "\" has a control with the same name</td></tr>");
		// show the control id if requested
		if (_version.showControlIds) propertiesTable.append("<tr><td>ID</td><td class='canSelect'>" + control.id + "</td></tr>");
		// check there are class properties
		var properties = controlClass.properties;
		if (properties) {
			// (if a single it's a class not an array due to JSON class conversion from xml)
			if ($.isArray(properties.property)) {
				properties = properties.property;
			} else if (properties.property) {
				properties = [properties.property];
			}
			// loop the class properties
			for (var i = 0; i < properties.length; i++) {
				// retrieve a property object from the control class
				var property = properties[i];
				// if we support integration properties and this is a formControl check for special form integration properties (a bit like action comments)
				if (control.type != "page" && _version.canSupportIntegrationProperties && property.key == "label" && properties[properties.length - 1].key != "formObjectText") {
					// add form properties
					properties.push({"name":"Form integration","changeValueJavaScript":"gap", "setConstructValueFunction": "return 'Form integration'", "helpHtml":"Use this powerful feature to map form control values to meaningful real-world things. On submission of the form the integration mappings will be used to pass data to external systems. They can also be used to pre-populate form data from external systems."});
					properties.push({"key":"formObject","name":"Object", "refreshProperties": true, "helpHtml":"The object that the control is holding data for in the form. Used for advanced form integration."}); // refreshProperties shows the dynamic properties below				
					properties.push({"key":"formObjectRole","name":"Role", "refreshProperties": true, "helpHtml":"The role of the object. For example a case party, linked party, or the case itself."}); // refreshProperties allows party / case party to not show the type (used only for linked parties)
					properties.push({"key":"formObjectPartyNumber","name":"Party number", "helpHtml":"A number identifying the party. The main or first party is 1. Use the same number to specify the other attributes for the same party."});
					properties.push({"key":"formObjectAddressNumber","name":"Address number", "helpHtml":"A number identifying the address. Use the same number to specify the other attributes for the same address."});
					properties.push({"key":"formObjectType","name":"Type", "helpHtml":"A type for the object, or role. For example a contact email, mobile or telephone, notepad general or medical type, or an address physical or postal address."});					
					properties.push({"key":"formObjectAttribute","name":"Attribute", "helpHtml":"A further attribute of the object, for example a parties title, or address start date."});					
					properties.push({"key":"formObjectQuestionNumber","name":"Question number", "helpHtml":"The question number in the application with which this form is integrating"});
					properties.push({"key":"formObjectText","name":"Text", "helpHtml":"Text that will appear in the note before the contents of the control."});
				}
				// add a row
				propertiesTable.append("<tr></tr>");
				// get a reference to the row
				var propertiesRow = propertiesTable.children().last();				
				// check that visibility is not false
				if (property.visible === undefined || !property.visible === false) {
					// assume no help
					var help = "";
					// if the property has help html
					if (property.helpHtml) {
						// make the helpId
						var helpId = control.id + property.key + "help";
						// create help html
						help = "<i id='" + helpId + "' class='propertyHelp glyph fa hintIcon'></i>";
					}
					// get the property itself from the control
					propertiesRow.append("<td>" + property.name + help + "</td><td></td>");
					// add the help listener
					if (help) addHelp(helpId,true,true,property.helpHtml);
					// get the cell the property update control is going in
					var cell = propertiesRow.children().last();
					// if no changeValueJavaScript, set it to the key
					if (!property.changeValueJavaScript) property.changeValueJavaScript = property.key; 
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
			
		} // property value changed check		
	} // page lock check	
}

// set the visibility of a known property
function setPropertyVisibilty(propertyObject, propertyKey, visibile) {
	// if we got a propertyObject
	if (propertyObject) {
		// get the class from controls
		var objectClass = _controlTypes[propertyObject.type];
		// try actions if not found
		if (!objectClass) objectClass = _actionTypes[propertyObject.type];
		// if we have what we need
		if (objectClass && objectClass.properties) {
			// get the properties
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
}

function addDialogueResizeX(dialogue, codeMirror) {
	// add the mouse over div
	var resizeX = dialogue.append("<div class='resizeX'></div>").find("div.resizeX");
	// add the listener
	addListener(resizeX.mousedown( {id: dialogue.attr("id"), dialogue: dialogue}, function(ev) {
		// retain that we'r resizing a dialogue
		_dialogueSize = true;
		// retain it's id
		_dialogueSizeId = ev.data.id;
		// retain the type of resize
		_dialogueSizeType = "X";
		// calculate the offset
		_mouseDownXOffset = ev.pageX - $("#" + _dialogueSizeId).offset().left + (ev.data.dialogue.is(".CodeMirror") ? 20 : 0);
		
	}));
	
	resizeX.mouseup(function(event){
		//keep focus even when mouse is up
		if(codeMirror){
			codeMirror.focus();
			//Set the cursor at the end of existing content
			codeMirror.setCursor(codeMirror.lineCount(), 0);
		}
	});
	
	resizeX.mouseleave(function(){
		//keep focus even when mouse has left the resize dialogue
		if(codeMirror){
			codeMirror.focus();
			//Set the cursor at the end of existing content
			codeMirror.setCursor(codeMirror.lineCount(), 0);
		}
	});
}

// this is a reusable function for creating dialogue boxes
function getDialogue(cell, propertyObject, property, details, width, title, options) {	
		
	// derive the id for this dialogue
	var dialogueId = propertyObject.id + property.key;
	
	// add the data-dialogueId to the cell
	cell.attr("data-dialogueId", dialogueId);
	
	// change the cursor
	cell.css("cursor","pointer");
	
	// retrieve the dialogue
	var dialogue = $("#propertiesDialogues").find("#" + dialogueId);

	// if we couldn't retrieve one, make it now
	if (!dialogue[0]) {		
		// add the div
		dialogue = $("#propertiesDialogues").append("<div id='" + dialogueId + "' class='propertyDialogue'></div>").children().last();
		// check the options
		if (options) {
			// if resizeX
			if (options.sizeX) addDialogueResizeX(dialogue)
			// set min-width to explicit or standard width
			if (options.minWidth) {
				dialogue.css("min-width", options.minWidth);
			} else {
				dialogue.css("min-width", width);
			}
		}
		
		// add a close link
		var close = dialogue.append("<b class='dialogueTitle' style='float:left;margin-top:-5px;'>" + title + "</b><i class='fa dialogueClose fa-external-link-square' style='float:right; font-size:18px; color:#494949; padding-bottom:5px;' title='Close dialogue'></i></div>").children().last();
	
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
					var close = childDialogue.find("fa.dialogueClose");
					// click it
					close.click();
				}
				
			});
			
			dialogue.slideUp(200, function (){
				// remove this dialogue
				$(this).remove();
				
				// call an update on the master property to set the calling cell text
				updateProperty(cell, propertyObject, property, details, propertyObject[property.key]);
			});

		}));
		
		// add an options table
		dialogue.append("<br/><table class='dialogueTable'><tbody></tbody></table>");
	}	
	
	// listener to show the dialogue 
	addListener(cell.click({dialogueId: dialogueId, propertyFunction: arguments.callee.caller}, function(ev) {
		// retrieve the dialogue using the id
		var dialogue = $("#propertiesDialogues").find("#" + ev.data.dialogueId);
		// if it doesn't exist 
		if (!dialogue[0]) {
			//call the original property function
			ev.data.propertyFunction(cell, propertyObject, property, details);
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
	$("i.fa.dialogueClose:visible").click();
	// remove all listeners
	removeListeners();	
	// empty any propertyDialogues that we may have used before
	$("#propertiesDialogues").children().remove();
	// hide any help hints that might be hanging around
	$("span.hint:visible").hide();
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
	if (value && value.replaceAll) {
		return value.replaceAll("'","&apos;");
	} else {
		return value;
	}
}

// create a code mirror instance
function getCodeMirror(element, mode) {
	// set mode if none provided
	if (!mode) mode = "text/plain";
	// assume default theme
	var theme = "default";
	// if there is a local storage theme, use that
	if (window.localStorage && window.localStorage.getItem("_codeEditorTheme")) theme = window.localStorage.getItem("_codeEditorTheme");
	// assume default extra keys
	var extraKeys = {"Ctrl-Space": "autocomplete"};
	// add more keys if xml
	if (mode == "xml") extraKeys = {"Ctrl-Space": "autocomplete",
	  "'<'": completeAfter,
	  "'/'": completeIfAfterLt,
      "' '": completeIfInTag,
      "'='": completeIfInTag
	};
	// make the code mirror
	var codeMirror = CodeMirror(element, {
		  mode:  mode,
		  theme: theme,
		  lineWrapping: true,
		  lineNumbers: true,
		  matchBrackets: true,
		  autoCloseBrackets: true,
		  extraKeys: extraKeys,
		  styleActiveLine: true,
		  readOnly: false,
		  autoRefresh: true,
		  viewportMargin: Infinity
	});
	// return
	return codeMirror;
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
	// if this is a control's name property
	if (propertyObject.object && property.key == "name") {
		// retain the name
		_controlName = value;
		// add a listener to rebuild the page map with the new control name - we need blur as change does not pick up when the user selects a new control
		addListener( input.change( function(ev) {
			// retain the changed name
			_controlName = $(ev.target).val();
			// remove any conflict message
			cell.closest("table").find("td.conflict").remove();
			// update the page map
			buildPageMap();
			// if a conflict is present, add it
			if (propertyObject._conflict) cell.closest("table").find("tr:nth-child(2)").after("<tr><td colspan='2' class='conflict propertyHeader'>Page \"" + propertyObject._conflict + "\" has a control with the same name</td></tr>");			
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
	addListener( input.keyup( function(ev) {
		var input = $(ev.target);
		var val = input.val();    
		// check integer match
		if (!val || val.match(new RegExp("^\\d+$"))) {
			// update value
			updateProperty(cell, propertyObject, property, details, ev.target.value);						
		} else {
			// restore value
			input.val(propertyObject[property.key]);
		}
	}));
}

//a handler for inputs that must be numbers
function Property_number(cell, propertyObject, property, details) {
	var value = "";
	// set the value if it exists (or is 0)
	if (propertyObject[property.key] || parseInt(propertyObject[property.key]) == 0) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to set the property back if not an integer
	addListener( input.keyup( function(ev) {
		var input = $(ev.target);
		var val = input.val();    
		// check decimal match
		if (val.match(new RegExp("^\\d*?\\.?\\d*$"))) {
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
	
	//track whether the property is created
	var dialogueId;
	var propertiesDialogue;
	var myCodeMirror;
	var myCodeMirrorDialogue;
	
	// add a listener to update the property
	addListener( cell.click(function(ev) {
		
		//we only need to create it once, when this cell is clicked for the first time
		if(!myCodeMirror){
			//create the codeEditor and its event handlers
			// derive the id for this dialogue
			dialogueId = propertyObject.id + property.key;
			// get a reference of the propertiesDialogues
			propertiesDialogue = $("#propertiesDialogues").append("<div id='" + dialogueId + "'></div>").find("div").last();
			// create an editor instance, and append it to the propertiesDialogue
			myCodeMirror = getCodeMirror(propertiesDialogue[0]);
			
			// get codeMirror dialogue element 
			myCodeMirrorDialogue = $(myCodeMirror.getWrapperElement());
			// give it an id
			myCodeMirrorDialogue.attr("id",dialogueId + "_codeMirror");
			
			// give it x resizing
			addDialogueResizeX(myCodeMirrorDialogue, myCodeMirror);
				
			//check if this property is a javascript or command
			if(["javascript", "command"].indexOf(property.key) > -1) {
				myCodeMirror.setOption("mode", "javascript");
			}
			
			//track whether the mouse is on the codeEditor (result is used for the blur event)
			//because codeEditor gets unfocused (i.e. blur) when clicking on the scrollbar
			myCodeMirrorDialogue.mouseenter(function() {
				_onCodeEditor = true;
			});
			
			//
			myCodeMirrorDialogue.mouseleave(function() {
				_onCodeEditor = false;
			});
			
		}//end of if
		//otherwise, the codeEditor must have already been created, so just display it
		//add the text into the codeEditor
		myCodeMirror.setValue(value);
		//style and position the codeEditor, hide it at start
		// assume right is 10
		var right = 10;
	
		// if we're in a dialogue
		if ($(ev.target).closest(".propertyDialogue")) right = 11;
		myCodeMirrorDialogue.css({
			"position":"absolute", "height":"300px", "width":"600px", "right": right, "top": cell.offset().top, "z-index":_dialogueZindex ++, "display":"none"
		});
		
		//display animated with slideDown effect
		myCodeMirrorDialogue.slideDown(500, function() {	
			// focus it so a click anywhere else fires the unfocus and hides the textbox
			myCodeMirror.focus();
			// set the cursor at the end of existing content
			myCodeMirror.setCursor(myCodeMirror.lineCount(), 0);
		});
		
		//now create its event handlers
		var keyUpCallback = function(){
			updateProperty(cell, propertyObject, property, details, myCodeMirror.getValue());
		};
		var blurCallback = function() {

			// Blur only if mouse is not on the (resize dialogue and codeeditor)
			if (!_dialogueSize && !_onCodeEditor) {
				
				//update the value - so that it can be reused in the next click
				value = myCodeMirror.getValue();
				//put the content of the new value in this cell
				cell.text(myCodeMirror.getValue());
				//hide the codemirror
				myCodeMirrorDialogue.slideUp(200);
				
				windowResize("Property_bigtext hide");
				//remove the event handlers on the editor instance, on unfocus
				myCodeMirror.off("blur", blurCallback);
				myCodeMirror.off("keyup", keyUpCallback);
			}

		};
		
		// create listener - hide the textarea and update the cell on unfocus
		myCodeMirror.on("blur", blurCallback);
		
		// modify if the text is updated - addListener is only for jQuery events
		myCodeMirror.on("keyup", keyUpCallback);
		
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
					if (value === undefined) value = values[i][0];
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
		} else if ($.isPlainObject(values)) {
			// add a please select
			options = "<option value=''>Please select...</option>";
			// loop keys
			for (var i in values) {
				// if there is a name
				if (values[i].name) {
					// add option with name
					options += "<option value='" + i + "'>" + escapeApos(values[i].name) + "</option>";
				} else {
					// add option
					options += "<option>" + i + "</option>";
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
	// empty the box so it's just a tiny gap
	cell.prev().html("");
	// if we have a setConstructValueFunction
	if (property.setConstructValueFunction) {
		// create a function from the property contents
		var f = new Function(property.setConstructValueFunction);
		// get label from invoking the function
		var name = f.apply(this, []); 
		// display the name
		cell.parent().after("<tr><td colspan='2' class='propertySubHeader'><h3>" + name + "</h3></td></tr>");
		// if there is helphtml
		if (property.helpHtml) {
			// add the icon
			cell.parent().next().find("td").last().append("<i id='" + propertyObject.id + "help_gap' class='actionHelp glyph fa hintIcon'></i>");
			// add the listener
			addHelp(propertyObject.id + "help_gap",true,true,property.helpHtml);
		}
	}
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
			// add comma if need be
			if (i < fields.length -1) text += ", ";
			// add reorder 			
			table.append("<tr><td><input value='" + escapeApos(fields[i]) + "'/></td><td style='width:45px'>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");
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
	
	// add reorder listeners
	addReorder(fields, table.find("div.reorder"), function() { 
		Property_fields(cell, action, property); 
	});
		
	// add delete listeners
	addListener( table.find("div.delete").click( function (ev) {
		// add undo
		addUndo();
		// get the image
		var img = $(ev.target);
		// remove the field at this location
		fields.splice(img.closest("tr").index() - 1,1);
		// update the dialogue
		Property_fields(cell, action, property, details);
	}));
	
	// append add
	table.append("<tr><td colspan='3'><span class='propertyAction'>add...</span></td></tr>");
	
	// add listener
	addListener( table.find("span.propertyAction").click( function (ev) {
		// add a field
		fields.push("");
		// refresh dialogue
		Property_fields(cell, action, property, details);
	}));
	
}

function Property_inputMaxLength(cell, input, property, details) {
	// default to a 100 if undefined (for backwards population)
	if (input.maxLength === undefined) input.maxLength = 100;
	// if this is a large and is at the regular default, default to 1000
	if (input.controlType == "L" && input.maxLength == 100) input.maxLength = 1000;
	// now use a regular text
	Property_text(cell, input, property);
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
	table.append("<tr><td  style='text-align:center;'>Url</td>" + (gallery.gotCaptions ? "<td  style='text-align:center;'>Caption</td>" : "") + "<td></td></tr>");
	
	// loop the images
	for (var i in images) {
		// get this image
		var image = images[i];
		// set caption to empty string if not set
		if (!image.caption) image.caption = "";
		// append
		table.append("<tr><td><input class='url' value='" + escapeApos(image.url) + "' /></td>" + (gallery.gotCaptions ? "<td><input class='caption' value='" + escapeApos(image.caption) + "' /></td>" : "") + "<td>" +
				"<div class='iconsPanel'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
				"</div></td></tr>");
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
	
	// add reorder listeners
	addReorder(images, table.find("div.reorder"), function() {
		// rebuild the gallery html
		rebuildHtml(gallery); 
		// rebuild this dialogue
		Property_galleryImages(cell, gallery, property); 
	});
	
	// add delete listeners
	addListener( table.find("div.delete").click( function (ev) {
		// add undo
		addUndo();
		// get the row
		var row = $(this).closest("tr");
		// remove the image at this position
		images.splice(row.index() - 1, 1);
		// remove the row
		row.remove();
		// rebuild the gallery html
		rebuildHtml(gallery); 
		// update the dialogue;
		Property_galleryImages(cell, gallery, property, details);
	}));
	
	// check we don't have a checkbox already
	if (!dialogue.find("input[type=checkbox]")[0]) {
		// append add
		table.after("<span class='propertyAction'>add...</span>");
		
		// add listener
		addListener( dialogue.find("span.propertyAction").click( function (ev) {
			// add an image
			images.push({url:""});
			// refresh dialogue
			Property_galleryImages(cell, gallery, property, details);
		}));
	}
			
	// check we don't have a checkbox already
	if (!dialogue.find("input[type=checkbox]")[0]) {
		
		// append caption check box
		table.after("<div style='padding: 5px 0;'><label><input type='checkbox' " + (gallery.gotCaptions ? "checked='checked'" : "") + "/>captions</label></div>");
		
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
	} else if (propertyObject.navigationType == "P") {
		//generate the special navigation select		
		Property_navigationPage(cell, propertyObject, property, details);			
	} else {
		// remove the row
		cell.parent().remove();
	}
}

function Property_linkURL(cell, propertyObject, property, details) {
	// if the type is a url
	if (propertyObject.linkType == "U" || propertyObject.navigationType == "U")	{
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
	cell.append("<input class='propertiesPanelTable' value='" + escapeApos(value) + "' maxlength='100' />");
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
			table.append("<tr><td>" + control.name + "</td><td style='width:45px'>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");			
		} else {
			// remove this control
			controls.splice(i,1);
		}		
	}	
	
	// add reorder listeners
	addReorder(controls, table.find("div.reorder"), function() { 
		Property_validationControls(cell, propertyObject, property); 
	});
	
	// add listeners to the delete image
	addListener( table.find("div.delete").click( function(ev) {
		// add undo
		addUndo();
		// get the row
		var row = $(this).closest("tr");
		// remove the control
		propertyObject.controls.splice(row.index(),1);
		// remove the row
		row.remove();
		// rebuild the dialogue to sychronise for reorder
		Property_validationControls(cell, propertyObject, property);
	}));
	
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
		table.append("<tr><td colspan='2' style='padding-bottom:3px;'>" +
				"<div class='copyActions fa-stack fa-xs' title='Copy all actions'><i class='fa fa-file fa-stack-1x'></i><i class='bottomFile fa fa-file fa-stack-1x'></i>" +
				"</td></tr>");
		// add the listener
		addListener( table.find("div.copyActions").last().click( { actionType:propertyObject.type, propertyName:property.name, actions:actions, cell: cell, propertyObject: propertyObject, property: property, details: details }, function(ev) {
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
	addReorder(actions, table.find("div.reorder"), function() { Property_childActions(cell, propertyObject, property); });
	
	// get the dialogue id
	var dialogueId = dialogue.attr("id");
	
	// store what we need when refreshing inside this dialogue
	_dialogueRefeshProperties[dialogueId] = {cell: cell, propertyObject: propertyObject, property: property};
					
}

// generic inputs to a server-side action
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
			table.append("<tr><td>" + dataItem.name + "</td><td><input value='" + input.field + "' /></td><td><input value='" + input.inputField + "' /></td><td style='width:45px'>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");
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
		} else {
			// remove this entry from the collection
			inputs.splice(i,1);
			// set i back 1 position
			i--;
		}			
	}
			
	// add reorder listeners
	addReorder(inputs, table.find("div.reorder"), function() { 
		Property_inputs(cell, propertyObject, property, details); 
	});

	// add a listener
	addListener( table.find("div.delete").click( {inputs: inputs}, function(ev) {
		// get the input
		var imgDelete = $(ev.target);
		// remove from parameters
		ev.data.inputs.splice(imgDelete.closest("tr").index()-1,1);
		// remove row
		imgDelete.closest("tr").remove();
		// refresh dialogue
		Property_inputs(cell, propertyObject, property, details); 
	}));
	
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

//generic outputs to a server-side action
function Property_outputs(cell, propertyObject, property, details) {
	
	// assume datacopy destinations
	var title = "Outputs";
	// change if email attachment source
	if (propertyObject.type == "email") title = "Attachment source";
	// change if pdf filename output
	if (propertyObject.type == "pdf") title = "Filename outputs";
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 400, title, {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	// build what we show in the parent cell
	var outputs = [];
	// get the value if it exists
	if (propertyObject[property.key]) outputs = propertyObject[property.key];	
	// make some text for our cell (we're going to build in in the loop)
	var text = "";
	
	// add a header
	table.append("<tr><td><b>Output field</b></td><td><b>Control</b></td><td colspan='2'><b>Field</b></td></tr>");
		
	// show current choices (with delete and move)
	for (var i = 0; i < outputs.length; i++) {
		// get a single reference
		var output = outputs[i];	
		// if we got one
		if (output) {
			// get a data item object for this
			var dataItem = getDataItemDetails(output.itemId);
			// apend to the text
			text += dataItem.name + ",";
			// add a row
			table.append("<tr><td><input value='" + output.outputField + "' /></td><td>" + dataItem.name + "</td><td><input value='" + output.field + "' /></td><td style='width:45px'>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");			
			// get the outputfield
			var editOutputField = table.find("tr").last().children("td:nth(0)").children("input");
			// add a listener
			addListener( editOutputField.keyup( {outputs: outputs}, function(ev) {
				// get the output
				var output = $(ev.target);
				// update the field
				ev.data.outputs[output.parent().parent().index()-1].outputField = output.val();				
			}));
			// get the field
			var editField = table.find("tr").last().children("td:nth(2)").children("input");
			// add a listener
			addListener( editField.keyup( {outputs: outputs}, function(ev) {
				// get the output
				var output = $(ev.target);
				// update the field
				ev.data.outputs[output.parent().parent().index()-1].field = output.val();
			}));			
		} else {
			// remove this entry from the collection
			outputs.splice(i,1);
			// set i back 1 position
			i--;
		}			
	}
			
	// add reorder listeners
	addReorder(outputs, table.find("div.reorder"), function() { 
		Property_outputs(cell, propertyObject, property, details);
	});
	
	// add a listener
	addListener( table.find("div.delete").click( {outputs: outputs}, function(ev) {
		// get the output
		var imgDelete = $(ev.target);
		// remove from parameters
		ev.data.outputs.splice(imgDelete.closest("tr").index()-1,1);
		// remove row
		imgDelete.closest("tr").remove();
		// update the dialogue
		Property_outputs(cell, propertyObject, property, details);
	}));
	
	// add the add
	table.append("<tr><td colspan='4' style='padding:0px;'><select style='margin:0px'><option value=''>Add output...</option>" + getOutputOptions() + "</select></td></tr>");
	// find the add
	var outputAdd = table.find("tr").last().children().last().children().last();
	// listener to add output
	addListener( outputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		
		// initialise array if need be
		if (!ev.data.propertyObject[ev.data.property.key]) ev.data.propertyObject[ev.data.property.key] = [];
		// get the parameters (inputs or outputs)
		var outputs = ev.data.propertyObject[ev.data.property.key];
		// add a new one
		outputs.push({itemId: $(ev.target).val(), field: "", outputField: ""});
		// rebuild the dialogue
		Property_outputs(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);	
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
					table.append("<tr><td>" + control.name + "</td><td>" +
							"<div class='iconsPanel'>" +
							"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
							"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
							"</div></td></tr>");
				}				
			}
			
			// add reorder listeners
			addReorder(controls, table.find("div.reorder"), function() { 
				Property_controlsForType(cell, propertyObject, property, details); 
			});
			
			// add listeners to the delete image
			addListener( table.find("div.delete").click( function(ev) {
				// get the row
				var row = $(this).closest("tr");
				// remove the control
				propertyObject[property.key].splice(row.index(),1);
				// remove the row
				row.remove();
				// refresh the dialogue
				Property_controlsForType(cell, propertyObject, property, details);
			}));
			
			// start the options
			var options = "<option>add..</option>";
			// loop the classes
			for (var i in controlClasses) {
				// add to the options
				options += getControlOptions(null, null, controlClasses[i].type, true);
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
			var dialogue = getDialogue(cell, propertyObject, property, details, 200, "Child " + actionClass.name.toLowerCase() + " actions", {sizeX: true});		
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
			var addAction = table.append("<tr><td colspan='2'><span class='propertyAction' style='float:left;'>add...</span></td></tr>").find("span.propertyAction").last();
			
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
				var pasteAction = addAction.after("<span class='propertyAction' style='float:right;margin-right:5px;'>paste...</span>").next();
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
				// add the parent
				action._parent = propertyObject;
				// show the action (in actions.js)
				showAction(table, action, actions, function() { Property_childActionsForType(cell, propertyObject, property, details); });
			}	
			
			// add reorder listeners
			addReorder(actions, table.find("div.reorder"), function() { Property_childActionsForType(cell, propertyObject, property, details); });
			
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

//this is a dialogue to specify the inputs, sql, and outputs for the database action
function Property_databaseQuery(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 800, "Query", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// make sure its empty
	table.children().remove();
	// remove the class for the master table
	table.parent().removeClass("dialogueTable");
		
	// initialise the query object if need be
	if (!propertyObject[property.key]) propertyObject[property.key] = {inputs:[], databaseConnectionIndex: 0, outputs:[]};
	// get the query
	var query = propertyObject[property.key];
	// get the sql into a variable
	var text = query.SQL;
	// change to message if not provided
	if (!text) text = "Click to define...";
	// put the elipses in the cell
	cell.text(text);
	
	// add inputs table, sql, and outputs table
	table.append("<tr>" +
				 "<td style='padding:0px;vertical-align:top;width:30%;'>" +
				 "<div class='overflowDiv'>" + 
				 "<table class='dialogueTable inputs inputTable'><tr><td></td><td><b>Input</b></td><td><b>Field</b></td><td></td></tr></table></div>" +
				 "</td>" +
				 "<td id='" + dialogue.attr("id") + "_dbTextAreaCell' style='vertical-align:top;padding:2px 10px 0 10px;'>" +
				 "<b>SQL</b><br/>" +
				 "</td>" +
				 "<td style='padding:0px;vertical-align:top;width:30%;'>" +
				 "<div class='overflowDiv'>" + 
				 "<table  class='dialogueTable outputs outputTable'><tr><td><b>Field</b></td><td><b>Output</b></td><td></td></tr></table></div>" +
				 "</td></tr>");
	
	// get the query cell
	var queryCell = document.getElementById(dialogue.attr("id") + "_dbTextAreaCell");
	// get the sql editor
	var sqlEditor = getCodeMirror(queryCell, "sql");
	
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
		inputsTable.append("<tr><td style='text-align:center;'>" + (+i + 1) + ".</td><td>" + (query.multiRow && i > 0 ? "&nbsp;" : itemName) + "</td><td><input value='" + escapeApos(field) + "' /></td><td style='width:45px;'>" +
				   "<div class='iconsPanel'>" +
				   "<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				   "<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
				   "</div></td></tr>");
		
		// get the field input
		var fieldInput = inputsTable.find("tr").last().find("input");
		// add a listener
		addListener( fieldInput.keyup( {parameters: query.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
	}
	// add reorder listeners
	addReorder(query.inputs, inputsTable.find("div.reorder"), function() { 
		Property_databaseQuery(cell, propertyObject, property); 
	});
	// get the delete
	var fieldDelete = inputsTable.find("div.delete");
	// add a listener
	addListener( fieldDelete.click( {parameters: query.inputs}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.parameters.splice(input.closest("tr").index()-1,1);
		// remove row
		input.closest("tr").remove();
		// refresh the query so that the reorder collection is synchronised and the numbers are updated
		Property_databaseQuery(cell, propertyObject, property); 
	}));
	
	// if multi row and at least one input
	if (query.multiRow && query.inputs && query.inputs.length > 0) {
		// add the add input linke
		inputsTable.append("<tr><td style='padding:0px;' colspan='3'><span class='propertyAction' style='padding-left:5px;'>add input</span></td><td>&nbsp;</td></tr>");
		// find the input add
		var inputAdd = inputsTable.find("span.propertyAction").last();
		// listener to add input
		addListener( inputAdd.click( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
			// get the input parameters
			var inputs = ev.data.propertyObject[property.key].inputs;
			// add a new one
			inputs.push({itemId: inputs[0].itemId, field: ""});
			// rebuild the dialogue
			Property_databaseQuery(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
		}));
	} else {
		// add the add input select
		inputsTable.append("<tr><td style='padding:0px;' colspan='3'><select style='margin:0px'><option value=''>add input...</option>" + getInputOptions() + "</select></td><td></td></tr>");
		// find the input add
		var inputAdd = inputsTable.find("tr").last().children().first().children("select");
		// listener to add input
		addListener( inputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
			// initialise array if need be
			if (!ev.data.propertyObject[property.key].inputs) ev.data.propertyObject[property.key].inputs = [];
			// get the parameters (inputs or outputs)
			var parameters = ev.data.propertyObject[property.key].inputs;
			// add a new one
			parameters.push({itemId: $(ev.target).val(), field: ""});
			// rebuild the dialgue
			Property_databaseQuery(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
		}));
	}
			
	//set the value of the sqlEditor or empty if null
	sqlEditor.setValue((query.SQL || ""));
	sqlEditor.on("keyup", function (){
		query.SQL = sqlEditor.getValue();
	});
	
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
		outputsTable.append("<tr><td><input value='" + escapeApos(field) + "' /></td><td>" + itemName + "</td><td style='width:45px;'>" +
				"<div class='iconsPanel'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
				"</div></td></tr>");
		// get the field input
		var fieldOutput = outputsTable.find("tr").last().children().first().children().last();
		// add a listener
		addListener( fieldOutput.keyup( {parameters: query.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
					
	}
	// add reorder listeners
	addReorder(query.outputs, outputsTable.find("div.reorder"), function() { 
		Property_databaseQuery(cell, propertyObject, property); 
	});
	// get the delete
	var fieldDelete = outputsTable.find("div.delete");
	// add a listener
	addListener( fieldDelete.click( {parameters: query.outputs}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.parameters.splice(input.closest("tr").index()-1,1);
		// remove row
		input.closest("tr").remove();
		// refresh the query so that the reorder collection is synchronised
		Property_databaseQuery(cell, propertyObject, property); 
	}));
	// add the add
	outputsTable.append("<tr><td style='padding:0px;' colspan='2'><select class='addOutput' style='margin:0px'><option value=''>add output...</option>" + getOutputOptions() + "</select></td><td></td></tr>");
	// find the output add
	var outputAdd = outputsTable.find("select.addOutput");
	// listener to add output
	addListener( outputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject[property.key].outputs) ev.data.propertyObject[property.key].outputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject[property.key].outputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_databaseQuery(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);	
	}));
	
	// assume no database connection drop down in dialogue
	var databaseConnection = "";
		
	// check there is a parent (database) property object
	if (propertyObject._parent) {
		// set this DatabaseConnectionIndex to the parent one
		query.databaseConnectionIndex = propertyObject._parent[property.key].databaseConnectionIndex;
	} else {
		// build this databaseConnection html
		databaseConnection = "Database connection <select style='width:auto;margin:0 10px 5px 0'>" + getDatabaseConnectionOptions(query.databaseConnectionIndex) + "</select>";
	}
	
	// add the multi-row, database connection, and test button
	table.append("<tr><td>Multi-row input data?&nbsp;<input class='multi' type='checkbox'" + (query.multiRow ? "checked='checked'" : "" ) + " style='vertical-align: middle;margin-top: -3px;'/></td>" +
			"<td style='text-align: left;overflow:inherit;padding:0 10px;'>" + databaseConnection + "<button style='float:right;'>Test SQL</button></td></tr>");
	
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

// database child actions like success and fail do not go against child database actions
function Property_databaseNotChildCheckbox(cell, propertyObject, property, details) {
	// look for any dialogue header text
	var dialogueId = cell.closest(".propertyDialogue").attr("id");
	// only if this is not a child database action
	if (!dialogueId || dialogueId.indexOf("childDatabaseActions") < 0) {
		// use the standard child actions as anything is allowed
		Property_checkbox(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}	
}

//database child actions like success and fail do not go against child database actions
function Property_databaseNoChildrenCheckbox(cell, propertyObject, property, details) {
	// look for any dialogue header text
	var dialogueId = cell.closest(".propertyDialogue").attr("id");
	// only if this is not a child database action
	if ((!dialogueId || dialogueId.indexOf("childDatabaseActions")) && propertyObject.childDatabaseActions && propertyObject.childDatabaseActions.length > 0) {
		// use the standard child actions as anything is allowed
		Property_checkbox(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}	
}

// reuse the generic childActionsForType but set the details with type = database
function Property_databaseChildActions(cell, propertyObject, property, details) {
	// look for any dialogue header text
	var dialogueId = cell.closest(".propertyDialogue").attr("id");
	// only if this is not a child database action
	if (!dialogueId || dialogueId.indexOf("childDatabaseActions") < 0) {
		// specify that only database actions are allowed here
		Property_childActionsForType(cell, propertyObject, property, {type:"database"});
	} else {
		// remove this row
		cell.closest("tr").remove();
	}	
}

//database child actions like success and fail do not go against child database actions
function Property_databaseNotChildActions(cell, propertyObject, property, details) {
	// look for any dialogue header text
	var dialogueId = cell.closest(".propertyDialogue").attr("id");
	// only if this is not a child database action
	if (!dialogueId || dialogueId.indexOf("childDatabaseActions") < 0) {
		// use the standard child actions as anything is allowed
		Property_childActions(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}	
}

//this is a dialogue to specify the inputs, post body, and outputs for the webservice action
function Property_webserviceRequest(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 1000, "Webservice request", {sizeX: true, minWidth: 750});		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// make sure its empty
	table.children().remove();
	// remove the class for the master table
	table.parent().removeClass("dialogueTable");
	
	// initialise the request object if need be
	if (!propertyObject.request) propertyObject.request = {inputs:[], type:"SOAP", url: '', action: 'demo/Sample SQL webservice', body: '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:soa="http://soa.rapid-is.co.uk">\n  <soapenv:Body>\n    <soa:personSearchRequest>\n      <soa:surname>A</soa:surname>\n    </soa:personSearchRequest>\n  </soapenv:Body>\n</soapenv:Envelope>', outputs:[]};
	// get the request
	var request = propertyObject.request;
	// get the sql into a variable
	var text = request.type;
	// change to message if not provided
	if (!text) text = "Click to define...";
	// change TEXT to Text
	if (text == "TEXT") text = "Text";
	// put the elipses in the cell
	cell.text(text);
	
	// derive the id for this dialogue
	var dialogueId = propertyObject.id + property.key;
	// add inputs table, body, and outputs table
	table.append("<tr>" +
			     "<td style='padding:0px;vertical-align:top;width:35%;'>" + 
			     "<table class='dialogueTable inputTable'><tr><td></td><td><b>Input</b></td><td><b>Field</b></td><td></td></tr></table>" +
			     "</td>" +
			     "<td class='normalInputs' style='padding:0 6px;'><b style='display:block;'>Request type</b>" + 
			     "<input type='radio' name='WSType" + propertyObject.id + "' id='WSTypeSOAP" + propertyObject.id + "' value='SOAP'/><label for='WSTypeSOAP" + propertyObject.id + "'>SOAP</label>" + 
			     "<input type='radio' name='WSType" + propertyObject.id + "' id='WSTypeJSON" + propertyObject.id + "' value='JSON'/><label for='WSTypeJSON" + propertyObject.id + "'>JSON</label>" + 
			     "<input type='radio' name='WSType" + propertyObject.id + "' id='WSTypeXML" + propertyObject.id + "' value='XML'/><label for='WSTypeXML" + propertyObject.id + "'>XML</label>" + 
			     "<input type='radio' name='WSType" + propertyObject.id + "' id='WSTypeTEXT" + propertyObject.id + "' value='TEXT'/><label for='WSTypeTEXT" + propertyObject.id + "'>Text</label>" + 
			     "<b style='display:block;margin-top:5px;margin-bottom:5px;'>URL</b><input class='WSUrl' /></br><b style='display:block;margin-top:5px;margin-bottom:5px;'>Action</b><input class='WSAction' /></br><b style='display:block;margin-top:5px;margin-bottom:5px;'>Headers</b><input class='WSHeaders' />" +
			     "<b style='display:block;margin-top:5px;margin-bottom:2px;'>Body</b>" +
			     "<div id='bodySOA_" + dialogueId + "' style='width:100%;' class='WSBody'></div><b style='display:block;'>Response transform</b><textarea style='width:100%;' class='WSTransform'></textarea><b style='display:block;;margin-bottom:5px;'>Response root element</b><input class='WSRoot' style='margin-bottom:5px;' /></td>"  + 
			     "<td style='padding:0px;vertical-align:top;width:35%;'>" + 
			     "<table class='dialogueTable outputTable'><tr><td><b>Field</b></td><td><b>Output</b></td><td></td></tr></table>"
			     +"</td></tr>");
	
	//append the 
	var bodySOA = document.getElementById('bodySOA_' + dialogueId);
	
	//create an editor instance, and append it to the bodySOA
	var myCodeEditor = getCodeMirror(bodySOA, "xml");
	
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
		inputsTable.append("<tr><td style='text-align:center;'>" + (+i + 1) + ".</td><td>" + itemName + "</td><td><input value='" + escapeApos(field) + "' /></td><td style='width:45px;'>" +
				"<div class='iconsPanel'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
				"</div></td></tr>");
		
		// get the field input
		var fieldInput = inputsTable.find("tr").last().find("input");
		// add a listener
		addListener( fieldInput.keyup( {parameters: request.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));		
	}
	// add reorder listeners
	addReorder(request.inputs, inputsTable.find("div.reorder"), function() { 
		Property_webserviceRequest(cell, propertyObject, property); 
	});
	// add delete listeners
	addListener( inputsTable.find("div.delete").click( {parameters: request.inputs}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.parameters.splice(input.closest("tr").index()-1,1);
		// remove row
		input.closest("tr").remove();
		// update property so collection is sychcronised for reordering
		Property_webserviceRequest(cell, propertyObject, property); 
	}));
	// add the add input
	inputsTable.append("<tr><td style='padding:0px;' colspan=3><select style='margin:0;'><option value=''>Add input...</option>" + getInputOptions() + "</select></td><td></td></tr>");
	// find the input add
	var inputAdd = inputsTable.find("tr").last().find("select");
	// listener to add input
	addListener( inputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.request.inputs) ev.data.propertyObject.request.inputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.request.inputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialogue
		Property_webserviceRequest(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
	}));
	
	// find the type radios
	var typeControls = table.find("input[type=radio]");	
	// listener for click on any of them
	addListener( typeControls.click( {request: request}, function(ev) {
		ev.data.request.type = $(ev.target).val();
	}));
	// set the value of the one matching the type
	typeControls.filter("[value=" + request.type + "]").prop("checked",true).attr("checked","checked");
	
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
	
	// find the headers input box
	var actionControl = table.find("input.WSHeaders");
	actionControl.val(request.headers);
	// listener for the action
	addListener( actionControl.keyup( {request: request}, function(ev) {
		ev.data.request.headers = $(ev.target).val();
	}));
	
	// find the request body textarea - i.e. codeEditor
	myCodeEditor.setValue(request.body);
	// listener for the body
	myCodeEditor.on("keyup",function(ev) {
		request.body = myCodeEditor.getValue();
	});
	
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
		outputsTable.append("<tr><td><input value='" + escapeApos(field) + "' /></td><td>" + itemName + "</td><td style='width:45px;'>" +
				"<div class='iconsPanel'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
				"</div></td></tr>");
		// get the field input
		var fieldOutput = outputsTable.find("tr").last().children().first().children().last();
		// add a listener
		addListener( fieldOutput.keyup( {parameters: request.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));		
	}
	// add reorder listeners
	addReorder(request.outputs, outputsTable.find("div.reorder"), function() { 
		Property_webserviceRequest(cell, propertyObject, property, details); 
	});
	// add delete listeners
	addListener( outputsTable.find("div.delete").click( {parameters: request.outputs}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.parameters.splice(input.closest("tr").index()-1,1);
		// remove row
		input.closest("tr").remove();
		// rebuild the dialogue to sychronise reorders
		Property_webserviceRequest(cell, propertyObject, property, details);
	}));
	// add the add
	outputsTable.append("<tr><td style='padding:0px;' colspan=2><select style='margin:0px'><option value=''>Add output...</option>" + getOutputOptions() + "</select></td><td>&nbsp;</td></tr>");
	// find the output add
	var outputAdd = outputsTable.find("tr").last().find("select");
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
				table.append("<tr><td><input class='variable' value='" + escapeApos(variables[i]) + "' /></td><td style='width:25px; text-align:right;'>" +
						"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div></td></tr>");
				
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
				var optionDelete = table.find("div.delete");
				// add a listener
				addListener( optionDelete.click( {variables: variables}, function(ev) {
					// add an undo snapshot
					addUndo();
					// get the input
					var input = $(ev.target);
					// remove from parameters
					ev.data.variables.splice(input.closest("tr").index(),1);
					// remove row
					input.closest("tr").remove();
				}));
			}
				
			// have an add row
			table.append("<tr><td colspan='2'><span class='propertyAction'>add...</span></td></tr>");
			// get a reference to the add
			var add = table.find("span.propertyAction").last();
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
	// add the text clip class
	table.addClass("dialogueTextClip");
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
		table.append("<tr><td>" + roles[i] + "</td><td style='width:25px; text-align:right;'>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div></td></tr>");
						
		// find the delete
		var optionDelete = table.find("div.delete");
		// add a listener
		addListener( optionDelete.click( {roles: roles, cell: cell, control: control, property: property, details: details}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.roles.splice(input.closest("tr").index(),1);
			// remove row
			input.closest("tr").remove();
			// refresh
			Property_roles(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);
		}));
	}
		
	// get roles options html
	var rolesOptions = getRolesOptions(null, roles);
	// only if there are some to add
	if (rolesOptions) {
		
		// have an add dropdown
		table.append("<tr><td colspan='2'><select><option value=''>add...</option>" + rolesOptions + "</td></tr>");
		// get a reference to the add
		var add = table.find("tr").last().find("select");
		// if there are value
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

}

function Property_navigateDialogue(cell, propertyObject, property, details) {
	// this is some reuse in the link control - if it's type isn't P for page
	if ((propertyObject.navigationType && propertyObject.navigationType != "P") || (propertyObject.linkType && propertyObject.linkType != "P")) {
		// remove this row
		cell.parent().remove();
		// stop going any further
		return false;
	}
	
	Property_checkbox(cell, propertyObject, property, details);
}

function Property_navigateNewtab(cell, propertyObject, property, details) {
	//if property 'show as dialogue' is checked
	if(propertyObject.dialogue == true || propertyObject.dialogue == "true") {
		// remove this row
		cell.parent().remove();
		// stop going any further
		return false;
	}
	
	Property_checkbox(cell, propertyObject, property, details);

}

// this is a dialogue to specify the session variables to set when navigating
function Property_navigationSessionVariables(cell, navigation, property, details) {
	
	// this is some reuse in the link control - if it's type isn't P for page
	if ((navigation.navigationType && navigation.navigationType != "P") || (navigation.linkType && navigation.linkType != "P")) {
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
	// add dialogueTableNoDelete
	table.addClass("dialogueTableNoDelete");
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
		table.append("<tr>" + (control.codes ? "<td><b>Text</b></td><td><b>Code</b></td>" : "<td><b>Text</b></td>") + "<td></td></tr>");
		
		// show options
		for (var i in buttons) {
			// add the line
			table.append("<tr><td><input class='label' value='" + escapeApos(buttons[i].label) + "' /></td>" + (control.codes ? "<td><input class='value' value='" + escapeApos(buttons[i].value) + "' /></td>" : "") + "<td>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");
			
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
		if (!dialogue.find("span.propertyAction").last()[0]) {
			// have an add
			table.after("<span class='propertyAction'>add...</span>");
			// get a reference to the add
			var add = dialogue.find("span.propertyAction").last();
			// add a listener
			addListener( add.click( {cell: cell, control: control, property: property, details: details}, function(ev) {
				// add a blank option
				ev.data.control.buttons.push({value: "", label: ""});
				// refresh
				Property_radiobuttons(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);		
			}));
		}

		// add reorder listeners
		addReorder(buttons, table.find("div.reorder"), function() { 
			// refresh the html and regenerate the mappings
			rebuildHtml(control);
			// refresh the property
			Property_radiobuttons(cell, control, property, details); 
		});
		
		// find the deletes
		var buttonDelete = table.find("div.delete");
		// add a listener
		addListener( buttonDelete.click( {cell: cell, control: control, property: property, details: details}, function(ev) {
			// get the del image
			var delImage = $(ev.target);
			// remove from parameters
			ev.data.control.buttons.splice(delImage.closest("tr").index()-1,1);
			// remove row
			delImage.closest("tr").remove();
			// update html if top row
			if (delImage.parent().index() == 1) rebuildHtml(ev.data.control);
			// refresh
			Property_radiobuttons(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);
		}));
		
		// check we don't have a checkbox already
		if (!dialogue.find("div.useCodes")[0]) {
			// add checkbox
			table.after("<div class='useCodes' style='padding: 5px 0;'><label><input type='checkbox' " + (control.codes ? "checked='checked'" : "") + " /> Use codes</label></div>");
			// get a reference
			var optionsCodes = dialogue.find("div.useCodes").first();
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
			dialogue.append("Value list<br/><select><option value=''>None</option>" + getValueListsOptions(control.valueList) + "</select>");
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
		// if this is a form add the form options
		if (_version.isForm) options = getFormValueOptions(value.id);
		// add the regular input options
		options += getInputOptions(value.id);
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
	// add the dialogueTable class
	table.addClass("propertiesDialogueTable");
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
		table.append("<tr><td></td><td style='width:150px;'></td><td></td><td style='width:40px;'>" +
				"<div class='iconsPanel'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
				"</div></td></tr>");
		
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
			
	// add reorder listeners
	addReorder(conditions, table.find("div.reorder"), function() { 		
		// refresh the dialogue
		Property_logicConditions(cell, action, property, details); 
	});
	
	// find the deletes
	var deleteImages = table.find("div.delete");
	// add a listener
	addListener( deleteImages.click( {conditions: conditions}, function(ev) {
		// get the del image
		var delImage = $(ev.target);
		// remove from conditions
		ev.data.conditions.splice(delImage.closest("tr").index(),1);
		// if there are now less than 2 conditions remove and/or row too
		if (ev.data.conditions.length < 2) $(ev.target).closest("table").find("tr:last").prev().remove();
		// remove row
		delImage.closest("tr").remove();
		// refresh the dialogue
		Property_logicConditions(cell, action, property, details);
	}));
	
	// only if there are 2 or more conditions
	if (conditions.length > 1) {
		// only if the condition type div is not already there
		if (!dialogue.find("div.logicType")[0]) {
			// add type
			table.after("<div class='logicType' style='padding: 5px 0;'><label><input type='radio' name='" + action.id + "type' value='and'" + (action.conditionsType == "and" ? " checked='checked'" : "") + "/>all conditions must be true (And)</label><label><input type='radio' name='" + action.id + "type' value='or'" + (action.conditionsType == "or" ? " checked='checked'" : "") + "/>any condition can be true (Or)</div>");
			// add change listeners
			dialogue.find("input[name=" + action.id + "type]").change( function(ev){
				// set the condition type to the new val
				action.conditionsType = $(ev.target).val();
			});
		}
	} else {
		// remove any type
		dialogue.find("div.logicType").remove();
	}
	
	// if add is not there already
	if (!dialogue.find("span.propertyAction")[0]) {
		// add add
		dialogue.append("<span class='propertyAction'>add...</span>");
		// add listener
		dialogue.find("span.propertyAction").last().click( function(ev) {
			// instatiate if need be
			if (!action[property.key]) action[property.key] = [];
			// add new condition
			action[property.key].push({value1:{type:"CTL"}, operation: "==", value2: {type:"CTL"}});
			// update this table
			Property_logicConditions(cell, action, property, details);
		});
	}
	
}

// this very similar to the above but hidden if no form adapter
function Property_visibilityConditions(cell, control, property, details) {
	// if this is not a simple page
	if (_version.isForm && !_page.simple) {
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
		table.append("<tr><td><b>Text</b></td>" + (control.codes ? "<td><b>Code</b></td>" : "") + "<td></td></tr>");
		
		// show options
		for (var i in options) {
			// add the line
			table.append("<tr><td><input class='text' value='" + escapeApos(options[i].text) + "' /></td>" + (control.codes ? "<td><input class='value' value='" + escapeApos(options[i].value) + "' /></td>" : "") + "<td>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");
							
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
		
		// add reorder listeners
		addReorder(options, table.find("div.reorder"), function() { 
			// refresh the html and regenerate the mappings
			rebuildHtml(control);
			// refresh the dialogue
			Property_options(cell, control, property, details); 
		});
		
		// find the deletes
		var deleteImages = table.find("div.delete");
		// add a listener
		addListener( deleteImages.click( {options: options}, function(ev) {
			// get the del image
			var delImage = $(ev.target);
			// remove from parameters
			ev.data.options.splice(delImage.closest("tr").index()-1,1);
			// remove row
			delImage.closest("tr").remove();
			// update html if top row
			if (delImage.parent().index() == 1) rebuildHtml(control);
			// refresh the dialogue
			Property_options(cell, control, property, details);
		}));		

		// have an add row
		if (!dialogue.find("span.propertyAction")[0]) {
			// have an add
			table.after("<span class='propertyAction'>add...</span>");
			// get a reference to the add
			var add = dialogue.find("span.propertyAction").last();
			// add a listener
			addListener( add.click( {cell: cell, control: control, property: property, details: details}, function(ev) {
				// add a blank option
				ev.data.control.options.push({value: "", text: ""});
				// refresh
				Property_options(ev.data.cell, ev.data.control, ev.data.property, ev.data.details);		
			}));
		}
		
		// check we don't have a checkbox already
		if (!dialogue.find("div.useCodes")[0]) {
			// add checkbox
			table.after("<div class='useCodes' style='padding:5px 0;'><label><input type='checkbox' " + (control.codes ? "checked='checked'" : "") + " /> Use codes</label></div>");
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
// the cell content edit html
var _gridCellEditHtml = "<i class='fa fa-pencil' title='Edit function'></i>";

// this is a dialogue to refine the options available in a grid control
function Property_gridColumns(cell, grid, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, grid, property, details, 650, "Columns",{sizeX: true, minWidth: 700});		
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
		var sortSelect = "<select " + (columns[i].sort == "c" ? "style='width: 75px;'" : "" ) + ">";
		// loop the values and add
		for (var j in _gridColumnSorts) {
			sortSelect += "<option value='" + j + "'"+ (columns[i].sort == j ? " selected='selected'" : "") + ">" + _gridColumnSorts[j].text + "</option>";
		}
		// close it
		sortSelect += "</select>";
			
		// set the cellFunction text to ellipses
		var cellFunctionText = "<span title='Add cell function'>...</span>";
		// update to edit glyph if present
		if (columns[i].cellFunction && cellFunctionText != _gridCellFunctionHelpText) cellFunctionText = _gridCellEditHtml;
		
		// add the line
		table.append("<tr><td class='center'><input type='checkbox' " + (columns[i].visible ? "checked='checked'" : "")  + " /></td><td><input value='" + escapeApos(columns[i].title) + "' /></td><td><input value='" + escapeApos(columns[i].titleStyle) + "' /></td><td><input value='" + escapeApos(columns[i].field) + "' /></td><td><input value='" + escapeApos(columns[i].fieldStyle) + "' /></td><td>" + sortSelect + "&nbsp;" + _gridCellEditHtml + "</td><td class='paddingLeft5'>" + cellFunctionText + "</td><td>" +
				"<div class='iconsPanel'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Delete this column'></i></div>" +
				"</div></td></tr>");
		
		// find the visible checkbox
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
		var sortDropDown = table.find("tr").last().children(":nth(5)").first().children().first();
		// add a listener
		addListener( sortDropDown.change( {grid: grid, cell: cell, grid: grid, property: property, details: details}, function(ev) {
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
		var sortFunction = table.find("tr").last().children(":nth(5)").find("i");
		// add a listener
		addListener( sortFunction.click( {grid: grid}, function(ev) {
			// get the span
			var span = $(ev.target);
			// get the index
			var index = span.closest("tr").index()-1;
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
		var cellFunction = table.find("tr").last().children(":nth(6)").first();
		// add a listener
		addListener( cellFunction.click( {grid: grid}, function(ev) {
			// get the td
			var td = $(ev.target);
			// get the index
			var index = td.closest("tr").index()-1;
			// set the index
			textArea.attr("data-index",index);
			// set the type
			textArea.attr("data-type","f");
			// get the function text
			var cellFunctionText = ev.data.grid.columns[index].cellFunction;
			// check the text
			if (cellFunctionText && cellFunctionText != _gridCellFunctionHelpText) {
				textArea.val(cellFunctionText);
			} else {
				textArea.val(_gridCellFunctionHelpText);
			}
			// show and focus the textarea
			textArea.show().focus();
		}));
		
	}
	
	// look for the cell function text area
	var textArea = dialogue.find("textarea");
	// add one if need be
	if (!textArea[0]) textArea = dialogue.append("<textarea data-index='-1' style='position:absolute;display:none;width:500px;height:300px;top:26px;right:10px;' wrap='off'></textarea>").find("textarea:first");
	// hide it on unfocus
	addListener( textArea.blur( function(ev) {		
		// assume no html
		var cellHtml = "<span title='Add cell function'>...</span>"; 
		// get the value of the text area
		var value = textArea.val();
		// update to elipses if nothing or only help text
		if (value && value != _gridCellFunctionHelpText) cellHtml = _gridCellEditHtml;
		// get the index
		var index = textArea.attr("data-index")*1;		
		// get the type
		var type = textArea.attr("data-type");	
		// update the td text if known cell function
		if (index >= 0 && type == "f") table.find("tr:nth(" + (index + 1) + ")").last().children("td:nth(6)").html(cellHtml);
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
	
	// add reorder listeners
	addReorder(columns, table.find("div.reorder"), function() { 
		// refresh the html and regenerate the mappings
		rebuildHtml(grid);
		// refresh the dialogue
		Property_gridColumns(cell, grid, property, details); 
	});
	
	// add delete listeners
	var deleteImages = table.find("div.delete");
	// add a listener
	addListener( deleteImages.click( {columns: columns}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.columns.splice(input.closest("tr").index()-1,1);
		// remove row
		input.closest("tr").remove();
		// refresh the html and regenerate the mappings
		rebuildHtml(grid);
		// refresh the dialogue
		Property_gridColumns(cell, grid, property, details);
	}));
	
	// have an add row
	table.append("<tr><td colspan='5'><span class='propertyAction'>add...</span></td></tr>");
	// get a reference to the add
	var add = table.find("span.propertyAction").last();
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
		// add the text property
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
		// add the text property
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
		// add the checkbox property
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
	// add class for hint text cell cleanup
	table.addClass("dialogueTableWhiteBackgroundText");
	// make sure table is empty
	table.children().remove();
	
	var text = "";
	// get the hint controls
	var controlHints = hints.controlHints;
	// if they don't exist or an empty array string make them an empty array
	if (!controlHints || controlHints == "[]") controlHints = [];
	
	// add a header
	table.append("<tr><td><b>Control</b></td><td><b>Action</b></td><td style='min-width:150px;max-width:150px;'><b>Hint text</b></td><td><b>Style</b></td><td></td></td></tr>");
		
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
		table.append("<tr class='nopadding'><td><select class='control'><option value=''>Please select...</option>" + getControlOptions(controlHint.controlId) + "</select></td><td><select class='type'>" + typeOptions + "</select></td><td style='max-width:150px;'><span>" + controlHint.text + "</span></td><td><input value='" + escapeApos(controlHint.style) + "'/></td><td style='width:45px;'>" +
				"<div class='iconsPanel'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
				"</div></td></tr>");
	
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
	texts.each( function() {		
		// get a reference to the span
		var span = $(this);
		// add a bigtext property for it
		Property_bigtext(span.parent(), controlHints[span.parent().parent().index()-1], {key: "text"});		
	});
	
	// add style listeners
	var styles = table.find("input");
	// add a listener
	addListener( styles.keyup( {controlHints: controlHints}, function(ev) {
		// get the input
		var input = $(ev.target);
		// update the control id
		ev.data.controlHints[input.parent().parent().index()-1].style = input.val();
	}));

	// add reorder listeners
	addReorder(controlHints, table.find("div.reorder"), function() { 
		// refresh the html and regenerate the mappings
		rebuildHtml(hints);
		// refresh the dialogue
		Property_controlHints(cell, hints, property, details);
	});
	
	// add delete listeners
	var deleteImages = table.find("div.delete");
	// add a listener
	addListener( deleteImages.click( {controlHints: controlHints}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.controlHints.splice(input.closest("tr").index()-1,1);
		// remove row
		input.closest("tr").remove();
		// refresh the html and regenerate the mappings
		rebuildHtml(controlHints);
		// refresh the dialogue
		Property_controlHints(cell, hints, property, details);
	}));
		
	// have an add link
	if (!dialogue.find("span.propertyAction")[0]) {
		table.after("<span class='propertyAction'>add...</span>");
		// get a reference to the add
		var add = dialogue.find("span.propertyAction").last();
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

	// only if datacopyAction type is not bulk, nor a date
	if (datacopyAction.copyType == "bulk" || (datacopyAction.dataSource && datacopyAction.dataSource.indexOf("Datetime.") == 0)) {
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
				table.append("<tr><td>" + dataItem.name + "</td><td><input value='" + dataDestination.field + "' /></td><td style='width:45px'>" +
						"<div class='iconsPanel'>" +
						"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
						"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
						"</div></td></tr>");
				// get the field
				var editField = table.find("tr").last().children("td:nth(1)").children("input");
				// add a listener
				addListener( editField.keyup( {dataDestinations: dataDestinations}, function(ev) {
					// get the input
					var editField = $(ev.target);
					// update the field
					ev.data.dataDestinations[editField.parent().parent().index()-1].field = editField.val();
				}));				
			} else {
				// remove this entry from the collection
				dataDestinations.splice(i,1);
				// set i back 1 position
				i--;
			}			
		}
				
		// add reorder listeners
		addReorder(dataDestinations, table.find("div.reorder"), function() { 
			Property_datacopyDestinations(cell, propertyObject, property, details);
		});
		
		// get the delete images
		var imgDelete = table.find("div.delete");
		// add a listener
		addListener( imgDelete.click( {dataDestinations: dataDestinations}, function(ev) {
			// get the input
			var imgDelete = $(ev.target);
			// remove from parameters
			ev.data.dataDestinations.splice(imgDelete.closest("tr").index()-1,1);
			// remove row
			imgDelete.closest("tr").remove();
			// refresh the dialogue
			Property_datacopyDestinations(cell, propertyObject, property, details);
		}));
		
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

function getDataCopyFieldFromControl(id) {
	// assume empty string
	var field = "";
	// get control
	var control = getControlById(id);
	// if we got one
	if (control) {
		// start with the name
		field = control.name;
		// if there are enough letters
		if (field.length > 3) {
			// loop the letters from 3rd
			for (var i = 2; i < field.length - 1; i ++) {
				// get first upper case letter
				if (field[i] == field[i].toUpperCase()) {
					// get rest of field
					var f = field.substring(i + 1);
					// if would all be upper case
					if (f == f.toUpperCase()) {
						// lower case the lot
						field = field.substring(i).toLowerCase();
					} else {
						// change first letter to lower case and use the rest
						field = field[i].toLowerCase() + f;
					}
					// we're done
					break;
				}
			}
		}		
	}
	// return
	return field;
}

function Property_datacopyCopies(cell, datacopyAction, property, details) {

	// only if datacopyAction type is bulk
	if (datacopyAction.copyType == "bulk") {	
		
		// retrieve or create the dialogue
		var dialogue = getDialogue(cell, datacopyAction, property, details, 710, "Bulk data copies", {sizeX: true});		
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
		table.append("<tr><td><b>Source</b><button class='titleButton setSources' title='Set all sources to the same as the top one'><span>&#xf063;</span></button><button class='titleButton sources' title='Add all page controls as sources'><span>&#xf055;</span></button></td><td><b>Source field</b><button class='titleButton sourceFields' title='Derive source field from destination control name'><span>&#xf021;</span></button></td><td><b>Destination</b><button class='titleButton setDestinations' title='Set all destinations to the same as the top one'><span>&#xf063;</span></button><button class='titleButton destinations' title='Add all page controls as destinations'><span>&#xf055;</span></button></td><td><b>Destination field</b><button class='titleButton destinationFields' title='Derive destination field from source control name'><span>&#xf021;</span></button></td><td><b>Copy type</b><button class='titleButton setCopyTypes' title='Set all copy types to the same as the top one'><span>&#xf063;</span></button></td><td><button class='titleButton swap' title='Swap all source and destinations'><span>&#xf0ec;</span></button></td></tr>");
			
		// add sources listener
		addListener( table.find("button.sources").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {
			// add an undo snapshot
			addUndo();
			// bring in all source controls
			getPageControlsBulkCopies(ev.data.datacopyAction, true);
			// refresh
			Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
		}));
		
		// set sources listener
		addListener( table.find("button.setSources").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {			
			// get the data copies
			var dataCopies = ev.data.datacopyAction[ev.data.property.key];
			// if there are 2 or more copies
			if (dataCopies && dataCopies.length > 1) {
				// add an undo snapshot
				addUndo();
				// loop all other sources and set
				for (var i = 1; i < dataCopies.length; i++) dataCopies[i].source = dataCopies[0].source;
				// refresh
				Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
			}						
		}));
		
		// set source fields listener
		addListener( table.find("button.sourceFields").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {			
			// get the data copies
			var dataCopies = ev.data.datacopyAction[ev.data.property.key];
			// if there are copies
			if (dataCopies && dataCopies.length > 0) {
				// add an undo snapshot
				addUndo();
				// loop all copies sources and set
				for (var i = 0; i < dataCopies.length; i++) dataCopies[i].sourceField = getDataCopyFieldFromControl(dataCopies[i].destination);
				// refresh
				Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
			}						
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
		
		// set destinations listener
		addListener( table.find("button.setDestinations").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {			
			// get the data copies
			var dataCopies = ev.data.datacopyAction[ev.data.property.key];
			// if there are 2 or more copies
			if (dataCopies && dataCopies.length > 1) {
				// add an undo snapshot
				addUndo();
				// loop all other sources and set
				for (var i = 1; i < dataCopies.length; i++) dataCopies[i].destination = dataCopies[0].destination;
				// refresh
				Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
			}						
		}));
		
		// set destination fields listener
		addListener( table.find("button.destinationFields").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {			
			// get the data copies
			var dataCopies = ev.data.datacopyAction[ev.data.property.key];
			// if there are copies
			if (dataCopies && dataCopies.length > 0) {
				// add an undo snapshot
				addUndo();
				// loop all copies sources and set
				for (var i = 0; i < dataCopies.length; i++) dataCopies[i].destinationField = getDataCopyFieldFromControl(dataCopies[i].source);
				// refresh
				Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
			}						
		}));
		
		// set copy types listener
		addListener( table.find("button.setCopyTypes").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {			
			// get the data copies
			var dataCopies = ev.data.datacopyAction[ev.data.property.key];
			// if there are 2 or more copies
			if (dataCopies && dataCopies.length > 1) {
				// add an undo snapshot
				addUndo();
				// loop all other sources and set
				for (var i = 1; i < dataCopies.length; i++) dataCopies[i].type = dataCopies[0].type;
				// refresh
				Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
			}						
		}));
		
		// set swap listener
		addListener( table.find("button.swap").click( {cell:cell, datacopyAction:datacopyAction, property:property}, function(ev) {			
			// get the data copies
			var dataCopies = ev.data.datacopyAction[ev.data.property.key];
			// if there are copies
			if (dataCopies && dataCopies.length > 0) {
				// add an undo snapshot
				addUndo();
				// loop all copies sources and swap
				for (var i = 0; i < dataCopies.length; i++) {
					// store current source
					var source = dataCopies[i].source;
					var sourceField = dataCopies[i].sourceField;
					// overwrite source with destination
					dataCopies[i].source = dataCopies[i].destination;
					dataCopies[i].sourceField = dataCopies[i].destinationField;
					// set destination from original source
					dataCopies[i].destination = source;
					dataCopies[i].destinationField = sourceField;
				}
				// refresh
				Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, ev.data.property); 
			}						
		}));
		
		// show current choices (with delete and move)
		for (var i = 0; i < dataCopies.length; i++) {
			
			// get this data copy
			var dataCopy = dataCopies[i];
						
			// add a row
			table.append("<tr><td><select class='source'><option value=''>Please select...</option>" + getInputOptions(dataCopy.source) + "</select></td><td><input  class='source' value='" + escapeApos(dataCopy.sourceField) + "' /></td><td><select class='destination'><option value=''>Please select...</option>" + getOutputOptions(dataCopy.destination) + "</select></td><td><input class='destination' value='" + escapeApos(dataCopy.destinationField) + "' /></td><td><select class='type' style='min-width:80px;'>" + getCopyTypeOptions(dataCopy.type) + "</select></td><td style='width:45px'>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");
			
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
		
		// add reorder listeners
		addReorder(dataCopies, table.find("div.reorder"), function() { 
			Property_datacopyCopies(cell, datacopyAction, property);
		});
		
		// get the delete images
		var imgDelete = table.find("div.delete");
		// add a listener
		addListener( imgDelete.click( {dataCopies: dataCopies}, function(ev) {
			// get the input
			var imgDelete = $(ev.target);
			// remove from parameters
			ev.data.dataCopies.splice(imgDelete.closest("tr").index()-1,1);
			// remove row
			imgDelete.closest("tr").remove();
			// update dialogue
			Property_datacopyCopies(cell, datacopyAction, property);
		}));

		// add the add
		table.append("<tr><td colspan='8'><span class='propertyAction' style='margin-left:5px;'>add...</span></td></tr>");
		// find the add
		var destinationAdd = table.find("span.propertyAction").last();
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

var _fontGlyphs = [["","none"],["&#xe900","rapid"],["&#xf042;","adjust"],["&#xf170;","adn"],["&#xf037;","align-center"],["&#xf039;","align-justify"],["&#xf036;","align-left"],["&#xf038;","align-right"],["&#xf0f9;","ambulance"],["&#xf13d;","anchor"],["&#xf17b;","android"],["&#xf209;","angellist"],["&#xf103;","angle-double-down"],["&#xf100;","angle-double-left"],["&#xf101;","angle-double-right"],["&#xf102;","angle-double-up"],["&#xf107;","angle-down"],["&#xf104;","angle-left"],["&#xf105;","angle-right"],["&#xf106;","angle-up"],["&#xf179;","apple"],["&#xf187;","archive"],["&#xf1fe;","area-chart"],["&#xf0ab;","arrow-circle-down"],["&#xf0a8;","arrow-circle-left"],["&#xf01a;","arrow-circle-o-down"],["&#xf190;","arrow-circle-o-left"],["&#xf18e;","arrow-circle-o-right"],["&#xf01b;","arrow-circle-o-up"],["&#xf0a9;","arrow-circle-right"],["&#xf0aa;","arrow-circle-up"],["&#xf063;","arrow-down"],["&#xf060;","arrow-left"],["&#xf061;","arrow-right"],["&#xf047;","arrows"],["&#xf0b2;","arrows-alt"],["&#xf07e;","arrows-h"],["&#xf07d;","arrows-v"],["&#xf062;","arrow-up"],["&#xf069;","asterisk"],["&#xf1fa;","at"],["&#xf04a;","backward"],["&#xf05e;","ban"],["&#xf080;","bar-chart"],["&#xf02a;","barcode"],["&#xf0c9;","bars"],["&#xf0fc;","beer"],["&#xf1b4;","behance"],["&#xf1b5;","behance-square"],["&#xf0f3;","bell"],["&#xf0a2;","bell-o"],["&#xf1f6;","bell-slash"],["&#xf1f7;","bell-slash-o"],["&#xf206;","bicycle"],["&#xf1e5;","binoculars"],["&#xf1fd;","birthday-cake"],["&#xf171;","bitbucket"],["&#xf172;","bitbucket-square"],["&#xf032;","bold"],["&#xf0e7;","bolt"],["&#xf1e2;","bomb"],["&#xf02d;","book"],["&#xf02e;","bookmark"],["&#xf097;","bookmark-o"],["&#xf0b1;","briefcase"],["&#xf15a;","btc"],["&#xf188;","bug"],["&#xf1ad;","building"],["&#xf0f7;","building-o"],["&#xf0a1;","bullhorn"],["&#xf140;","bullseye"],["&#xf207;","bus"],["&#xf1ec;","calculator"],["&#xf073;","calendar"],["&#xf133;","calendar-o"],["&#xf030;","camera"],["&#xf083;","camera-retro"],["&#xf1b9;","car"],["&#xf0d7;","caret-down"],["&#xf0d9;","caret-left"],["&#xf0da;","caret-right"],["&#xf150;","caret-square-o-down"],["&#xf191;","caret-square-o-left"],["&#xf152;","caret-square-o-right"],["&#xf151;","caret-square-o-up"],["&#xf0d8;","caret-up"],["&#xf20a;","cc"],["&#xf1f3;","cc-amex"],["&#xf1f2;","cc-discover"],["&#xf1f1;","cc-mastercard"],["&#xf1f4;","cc-paypal"],["&#xf1f5;","cc-stripe"],["&#xf1f0;","cc-visa"],["&#xf0a3;","certificate"],["&#xf127;","chain-broken"],["&#xf00c;","check"],["&#xf058;","check-circle"],["&#xf05d;","check-circle-o"],["&#xf14a;","check-square"],["&#xf046;","check-square-o"],["&#xf13a;","chevron-circle-down"],["&#xf137;","chevron-circle-left"],["&#xf138;","chevron-circle-right"],["&#xf139;","chevron-circle-up"],["&#xf078;","chevron-down"],["&#xf053;","chevron-left"],["&#xf054;","chevron-right"],["&#xf077;","chevron-up"],["&#xf1ae;","child"],["&#xf111;","circle"],["&#xf10c;","circle-o"],["&#xf1ce;","circle-o-notch"],["&#xf1db;","circle-thin"],["&#xf0ea;","clipboard"],["&#xf017;","clock-o"],["&#xf0c2;","cloud"],["&#xf0ed;","cloud-download"],["&#xf0ee;","cloud-upload"],["&#xf121;","code"],["&#xf126;","code-fork"],["&#xf1cb;","codepen"],["&#xf0f4;","coffee"],["&#xf013;","cog"],["&#xf085;","cogs"],["&#xf0db;","columns"],["&#xf075;","comment"],["&#xf0e5;","comment-o"],["&#xf086;","comments"],["&#xf0e6;","comments-o"],["&#xf14e;","compass"],["&#xf066;","compress"],["&#xf1f9;","copyright"],["&#xf09d;","credit-card"],["&#xf125;","crop"],["&#xf05b;","crosshairs"],["&#xf13c;","css3"],["&#xf1b2;","cube"],["&#xf1b3;","cubes"],["&#xf0f5;","cutlery"],["&#xf1c0;","database"],["&#xf1a5;","delicious"],["&#xf108;","desktop"],["&#xf1bd;","deviantart"],["&#xf1a6;","digg"],["&#xf192;","dot-circle-o"],["&#xf019;","download"],["&#xf17d;","dribbble"],["&#xf16b;","dropbox"],["&#xf1a9;","drupal"],["&#xf052;","eject"],["&#xf141;","ellipsis-h"],["&#xf142;","ellipsis-v"],["&#xf1d1;","empire"],["&#xf0e0;","envelope"],["&#xf003;","envelope-o"],["&#xf199;","envelope-square"],["&#xf12d;","eraser"],["&#xf153;","eur"],["&#xf0ec;","exchange"],["&#xf12a;","exclamation"],["&#xf06a;","exclamation-circle"],["&#xf071;","exclamation-triangle"],["&#xf065;","expand"],["&#xf08e;","external-link"],["&#xf14c;","external-link-square"],["&#xf06e;","eye"],["&#xf1fb;","eyedropper"],["&#xf070;","eye-slash"],["&#xf09a;","facebook"],["&#xf082;","facebook-square"],["&#xf049;","fast-backward"],["&#xf050;","fast-forward"],["&#xf1ac;","fax"],["&#xf182;","female"],["&#xf0fb;","fighter-jet"],["&#xf15b;","file"],["&#xf1c6;","file-archive-o"],["&#xf1c7;","file-audio-o"],["&#xf1c9;","file-code-o"],["&#xf1c3;","file-excel-o"],["&#xf1c5;","file-image-o"],["&#xf016;","file-o"],["&#xf1c1;","file-pdf-o"],["&#xf1c4;","file-powerpoint-o"],["&#xf0c5;","files-o"],["&#xf15c;","file-text"],["&#xf0f6;","file-text-o"],["&#xf1c8;","file-video-o"],["&#xf1c2;","file-word-o"],["&#xf008;","film"],["&#xf0b0;","filter"],["&#xf06d;","fire"],["&#xf134;","fire-extinguisher"],["&#xf024;","flag"],["&#xf11e;","flag-checkered"],["&#xf11d;","flag-o"],["&#xf0c3;","flask"],["&#xf16e;","flickr"],["&#xf0c7;","floppy-o"],["&#xf07b;","folder"],["&#xf114;","folder-o"],["&#xf07c;","folder-open"],["&#xf115;","folder-open-o"],["&#xf031;","font"],["&#xf04e;","forward"],["&#xf180;","foursquare"],["&#xf119;","frown-o"],["&#xf1e3;","futbol-o"],["&#xf11b;","gamepad"],["&#xf0e3;","gavel"],["&#xf154;","gbp"],["&#xf06b;","gift"],["&#xf1d3;","git"],["&#xf09b;","github"],["&#xf113;","github-alt"],["&#xf092;","github-square"],["&#xf1d2;","git-square"],["&#xf184;","gittip"],["&#xf000;","glass"],["&#xf0ac;","globe"],["&#xf1a0;","google"],["&#xf0d5;","google-plus"],["&#xf0d4;","google-plus-square"],["&#xf1ee;","google-wallet"],["&#xf19d;","graduation-cap"],["&#xf1d4;","hacker-news"],["&#xf0a7;","hand-o-down"],["&#xf0a5;","hand-o-left"],["&#xf0a4;","hand-o-right"],["&#xf0a6;","hand-o-up"],["&#xf0a0;","hdd-o"],["&#xf1dc;","header"],["&#xf025;","headphones"],["&#xf004;","heart"],["&#xf08a;","heart-o"],["&#xf1da;","history"],["&#xf015;","home"],["&#xf0f8;","hospital-o"],["&#xf0fd;","h-square"],["&#xf13b;","html5"],["&#xf20b;","ils"],["&#xf01c;","inbox"],["&#xf03c;","indent"],["&#xf129;","info"],["&#xf05a;","info-circle"],["&#xf156;","inr"],["&#xf16d;","instagram"],["&#xf208;","ioxhost"],["&#xf033;","italic"],["&#xf1aa;","joomla"],["&#xf157;","jpy"],["&#xf1cc;","jsfiddle"],["&#xf084;","key"],["&#xf11c;","keyboard-o"],["&#xf159;","krw"],["&#xf1ab;","language"],["&#xf109;","laptop"],["&#xf202;","lastfm"],["&#xf203;","lastfm-square"],["&#xf06c;","leaf"],["&#xf094;","lemon-o"],["&#xf149;","level-down"],["&#xf148;","level-up"],["&#xf1cd;","life-ring"],["&#xf0eb;","lightbulb-o"],["&#xf201;","line-chart"],["&#xf0c1;","link"],["&#xf0e1;","linkedin"],["&#xf08c;","linkedin-square"],["&#xf17c;","linux"],["&#xf03a;","list"],["&#xf022;","list-alt"],["&#xf0cb;","list-ol"],["&#xf0ca;","list-ul"],["&#xf124;","location-arrow"],["&#xf023;","lock"],["&#xf175;","long-arrow-down"],["&#xf177;","long-arrow-left"],["&#xf178;","long-arrow-right"],["&#xf176;","long-arrow-up"],["&#xf0d0;","magic"],["&#xf076;","magnet"],["&#xf183;","male"],["&#xf041;","map-marker"],["&#xf136;","maxcdn"],["&#xf20c;","meanpath"],["&#xf0fa;","medkit"],["&#xf11a;","meh-o"],["&#xf130;","microphone"],["&#xf131;","microphone-slash"],["&#xf068;","minus"],["&#xf056;","minus-circle"],["&#xf146;","minus-square"],["&#xf147;","minus-square-o"],["&#xf10b;","mobile"],["&#xf0d6;","money"],["&#xf186;","moon-o"],["&#xf001;","music"],["&#xf1ea;","newspaper-o"],["&#xf19b;","openid"],["&#xf03b;","outdent"],["&#xf18c;","pagelines"],["&#xf1fc;","paint-brush"],["&#xf0c6;","paperclip"],["&#xf1d8;","paper-plane"],["&#xf1d9;","paper-plane-o"],["&#xf1dd;","paragraph"],["&#xf04c;","pause"],["&#xf1b0;","paw"],["&#xf1ed;","paypal"],["&#xf040;","pencil"],["&#xf14b;","pencil-square"],["&#xf044;","pencil-square-o"],["&#xf095;","phone"],["&#xf098;","phone-square"],["&#xf03e;","picture-o"],["&#xf200;","pie-chart"],["&#xf1a7;","pied-piper"],["&#xf1a8;","pied-piper-alt"],["&#xf0d2;","pinterest"],["&#xf0d3;","pinterest-square"],["&#xf072;","plane"],["&#xf04b;","play"],["&#xf144;","play-circle"],["&#xf01d;","play-circle-o"],["&#xf1e6;","plug"],["&#xf067;","plus"],["&#xf055;","plus-circle"],["&#xf0fe;","plus-square"],["&#xf196;","plus-square-o"],["&#xf011;","power-off"],["&#xf02f;","print"],["&#xf12e;","puzzle-piece"],["&#xf1d6;","qq"],["&#xf029;","qrcode"],["&#xf128;","question"],["&#xf059;","question-circle"],["&#xf10d;","quote-left"],["&#xf10e;","quote-right"],["&#xf074;","random"],["&#xf1d0;","rebel"],["&#xf1b8;","recycle"],["&#xf1a1;","reddit"],["&#xf1a2;","reddit-square"],["&#xf021;","refresh"],["&#xf18b;","renren"],["&#xf01e;","repeat"],["&#xf112;","reply"],["&#xf122;","reply-all"],["&#xf079;","retweet"],["&#xf018;","road"],["&#xf135;","rocket"],["&#xf09e;","rss"],["&#xf143;","rss-square"],["&#xf158;","rub"],["&#xf0c4;","scissors"],["&#xf002;","search"],["&#xf010;","search-minus"],["&#xf00e;","search-plus"],["&#xf064;","share"],["&#xf1e0;","share-alt"],["&#xf1e1;","share-alt-square"],["&#xf14d;","share-square"],["&#xf045;","share-square-o"],["&#xf132;","shield"],["&#xf07a;","shopping-cart"],["&#xf012;","signal"],["&#xf090;","sign-in"],["&#xf08b;","sign-out"],["&#xf0e8;","sitemap"],["&#xf17e;","skype"],["&#xf198;","slack"],["&#xf1de;","sliders"],["&#xf1e7;","slideshare"],["&#xf118;","smile-o"],["&#xf0dc;","sort"],["&#xf15d;","sort-alpha-asc"],["&#xf15e;","sort-alpha-desc"],["&#xf160;","sort-amount-asc"],["&#xf161;","sort-amount-desc"],["&#xf0de;","sort-asc"],["&#xf0dd;","sort-desc"],["&#xf162;","sort-numeric-asc"],["&#xf163;","sort-numeric-desc"],["&#xf1be;","soundcloud"],["&#xf197;","space-shuttle"],["&#xf110;","spinner"],["&#xf1b1;","spoon"],["&#xf1bc;","spotify"],["&#xf0c8;","square"],["&#xf096;","square-o"],["&#xf18d;","stack-exchange"],["&#xf16c;","stack-overflow"],["&#xf005;","star"],["&#xf089;","star-half"],["&#xf123;","star-half-o"],["&#xf006;","star-o"],["&#xf1b6;","steam"],["&#xf1b7;","steam-square"],["&#xf048;","step-backward"],["&#xf051;","step-forward"],["&#xf0f1;","stethoscope"],["&#xf04d;","stop"],["&#xf0cc;","strikethrough"],["&#xf1a4;","stumbleupon"],["&#xf1a3;","stumbleupon-circle"],["&#xf12c;","subscript"],["&#xf0f2;","suitcase"],["&#xf185;","sun-o"],["&#xf12b;","superscript"],["&#xf0ce;","table"],["&#xf10a;","tablet"],["&#xf0e4;","tachometer"],["&#xf02b;","tag"],["&#xf02c;","tags"],["&#xf0ae;","tasks"],["&#xf1ba;","taxi"],["&#xf1d5;","tencent-weibo"],["&#xf120;","terminal"],["&#xf034;","text-height"],["&#xf035;","text-width"],["&#xf00a;","th"],["&#xf009;","th-large"],["&#xf00b;","th-list"],["&#xf165;","thumbs-down"],["&#xf088;","thumbs-o-down"],["&#xf087;","thumbs-o-up"],["&#xf164;","thumbs-up"],["&#xf08d;","thumb-tack"],["&#xf145;","ticket"],["&#xf00d;","times"],["&#xf057;","times-circle"],["&#xf05c;","times-circle-o"],["&#xf043;","tint"],["&#xf204;","toggle-off"],["&#xf205;","toggle-on"],["&#xf1f8;","trash"],["&#xf014;","trash-o"],["&#xf1bb;","tree"],["&#xf181;","trello"],["&#xf091;","trophy"],["&#xf0d1;","truck"],["&#xf195;","try"],["&#xf1e4;","tty"],["&#xf173;","tumblr"],["&#xf174;","tumblr-square"],["&#xf1e8;","twitch"],["&#xf099;","twitter"],["&#xf081;","twitter-square"],["&#xf0e9;","umbrella"],["&#xf0cd;","underline"],["&#xf0e2;","undo"],["&#xf19c;","university"],["&#xf09c;","unlock"],["&#xf13e;","unlock-alt"],["&#xf093;","upload"],["&#xf155;","usd"],["&#xf007;","user"],["&#xf0f0;","user-md"],["&#xf0c0;","users"],["&#xf03d;","video-camera"],["&#xf194;","vimeo-square"],["&#xf1ca;","vine"],["&#xf189;","vk"],["&#xf027;","volume-down"],["&#xf026;","volume-off"],["&#xf028;","volume-up"],["&#xf18a;","weibo"],["&#xf1d7;","weixin"],["&#xf193;","wheelchair"],["&#xf1eb;","wifi"],["&#xf17a;","windows"],["&#xf19a;","wordpress"],["&#xf0ad;","wrench"],["&#xf168;","xing"],["&#xf169;","xing-square"],["&#xf19e;","yahoo"],["&#xf1e9;","yelp"],["&#xf167;","youtube"],["&#xf16a;","youtube-play"],["&#xf166;","youtube-square"]];

function getGlyphNameByCode(code) {
	for (var i in _fontGlyphs) {
		if (_fontGlyphs[i][0] == code) return _fontGlyphs[i][1];
	}
}

function Property_glyphCode(cell, controlAction, property, details) {
	
	// retieve the glyph code
	var code = controlAction[property.key];
	// if we got one
	if (code) {
		// add it and it's name into the cell
		var fontClass = "fa";
		if (code == "&#xe900") fontClass = "fr";
		cell.append("<span class='" + fontClass + "'>" + code + "</span>&nbsp;" + getGlyphNameByCode(code));
	} else {
		// add message
		cell.append("Please select...");
	}
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, controlAction, property, details, 200, "Glyph");		
	// remove any previous table
	dialogue.find("table").remove();
	// add a scrolling div with the search inside, if we need one
	if (!dialogue.find("div")[0]) dialogue.append("<div style='overflow-y:scroll;max-height:400px;margin-top:10px;'><input class='glyphSearch' placeholder='Search'></div>");
	// add the table
	dialogue.find("div").append("<table></table>");
	// get the new table
	table = dialogue.find("table").first();
	// add all of the glyphs, with the current one highlighted
	for (var i in _fontGlyphs) {
		var fontClass = "fa";
		var fontCode = _fontGlyphs[i][0];
		if (fontCode == "&#xe900") fontClass = "fr";
		table.append("<tr><td data-code='" + fontCode.replace("&","&amp;") + "' class='hover" + (code && code == fontCode ? " selected" : "") + "'><span class='" + fontClass + "'>" + fontCode + "</span><span class='fa_name'>&nbsp;" + _fontGlyphs[i][1] + "</span></td></tr>");
	}
	// if a position was set go back to it
	if (dialogue.attr("data-scroll")) table.parent().scrollTop(dialogue.attr("data-scroll"));
	
	// add listener for searching for a gylph
	addListener( dialogue.find("input.glyphSearch").keyup({table: table}, function(ev) {
		// get the search term
		var s = $(this).val();
		// if there was one
		if (s) {
			// check every row
			ev.data.table.find("tr").each(function(){
				// get the row
				var r = $(this);
				// look for search term
				if (r.text().indexOf(s) >= 0) {
					r.show();
				} else {
					r.hide();
				}
			});
		} else {
			// show all rows
			ev.data.table.find("tr").show();
		}
	}));
	
	// fire it for any previous searches
	dialogue.find("input.glyphSearch").keyup();
	
	// add listener for selecting a gylph	
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
		// update the property - this will apply the html change
		updateProperty(ev.data.cell, ev.data.controlAction, ev.data.property, ev.data.details, code);
	}));
	
}

function Property_buttonGlyphPosition(cell, propertyObject, property, details) {
	// only if glyph code is specified
	if (propertyObject.glyphCode) {
		// add the drop down property
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_buttonGlyphBackground(cell, propertyObject, property, details) {
	// only if glyph code is specified
	if (propertyObject.glyphCode) {
		// add the drop down property
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

function Property_heatmapNumber(cell, propertyObject, property, details) {
	// only if this is a heatmap
	if (propertyObject.heatmap) {
		// add the handler for this property
		Property_number(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_heatmapCheckbox(cell, propertyObject, property, details) {
	// only if this is a heatmap
	if (propertyObject.heatmap) {
		// add the handler for this property
		Property_checkbox(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
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
	var values = [[0.5,"50%"],[0.75,"75%"],[1,"100%"],[1.5,"150%"],[2,"200%"],[3,"300%"],[4,"400%"]];
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
var _mobileActionTypes = [["dial","Dial number"],["sms","Send text/sms message"],["email","Send email"],["url","Open url"],["addImage","Get image"],["uploadImages","Upload images"],["addBarcode","Scan barcode"],["navigate","Navigate to"],["sendGPS","Send GPS position"],["stopGPS","Stop GPS updates"],["message","Status bar message"],["disableBackButton","Disable back button"],["swipe","Swipe"],["online","Online actions"]];

// this property changes the visibility of other properties according to the chosen type
function Property_mobileActionType(cell, mobileAction, property, details) {
	// assume not the page load
	var pageLoad = false;
	// look for first heading
	var heading = cell.closest("table").find("h3").first();
	// if heading is for page load, set it
	if (heading.text() == "Load event") pageLoad = true;
	// the selectHtml
	var selectHtml = "<select>";
	// loop the mobile action types
	for (var i in _mobileActionTypes) {
		// leave out swipe if not page load
		if (_mobileActionTypes[i][0] != "swipe" || pageLoad) selectHtml += "<option value='" + _mobileActionTypes[i][0] + "'" + (mobileAction.actionType == _mobileActionTypes[i][0]? " selected='selected'" : "") + ">" + _mobileActionTypes[i][1] + "</option>";
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
	setPropertyVisibilty(mobileAction, "swipeDirection", false);
	setPropertyVisibilty(mobileAction, "swipeFingers", false);
	setPropertyVisibilty(mobileAction, "swipeControl", false);
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
		case "swipe" :
			// only if for page load
			if (pageLoad) {
				setPropertyVisibilty(mobileAction, "swipeDirection", true);
				setPropertyVisibilty(mobileAction, "swipeFingers", true);
				setPropertyVisibilty(mobileAction, "swipeControl", true);
				setPropertyVisibilty(mobileAction, "onlineActions", true);
			}
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

// reuse the generic childActionsForType but set the details with type = database
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
	// add the text clip class
	table.addClass("dialogueTextClip");
	// make sure table is empty
	table.children().remove();
	
	// get the dialogue title
	var title = dialogue.find(".dialogueTitle").first();
	if (!title.next().is("button")) {
		title.after("<button style='float:left;margin-top:-7px;' class='titleButton sources' title='Reset page order to be alphabetical'><span>&#xf0e2;</span></button>");
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
		table.append("<tr><td>" + page.name + " - " + page.title + "</td><td style='text-align:right;'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div></td></tr>");
		
	}
		
	// add reorder listeners
	addReorder(_pages, table.find("div.reorder"), function() {		
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
	// only if this is a form with an adapter
	if (_version.isForm) {
		// add the select handler for this property
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

//a handler for text properties where there is a form adapter
function Property_formActionType(cell, propertyObject, property, details) {
	// only if this is a form with a form adapter
	if (_version.isForm) {
		// start the main get value function with all basic types
		var getValuesFunction = "return [[\"\",\"Please select...\"],[\"next\",\"go to next page\"],[\"prev\",\"go to previous page\"],[\"maxpage\",\"go to last page\"],[\"summary\",\"go to summary\"],[\"savepage\",\"go to save page\"],[\"id\",\"copy form id\"],[\"val\",\"copy form value\"]";
		// check the page type
		switch (_page.formPageType * 1) {
		case 1:
			// submit message
			getValuesFunction += ",[\"sub\",\"copy form submit message\"]";
			// pdf url, if allowed by form adapter
			if (_version.canGeneratePDF) getValuesFunction += ",[\"pdf\",\"copy form pdf url\"]";
			break;
		case 2:
			// error message
			getValuesFunction += ",[\"err\",\"copy form error message\"]";
			break;
		case 3:
			// save action, if allowed by form adapter
			if (_version.canSaveForms) getValuesFunction += ",[\"save\",\"save form\"]";
			break;
		case 4:
			// resume action, if allowed by form adapter
			if (_version.canSaveForms) getValuesFunction += ",[\"resume\",\"resume form\"]";
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
	// only if this is a form with a form adapter and we're not not any of the special (post summary) pages
	if (_version.isForm && (!_page.formPageType || _page.formPageType == 0)) {
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
	if (
		(propertyObject.actionType == "val" && property.key != "formId" && property.key != "email" && property.key != "password") ||
		(propertyObject.actionType == "save" && property.key != "dataSource" && property.key != "formId") ||
		(propertyObject.actionType == "resume" && property.key != "dataSource" && property.key != "email")
	) {
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

// this is for the form save and resume types action success and fail
function Property_formChildActions(cell, propertyObject, property, details) {
	// only if the type is one that requires a destination
	if (propertyObject.actionType == "save" || propertyObject.actionType == "resume") {
		// add the select
		Property_childActions(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
} 

// chart properties
function Property_chartType(cell, chart, property, details) {
	// create a select property
	Property_select(cell, chart, property, details);
	// reset all property visibilities
	setPropertyVisibilty(chart, "curveType", false);
	setPropertyVisibilty(chart, "isStacked", false);
	setPropertyVisibilty(chart, "pieSliceText", false);
	setPropertyVisibilty(chart, "sliceVisibilityThreshold", false);	
	setPropertyVisibilty(chart, "is3D", false);
	setPropertyVisibilty(chart, "pieHole", false);
	// check the type
	switch (chart.chartType) {
		case "Line" :
			setPropertyVisibilty(chart, "curveType", true);
		break;
		case "Bar" :
			setPropertyVisibilty(chart, "isStacked", true);
		break;
		case "Pie" :
			// show pie-only properties
			setPropertyVisibilty(chart, "pieSliceText", true);			
			setPropertyVisibilty(chart, "sliceVisibilityThreshold", true);
			setPropertyVisibilty(chart, "is3D", true);
			if (!chart.is3D)	setPropertyVisibilty(chart, "pieHole", true);
		break;
	}	
}

// this is a dialogue to specify the inputs, content and merging for an email
function Property_emailContent(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 650, "Email content", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// make sure its empty
	table.children().remove();
	
	// sample subject template
	var subjectSample = "Email subject";
	// sample body for type text
	var bodySampleText = "Dear ?,\n\nThanks for your letter sent on ?";
	// sample body  for type html
	var bodySampleHtml = "<html>\n  <body>\nDear ?,\n\nThanks for your letter sent on ?\n  </body>\n</html>";
		
	// initialise the content according to the type object if need be
	if (!propertyObject.content) propertyObject.content = {inputs:[], subject:subjectSample, body:(propertyObject.emailType == "html" ? bodySampleHtml : bodySampleText )};
	// get the content
	var content = propertyObject.content;
	// if content is a string
	if ($.type(content) === "string") {
		// parse the JSON string back into proper objects
		content = JSON.parse(content);
		// update the propertyObject
		propertyObject.content = content;
	}
	// swap the default types if need be	
	if (propertyObject.emailType == "html") {
		if (content.body == bodySampleText) content.body = bodySampleHtml;
	} else {
		if (content.body == bodySampleHtml) content.body = bodySampleText;
	}
	// get the body template into a variable
	var text = content.body;
	// change to message if not provided
	if (!text || (propertyObject.emailType != "html" && text == bodySampleText) || (propertyObject.emailType == "html" && text == bodySampleHtml)) text = "Click to define...";
	// put the elipses in the cell
	cell.text(text);
	
	// add inputs table, subject, and body
	table.append("<tr><td colspan='2' style='padding:0px;vertical-align: top;'><table class='dialogueTable inputs'><tr><td><b>Input</b></td><td><b>Field</b></td></tr></table></td><td style='width:65%;padding:2px 10px 0 10px;'><b>Subject</b><br/><input class='subject' style='padding:2px;width:100%;box-sizing:border-box;border:1px solid #aaa;height:22px;' /><br/><b>Body</b><br/><textarea style='width:100%;min-height:180px;box-sizing:border-box;'></textarea></td></tr>");	
	
	// find the inputs table
	var inputsTable = table.find("table.inputs");
	// loop input parameters
	for (var i in content.inputs) {		
		// get the input name
		var itemName = content.inputs[i].itemId;
		// look for a control with an item of this item
		var control = getDataItemDetails(itemName);
		// if we found a control use this as the name
		if (control && control.name) itemName = control.name;		
		// get the field
		var field = content.inputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		inputsTable.append("<tr><td>" + itemName + "</td><td><input value='" + escapeApos(field) + "' /></td><td style='width:45px;'>" +
				"<div class='iconsPanel'>" +
				"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
				"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
				"</div></td></tr>");
		// get the field input
		var fieldInput = inputsTable.find("tr").last().children(":nth(1)").last().children().last();
		// add a listener
		addListener( fieldInput.keyup( {parameters: content.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));		
	}
	
	// add reorder listeners
	addReorder(content.inputs, inputsTable.find("div.reorder"), function() { 
		Property_emailContent(cell, propertyObject, property, details);
	});
	
	// get the delete images
	var fieldDelete = inputsTable.find("div.delete");
	// add a listener
	addListener( fieldDelete.click( {parameters: content.inputs}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.parameters.splice(input.closest("tr").index()-1,1);
		// remove row
		input.closest("tr").remove();
		// update the dialogue
		Property_emailContent(cell, propertyObject, property, details);
	}));
	
	// add the add input select
	inputsTable.append("<tr><td style='padding:0px;' colspan='2'><select style='margin:0px'><option value=''>add input...</option>" + getInputOptions() + "</select></td><td>&nbsp;</td></tr>");
	// find the input add
	var inputAdd = inputsTable.find("tr").last().children().first().children("select");
	// listener to add input
	addListener( inputAdd.change( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.content.inputs) ev.data.propertyObject.content.inputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.content.inputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialogue
		Property_emailContent(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
	}));
	
	// find the subject input
	var subjectControl = table.find("input.subject").first();
	subjectControl.val(content.subject);
	// listener for the subject
	addListener( subjectControl.keyup( {content: content}, function(ev) {
		ev.data.content.subject = $(ev.target).val();
	}));
	
	// find the body textarea
	var bodyControl = table.find("textarea").first();
	bodyControl.text(content.body);
	// listener for the body
	addListener( bodyControl.keyup( {content: content}, function(ev) {
		ev.data.content.body = $(ev.target).val();
	}));
	
}

// this is for the date select other months which is conditional upon showing other months
function Property_dateSelectOtherMonths(cell, propertyObject, property, details) {
	// only if the type is to copy values
	if (propertyObject.showOtherMonths == "true" || propertyObject.showOtherMonths == true) {
		// add the checkbox
		Property_checkbox(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// globals for common formObjectRoles
var _formObjectRoles = {
	"PL" : [["caseParty","Case party"],["linkedParty","Linked party"]],
	"CPL" : [["case","Case"],["caseParty","Case party"],["linkedParty","Linked party"]]
}

// a global for form objects
var _formObjects = {
		"address": {"name" : "Address", "roles" : _formObjectRoles["PL"], "types": [["PHYSICAL","Physical address"],["POSTAL","Postal address"]], "attributes" : [["address","Full address"],["line1","Line 1"],["line2","Line 2"],["city","City"],["postcode","Postcode"],["startDate","Start date"],["endDate","End date"],["addressType","Type"],["other","Other"]]},
		"contact": {"name" : "Contact", "roles" : _formObjectRoles["PL"], "types" : [["","Please select..."],["email","Email address"],["mobile","Mobile number"],["phone","Phone number"],["home","Home number"],["work","Work number"],["other","Other contact"]], "attributes" : [["value","Value"],["startDate","Start date"],["endDate","End date"],["other","Other"]]},
		"document": {"name" : "Document", "roles" : _formObjectRoles["CPL"], "types" : [["OTHER","Other"],["MEDICAL","Medical"],["TENANCY","Tenancy"]]},
		"note": {"name" : "Note", "roles" : _formObjectRoles["CPL"], "types" : [["OTHER","Other"],["MEDICAL","Medical"],["TENANCY","Tenancy"]]},
		"party": {"name" : "Party", "roles" : _formObjectRoles["PL"], "types": [["OTHER","Other"],["DOCTOR","Doctor"],["LANDLORD","Landlord"],["LETAGENT","Lettings agent"],["SOLICITOR","Solicitor"],["SUPTWORKER","Support worker"]], "attributes" : [["","Please select..."],["name","Full name"],["title","Title"],["forename","Forename"],["surname","Surname"],["dob","Date of birth"],["gender","Gender"],["ethnicity","Ethnicity"],["nationality","Nationality"],["relationship","Relationship"],["isJoint","Is joint case party"],["isWith","Is with main case party"],["name","Organisation name"],["startDate","Start date"],["endDate","End date"],["reference","Reference"],["contact","Contact preference"],["other","Other"]]},
		"question": {"name" : "Question", "roles" : _formObjectRoles["CPL"]},
		"payment": {"name" : "Payment", "roles" : [["case","Case"]], "types" : [["DESC","Description"],["AMT","Amount"]]},
		"other" : {"name" : "Other", "roles" : _formObjectRoles["CPL"]}
}

// this is for advanced form integration
function Property_formObject(cell, propertyObject, property, details) {
	// show any conflict message
	if (propertyObject._formConflict) cell.closest("tr").before("<tr><td colspan='2' class='formConflict propertyHeader'>Page \"" + propertyObject._formConflict + "\" has a control with the same form integration</td></tr>");
	// update the property getValuesFunction
	property.getValuesFunction = "return _formObjects";
	// send it in the select
	Property_select(cell, propertyObject, property, details);
}

// this is for advanced form integration
function Property_formObjectRole(cell, propertyObject, property, details) {
	// only if there is a formObject set and it has attributes
	if (propertyObject.formObject && _formObjects[propertyObject.formObject].roles) {
		// update the property getValuesFunction
		property.getValuesFunction = "return _formObjects['" + propertyObject.formObject + "'].roles";
		// if there is a value in the first type default to it
		if (_formObjects[propertyObject.formObject].roles[0] && !propertyObject[property.key]) propertyObject[property.key] = _formObjects[propertyObject.formObject].roles[0][0]; 
		// send it in the select
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();		
	}
}

// this function gets the value from any previous controls - also needs adding in Page.java getOtherPageChildControls
function getPreviousObjectPropertyValue(propertyObject, key, defaultValue) {
	// assume default value is nul
	var value = null;
	// get all controls on this page
	var controls = getControls();
	// assume not previous control
	var prevControl = false;
	// loop them backwards
	for (var i = controls.length; i > 0; i--) {
		// get  the control
		var control = controls[i-1];			
		// if previous control - i.e. above this
		if (prevControl) {				
			// if we got one and it has a formObjectPartyNumber
			if (control && control[key]) {
				// set value
				value = control[key];
				// we're done
				break;
			}
		} else {
			// if not previous and this is the current control the next one will be the previous we're looking for
			if (control.id == propertyObject.id) prevControl = true;
		}
	}
	// if we didn't find a value and there are other pages
	if (!value && _page && _pages) {
		// assume not previous page
		var prevPage = false;
		// loop the pages backwards
		for (var i = _pages.length; i > 0; i--) {
			// if the page is before this one
			if (prevPage) {
				// if controls on this page
				if (_pages[i-1].controls) {
					// loop page controls backwards
					for (var j = _pages[i-1].controls.length; j > 0; j--) {
						// get the control
						var control = _pages[i-1].controls[j-1];
						// if eligible and has a control has value under relevant key
						if (control.pageVisibility && control.name && control[key]) {
							// set value
							value = control[key];
							// we're done!
							break;
						}					
					}			
				} // control loop
				// break here if we have a value
				if (value) break;
			} else {
				// if not previous and this is the current page the next one will be previous
				if (_pages[i-1].id == _page.id) prevPage = true;
			} // prev check
		} // page loop
	}
	// if still no value and a default set it
	if (!value) value = 1;
	// return 
	return value;
}

// this is for advanced form integration
function Property_formObjectPartyNumber(cell, propertyObject, property, details) {
	//  if there is a formObject set and the role is for a party
	if (propertyObject.formObject && (propertyObject.formObjectRole == "caseParty" || propertyObject.formObjectRole == "linkedParty")) {
		// if it's not been set, use value of previous
		if (!propertyObject[property.key]) propertyObject[property.key] = getPreviousObjectPropertyValue(propertyObject, property.key);		
		// add a number property
		Property_integer(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();		
	}
}

// this is for advanced form integration
function Property_formObjectAddressNumber(cell, propertyObject, property, details) {
	//  if there is a formObject set and it's a question
	if (propertyObject.formObject && propertyObject.formObject == "address") {
		// if it's not been set, use value of previous
		if (!propertyObject[property.key]) propertyObject[property.key] = getPreviousObjectPropertyValue(propertyObject, property.key);
		// add a number property
		Property_integer(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();		
	}
}

// this is for advanced form integration
function Property_formObjectType(cell, propertyObject, property, details) {
	// only if there is a formObject, and we are not a party / caseParty (the types here only apply for linked parties)
	if (propertyObject.formObject && _formObjects[propertyObject.formObject].types && !(propertyObject.formObject  == "party" && propertyObject.formObjectRole == "caseParty")) {
		// update the property getValuesFunction
		property.getValuesFunction = "return _formObjects['" + propertyObject.formObject + "'].types";
		// if there is a value in the first type default to it
		if (_formObjects[propertyObject.formObject].types[0] && !propertyObject[property.key]) propertyObject[property.key] = _formObjects[propertyObject.formObject].types[0][0]; 
		// send it in the select
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// this is for advanced form integration
function Property_formObjectAttribute(cell, propertyObject, property, details) {
	// only if there is a formObject set and it has attributes
	if (propertyObject.formObject && _formObjects[propertyObject.formObject].attributes) {
		// update the property getValuesFunction
		property.getValuesFunction = "return _formObjects['" + propertyObject.formObject + "'].attributes";
		// if there is a value in the first type  default to it if we don't have one yet
		if (_formObjects[propertyObject.formObject].attributes[0] && !propertyObject[property.key]) propertyObject[property.key] = _formObjects[propertyObject.formObject].attributes[0][0]; 
		// send it in the select
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();		
	}
}

// this is for advanced form integration
function Property_formObjectQuestionNumber(cell, propertyObject, property, details) {
	//  if there is a formObject set and it's a question
	if (propertyObject.formObject && (propertyObject.formObject == "question" || propertyObject.formObject == "other")) {
		// add a number property
		Property_integer(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();		
	}
}

// this is for advanced form integration
function Property_formObjectText(cell, propertyObject, property, details) {
	// only if there is a formObject set and it is a note object
	if (propertyObject.formObject && (propertyObject.formObject == "note" || propertyObject.formObject == "other")) {
		// send it in the bigtext
		Property_bigtext(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();		
	}
}

// this is for advanced form integration
function Property_webserviceAuthType(cell, propertyObject, property, details) {
	// only if there is a formObject set and it is a note object
	if (propertyObject.auth && (propertyObject.auth == true || propertyObject.auth == "true")) {
		// create a select property handler
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// this is for web service authorisation
function Property_webserviceAuthProperty(cell, propertyObject, property, details) {
	// only if there is a formObject set and it is a note object
	if (propertyObject.auth && (propertyObject.auth == true || propertyObject.auth == "true")) {
		// create a select property handler
		Property_text(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// this is for the email attachments
function Property_uploadControls(cell, propertyObject, property, details) {
	// run the controls for type
	Property_controlsForType(cell, propertyObject, property, {type:["upload"]});
}

// the validation logic dialogue handler
function Property_validationLogic(cell, propertyObject, property, details) {
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 320, "Messages", {sizeX: true});		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// make sure its empty
	table.children().remove();
	
	// change the default class
	table.parent().removeClass("dialogueTable").addClass("propertiesPanelTable");
	
	// initialise the content according to the type object if need be
	if (!propertyObject.logicMessages) propertyObject.logicMessages = [];
	// get the messages
	var logicMessages = propertyObject.logicMessages;
	// if content is a string
	if ($.type(logicMessages) === "string") {
		// parse the JSON string back into proper objects
		logicMessages = JSON.parse(logicMessages);
		// update the propertyObject
		propertyObject.logicMessages = logicMessages;
	}
	
	// assume no cell text
	var cellText = "";
		
	// loop conditons
	for (var i in propertyObject.logicMessages) {		
		// get the message
		var message = propertyObject.logicMessages[i];
		// give it an id for the dialogues
		message.id = propertyObject.id + "message_" + i;
		// add to cell text
		cellText += message.text;
		// add a trailing comma if not last one
		if (i < propertyObject.logicMessages.length - 1) cellText += ", ";
		
		// add the condition row
		table.append("<tr data-index='" + i + "'><td style='width:60px;'>Conditions</td><td></td><td style='min-width:45px'>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");
		// get the conditions cell
		var conditionsCell = table.find("tr").last().children(":nth-child(2)");
		// add a dialogue id attribute
		conditionsCell.attr("data-dialogueId", cell.attr("data-dialogueId") + "conditions_" + i);
		// add a logic conditions property for the conditions cell
		Property_logicConditions(conditionsCell, message, {key:"conditions"}, details);
		// add the message row and cell - listener is below
		table.append("<tr><td style='width:60px;'>Message</td><td>" + message.text + "</td></tr>");
		// get the text cell
		var textCell = table.find("td").last();
		// add a dialogue id attribute
		textCell.attr("data-dialogueId", cell.attr("data-dialogueId") + "text_" + i);
		// add a logic conditions property for the conditions cell
		Property_bigtext(textCell, message, {key:"text"}, details);
	}
	
	// set cell text if not
	if (!cellText) cellText = "Click to add..."
	// update cell text
	cell.text(cellText);
		
	// add reorder listeners
	addReorder(propertyObject.logicMessages, table.find("div.reorder"), function() { 
		Property_validationLogic(cell, propertyObject, property, details);
	});
	
	// get the delete images
	var fieldDelete = table.find("div.delete");
	// add a listener
	addListener( fieldDelete.click( {parameters: propertyObject.logicMessages}, function(ev) {
		// get the input
		var input = $(ev.target);
		// get its index
		var i = input.closest("tr").attr("data-index");
		// remove from parameters
		ev.data.parameters.splice(i,1);
		// update the dialogue
		Property_validationLogic(cell, propertyObject, property, details);
	}));
	
	// have an add row
	table.append("<tr><td colspan='3'><span class='propertyAction'>add...</span></td></tr>");
	// get a reference to the add
	var add = table.find("span.propertyAction").last();
	// add a listener
	addListener( add.click( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// add an undo snapshot
		addUndo();
		// initialise if required
		if (!ev.data.propertyObject.logicMessages) ev.data.propertyObject.logicMessages = [];
		// get the current control id
		var controlId = _selectedControl.id;
		// if this is a date
		
		// add a blank option with values defaulting to the selected control
		ev.data.propertyObject.logicMessages.push({text:"Message",conditions:[{operation:"==",value1:{type:"CTL",id:controlId},value2:{type:"CTL",id:controlId}}]});
		// refresh
		Property_validationLogic(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);		
	}));
		
}

//  ["current date","current time","current date and time"];

// presents different date formats for the data copy datetime options
function Property_datacopyDateFormat(cell, propertyObject, property, details) {
	// only if the data copy dataSource property starts with "Date"
	if (propertyObject.dataSource && propertyObject.dataSource.indexOf("Datetime.") == 0 && propertyObject.dataSource.indexOf("date") > 10) {
		// create the drop down, function for values is above.
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// presents different time formats for the data copy datetime options
function Property_datacopyTimeFormat(cell, propertyObject, property, details) {
	// only if the data copy dataSource property starts with "time"
	if (propertyObject.dataSource && propertyObject.dataSource.indexOf("Datetime.") == 0 && propertyObject.dataSource.lastIndexOf("time") > 10) {
		// create the drop down, function for values is above.
		Property_select(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

// inputs to a the pdf action
function Property_pdfInputs(cell, propertyObject, property, details) {
	
	// retrieve or create the dialogue
	var dialogue = getDialogue(cell, propertyObject, property, details, 400, "PDF inputs", {sizeX: true});		
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
	table.append("<tr><td><b>Control</b></td><td><b>Field</b></td><td colspan='2'><b>Label</b><button class='titleButton' title='Add all page data controls'><span>&#xf055;</span></td></tr>");
		
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
			table.append("<tr><td>" + dataItem.name + "</td><td><input value='" + input.field + "' /></td><td><input value='" + input.label + "' /></td><td style='width:45px'>" +
					"<div class='iconsPanel'>" +
					"<div class='reorder fa-stack fa-sm' title='Drag to change order'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
					"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Click to delete'></i></div>" +
					"</div></td></tr>");
			// get the field
			var editField = table.find("tr").last().children("td:nth(1)").children("input");
			// add a listener
			addListener( editField.keyup( {inputs: inputs}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update the field
				ev.data.inputs[input.parent().parent().index()-1].field = input.val();
			}));
			// get the label
			var editLabel = table.find("tr").last().children("td:nth(2)").children("input");
			// add a listener
			addListener( editLabel.keyup( {inputs: inputs}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update the field
				ev.data.inputs[input.parent().parent().index()-1].label = input.val();				
			}));			
		} else {
			// remove this entry from the collection
			inputs.splice(i,1);
			// set i back 1 position
			i--;
		}			
	}

	// add reorder listeners
	addReorder(inputs, table.find("div.reorder"), function() { 
		Property_pdfInputs(cell, propertyObject, property, details); 
	});
	
	// add a listener
	addListener( table.find("div.delete").click( {inputs: inputs}, function(ev) {
		// get the input
		var imgDelete = $(ev.target);
		// remove from parameters
		ev.data.inputs.splice(imgDelete.closest("tr").index()-1,1);
		// remove row
		imgDelete.closest("tr").remove();
		// refresh dialogue
		Property_pdfInputs(cell, propertyObject, property, details); 
	}));
	
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
		inputs.push({itemId: $(ev.target).val(), field: "", label: ""});
		// rebuild the dialogue
		Property_pdfInputs(ev.data.cell, ev.data.propertyObject, ev.data.property, ev.data.details);
		
	}));
	
	// add add all listener
	addListener( table.find("button.titleButton").click( {cell: cell, propertyObject: propertyObject, property: property, details: details}, function(ev) {
		// add an undo snapshot
		addUndo();
		// get the list of inputs
		var inputs = ev.data.inputs;
		// initialise if need be
		if (!inputs) inputs = [];
		// get all page controls
		var pageControls = getControls();
		// prepare a list of controls to insert
		var insertControls = [];
		// loop the page controls
		for (var i in pageControls) {
			// get the pageControl
			var pageControl = pageControls[i];
			// if it has a name
			if (pageControl.name) {
				// get the control class
				var controlClass = _controlTypes[pageControl.type];
				// if this has a get data method
				if (controlClass.getDataFunction) {
					// get the label
					var label = pageControl.label;
					// if not got one yet 
					if (!label) {
						// look for responsive label
						label = pageControl.responsiveLabel;
						// if still not got one and control control is text, set label to text property
						if (!label && i > 0 && (pageControls[i-1].type == "text" || pageControls[i-1].type == "responsivetext")) label = pageControls[i-1].text; 
					}
					// if there is a label
					if (label) {
						// trim label
						label = label.trim();
						// check for trailing :
						if (label[label.length-1] == ":") {
							// remove :
							label = label.substr(0,label.length-1);
							// trim again
							label = label.trim();
						}
					} else {
						// set label to empty string to avoid undefined
						label = "";
					}
					// assume we don't have this control already
					var gotControl = false;
					// loop our controls
					for (var i in inputs) {
						if (inputs[i].itemId == pageControl.id) {
							gotControl = true;
							break;
						}
					}
					// if not add to insert collection
					if (!gotControl) insertControls.push({itemId: pageControl.id, field: "", label: label});
				} // control get data method check
			} // control name check
		}
		// now loop the insert controls
		for (var i in insertControls) {
			// get the insert control
			var insertControl = insertControls[i];
			// get the insert control position in the page
			var insertPos = getKeyIndexControls(pageControls, insertControl.itemId);
			// assume we haven't inserted it
			var inserted = false;
			// now loop the existing validation controls
			for (var j in inputs) {
				// get the existing position 
				var existingPos = getKeyIndexControls(pageControls, inputs[j].itemId);
				// if the existing pos is after the insert control position
				if (existingPos > insertPos) {
					// insert here
					inputs.splice(j, 0, insertControl);
					// retain insert
					inserted = true;
					// we're done
					break;
				} // found a control after this one so insert before the found one
			} // loop dataCopies
			// if we haven't inserted yet do so now
			if (!inserted) inputs.push({itemId: insertControl.itemId, field: "", label: insertControl.label});
		} // loop inserts
		// add back the changed controls
		ev.data.propertyObject.inputs = inputs;
		// refresh dialogue
		Property_pdfInputs(cell, propertyObject, property, details); 
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

// allows users to enter their own custom value into the drop down if searching is on, and codes are off
function Property_dropdownCustomValue(cell, propertyObject, property, details) {
	// only if searching is on, and codes are off
	if (propertyObject.filter && !propertyObject.codes) {
		// create the drop down, function for values is above.
		Property_checkbox(cell, propertyObject, property, details);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}
