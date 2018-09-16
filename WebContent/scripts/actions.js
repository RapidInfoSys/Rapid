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

// a map of help html for recognised events
_eventHelpHtml = {
	"blur" : "Occurs when a form control has lost its focus.",
	"change" : "Occurs when a form control's value has changed, after its blur, if applicable.",
	"click" : "Occurs when the control is clicked with the mouse.",
	"dblclick" : "Occurs when a control is double-clicked with the mouse.",
	"focus" : "Occurs when a control receives focus, generally when clicked on or tabbed to.",
	"focusin" : "Occurs when a control, or child control, receives focus, generally when clicked on or tabbed to.",
	"focusout" : "Occurs when a control, or child control, loses focus, generally when clicked or tabbed away from.",
	"hover" : "Occurs when the mouse is placed over a control.",
	"keydown" : "Occurs when a control is selected and a key is pushed down.",
	"keypress" : "Occurs when a control is selected and a key represnting a printable character is released after being pushed down.",
	"keyup" : "Occurs when a control is selected and a key is pushed down and then released.",
	"mousedown" : "Occurs when the mouse cursor is over the control and a mouse button is pushed down.",
	"mouseenter" : "Occurs when the mouse enters the whole space under a control.",
	"mouseleave" : "Occurs when the mouse leaves the whole space under a control.",
	"mousemove" : "Occurs when the mouse moves over the space under a control.",
	"mouseout" : "Occurs when the mouse is moved off of this control, or any child control.",
	"mouseover" : "Occurs when the mouse moves over this control, or any child control.",
	"mouseup" : "Occurs when the mouse cursor is over the control and a mouse button is released.",
	"scroll" : "Occurs when a control is scrolled."
}

// this object function serves as a closure for holding the static values required to construct each type of action - they're created and assigned globally when the designer loads and originate from the .action.xml files
function ActionClass(actionClass) {
	// retain all values passed in the json for the action (from the .action.xml file)
	for (var i in actionClass) this[i] = actionClass[i];	
}

// this object function will create the action as specified in the actionType
function Action(actionType, jsonAction, paste, undo) {
			
	// get the action class from the type
	var actionClass = _actionTypes[actionType];
	// check controlClass exists
	if (actionClass) {
				
		// retain the type
		this.type = actionClass.type;
		
		// retain the version
		this.version = actionClass.version;
				
		// if we were given properties retain then and avoid initialisation
		if (jsonAction) {
			
			// if we're pasting we don't have a properties collection
			if (paste || undo) {
				// copy all of the properties into the new action
				for (var i in jsonAction) this[i] = jsonAction[i];
				// when pasting use incremented ids
				if (paste) {
					// set a unique ID for this control (the under score is there to stop C12 being replaced by C1)
					this.id = _page.id + "_A" + _nextId + "_" + _controlAndActionSuffix;
					// check the pasteMap
					if (_pasteMap[jsonAction.id]) {
						this.id = jsonAction.id;
					} else {
						// add an entry in the pastemap
						_pasteMap[this.id] = jsonAction.id;
					}
					// inc the next id
					_nextId++;
					// loop properties
					for (var i in this) {
						// look for "*actions" collections (but not redundantActions)
						if ($.isArray(this[i]) && i.toLowerCase().indexOf("actions") > 0 && i != "redundantActions") {
							// make a new collection
							var childActions = [];
							// loop this collection
							for (var j in this[i]) {
								// get a new childAction based on this one
								var childAction = new Action(this[i][j].type, this[i][j], true);
								// add it to the new collection
								childActions.push(childAction);
							}		
							// replace the property with our new array of new child actions
							this[i] = childActions;
						}
					}
				}
				// when undoing make sure the next id is higher than all others
				if (undo) {
					// get the id value into a variable
					var id = this.id;
					// the id will be something like P99_C12, find the number after the _C
					var idInt = parseInt(id.substr(id.indexOf("_C") + 2));
					// set the next id to one past this if it is less
					if (idInt >= _nextId) _nextId = idInt + 1;	
				}
			} else {
				// copy all of the properties into the new action (except for the id, type, and properties collection), childActions need instanitating too
				for (var i in jsonAction) {
					// these three properties are special and are ignored
					if (i != "id" && i != "type" && i != "properties") {				
						// check whether we have a Rapid Object 
						if (jsonAction[i].type && _actionTypes[jsonAction[i].type]) {
							// this is a simple object, instantiate here
							this[i] = new Action(jsonAction[i].type, jsonAction[i]);
						} else if ($.isArray(jsonAction[i]) && jsonAction[i].length > 0 && jsonAction[i][0].type && _actionTypes[jsonAction[i][0].type]) {
							// this is an array of objects
							this[i] = [];
							// loop array
							for (var j in jsonAction[i]) {
								this[i].push(new Action(jsonAction[i][j].type, jsonAction[i][j]) );
							}
						} else {							
							// simple property copy
							this[i] = jsonAction[i];
						}	
					} // not id, type, properties
				}
				// loop and retain the properties in the save control properties collection directly
				for (var i in jsonAction.properties) {	
					// get the property
					var p = jsonAction.properties[i];
					// if it looks like an array parse it with JSON
					if (p && p.length >= 2 && p.substr(0,1) == "[" && p.substr(p.length-1,1) == "]") p = JSON.parse(p);						
					// retain the property in the control class
					this[i] = p;
					// make sure the id's are always unique 
					if (i == "id") {	
						// get the id value into a variable
						var id = jsonAction.properties["id"];
						// the id will be something like P99_A12, find the number after the _C
						var idInt = parseInt(id.substr(id.indexOf("_A") + 2));
						// set the next id to one past this if it is less
						if (idInt >= _nextId) _nextId = idInt + 1;					
					}
				}
			} // if paste
						
		} else {
			
			// set a unique ID for this action (with the final underscore the stops C12 being replaced by C1)
			this.id = _page.id + "_A" + _nextId + "_" + _controlAndActionSuffix;
						
			// if the action class has properties set them here
			if (actionClass.properties) {
				// get a reference to the properties
				var properties = actionClass.properties;
				// due to the JSON library this is the array
				if ($.isArray(properties.property)) properties = properties.property;
				for (var i in properties) {
					// get a reference to the property
					var property = properties[i];
					// if there is a setConstruct value function
					if (property.setConstructValueFunction) {
						// run the function
						var setValueFunction = new Function(property.setConstructValueFunction);
						this[property.key] = setValueFunction.apply(this,[]);
					} else if (!this[property.key]) {
						// set empty property if not already there
						this[property.key] = null;
					}
				}
			}														
			
			// inc the next id
			_nextId++;
							 																						
		} 
						
	} else {
		alert("ActionClass " + actionType + " could not be found");
	}
	
	return this;
}

// used by showEvents and property_childActions
function getCopyActionName(sourceId) {
	// assume we're not going to find it
	var actionName = "unknown";
	// look for an actions collection
	if (_copyAction.actions) {
		// assume number of actions is simple
		var actionsCount = _copyAction.actions.length;
		// if there is a source id
		if (sourceId) {
			// loop the actions
			for (var i in _copyAction.actions) {
				// if there's a match on the source id
				if (sourceId == _copyAction.actions[i].id) {
					// lose an action as we don't want to paste our own parent 
					actionsCount --;
					// bail
					break;
				}
			}
		}
		// look for a controlType
		if (_copyAction.controlType) {
			// get the source control class
			var sourceControlClass = _controlTypes[_copyAction.controlType];
			// JSON library single member check
			if ($.isArray(sourceControlClass.events.event)) sourceControlClass.events = sourceControlClass.events.event;
			// loop the source control events
			for (var j in sourceControlClass.events) {
				// look for a match
				if (sourceControlClass.events[j].type == _copyAction.event.type) {
					// use the event name and the number of actions
					actionName = sourceControlClass.events[j].name + " event (" + actionsCount + ")";
					// we're done
					break;
				}
			}
		} else if (_copyAction.actionType) {
			// use the property name
			actionName = _copyAction.propertyName + " (" + actionsCount + ")";			
		}
								
	} 
	// if we haven't got a name yet which may happen with single controls, or ones with their own actions collection
	if (actionName == "unknown") {
		// get the action class
		var actionClass = _actionTypes[_copyAction.type];
		// get the name
		actionName = actionClass.name;
		// specify to ignore the actions collection
		_copyAction.ignoreActionsCollection = true;
	}
	return actionName;
}

// this shows the events for the control and eventually the actions
function showEvents(control) {		
	
	// get a reference to the div we are writing in to
	var actionsPanel = $("#actionsPanelDiv");	
	// remove any listeners
	removeListeners("actionsPanel");
	// empty the panel
	actionsPanel.html("");
	
	// only if the page is not simple
	if (_page.simple === undefined || _page.simple != true) {
								
		// only if there is a control and there are events in the control class
		if (control) {
			// get a reference to the control class
			var controlClass = _controlTypes[control.type];
			// get a reference to the events
			var events = controlClass.events;
			// check we have some
			if (events) {
				// JSON library single member check
				if ($.isArray(controlClass.events.event)) events = controlClass.events.event;		
				// loop them
				for (var i in events) {
					// get a reference
					var event = events[i];
					// if the event visibilty has not been set or is not false
					if (event.visible === undefined || !event.visible === false) {
						
						// append a table
						actionsPanel.append("<table class='propertiesPanelTable' data-eventType='" + event.type + "'><tbody></tbody></table>");	
						// get a reference to the table
						var actionsTable = actionsPanel.find("table[data-eventType=" + event.type + "]");
						// add a heading for the event																
						actionsTable.append("<tr><td colspan='2' class='propertyHeader'><h3>" + event.name + " event</h3>" +
								"<div class='iconsPanel'><div class='copyEvent fa-stack fa-xs' title='Copy all event actions'><i class='fa fa-file fa-stack-1x'></i><i class='bottomFile fa fa-file fa-stack-1x'></i></div>" +
								"<i id='" + event.type + "help' class='eventHelp glyph fa hintIcon'></i></div></td></tr>");
						
						// look for any event help html
						var eventHelpHtml = _eventHelpHtml[event.type];
						
						// if this event has helpHtml
						if (eventHelpHtml) {
							// add the help listener
							addHelp(event.type + "help",true,true,eventHelpHtml);
						}
						
						// add a small gap - this is used to add the actions
						actionsTable.append("<tr style='display:none'><td colspan='2'></td></tr>");

						// check if copyAction
						if (_copyAction) {
							// start the action name
							var actionName = getCopyActionName();									 
							// add an add facility
							actionsTable.append("<tr><td class='propertySubHeader'>Add action : </td><td class='propertySubHeader'><select data-event='" + event.type + "'><option value='_'>Please select...</option><optgroup label='New action'>" + _actionOptions + "</optgroup><optgroup label='Paste action'><option value='pasteActions'>" + actionName + "</option></optgroup></select></td></tr>");
						} else {
							// add an add facility
							actionsTable.append("<tr><td class='propertySubHeader'>Add action : </td><td class='propertySubHeader'><select data-event='" + event.type + "'><option value='_'>Please select...</option>" + _actionOptions + "</select></td></tr>");
						}
						// get a reference to the select
						var addAction = actionsTable.find("select[data-event=" + event.type + "]");
						// add a change listener
						addListener( addAction.change( { control: control, event: event }, function(ev) {
							// get a reference to the control
							var control = ev.data.control;
							// get a reference to the eventType
							var eventType = ev.data.event.type;
							// look for the events collection in the control
							for (var i in control.events) {
								// check whether this is the event we want
								if (control.events[i].type == eventType) {
									// get the type of action we selected
									var actionType = $(ev.target).val();
									// check if pasteActions
									if (actionType == "pasteActions") {
										// if _copyAction
										if (_copyAction) {
											// reset the paste map
											_pasteMap = {};
											// check for actions collection that we want to be the root of the paste
											if (_copyAction.actions && !_copyAction.ignoreActionsCollection) {
												// loop them
												for (var j in _copyAction.actions) {
													// create a new object from the action
													var action = JSON.parse(JSON.stringify(_copyAction.actions[j]));
													// add the action using the paste functionality
													control.events[i].actions.push( new Action(action.type, action, true) );
												}										
											} else {
												// create a new object from the action
												var action = JSON.parse(JSON.stringify(_copyAction));
												// add the action using the paste functionality
												control.events[i].actions.push( new Action(action.type, action, true) );
											}
										}
										
									} else {
										// add a new action of this type to the event
										control.events[i].actions.push( new Action(actionType) );
									}							
									// rebuild actions
									showEvents(_selectedControl);
									// we're done
									break;
								}
							}
							
						}));
						
						// show any actions
						showActions(control, event.type);
						
					} // visibility not set or not false
					
				} // event loop	
													
			} // event check
			
		} // control check
		
		// show in case a previous simple property has hidden them
		actionsPanel.show();
		
	} else {
		
		// hide them
		actionsPanel.hide();
		
	} // page simple check
	
	// check size and if need be resize property panel
	windowResize("Show events");
	
}

// this renders the actions for a control's event into a properties panel
function showActions(control, eventType) {
	
	// if this control has events
	if (control.events) {
		
		// get a reference to the div we are writing in to
		var actionsPanel = $("#actionsPanelDiv");		
		
		// loop control events
		for (var i in control.events) {
			
			// if we've found the event we want
			if (control.events[i].type == eventType) {
				
				// get actions
				var actions = control.events[i].actions;
				
				// get a reference to the table
				var actionsTable = actionsPanel.find("table[data-eventtype=" + eventType + "]").children().first();
				
				// check there are actions under this event				
				if (actions && actions.length > 0) {
															
					// remove the lines we don't want
					actionsTable.children("tr:not(:first-child):not(:last-child):not(:nth-last-child(2))").remove();

					// remember how many actions we have
					var actionsCount = 0;
					// loop the actions
					for (var j in actions) {
						
						// inc the count
						actionsCount ++;
						// get the action
						var action = actions[j];											
						
						// show the action
						showAction(actionsTable, action, actions);
						
					} // actions loop
					
					// if there was more than 1 action
					if (actionsCount > 1) {
						// add reorder listeners
						addReorder(actions, actionsTable.find("div.reorder"), function() { showActions(control, eventType); });
					}
					
					// get a reference to the copy image
					var copyImage = actionsTable.find("div.copyEvent").last(); 
					// add a click listener to the copy image
					addListener( copyImage.click( {controlType: control.type, event: control.events[i], actions: actions}, function(ev) {
						// retain a copy of the event data in copyAction
						_copyAction = JSON.parse(JSON.stringify(ev.data));		
						// rebuild the dialogues
						showEvents(_selectedControl);
					}));
														
				} else {
					
					// remove the copyEvent image
					actionsTable.find("div.copyEvent").remove();
					
				} // got actions		
				
				// no need to keep looping events
				break;
			} // event match
		} // event loop		
	} //events

}

// this renders a single action into a table (used by events and childActions)
function showAction(actionsTable, action, collection, refreshFunction) {
	
	// add the action style class
	actionsTable.parent().addClass("actionsPanelDiv");
	
	// get  the action class
	var actionClass = _actionTypes[action.type];
	
	// the position we want to start inserting
	var insertRow = actionsTable.children("tr:nth-last-child(2)");
	
	// add a small break
	insertRow.before("<tr><td colspan='2' class='propertySubHeader'><h3>" + actionClass.name + " action</h3>" +
			"<div class='iconsPanel'><div class='reorder fa-stack fa-sm' title='Reorder this action'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
			"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Delete this action'></i></div>" +
			"<div class='copyAction fa-stack fa-xs' title='Copy this action'><i class='fa fa-file fa-stack-1x'></i><i class='bottomFile fa fa-file fa-stack-1x'></i>" +
			"</div></div></td></tr>");
	
	// if there is helpHtml
	if (actionClass.helpHtml) {
		// add a help icon after the title
		actionsTable.find("h3").last().after("<i id='" + action.id + "help' class='actionHelp glyph fa hintIcon'></i>");
		// add the help listener
		addHelp(action.id + "help",true,true,actionClass.helpHtml);
	}
	// get a reference to the delete image
	var deleteImage = actionsTable.find("div.delete").last(); 
	// add a click listener to the delete image
	addListener( deleteImage.click( {action: action, collection: collection, refreshFunction: refreshFunction}, function(ev) {
		// loop the collection
		for (var i in collection) {
			// if we've found the object
			if (action === collection[i]) {
				// add an undo snapshot
				addUndo();
				// remove from collection
				collection.splice(i,1);
				// refresh (if provided)
				if (refreshFunction) refreshFunction();
				// rebuild actions
				showEvents(_selectedControl);
				// we're done
				break;
			}
		}		
	}));
	// get a reference to the copy image
	var copyImage = actionsTable.find("div.copyAction").last(); 
	// add a click listener to the copy image
	addListener( copyImage.click( { action: action }, function(ev) {
		// copy the action
		_copyAction = JSON.parse(JSON.stringify(ev.data.action));		
		// rebuild actions
		showEvents(_selectedControl);
	}));
	// show the id if requested
	if (_version.showActionIds) insertRow.before("<tr><td>ID</td><td class='canSelect'>" + action.id + "</td></tr>");
	// get the action class properties
	var properties = actionClass.properties;
	// check
	if (properties) {
		// (if a single it's a class not an array due to JSON class conversion from xml)
		if ($.isArray(properties.property)) {
			properties = properties.property; 
		} else {
			properties = [properties.property];
		}
		// add a comments property to the end, if not there already
		if (properties.length > 0 && properties[properties.length - 1].key != "comments")	properties.push({"key":"comments","name":"Comments","changeValueJavaScript":"bigtext","helpHtml":"Comments left here can be useful for other developers that may work on your app."});
		// loop them
		for (var k in properties) {
			// add a row
			insertRow.before("<tr></tr>");
			// get a reference to the row
			var propertiesRow = actionsTable.children("tr:nth-last-child(3)");
			// retrieve a property object from the control class
			var property = properties[k];
			// check that visibility is not explicitly false
			if (property.visible === undefined || !property.visible === false) {
				// assume no help
				var help = "";
				// if the property has help html
				if (property.helpHtml) {
					// make the helpId
					var helpId = action.id + property.key + "help";
					// create help html
					help = "<i id='" + helpId + "' class='actionHelp glyph fa hintIcon'></i>"
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
						changeValueFunction.apply(this,[cell, action, property]);
					} catch (ex) {
						alert("Error - Couldn't apply changeValueJavaScript for " + action.name + "." + property.name + " " + ex);
					}
				} else {
					if (window["Property_" + property.changeValueJavaScript]) {
						window["Property_" + property.changeValueJavaScript](cell, action, property);
					} else {
						alert("Error - There is no known Property_" + property.changeValueJavaScript + " function");
					}
				}			
			} // visibility check
		} // properties loop
	} // properties check
	
}