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

// these are for loading into the validation type drop down and looking up the regEx afterwards to store in the validation object
var _validationTypes = [
  {value:'',text:'none',regex:''},                       
  {value:'value',text:'any value',regex:'[\\s\\S]'},
  {value:'number',text:'number',regex:'^[-]?\\d+\\.?\\d*$'},
  {value:'integer',text:'integer',regex:'^\\d+$'},
  {value:'date',text:'date (dd/mm/yyyy)',regex:'^(((([1-9])|([0-2][0-9])|(3[01]))[\\/]((0[13578])|([13578])|(1[02])))|((([1-9])|([0-2][0-9])|(30))[\\/]((0[469])|([469])|(11)))|((([1-9])|([0-1][0-9]|2[0-8]))[\\/](2|02)))[\\/](\\d{2}){1,2}$|^(29\\/(2|02)\\/(19|20)?(00|04|08|12|16|20|24|28|32|36|40|44|48|52|56|60|64|68|72|76|80|84|88|92|96))$'},
  {value:'dateUS',text:'date (mm/dd/yyyy)',regex:'^((((0[13578])|([13578])|(10)|(12))[\\/](([1-9])|([0-2][0-9])|(3[01]))|((0[469])|([469])|(11))[\\/]((1[02])|([1-9])|([0-2][0-9])|(30))|((2|02)[\\/](([1-9])|([0-1][0-9]|2[0-8]))))[\\/](\\d{2}){1,2})$|^((2|02)\\/29\\/(19|20)?(00|04|08|12|16|20|24|28|32|36|40|44|48|52|56|60|64|68|72|76|80|84|88|92|96))$'},
  {value:'dateISO',text:'date (yyyy-mm-dd)',regex:'^(\\d{2}){1,2}[\\-](((0[13578])|([13578])|(10)|(12))[\\-](([1-9])|([0-2][0-9])|(3[01]))|(((0[469])|([469])|(11))[\\-](([1-9])|([0-2][0-9])|(30))|((2|02)[\\-](([1-9])|([0-1][0-9]|2[0-8]))))|((0[2])|(2))[\\-](([1-9])|([0-2][0-8])))$|^((19|20)?(00|04|08|12|16|20|24|28|32|36|40|44|48|52|56|60|64|68|72|76|80|84|88|92|96)\\-(2|02)\\-29)$'},
  {value:'dateORA',text:'date (dd-MMM-yyyy)',regex:'^(((([1-9])|([0-2][0-9])|(3[01]))[\\-]((JAN|MAR|MAY|JUL|AUG|OCT|DEC)|(1[02])))|((([1-9])|([0-2][0-9])|(30))[\\-]((APR|JUN|SEP|NOV)))|((([1-9])|([0-1][0-9]|2[0-8]))[\\-](FEB)))[\\-](\\d{2}){1,2}$|^(29\\-(FEB)\\-(19|20)?(00|04|08|12|16|20|24|28|32|36|40|44|48|52|56|60|64|68|72|76|80|84|88|92|96))$'},
  {value:'currency',text:'currency',regex:'^\\d+\\.?\\d{2}$'},
  {value:'email',text:'email',regex:'^[_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{2,4})$'},
  {value:'custom',text:'custom regex',regex:''},
  {value:'logic',text:'logic',regex:''},
  {value:'javascript',text:'javascript',regex:''}
  ];

// the validation help html
var _validationHelpHtml = "Use validation to ensure values are entered correctly. Specify the type of value you require and an optional message to display if this is incorrect. Use validation actions to check control values and show or hide the styling and messages.";

function getValidationOptions(type) {
	var options = "";
	for (var i in _validationTypes) {
		options += "<option value='" + _validationTypes[i].value + "' " + (_validationTypes[i].value == type ? "selected='selected'" : "") + ">" + _validationTypes[i].text + "</option>";
	}
	return options;
}

function getRegEx(type) {
	for (var i in _validationTypes) {
		if (_validationTypes[i].value == type) return _validationTypes[i].regex; 
	}
}

// this function iteratively gets the select options for all controls with validation 
function getValidationControlOptions(control, ignoreControls) {
	var options = "";
	if (control) {
		if (control.validation && control.validation.type) {
			var ignore = false;
			if (ignoreControls) {
				for (var i in ignoreControls) {
					if (ignoreControls[i] == control.id) {
						ignore = true;
						break;
					}
				}
			}
			if (!ignore) options += "<option value='" + control.id + "'>" + control.name + "</option>";
		}
		if (control.childControls && control.childControls.length > 0) {
			for (var i in control.childControls) {
				options += getValidationControlOptions(control.childControls[i], ignoreControls);
			}
		}
	} else {
		for (var i in _page.childControls) {
			options += getValidationControlOptions(_page.childControls[i], ignoreControls);
		}
	}
	return options;
}

function showValidation(control) {	
	// get a reference to the div we are writing in to
	var validationPanel = $(".validationPanelDiv");	
	// empty it
	validationPanel.html("");		
	// if there is a control
	if (control) {		
		// get the control class
		var controlClass = _controlTypes[control.type];
		
		// only if a control and it can validate
		if (controlClass && controlClass.canValidate) {
			
			// create a validation object if we don't have one
			if (!control.validation) {
				control.validation = {
					type: "",
					allowNulls: false,
					passHidden: true,
					regEx: "",
					message: "",
					javaScript: null
				};
			}
			
			// retain a reference to the validation object
			var validation = control.validation;
			// set the id from the control
			validation.id = control.id + "validation_";
			// retrieve the type
			var type = validation.type;
			// set the regEx just for good measure (as long as not custom)
			if (type != "custom") validation.regEx = getRegEx(type);
					
			// append a table
			validationPanel.append("<table class='propertiesPanelTable'><tbody></tbody></table>");	
			// get a reference to the table
			var validationTable = validationPanel.children().last().children().last();
			// add a heading for the event
			validationTable.append("<tr><td colspan='2' class='propertySubHeader'><h3>Validation</h3><i id='" + control.id + "helpVal' class='actionHelp glyph fa hintIcon'></i></td></tr>");
			// add the help listener
			addHelp(control.id + "helpVal",true,true,_validationHelpHtml);
			// add a type drop down
			validationTable.append("<tr><td>Type<i id='" + control.id + "helpValType' class='propertyHelp glyph fa hintIcon'></i></td><td><select>" + getValidationOptions(type) + "</select></td></tr>");
			// get a reference to the type drop down
			var typeDropDown = validationTable.children().last().children().last().children().last();
			// add a listener
			addListener( typeDropDown.change( function(ev) {
				// get the selected type
				var type = $(ev.target).val();
				// set the validation type
				_selectedControl.validation.type = type;
				// set the regex (this takes into account custom and javascript have no regex)
				_selectedControl.validation.regEx = getRegEx(type); 			
				// refresh the validation
				showValidation(_selectedControl);
			}));	
			// add the help listener
			addHelp(control.id + "helpValType",true,true,"Choose how to apply the validation for this control. There are various patterns you can choose from to check the control value, or use logic, or even javascript, for more speicalised validation.");
			// if the type is not "none"
			if (type) {
				switch (type) {
				case "custom" :
					// add a javascript box
					validationTable.append("<tr><td>RegEx<i id='" + control.id + "helpValRegEx' class='propertyHelp glyph fa hintIcon'></i></td><td>" + validation.regEx + "</td></tr>");
					// get a reference to  the cell
					var cell = validationTable.find("td").last();
					// add a bigtext property listener	
					Property_bigtext(cell, validation, {key: "regEx"});
					// add the help listener
					addHelp(control.id + "helpValRegEx",true,true,"Specify your own regular expression to check the value against. Useful for post codes or other non-standard patterns.");
				break;
				case "logic" :
					// add a javascript box
					validationTable.append("<tr><td>Messages<i id='" + control.id + "helpValLogic' class='propertyHelp glyph fa hintIcon'></i></td><td></td></tr>");
					// get a reference to  the cell
					var cell = validationTable.find("td").last();
					// add a validationLogic property listener	
					Property_validationLogic(cell, validation, {key: "logicMessages"});
					// add the help listener
					addHelp(control.id + "helpValLogic",true,true,"Specify any number of logic conditions and the messages to show should they not be true.");
				break;
				case "javascript" :
					// add a javascript box
					validationTable.append("<tr><td>JavaScript<i id='" + control.id + "helpValJavaScript' class='propertyHelp glyph fa hintIcon'></i></td><td></td></tr>");
					// get a reference to  the cell
					var cell = validationTable.find("td").last();
					// set a helpful default value for the
					if (!validation.javaScript) validation.javaScript = "// Enter your JavaScript here, return a message if the validation fails. The control value is available through the \"value\" variable, the event is \"ev\" and the control id is \"id\"";
					// add a bigtext property listener	
					Property_bigtext(cell, validation, {key: "javaScript"});
					// add the help listener
					addHelp(control.id + "helpValJavaScript",true,true,"Write your own JavaScript. Return a message if validation has failed.");
				break;
				}
				
				// message is in the javascript so no need for it here (can null check there too)
				if (type != "javascript") {
					
					// logic has its own messages
					if (type != "logic") {
						// add a message box
						validationTable.append("<tr><td>Message<i id='" + control.id + "helpValMessage' class='propertyHelp glyph fa hintIcon'></i></td><td></td></tr>");
						// get a reference to  the cell
						var cell = validationTable.children().last().children().last();
						// add a bigtext property listener	
						Property_bigtext(cell, validation, {key: "message"});
						// add the help listener
						addHelp(control.id + "helpValMessage",true,true,"The message to display if the value does not match the pattern in the type property.");
					}
					
					// add a allowNulls checkbox
					validationTable.append("<tr><td>Pass if no value<i id='" + control.id + "helpValNoValue' class='propertyHelp glyph fa hintIcon'></i></td><td></td></tr>");
					// get a reference to  the cell
					cell = validationTable.children().last().children().last();
					// add a checkbox property listener	
					Property_checkbox(cell, validation, {key: "allowNulls"});
					// add the help listener
					addHelp(control.id + "helpValNoValue",true,true,"If checked, validation will pass if the control has no value.");
					
					// add a allowNulls checkbox
					validationTable.append("<tr><td>Pass if hidden<i id='" + control.id + "helpValHidden' class='propertyHelp glyph fa hintIcon'></i></td><td></td></tr>");
					// get a reference to  the cell
					cell = validationTable.children().last().children().last();
					// add a checkbox property listener	
					Property_checkbox(cell, validation, {key: "passHidden"});
					// add the help listener
					addHelp(control.id + "helpValHidden",true,true,"If checked, validation will pass if the control is hidden.");
				}
				
			} // type check
								
		} // control class check
		
	} // control check
			
}