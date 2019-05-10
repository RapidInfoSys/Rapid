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

// a global for all map listeners so we can de-register
var _mapListeners = [];

// add a map listener
function addMapListener(l) {
	_mapListeners.push(l);
}

// remove all map listeners
function removeMapListeners() {
	// loop all listeners
	for (var l in _mapListeners) {
		_mapListeners[l].unbind();
	}
	// reset the collection
	_mapListeners = [];
}

function createMapEntry(list, c) {
	// get the control class
	var controlClass = _controlTypes[c.type];
	
	// assume no conflict
	var conflict = "";
	// if this can be used in page visibility (it's a form control)
	if (controlClass.canBeUsedForFormPageVisibilty) {	
		// look for any conflict
		conflict = getControlConflict(c);
		// if we got one 
		if (conflict) conflict = "<img src='images/conflict_16x16.png' title='Page \"" + conflict + "\" has a control with the same name' class='conflictimgage'/>";
		// if no regular conflict and this is a form (without the existing conflict check it will be wiped out by a successful form conflict pass)
		if (!conflict && _version.isForm) {
			// look for any form conflict
			conflict = getControlConflict(c, "form");
			// if we got one 
			if (conflict) conflict = "<img src='images/conflict_16x16.png' title='Page \"" + conflict + "\" has a control with the same form integration' class='conflictimgage'/>";
		}
	}

	// create the list entry
	list.append("<li class='" + (c.name ? "pageMapNamed" : "pageMapNoName") + "'><span data-id='" + c.id + "' " + (conflict ? " class='conflict'": "") + ">" + ((controlClass.image) ? "<img src='" + controlClass.image + "' title='" + controlClass.name + "' />" : "") + (c.name ? c.name: controlClass.name) + conflict + "</span></li>");
	// get the list entry
	var li = list.children("li").last();	
	// check for child controls
	if (c.childControls && c.childControls.length > 0) {		
		// add a list for the child controls
		li.append("<ul></ul>");
		// get the child list
		var childList = li.children("ul").last();
		// loop the child controls
		for (var i in c.childControls) {
			// create an entry for the child controls
			createMapEntry(childList, c.childControls[i]);
		}		
	}
}

// scroll to selectec ontrol
function scrollMapToSelectedControl(scrollPage) {
	// get the list
	var list = $("#pageMapList");
	// if there is a selected control
	if (_selectedControl) {
		// find the selected control map entry
		var selectedMapEntry = list.find("span[data-id=" + _selectedControl.id + "]");
		// if there is one
		if (selectedMapEntry[0]) {
			// scroll the list so the selected control is in the middle
			list.scrollTop(list.scrollTop() + selectedMapEntry.offset().top - list.offset().top - list.height()/2);
			// if we're scrolling the page too
			if (scrollPage) {
				// get the verticel scrol bar
				var vScroll = $("#scrollV");
				// if it's visible
				if (vScroll.is(":visible")) {
					// scroll the scroll bar
					$("#scrollV").scrollTop(_selectedControl.object.offset().top);
				} else {
					$(document).scrollTop(_selectedControl.object.offset().top);
				}
			}
		}
	}
}

// rebuild the page map
function buildPageMap(force) {	
	// get the map div
	var map = $("#pageMap");
	// unregister all listener
	removeMapListeners();
	// only if visible or forced
	if (map.is(":visible") || force) {
		// get the list
		var list = $("#pageMapList");
		// empty the current list
		list.children().remove();
		// check we have a page and childControls
		if (_page) {
			// build the map
			createMapEntry(list, _page);
			// prevent dragging on map images
			list.find("img").on('dragstart', function(ev) { ev.preventDefault(); });
			// scroll the map to the selected control
			scrollMapToSelectedControl();
			// add a mousedown listener for all controls
			addMapListener( list.find("li").on("mousedown touchstart", function(ev) {
				// get the target
				var t = $(ev.target);
				// get the id
				var id = t.attr("data-id");
				// get the control
				var c = getControlById(id);
				// if we got one
				if (c) {
					// select the control
					selectControl(c);
					// set mouse down if this control can be moved
					if (_controlTypes[_selectedControl.type].canUserMove) _mouseDown = true;
				}
				// stop bubbling
				event.stopPropagation();
			}));	
			// if the page isn't locked
			if (!_locked) {							
				// add a mouseover listener for all controls
				addMapListener( list.find("li").on("mousemove touchmove", function(ev) {				
					// only if mouse is down and we're not in the process of adding a control
					if (_mouseDown && !_addedControl) {
						// get the target
						var t = $(ev.target);					
						// get the id
						var id = t.attr("data-id");
						// get the control
						var c = getControlById(id);
						// if we got one
						if (c) {
							// if different from current movedover control
							if (c != _movedoverControl || !_movingControl) {
								// remove all insert covers
								$("#pageMapList").find("span.selectionInsertCover").removeClass("selectionInsertCover");
								// add the insert cover if not the selected control too
								t.addClass("selectionInsertCover");
								// rememeber we are moving a control
								_movingControl = true;
							}
							// set the movedoverControl
							_movedoverControl = c;							
							// check whether a decendent,
							if (isDecendant(_selectedControl,_movedoverControl) || _movedoverControl == null) {
								// null the movedoverControl
								_movedoverControl = null;	
							} else {
								
								var movedOverType = _controlTypes[_movedoverControl.type];
								
								// get the span width
								var width =  t.width();
								// calculate a move threshold which is the number of pixels to the left or right of the object the users needs to be within
								var moveThreshold = Math.min(50, width/3);
								// if it's not possible to insert make the move thresholds half the width to cover the full object
								if (!movedOverType.canUserInsert) moveThreshold = width/2;
								
								// get the current cusor
								var cursor = t.attr("move");
								// are we within the move threshold on the left or the right controls that can be moved, or in the middle with an addChildControl method?
								if (movedOverType.canUserMove && ev.pageX  < t.offset().left + moveThreshold) {								
									// set the cursor if need be
									if (!cursor || cursor.indexOf("moveLeft") < 0) t.removeClass("mapMoveRight mapInsert").addClass("mapMoveLeft").attr("move","moveLeft");									
									// remember it's on the left
									_movedoverDirection = "L";
								} else if (movedOverType.canUserMove && ev.pageX > t.offset().left + width - moveThreshold) {
									// set the cursor if need be
									if (!cursor || cursor.indexOf("moveRight") < 0) t.removeClass("mapMoveLeft mapInsert").addClass("mapMoveRight").attr("move","moveRight");								
									// remember it's on the right
									_movedoverDirection = "R";
								} else if (movedOverType.canUserInsert) {
									// set the cursor if need be
									if (!cursor || cursor.indexOf("insert") < 0) t.removeClass("mapMoveLeft mapMoveRight").addClass("mapInsert").attr("move","insert");	
									// remember it's in the the centre
									_movedoverDirection = "C";
								} else {
									// reset the cursor
									t.removeClass("mapMoveLeft mapMoveRight mapInsert").attr("move","");	
									// null the direction
									_movedoverDirection = null;
									// remove all insert covers
									$("#pageMapList").find("span.selectionInsertCover").removeClass("selectionInsertCover");
								}		
															
							} // decendant check 								
						} // new moved over controls							
	
					} // mouse down
					// stop bubbling
					event.stopPropagation();
				}));	
				// add a mouseover listener an up anywhere in the box
				addMapListener( list.on("mouseup touchend", function(ev) {				
					// if there is a moved over control
					if (_movedoverControl) {
						// fire the main page mouse up
						
					}
				}));	
				// add a mouseout listener for the box
				addMapListener( list.on("mouseleave touchcancel", function(ev) {				
					// reset moved over control
					_movedoverControl = null;
					// remove all insert covers
					$("#pageMapList").find("span.selectionInsertCover").removeClass("selectionInsertCover");
					// set all cursors to default
					$("#pageMapList").find("span").css("cursor","initial");
				}));	
			}
			// highlight the selected control
			if (_selectedControl) {
				// highlight selected control span and parent li
				list.find("span[data-id=" + _selectedControl.id + "]").addClass("selected").parent().addClass("selected");
			}
		}	
	}
}