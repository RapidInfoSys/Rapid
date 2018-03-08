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

// details of the soa service currently being viewed
var _soaDetails = {};

// these types match the static final values in the SOASchema class 
var _soaDataTypes = [
	{name: "string", value: 1},
	{name: "integer", value: 2},
	{name: "decimal", value: 3},
	{name: "date", value: 4},
	{name: "dateTime", value: 5},
	{name: "boolean", value: 6}
];

// these are the restrictions of the element current being viewed
var _soaRestrictions = [];

// the type must be the name of the class extending SOAElementRestriction
var _soaRestrictionTypes = [	                   
	{type: "MinOccursRestriction", name: "minOccurs"},
	{type: "MaxOccursRestriction", name: "maxOccurs"},
	{type: "MinLengthRestriction", name: "minLength", dataTypes: [1]},
	{type: "MaxLengthRestriction", name: "maxLength", dataTypes: [1]},
	{type: "EnumerationRestriction", name: "enumeration", dataTypes: [1]}
];

// this is required for some of the code from the properties
var _listeners = [];

// this is required for re-using reordering
$(document).mouseup( function(ev) {
	_reorderDetails = null;
});

function getSOADataTypeSelect(selectedDataType) {
	var select = "<select>";
	for (var i in _soaDataTypes) {
		select += "<option value='" + _soaDataTypes[i].value + "' " + (selectedDataType == _soaDataTypes[i].value ? "selected='selected'" : "") + ">" + _soaDataTypes[i].name + "</option>";
	}
	select += "</select>";
	return select;
}

function getSOARestrictionSelect(selectedDataType, selectedRestriction) {
	var select = "<select>";
	for (var i in _soaRestrictionTypes) {
		// get a reference to this type
		var restrictionType = _soaRestrictionTypes[i];
		// assume the restriction is not allowed with the type
		var restrictionMatchesType = false;
		// if a dataTypes collection is present the restriction type must exist
		if (restrictionType.dataTypes) {
			// loop the allowed type collection
			for (var j in restrictionType.dataTypes) {
				if (selectedDataType == restrictionType.dataTypes[j]) {
					restrictionMatchesType = true;
					break;
				}
			}
		} 
		// display either if there is no dataTypes limit list or there was a match
		if (!restrictionType.dataTypes || restrictionMatchesType) {
			select += "<option value='" +  restrictionType.type + "' " + (selectedRestriction == restrictionType.type ? "selected='selected'" : "") + ">" + restrictionType.name + "</option>";
		}
	}
	select += "</select>";
	return select;
}

function getRestrictionLabel(restriction) {
	for (var i in _soaRestrictionTypes) {
		// get a reference to this type
		var restrictionType = _soaRestrictionTypes[i];
		// if a dataType is present it must match
		if (restrictionType.type == restriction.type) {
			return restrictionType.name + "(" + restriction.value + ")";;
		}
	}
	return "unknown";
}

function showSOARestrictions(collection, control, details) {
	
	// get element from collection
	var element = collection[control.parent().index() - 1];
	// add restrictions collection if need be
	if (!element.restrictions) element.restrictions = [];
	// set restrictions collection
	_soaRestrictions = element.restrictions;
	
	// get a reference to the dialogue
	var dialogue = $("#restrictionsDialogue");
	// if we didn't find one add
	if (!dialogue[0]) {
		// add the div
		$("body").append("<div id='restrictionsDialogue' style='position:absolute;display:none;width:280px;border:1px solid black;background-color:white;font-size:11px;padding:10px;'></div>");
		// now get the reference
		dialogue = $("#restrictionsDialogue");
		// add close link
		var close = dialogue.append("<b style='float:left;margin-top:-5px;'>Restrictions</b><a href='#' style='float:right;margin-top:-5px;'>close</a></div>").children().last();
		// add close listener
		_listeners.push( close.click( {details: details}, function(ev) {
			// hide the dialogue
			dialogue.hide();
			// refresh dialogue
			loadSOA(ev.data.details);			
		}));	
		// add table
		dialogue.append("<table class='dialogueTable'></table>");		
	}

	// remove the add link
	dialogue.find("a.add").last().remove();
	
	// replace the add link
	dialogue.append("<a href='#' class='add'>add...</a>").find("a").last().click( {collection: collection, control : control, details:details}, function(ev) {
		// add new restriction
		_soaRestrictions.push({type:"MinOccursRestriction", value:"1"});
		// refresh dialogue
		showSOARestrictions(ev.data.collection, ev.data.control, ev.data.details);				
	});
	
	// get a reference to the table
	var table = dialogue.find("table");
	// rebuild table
	table.html("<tr><td style='padding-top:0;'><b>Type</b></td><td><b>Value</b></td><td style='width:32px;'>&nbsp;</td></tr>");
			
	// loop restrictions
	for (var i in _soaRestrictions) {
		// get individual restriction
		var restriction = _soaRestrictions[i];
		// add a row for this restriction
		table.append("<tr><td>" + getSOARestrictionSelect(element.dataType, restriction.type) + "</td><td><input value='" + restriction.value + "'/></td><td><img class='delete' src='images/bin_16x16.png' /><img class='reorder' src='images/moveUpDown_16x16.png' /></td></tr>");				
	}
	
	// type change listener
	table.find("select").change( function(ev) {
		// get select
		var select = $(ev.target);
		// assign new value
		_soaRestrictions[select.parent().parent().index() - 1].type = select.val();
	});
	
	// value change listener
	table.find("input").keyup( function(ev) {
		// get input
		var input = $(ev.target);
		// assign new value
		_soaRestrictions[input.parent().parent().index() - 1].value = input.val();
	});
	
	// delete listener
	table.find("img.delete").click( {collection: collection, control: control, details:details}, function(ev) {
		// remove item
		_soaRestrictions.splice($(ev.target).parent().parent().index() - 1, 1);
		// refresh
		showSOARestrictions(ev.data.collection, ev.data.control, ev.data.details);
	});
	
	// reorder listener
	addReorder(_soaRestrictions, table.find("img.reorder"), function(ev) {
		showSOARestrictions(collection, control);
	});
	
	// positon and show the dialog
	dialogue.css({
		top: control.offset().top,
		left: control.offset().left - 200,
		display: "block"
	});
}

// this function is called from the GETSOA Rapid action
function loadSOA(details) {
		
	// hide the sql webservice panel
	$("#rapid_P0_C489_").hide();
	// hide the java class webservice panel
	$("#rapid_P0_C991_").hide();
	
	switch (details.type) {
	case "SQLWebservice" :			
		
		// retain the details in the global
		_soaDetails = details;
		
		// update the name to an empty string if not provided
		if (!details.name) details.name = "";
		// put the sql in the input box
		$('#rapid_P0_C496_').val(details.name);
		
		// update the sql to an empty string if not provided
		if (!details.databaseConnectionIndex) details.databaseConnectionIndex = 0;
		// put the sql in the input box
		$('#rapid_P0_C536_').val(details.databaseConnectionIndex);
		
		// update the sql to an empty string if not provided
		if (!details.SQL) details.SQL = "";
		// put the sql in the input box
		$('#rapid_P0_C559_').val(details.SQL);
		
		
		// get the cell we write the request properties into
		var requestCell = $('#rapid_P0_C589_');		
		// look for a table
		var requestTable = requestCell.children("table");
		// if not there
		if (!requestTable[0]) {
			// add a header
			requestCell.append("<span class='webserviceTitle'>Child elements :</span>");
			// create table
			requestTable = requestCell.append("<table class='dialogueTable webserviceTable'></table>").children("table");
		}
		// populate the table header row
		requestTable.html("<tr><td>Name</td><td>Data type</td><td>Restrictions</td><td style='width:32px;min-width:32px;'>&nbsp;</td></tr>");
		// if there's a request object in the details with a root element
		if (details.requestSchema && details.requestSchema.rootElement) {
			// retrieve the request root element
			var requestElement = details.requestSchema.rootElement;
			// populate the name
			$('#rapid_P0_C593_').val(requestElement.name);
			// loop the child elements
			for (var i in requestElement.childElements) {
				// get child element
				var element = requestElement.childElements[i];
				// get a string for the restrictions
				var text = "click to add...";
				// check we have some
				if (element.restrictions && element.restrictions.length > 0) {
					text = "";
					for (var j in element.restrictions) {
						text += getRestrictionLabel(element.restrictions[j]);
						if (j < element.restrictions.length -1) text += ",";
					}
				}
				// populate child element
				requestTable.append("<tr><td class='elementName'><input value='" + element.name + "' /></td><td>" + getSOADataTypeSelect(element.dataType) + "</td><td class='restriction'>" + text + "</td><td><img class='delete' src='images/bin_16x16.png' /><img class='reorder' src='images/moveUpDown_16x16.png' /></td></tr>");				
			}
			
			// name
			requestTable.find("input").keyup( details, function(ev) {
				// get input
				var input = $(ev.target);
				// update value
				ev.data.requestSchema.rootElement.childElements[input.parent().parent().index()-1].name = input.val();
			});
			
			// data type
			requestTable.find("select").change( details, function(ev) {
				// get select
				var select = $(ev.target);
				// update value
				ev.data.requestSchema.rootElement.childElements[select.parent().parent().index()-1].dataType = select.val();
			});
			
			// restrictions
			requestTable.find("td.restriction").click( details, function(ev) {
				// show details
				showSOARestrictions(requestElement.childElements, $(ev.target), ev.data);
			});
			
			// reorder
			addReorder(requestElement.childElements, requestTable.find("img.reorder"), function(ev) {
				loadSOA(details);
			});
			
			// delete
			requestTable.find("img.delete").click( details, function(ev) {
				// remove from array
				ev.data.requestSchema.rootElement.childElements.splice($(ev.target).parent().parent().index()-1,1);
				// refresh
				loadSOA(ev.data);
			});
						
		} else {
			// empty the name
			$('#rapid_P0_C593_').val("");
		}
		
		// add link
		requestTable.append("<tr><td colspan='4'><a href='#'>add...</a></td></tr>");
		// add click
		requestTable.find("a").last().click(details, function(ev) {
			// initialise request object if need be
			if (!ev.data.requestSchema) ev.data.requestSchema = {}; 
			// get response object
			var requestSchema = ev.data.requestSchema;	
			// initialise root object if need be
			if (!requestSchema.rootElement) requestSchema.rootElement = {};
			// get the root element
			var requestElement = requestSchema.rootElement;
			// initialise array if need be
			if (!requestElement.childElements) requestElement.childElements = [];
			// add new element
			requestElement.childElements.push({name:"", dataType: 1});
			// refresh
			loadSOA(ev.data);
		});
		
		// response is ever so slightly different as it has a single/collection radio (for isArray) and field column
		
		// set the single/collection to single by default
		$("#rapid_P0_C598_0").prop("checked", true);
		
		// get the cell we're writing this into
		var responseCell = $('#rapid_P0_C592_');		
		// look for a table
		var responseTable = responseCell.children("table");
		// if not there
		if (!responseTable[0]) {
			// add a header
			responseCell.append("<span class='webserviceTitle'>Child elements :</span>");
			// create table
			responseTable = responseCell.append("<table class='dialogueTable webserviceTable'></table>").children("table");
		}
		// populate the table header row
		responseTable.html("<tr><td>Name</td><td>Field</td><td>Data type</td><td>Restrictions</td><td style='width:32px;min-width:32px;'>&nbsp;</td></tr>");
		// if there's a request object in the details with a root element
		if (details.responseSchema && details.responseSchema.rootElement) {
			// retrieve the request root element
			var responseElement = details.responseSchema.rootElement;
			// populate the name
			$('#rapid_P0_C594_').val(responseElement.name);
			// populate is array
			setData_radiobuttons($.Event("loadSOA"), "rapid_P0_C598_",null,null,responseElement.isArray);
			// loop the child elements
			for (var i in responseElement.childElements) {
				// get child element
				var element = responseElement.childElements[i];
				// get a string for the restrictions
				var text = "click to add...";
				// check we have some
				if (element.restrictions && element.restrictions.length > 0) {
					text = "";
					for (var j in element.restrictions) {
						text += getRestrictionLabel(element.restrictions[j]);
						if (j < element.restrictions.length -1) text += ",";
					}
				}
				// populate child element
				responseTable.append("<tr><td class='elementName'><input class='elementName' value='" + element.name + "' /></td><td class='elementField'><input class='elementField' value='" + element.field + "' /></td><td>" + getSOADataTypeSelect(element.dataType) + "</td><td class='restriction'>" + text + "</td><td><img class='delete' src='images/bin_16x16.png' /><img class='reorder' src='images/moveUpDown_16x16.png' /></td></tr>");				
			}
			
			// name
			responseTable.find("input.elementName").keyup( details, function(ev) {
				// get input
				var input = $(ev.target);
				// update value
				ev.data.responseSchema.rootElement.childElements[input.parent().parent().index()-1].name = input.val();
			});
			
			// field
			responseTable.find("input.elementField").keyup( details, function(ev) {
				// get input
				var input = $(ev.target);
				// update value
				ev.data.responseSchema.rootElement.childElements[input.parent().parent().index()-1].field = input.val();
			});
			
			// data type
			responseTable.find("select").change( details, function(ev) {
				// get select
				var select = $(ev.target);
				// update value
				ev.data.responseSchema.rootElement.childElements[select.parent().parent().index()-1].dataType = select.val();
			});
			
			// restrictions
			responseTable.find("td.restriction").click( details, function(ev) {
				// show details
				showSOARestrictions(responseElement.childElements, $(ev.target), ev.data);
			});
			
			// reorder
			addReorder(responseElement.childElements, responseTable.find("img.reorder"), function(ev) {
				loadSOA(details);
			});
			
			// delete
			responseTable.find("img.delete").click( details, function(ev) {
				// remove from array
				ev.data.responseSchema.rootElement.childElements.splice($(ev.target).parent().parent().index()-1,1);
				// refresh
				loadSOA(ev.data);
			});
						
		} else {
			// empty the name
			$('#rapid_P0_C594_').val("");
			// set to single element
			setData_radiobuttons("rapid_P0_C598_","false");
		}
		
		// add child element
		responseTable.append("<tr><td colspan='4'><a href='#'>add...</a></td></tr>");
		// add click
		responseTable.find("a").last().click(details, function(ev) {
			// initialise response object if need be
			if (!ev.data.responseSchema) ev.data.responseSchema = {}; 
			// get response object
			var responseSchema = ev.data.responseSchema;	
			// initialise root object if need be
			if (!responseSchema.rootElement) responseSchema.rootElement = {};
			// get the root element
			var responseElement = responseSchema.rootElement;
			// initialise array if need be
			if (!responseElement.childElements) responseElement.childElements = [];
			// add new element
			responseElement.childElements.push({name:"", field:"", dataType: 1});
			// refresh
			loadSOA(ev.data);
		});
		
		// show the updated SQL webservice panel
		$("#rapid_P0_C489_").show();
		
	break;
	
	case "JavaWebservice" :
		
		// set the name
		$("#rapid_P0_C944_").val(details.name);
		
		// set the class name
		$("#rapid_P0_C989_").val(details.className);
		
		// show the java class webservice panel
		$("#rapid_P0_C991_").show();
		
	break;
	
	}
		
}

function submitWebservice() {
		
	var action = $("#action").val();	
	var header = { SOAPAction : action };
	
	var contentType = $("input[name=contentType]:checked").val();
	if (contentType == "application/json") {
		header = { Action : action };
	} 
	
	var data = $("#request").val();
	
	// request the dialogue		
	$.ajax({
    	url: "soa",
    	type: "POST",          
        data: data,  
        headers: header,
        contentType: contentType,
        error: function(server, status, error) {
        	
        	$("#response").text(error + " " + server.responseText);
        	
        },
        success: function(response) {
        	
        	if (response.xmlVersion) {
        	
        		var xml = (typeof XMLSerializer !== "undefined") ? (new window.XMLSerializer()).serializeToString(response) : response.xml;
        	
        		$("#response").text(xml);
        		
        	} else {
        		
        		$("#response").text(JSON.stringify(response));
        		
        	}
        }
	});
	
}
