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

/*

Functions used to create an modify controls are here so they can be loaded by the server script engine too

*/

// this keeps a count of controls of each type in order to produce label 1, label 2, table 1, etc
var _controlNumbers = {};

// this function is called iteratively when loading in the controls (used by both designer and script engine)
function loadControl(jsonControl, parentControl, loadActions, paste, undo) {	
	// we need to init the control
	var control = new Control(jsonControl.type, parentControl, jsonControl, loadActions, paste, undo);
	// if we have child controls
	if (jsonControl.childControls) {
		// loop and add
		for (var i = 0; i < jsonControl.childControls.length; i++) {
			// get the child control json
			var jsonChildControl = jsonControl.childControls[i];
			// load us up the child control (using this function called iteratively)
			var childControl = loadControl(jsonChildControl, control, loadActions, paste, undo);
			// add it to our childControls
			control.childControls.push(childControl);
		}
	}
	// run any rebuild JavaScript (if present)
	if (control._rebuild) {
		try {			
			control._rebuild();			
		} catch(ex) {
			alert("rebuildJavaScript failed for " + control.type + ". " + ex);
		}
	}	
	// return 
	return control;
			
}

//this object function serves as a closure for holding the static values required to construct each type of control - they're created and assigned globally when the designer loads and originate from the .control.xml files
function ControlClass(controlClass) {
	// retain all values passed in the json for the control (from the .control.xml file)
	for (var i in controlClass) this[i] = controlClass[i];
}

// this object function will create the control as specified in the controlClass, jsonControl is from a previously saved control, or paste, loadActions can avoid loading the action objects into the control (the page renderer doesn't need them), and paste can generate new id's
function Control(controlType, parentControl, jsonControl, loadComplexObjects, paste, undo) {
			
	// get the type into the "class"
	var controlClass = _controlTypes[controlType];
	// check controlClass exists
	if (controlClass) {
								
		// retain the type
		this.type = controlClass.type;
		
		// retain the version
		this.version = controlClass.version;
		
		// store a count for number of controls of this type if not undoing
		if (!undo) {
			if (!_controlNumbers[controlClass.type]) {
				_controlNumbers[controlClass.type] = 1;
			} else {
				_controlNumbers[controlClass.type] ++;
			}
		}
		
		// retain the parentControl
		this._parent = parentControl;
		
		// create an empty childControl array
		this.childControls = new Array();
						
		// if we're loading complex object like validation, actions, and events (the Rhino page renderer doesn't need them)
		if (loadComplexObjects) {
			// get the class events
			var classEvents = controlClass.events;
			// only if this control has them
			if (classEvents) {
				if ($.isArray(classEvents.event)) classEvents = classEvents.event;
				// create an empty events array to hold events/actions
				this.events = new Array();			
				// and an event object for each event
				for (var i in classEvents) {
					// create an event object
					var event = {};
					// set the type
					event.type = classEvents[i].type;
					// set the filterFunction
					event.filterFunction = classEvents[i].filterFunction;
					// make an actions array
					event.actions = new Array();						
					// add event 
					this.events.push(event);			
				}
			}
			// create an array to hold any styles
			this.styles = new Array();
		}
						
		// if we were given jsonControl (usually in a page load) retain its properties and avoid initialisation
		if (jsonControl) {
					
			// check paste/undo/normal
			if (paste || undo) {
				// copy all properties into this control (expect for complex objects: _parent, roles, validation, childControls, events, and styles - [we've set them already and don't want them overwritten, unless it's the page child controls])
				for (var i in jsonControl) {
					if (i != "_parent" && i != "validation" && i != "events" && i != "styles" && (i != "childControls" || !controlClass)) this[i] = jsonControl[i];								
				}
				// only if not the page
				if (this._parent) {
					// give this control a new, unique id when pasting
					if (paste) {					
						// set a unique ID for this control
						this.id = _page.id + "_C" + _nextId + "_";
						// if this is the rapid app, prefix it
						if (_version.id == "rapid") this.id = "rapid_" + this.id;
						// check the pasteMap
						if (_pasteMap[jsonControl.id]) {
							// if there is an entry for the control we are making this one out of use it's id
							this.id = jsonControl.id;
						} else {
							// add an entry for this control into the paste map so we use this id (not the next one)
							_pasteMap[this.id] = jsonControl.id;
						}					
						// inc the next id
						_nextId++;	
					}
					// make sure the next id is greater than any control when undoing
					if (undo) {
						// get the id value into a variable
						var id = this.id;
						// the id will be something like P99_C12, find the number after the _C
						var idInt = parseInt(id.substr(id.indexOf("_C") + 2));
						// set the next id to one past this if it is less
						if (idInt >= _nextId) _nextId = idInt + 1;	
					}
				}				
			} else {
				// the page control doesn't have a properties array
				if (!jsonControl.properties && !jsonControl._parent) {
					// copy all properties in from the page control
					for (var i in jsonControl) {	
						// retain the property in the control class, unless events
						if (i != "events") this[i] = jsonControl[i];
					}
				} else {
					// loop and retain the properties in the save control
					for (var i in jsonControl.properties) {	
						// retrieve the property
						var p = jsonControl.properties[i];
						// if it looks like an array parse it with JSON
						if (p && p.length >= 2 && p.substr(0,1) == "[" && p.substr(p.length-1,1) == "]") p = JSON.parse(p);
						// retain the property in the control class
						this[i] = p;
						// make sure the id's are always unique 
						if (i == "id") {
							// get the id value into a variable
							var id = jsonControl.properties["id"];
							// the id will be something like P99_C12, find the number after the _C
							var idInt = parseInt(id.substr(id.indexOf("_C") + 2));
							// set the next id to one past this if it is less
							if (idInt >= _nextId) _nextId = idInt + 1;						
						}
					}
				}								
			} // if paste
											
			// page renderer does not use validation, actions, or styles
			if (loadComplexObjects) {
				// add any validation
				if (jsonControl.validation) this.validation = jsonControl.validation;
				// add any events and action objects if present
				if (jsonControl.events) {
					// loop the events we've been given
					for (var i in jsonControl.events) {						
						// loop the events we've created
						for (var j in this.events) {
							// get a reference to the event
							var event = this.events[j];
							// match the jsonEvent to the event object
							if (event.type == jsonControl.events[i].type) {								
								// loop actions
								for (var k in jsonControl.events[i].actions) {
									// get our jsonAction
									var jsonAction = jsonControl.events[i].actions[k];
									// add new action object into event
									event.actions.push(new Action(jsonAction.type, jsonAction, paste, undo));
								}
								// we're finished with this event
								break;
							}							
						}																									
					}
				} // got events
				// add any styles the json may have
				if (jsonControl.styles) this.styles = jsonControl.styles;
			} // loadActionsAndStyles			 
			
		} else {
			
			// set a unique ID for this control (the last underscore is to stop _C12 being replaced by _C1)
			this.id = _page.id + "_C" + _nextId + "_";
			// inc the next id
			_nextId++;
			// if this is the rapid app append a prefix so as not to be affected by any app they are used with
			if (_version.id == "rapid") this.id = "rapid_" + this.id;
			
			// if required set a name for this control, using our store of numbers of this control type
			if (!controlClass.noDefaultName) this.name = controlClass.name + " " + _controlNumbers[controlClass.type];	
			
			// if the control class has properties set them here
			if (controlClass.properties) {
				// get a reference to the properties
				var properties = controlClass.properties;
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
							 																						
		} // json control
				
		// set and run the getHtml statement
		try {			
			// create a function from this text and retain a refence to it
			this._getHtml = new Function(controlClass.getHtmlFunction.trim());
			// run it against this controls
			this._html = this._getHtml.apply(this, []);
		} catch (ex) {
			// show error message in place of xml
			this._html = "<span>getHtmlFunction failed for " + this.type + ". " + ex + "</span>";
			// remember there is an error (stops properties and styles being rendered)
			this.error = true;
		}
		
		// append the new html to the parent object 
		if (controlClass.appendJavaScript) {
			// clean up the JavaScript by triming and removing line breaks and tabs
			var js = controlClass.appendJavaScript.trim();
			// try and apply it
			try {				
				// get the js into a new function variable
				var f = new Function(js);
				// retain a reference to it (which is used for rebuilding the html on property updates)
				this._append = f;
				// apply it to this control, passing in the html
				f.apply(this, []);
			} catch (ex) {
				alert("appendJavaScript failed for " + this.type + ". " + ex + "\r\r" + js);
				// remember there is an error (stops properties and styles being rendered)
				this.error = true;
			}
		} else {
			this._parent.object.append(this._html);
		}
		
		// if this is not the page
		if (this._parent) {
			// grab a reference to the object
			this.object = this._parent.object.children().last();
		}
						
		// run the create statement if there is one (can reassign the object or add other html)
		if (controlClass.createJavaScript) {
			// clean up the JavaScript by triming and removing line breaks and tabs
			var js = controlClass.createJavaScript.trim();	
			// try and apply it
			try {									
				// get the js into a new function variable
				var f = new Function(js);
				// apply it to this control
				f.apply(this, []);
			} catch (ex) {
				alert("createJavaScript failed for " + this.type + ". " + ex + "\r\r" + js);
				// remember there is an error (stops properties and styles being rendered)
				this.error = true;
			}
		}
									
		// select the peer on on the left
		if (controlClass.selectLeftJavaScript) {
			// clean up the JavaScript by triming and removing line breaks and tabs
			var f = new Function(controlClass.selectLeftJavaScript.trim());
			// retain a reference to it (which is used later when deleting the control)
			this._selectLeft = f;
		}
		
		// select the peer on on the right
		if (controlClass.selectRightJavaScript) {
			// clean up the JavaScript by triming and removing line breaks and tabs
			var f = new Function(controlClass.selectRightJavaScript.trim());
			// retain a reference to it (which is used later when deleting the control)
			this._selectRight = f;		
		}
		
		// swap with the peer on on the left
		if (controlClass.swapLeftJavaScript) {
			// clean up the JavaScript by triming and removing line breaks and tabs
			var f = new Function(controlClass.swapLeftJavaScript.trim());
			// retain a reference to it (which is used later when deleting the control)
			this._swapLeft = f;
		}
		
		// swap with the peer on on the right
		if (controlClass.swapRightJavaScript) {
			// clean up the JavaScript by triming and removing line breaks and tabs
			var f = new Function(controlClass.swapRightJavaScript.trim());
			// retain a reference to it (which is used later when deleting the control)
			this._swapRight = f;		
		}
		
		// insert a peer on on the left
		if (controlClass.insertLeftJavaScript) {
			// clean up the JavaScript by triming and removing line breaks and tabs
			var f = new Function(controlClass.insertLeftJavaScript.trim());
			// retain a reference to it (which is used later when deleting the control)
			this._insertLeft = f;
		} 
		
		// insert a peer on on the right
		if (controlClass.insertRightJavaScript) {
			// clean up the JavaScript by triming and removing line breaks and tabs
			var f = new Function(controlClass.insertRightJavaScript.trim());
			// retain a reference to it (which is used later when deleting the control)
			this._insertRight = f;
		} 
		
		// remove the new html from the parent object 
		if (controlClass.removeJavaScript) {
			// get the js into a new function variable
			var f = new Function(controlClass.removeJavaScript.trim());
			// retain a reference to it (which is used later when deleting the control)
			this._remove = f;
		} else {
			this._remove = function() { this.object.remove(); };
		}
																	
		// run the constructJavaScript only for brand new controls (needs to be here so the parent object has been created)
		if (!jsonControl) {			
			// run any constructJavaScript		
			if (controlClass.constructJavaScript)	{
				// clean up the JavaScript by triming and removing line breaks and tabs
				var js = controlClass.constructJavaScript.trim();
				// try and apply it
				try {				
					// get the js into a new function variable
					var f = new Function(js);
					// apply it
					f.apply(this, []);
				} catch (ex) {
					alert("constructJavaScript failed for " + this.type + ". " + ex + "\r\r" + js);
					// remember there is an error (stops properties and styles being rendered)
					this.error = true;
				}
			}		
		} 				
		
		// apply and run any rebuildJavaScript		
		if (controlClass.rebuildJavaScript)	{
			// clean up the JavaScript by triming and removing line breaks and tabs
			var js = controlClass.rebuildJavaScript.trim();
			// try and apply it
			try {				
				// get the js into a new function variable
				var f = new Function(js);
				// retain a reference to it
				this._rebuild = f;
				// apply it
				f.apply(this, []);				
			} catch (ex) {
				alert("rebuildJavaScript failed for " + this.type + ". " + ex + "\r\r" + js);
				// remember there is an error (stops properties and styles being rendered)
				this.error = true;
			}
		}
		
		// there might also be some initDesignJavaScript
		if (controlClass.initDesignJavaScript)	{
			// clean up the JavaScript by triming and removing line breaks and tabs
			var js = controlClass.initDesignJavaScript.trim();
			// try and apply it
			try {				
				// get the js into a new function variable
				var f = new Function(js);
				// retain a reference to it for complex inserts/rebuild
				this._initDesign = f;
				// run it
				f.apply(this, []);
			} catch (ex) {
				alert("initDesignJavaScript failed for " + this.type + ". " + ex + "\r\r" + js);
				// remember there is an error (stops properties and styles being rendered)
				this.error = true;
			}
		}
		
		// and some saveJavaScript (cleans up the html on saving, can remove any html put in for demonstration)
		if (controlClass.saveJavaScript)	{
			// clean up the JavaScript by triming and removing line breaks and tabs
			var js = controlClass.saveJavaScript.trim();
			// try and apply it
			try {				
				// get the js into a new function variable
				var f = new Function(js);
				// retain a reference to it for complex inserts/rebuild
				this._save = f;
			} catch (ex) {
				alert("saveJavaScript failed for " + this.type + ". " + ex + "\r\r" + js);
				// remember there is an error (stops properties and styles being rendered)
				this.error = true;
			}
		}
		
		// apply and run any getDetailsFunction JavaScript		
		if (controlClass.getDetailsFunction)	{
			// clean up the JavaScript by triming and removing line breaks and tabs
			var js = controlClass.getDetailsFunction.trim();
			// try and apply it
			try {				
				// get the js into a new function variable
				var f = new Function(js);
				// retain a reference to it
				this._getDetails = f;
				// apply it
				this.details = f.apply(this, []);				
			} catch (ex) {
				alert("getDetailsFunction failed for " + this.type + ". " + ex + "\r\r" + js);
				// remember there is an error (stops properties and styles being rendered)
				this.error = true;
			}
		}
		
		// retain that there is initJavaScript (if applicable - this is placed into the page later)
		if (controlClass.initJavaScript) this.initJavaScript = true; 
		
		// retain that this control can be used from other pages, if applicable
		if (controlClass.canBeUsedFromOtherPages) this.canBeUsedFromOtherPages = true; 
		
		// if our control looks like a non visible
		if (this.object && this.object.is(".nonVisibleControl")) {
			// add a selection listener
			this.object.click( function(ev) {
				selectControl(getControlById($(ev.target).attr("id")));
			});
		}
								
	} else {
		var parentType = _parent.type;
		if (!parentType) parentType = "page";
		alert("ControlClass could not be found when called from " + parentType);
	}
}

// rebuild the html for a given control used by updateProperty and .control.xml initJavaScript listeners, and swap peers
function rebuildHtml(control) {
	// only if the page isn't locked
	if (!_locked) {
		// get the control class
		var controlClass = _controlTypes[control.type];
		// assume the panelpin offset 
		panelPinnedOffset = _panelPinnedOffset;
		// run any rebuild JavaScript (if present) - and this is not the page
		if (control._rebuild) {
			try {
				control._rebuild();			
			} catch(ex) {
				alert("rebuildJavaScript failed for " + control.type + ". " + ex);
			}
		} else if (control._parent) {
			// get the new html
			var html = control._getHtml();
			// append the new html to the page object 
			_page.object.append(html);
			// get a reference to the new object
			var object = _page.object.children().last();
			// controls like dropdown and radiobuttons must replace the existing child controls from the new control, others must keep them
			if (!controlClass.updateChildObjects && control.childControls.length > 0) {
				// remove any child elements from this new object as we're about to copy in the old ones
				object.children().remove();
				// move in any child elements
				control.object.children().each( function() {
					object.append(this);
				});
			}
			// move and remove only if there is more than one object with this id
			if (_page.object.find("[id=" + control.id + "]").length > 1) {
				// add it after the current object 
				control.object.after(object); 
				// remove the old object
				control.object.remove(); 
			}			
			// attach the new object
			control.object = object; 
		}
		
		// run any getDetailsJavaScript function (if present) - this creates a "details" object which is passed to the getData and setData calls
		if (control._getDetails) control.details = control._getDetails();
		
		// if our control looks like a non visible
		if (control.object.is(".nonVisibleControl")) {
			// add a selection listener
			control.object.click( function(ev) {
				selectControl(getControlById($(ev.target).attr("id")));
			});
			// arrange the controls
			arrangeNonVisibleControls();
		}
		
		// resize and reposition the selection as the geometry may have changed
		positionAndSizeBorder(control);		
	} // page lock check
	
}

// this function interatively finds a control by an id
function getControlById(id, control) {
	if (_page.id == id) return _page;
	var foundControl = null;
	if (control) {
		if (control.id == id) {
			return control;
		} else {
			if (control.childControls && control.childControls.length > 0) {
				for (var i in control.childControls) {
					foundControl = getControlById(id, control.childControls[i]);
					if (foundControl) break;
				}
			}
		}		
	} else {		
		// check first level controls
		for (var i in _page.childControls) {
			foundControl = getControlById(id, _page.childControls[i]);
			if (foundControl) break;
		}
		// check for other page controls if still not found
		if (!foundControl && _page && _pages) {
			for (var i in _pages) {
				if (_pages[i].id != _page.id && _pages[i].controls) {
					for (var j in _pages[i].controls) {
						if (_pages[i].controls[j].id ==  id) {
							// set the found control
							foundControl = _pages[i].controls[j];
							// add the page name
							foundControl._pageName = _pages[i].name;
							break;
						}							
					}
				}	
				if (foundControl) break;
			}
		}
	}
	return foundControl;
}

