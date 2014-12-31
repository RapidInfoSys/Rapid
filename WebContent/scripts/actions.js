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
					this.id = _page.id + "_A" + _nextId + "_";
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
						// look for "actions" collections
						if (i.toLowerCase().indexOf("actions") > -1 && $.isArray(this[i])) {
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
			this.id = _page.id + "_A" + _nextId + "_";
						
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
		alert("ActionClass could not be found");
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
								
	} else {
		// get the action class
		var actionClass = _actionTypes[_copyAction.type];
		// get the name
		actionName = actionClass.name;
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
				// append a table
				actionsPanel.append("<table class='propertiesPanelTable' data-eventType='" + event.type + "'><tbody></tbody></table>");	
				// get a reference to the table
				var actionsTable = actionsPanel.children().last().children().last();
				// add a heading for the event
				actionsTable.append("<tr><td colspan='2'><h3>" + event.name + " event</h3><img class='copyEvent' src='images/copy_16x16.png' title='Copy all event actions'/></td></tr>");																	
				// add a small break
				actionsTable.append("<tr><td colspan='2'></td></tr>");
				// check if copyAction
				if (_copyAction) {
					// start the action name
					var actionName = getCopyActionName();									 
					// add an add facility
					actionsTable.append("<tr><td>Add action : </td><td><select data-event='" + event.type + "'><option value='_'>Please select...</option><optgroup label='New action'>" + _actionOptions + "</optgroup><optgroup label='Paste action'><option value='pasteActions'>" + actionName + "</option></optgroup></select></td></tr>");
				} else {
					// add an add facility
					actionsTable.append("<tr><td>Add action : </td><td><select data-event='" + event.type + "'><option value='_'>Please select...</option>" + _actionOptions + "</select></td></tr>");
				}				
				// get a reference to the select
				var addAction = actionsTable.children().last().children().last().children().last();
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
									// check for actions collection
									if (_copyAction.actions) {
										// loop them
										for (var j in _copyAction.actions) {
											// get the action
											var action = _copyAction.actions[j];
											// add the action using the paste functionality
											control.events[i].actions.push( new Action(action.type, action, true) );
										}										
									} else {
										// add the action using the paste functionality
										control.events[i].actions.push( new Action(_copyAction.type, _copyAction, true) );
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
				
			} // event loop	
												
		} // event check
		
	} // control check
	
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
				var actionsTable = actionsPanel.children().last().children().last();
				
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
						addReorder(actions, actionsTable.find("img.reorder"), function() { showActions(control, eventType); });
					}
					
					// get a reference to the copy image
					var copyImage = actionsTable.find("img.copyEvent").last(); 
					// add a click listener to the copy image
					addListener( copyImage.click( {controlType: control.type, event: control.events[i], actions: actions}, function(ev) {
						// retain all of the event data in copyAction
						_copyAction = ev.data;		
						// rebuild the dialogues
						showEvents(_selectedControl);
					}));
														
				} else {
					
					// remove the copyEvent image
					actionsTable.find("img.copyEvent").remove();
					
				} // got actions		
				
				// no need to keep looping events
				break;
			} // event match
		} // event loop		
	} //events

}

//this renders a single action into a table (used by events and childActions)
function showAction(actionsTable, action, collection, refreshFunction) {
	
	// get  the action class
	var actionClass = _actionTypes[action.type];
	
	// the position we want to start inserting
	var insertRow = actionsTable.children("tr:nth-last-child(2)");
	
	// add a small break
	insertRow.before("<tr><td colspan='2'></td></tr>");
	// write action name into the table						
	insertRow.before("<tr><td colspan='2'><h4>" + actionClass.name + " action</h4><img class='delete' src='images/bin_16x16.png' title='Delete this action'/><img class='reorder' src='images/moveUpDown_16x16.png' title='Reorder this action'/><img class='copyAction' src='images/copy_16x16.png' title='Copy this action'/></td></tr>");
	// get a reference to the delete image
	var deleteImage = actionsTable.find("img.delete").last(); 
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
	var copyImage = actionsTable.find("img.copyAction").last(); 
	// add a click listener to the copy image
	addListener( copyImage.click( { action: action }, function(ev) {
		// copy the action
		_copyAction = ev.data.action;		
		// rebuild actions
		showEvents(_selectedControl);
	}));
	// show the id if requested
	if (_version.showActionIds) insertRow.before("<tr><td>ID</td><td class='canSelect'>" + action.id + "</td></tr>");
	// get the action class properties
	var properties = actionClass.properties;
	// check
	if (properties) {
		// (if a single it's a class not an array due to JSON class conversionf from xml)
		if ($.isArray(properties.property)) properties = properties.property; 
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
				// get the property itself from the action
				propertiesRow.append("<td>" + property.name + "</td><td></td>");
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