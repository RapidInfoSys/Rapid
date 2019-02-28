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

var _styleRules = {
"background" : {"values" : "<anything>"},
"background-attachment" : {"values" : "scroll|fixed"},
"background-color" : {"values" : "<color>|transparent"},
"background-image" : {"values" : "<uri>|none"},
"background-position" : {"values" : "<percentage>|<length>|left|center|right|top|bottom"},
"background-size" : {"values" : "<percentage>|<length>|auto|cover|contain|initial|inherit"},
"background-repeat" : {"values" : "repeat|repeat-x|repeat-y|no-repeat"},
"border" : {"values" : "<border-width>|<border-width> <border-style> border-top-color"},
"border-collapse" : {"values" : "collapse|separate"},
"border-color" : {"values" : "<color>|<color-4>"},
"border-radius" : {"values" : "<length>"},
"border-spacing" : {"values" : "<length>"},
"border-style" : {"values" : "<border-style>|<border-style-4>"},
"border-top" : {"values" : "<border-width>|<border-width> <border-style> border-top-color"},
"border-right" : {"values" : "<border-width>|<border-width> <border-style> border-top-color"},
"border-bottom" : {"values" : "<border-width>|<border-width> <border-style> border-top-color"},
"border-left" : {"values" : "<border-width>|<border-width> <border-style> border-top-color"},
"border-top-color" : {"values" : "<color>|transparent"},
"border-right-color" : {"values" : "<color>|transparent"},
"border-bottom-color" : {"values" : "<color>|transparent"},
"border-left-color" : {"values" : "<color>|transparent"},
"border-bottom-left-radius" : {"values" : "<length>"},
"border-bottom-right-radius" : {"values" : "<length>"},
"border-top-left-radius" : {"values" : "<length>"},
"border-top-right-radius" : {"values" : "<length>"},
"border-top-style" : {"values" : "<border-style>"},
"border-right-style" : {"values" : "<border-style>"},
"border-bottom-style" : {"values" : "<border-style>"},
"border-left-style" : {"values" : "<border-style>"},
"border-top-width" : {"values" : "<border-width>"},
"border-right-width" : {"values" : "<border-width>"},
"border-bottom-width" : {"values" : "<border-width>"},
"border-left-width" : {"values" : "<border-width>"},
"border-width" : {"values" : "<border-width>|<border-width-4>"},
"bottom" : {"values" : "<length>|<percentage>|auto"},
"box-shadow" : {"values" : "<uri>"},
"box-sizing" : {"values" : "content-box|border-box|initial|inherit"},
"clear" : {"values" : "none|left|right|both"},
"color" : {"values" : "<color>"},
"cursor" : {"values" : "<uri>,auto|crosshair|default|pointer|move|e-resize|ne-resize|nw-resize|n-resize|se-resize|sw-resize|s-resize|w-resize|text|wait|help|progress"},
"direction" : {"values" : "ltr|rtl"},
"display" : {"values" : "inline|block|list-item|inline-block|table|inline-table|table-row-group|table-header-group|table-footer-group|table-row|table-column-group|table-column|table-cell|table-caption|none"},
"empty-cells" : {"values" : "show|hide"},
"filter" : {"values" : "<anything>"},
"float" : {"values" : "left|right|none"},
"font-family" : {"values" : "<anything>"},
"font-size" : {"values" : "<absolute-size>|<relative-size>|<length>|<percentage>"},
"font-style" : {"values" : "normal|italic|oblique"},
"font-variant" : {"values" : "normal|small-caps"},
"font-weight" : {"values" : "normal|bold|bolder|lighter|100|200|300|400|500|600|700|800|900"},
"height" : {"values" : "<length>|<percentage>|auto"},
"left" : {"values" : "<length>|<percentage>|auto"},
"letter-spacing" : {"values" : "normal|<length>"},
"line-height" : {"values" : "normal|<number>|<length>|<percentage>"},
"list-style" : {"values" : "list-style-type|list-style-position|list-style-image"},
"list-style-image" : {"values" : "<uri>|none"},
"list-style-position" : {"values" : "inside|outside"},
"list-style-type" : {"values" : "disc|circle|square|decimal|decimal-leading-zero|lower-roman|upper-roman|lower-greek|lower-latin|upper-latin|armenian|georgian|lower-alpha|upper-alpha|none"},
"margin" : {"values" : "<margin-width>|<margin-width-4>"},
"margin-top" : {"values" : "<margin-width>"},
"margin-right" : {"values" : "<margin-width>"},
"margin-bottom" : {"values" : "<margin-width>"},
"margin-left" : {"values" : "<margin-width>"},
"max-height" : {"values" : "<length>|<percentage>|none"},
"max-width" : {"values" : "<length>|<percentage>|none"},
"min-height" : {"values" : "<length>|<percentage>"},
"min-width" : {"values" : "<length>|<percentage>"},
"opacity" : {"values" : "<number>"},
"outline" : {"values" : "outline-color|outline-style|outline-width"},
"outline-color" : {"values" : "<color>|invert"},
"outline-style" : {"values" : "<border-style>"},
"outline-width" : {"values" : "<border-width>"},
"overflow" : {"values" : "visible|hidden|scroll|auto"},
"overflow-x" : {"values" : "visible|hidden|scroll|auto"},
"overflow-y" : {"values" : "visible|hidden|scroll|auto"},
"padding" : {"values" : "<padding-width>|<padding-width-2>|<padding-width-4>"},
"padding-top" : {"values" : "<padding-width>"},
"padding-right" : {"values" : "<padding-width>"},
"padding-bottom" : {"values" : "<padding-width>"},
"padding-left" : {"values" : "<padding-width>"},
"page-break-after" : {"values" : "auto|always|avoid|left|right"},
"page-break-before" : {"values" : "auto|always|avoid|left|right"},
"page-break-inside" : {"values" : "avoid|auto"},
"position" : {"values" : "static|relative|absolute|fixed"},
"right" : {"values" : "<length>|<percentage>|auto"},
"table-layout" : {"values" : "auto|fixed"},
"text-align" : {"values" : "left|right|center|justify"},
"text-decoration" : {"values" : "none|underline|overline|line-through|blink"},
"text-indent" : {"values" : "<length>|<percentage>"},
"text-overflow " : {"values" : "clip|ellipsis"},
"text-shadow " : {"values" : "<uri>"},
"text-transform" : {"values" : "capitalize|uppercase|lowercase|none"},
"top" : {"values" : "<length>|<percentage>|auto"},
"vertical-align" : {"values" : "baseline|sub|super|top|text-top|middle|bottom|text-bottom|<percentage>|<length>"},
"visibility" : {"values" : "visible|hidden|collapse"},
"white-space" : {"values" : "normal|nowrap|pre|pre-line|pre-wrap|initial|inherit"},
"width" : {"values" : "<length>|<percentage>|auto"},
"z-index" : {"values" : "auto|<integer>"}
};

// the table into which all the styles go
var _styleTable;
// the table cell we are currently working with
var _styleCell;
// a span of what we are currently editing, either the style attribute, or value
var _styleSpan;
// an editable div in which we enter eitehr the attribute, or value
var _styleInput;
// a hint box that suggests the first available option
var _styleHint;
// the list of available options
var _styleList;
// whether we had just clicked on the style (so unfocus knows)
var _styleClicked = false;
// whether the styles have been applied yet (selecting another control can leave them unapplied)
var _stylesApplied = false;
// the page style sheet that we'll add all new and updated styling to
var _styleSheet = null;

function styleRule_mutliCheck(name, value, number) {
	var f = window["styleRule_" + name]; 
	if (f) {
		// split into number
		var values = value.split(" ");
		// fail if not number
		if (values.length != number) return false;
		// check each one and return as soon as any fails
		for (var i in values) if (!styleRule_color(values[i])) return false;
		// must be all ok!
		return true;
	} 
	return false;	
}

function styleRule_anything(value) {
	//if the value works, it works!
	return true;
}

function styleRule_absolute_size(value) {
	var values = ["xx-small","x-small","small","medium","large","x-large","xx-large"];
	for (var i in values) if (values[i] == value) return true; 
}

function styleRule_border_style(value) {
	var values = ["none","hidden","dotted","dashed","solid","double","groove","ridge","inset","outset"];
	for (var i in values) if (values[i] == value) return true; 
}

function styleRule_border_style_4() {
	return styleRule_mutliCheck("border_style", value, 4);	
}

function styleRule_border_width(value) {
	var values = ["thin","medium","thick"];
	for (var i in values) if (values[i] == value) return true;
	// a length is also allowed
	return styleRule_length(value);
}

function styleRule_border_width_4(value) {
	return styleRule_mutliCheck("border_width", value, 4);
}

function styleRule_color(value) {	
	// check #fff and #ffffff
	if (value.match(/^#(?:[0-9a-fA-F]{3}){1,2}$/)) return true;
	// check rgb(r,g,b)
	if (value.match(/^rgb\(([12]?[0-9]?[0-9]\,){2}([12]?[0-9]?[0-9])\)$/)) return true;
	// check rgba(r,g,b,a)
	if (value.match(/^rgba\(([12]?[0-9]?[0-9]\,){3}([12]?[0-9]?[0-9])\)$/)) return true;
	// check list of names
	var values = ["red","orange","yellow","green","blue","purple","white","black","gray","silver","maroon","teal","olive","lime","aqua","navy","fuchsia"];
	for (var i in values) if (values[i] == value) return true; 
}

function styleRule_color_4(value) {
	return styleRule_mutliCheck("color", value, 4);	
}

function styleRule_integer(value) {
	return value.match(/^\d+$|[+-]^\d+$/);
}

function styleRule_length(value) {
	return value.match(/^auto$|^[+-]?[0-9]+\.?([0-9]+)?(px|em|ex|%|in|cm|mm|pt|pc)?$/);
}

function styleRule_margin_width(value) {
	return styleRule_length(value);
}

function styleRule_margin_width_4(value) {
	return styleRule_mutliCheck("margin_width", value, 4);
}

function styleRule_number(value) {
	// not Not a Number
	return !isNaN(value);
}

function styleRule_padding_width(value) {
	// no negative values in padding
	return styleRule_length(value) && value.indexOf("-") == -1;
}

function styleRule_padding_width_2(value) {
	return styleRule_mutliCheck("padding_width", value, 2);
}

function styleRule_padding_width_4(value) {
	return styleRule_mutliCheck("padding_width", value, 4);
}

function styleRule_percentage(value) {
	return value && !isNaN(value.replace("%","")) && (value.indexOf("%") == value.length - 1);
}

function styleRule_relative_size(value) {
	var values = ["larger","smaller"];
	for (var i in values) if (values[i] == value) return true;
}

function styleRule_uri(value) {
	// bit like the font - if it works, it works!
	return true;
}

function validateStyle(name, value) {
	// get the style rule we're dealing with
	var styleRule = _styleRules[name];
	// check we got one
	if (!styleRule) return false;
	// retrieve the possible values
	var options = styleRule.values.split("|");
	// loop the possible values
	for (var i in options) {
		// get this option
		var option = options[i];
		// get the rules in this option
		var rules = option.split(" ");
		// get the values
		var values = value.split(" ");
		// must have same or more values than rules
		if (rules.length <= values.length) {
			// loop the rules
			for (var j in rules) {
				// get the option we're dealing with
				var r = rules[j];
				// get the value we're dealing with
				var v = values[j];			
				// the rule could be a literal or a function
				if (r.indexOf("<") > -1) {
					// clean the rule name into a function name, note "-" is not allowed in function names
					var styleRuleFunction = "styleRule_" + r.replace("<","").replace(">","").replace("-","_");
					// if we have a function
					if (window[styleRuleFunction]) {
						// go onto the next option if this value doesn't pass the function
						if (!window[styleRuleFunction](v)) break;					
					}
				} else {
					// go onto the next option if this value if not a literal match
					if (v != r) break;
				}	
				// if we passed all values in this option!
				return true;
			}
		}							
	}
	// got all the way to the end without passing any option
	return false;
}

function cursorToEnd(element) {
	if (element.innerHTML) {
		// ff / chrome
		if (window.getSelection) {
			var selection = window.getSelection();
			selection.removeAllRanges();
			var range = document.createRange();
			range.setStart(element,0);
			range.setEnd(element,0);
			selection.addRange(range);
		}
	}	
}

function styleEdit() {	
	// get the value we're working with
	var value = _styleSpan.text();
	// if there is a value trim it
	if (value) value = value.trim();
	// set the input text from the span we're working with
	_styleInput.html(value);		
	// show it
	_styleInput.show();
	// get the left and top
	var left = _styleSpan.offset().left;
	var top = _styleSpan.offset().top;
	// if the value is null add a space and re-get the top (this is trick for FireFox as it does't get the span position correctly when empty)
	if (!value) {
		_styleSpan.html("&nbsp;");
		left = _styleSpan.offset().left;
		top = _styleSpan.offset().top;
	}
	// position the input over the span
	_styleInput.css({
		left: left,
		top: top,
		height: (_styleSpan.innerHeight() ? _styleSpan.innerHeight() : 16)
	});
	// set the hint text to what we're working with
	_styleHint.html(value);	
	// show the input
	_styleInput.show();
	// focus the input
	_styleInput.focus();
	// move the cursor to the end
	cursorToEnd(_styleInput[0]);	
}

function styleClick(ev) {
	_styleCell = $(ev.target).closest("td");
	_styleTable = _styleCell.parent().parent();
	// add spans if not there
	if (_styleCell.children().length == 0) _styleCell.append("<span class='styleAttr'></span><span class='styleColon'>:</span><span class='styleValue'></span>");
	if ($(ev.target).hasClass("styleAttr")) {
		// choose attribute to position input box
		_styleSpan = _styleCell.children("span").first();		
	} else {
		// check whether an attribute has been set
		if (_styleCell.find(".styleAttr").text()) {
			// edit the value
			_styleSpan = _styleCell.children("span").last();
		} else {
			// no attribute set yet, edit that
			_styleSpan = _styleCell.children("span").first();
		}		
	}
	// call the style edit function above
	styleEdit();
}

// since the hints and input are floating there are a few places we might want to reposition them from
function positionHints() {
	
	// get the value we're working with
	var value = _styleSpan.text();
	// add a non-breaking space if empty so there is some geometery
	if (!value) _styleSpan.html("&nbsp;");
	// get the left and top from the span
	var left = _styleSpan.offset().left;
	var top = _styleSpan.offset().top;
	// position the input over the span
	_styleInput.css({
		left: left,
		top: top,
		height: (_styleSpan.innerHeight() ? _styleSpan.innerHeight() : 16)
	});
	// position the hint
	_styleHint.css({
		left: left,
		top: top,
		height: (_styleSpan.innerHeight() ? _styleSpan.innerHeight() : 16)
	});
	// position the list of possible values
	_styleList.css({
		left: left,
		top: top + (_styleSpan.innerHeight() ? _styleSpan.innerHeight() : 16)
	});	
	
}

function renderHints(val) {
	
	var html = "";
	// check all available style attributes for matching and add to ul/li html if so
	for (var i in _styleRules) {			
		if (i.indexOf(val) == 0) html += "<li>" +  i + "</li>";
	}				
	// apply the list of matching style attributes html
	_styleList.html(html);
	// apply the hint class to the first entry in teh list
	_styleList.children().first().addClass("styleHint");			
	// for each entry add mouse listeners
	_styleList.children().each( function() {
		$(this).mouseover( function(ev) { $(this).addClass("mouseover"); });					
		$(this).mouseout( function(ev) { $(this).removeClass("mouseover"); });
		$(this).mousedown( function(ev) {
			// set we clicked a style so the unfocus knows
			_styleClicked = true;
			// get the value of what we clicked on
			var value = $(this).text();						
			// set the span value
			_styleSpan.html(value);
			// set the input value
			_styleInput.html(value);
			// set the colon for good measure
			_styleCell.find(".styleColon").html(":");
		});
	});
	// show the available styles
	_styleList.show();
	// set the hint text to the first child
	_styleHint.html(_styleList.children().first().text());
	// show the hint
	_styleHint.show();
	
	// the hints could cause a scroll bar to appear so we need to reposition the hint objects here
	positionHints();
				
}

function removeStyle(appliesTo, href) {
	// for pointers with ie see:
	// http://www.javascriptkit.com/domref/stylesheet.shtml
	
	// get all of the stylesheets (we might be using pagepanels where the styling )
	var styleSheets = _pageIframe[0].contentWindow.document.styleSheets;
	// check we got some stylesheets
	if (styleSheets) {
		// loop them
		for (var i = 0; i < styleSheets.length; i++) {
			// get a reference
			styleSheet = styleSheets[i];
			// control styles are always in the page and will not have an href (unless we supply one explcity)
			if (!styleSheet.href || (href && styleSheet.href && styleSheet.href.indexOf(href) > 0)) {
				// get rules
				var rules = styleSheet.cssRules;					
				// check rules
				if (rules) {
					// loop rules
					for (var j = 0; j < rules.length;) {
						// get rule
						var rule = rules[j];
						// check there is cssText
						if (rule.cssText) {
							// get what the rule applies to
							ruleAppliesTo = rule.cssText.substr(0, rule.cssText.indexOf("{") - 1);
							// remove if matches our applies to (case insensitive)
							if (ruleAppliesTo.toLowerCase() == appliesTo.toLowerCase()) {
								styleSheet.deleteRule(j);
							} else {
								j++;
							}
						} // cssText check							
					} // rules loop
				} // rules check		
			} // in-page style check
		} // style sheets loop
	} // style sheets check
}

function addStyle(appliesTo, styleSheetRule) {
	// add the styleSheet rule
	if (_styleSheet) {
		// check whether the stylesheet has an insertRule method
		if (_styleSheet.insertRule) {
			// ff / chrome - create a single rule inside the applies to
			styleSheetRule = appliesTo + " {" + styleSheetRule + "}";
			// insert to send of style sheet
			_styleSheet.insertRule(styleSheetRule, _styleSheet.cssRules.length);
		} else {
			// ie - use addRule method with seperate applies to and rule 						
			_styleSheet.addRule(appliesTo, styleSheetRule);
		}									
	} // _styleSheet check
}

function rebuildStyles() {
	
	// create a new array to hold the styles for the selected control 	
	var styles = new Array();
	// loop the tables in the div
	_stylesPanelDiv.find("table").each( function() {
		// get a reference to the table				
		var _styleTable = $(this);
		// the applies to is in the first td
		var appliesTo = _styleTable.find("td[data-appliesTo]").attr("data-appliesTo");
		// check we have a style sheet object and an appliesTo
		if (appliesTo) {
			// remove any styling for this 			
			removeStyle(appliesTo);													
			// create a single style object which applies to the control element
			var style = {appliesTo : appliesTo, rules : new Array()};
			// create a style sheet rule
			var styleSheetRule = "";
			// loop the style rows and add to the style rules
			_styleTable.find("td.styleCell").each( function() {
				// get the rule 
				var rule = $(this).text();
				// if we got something 
				if (rule) {					
					// add it to the list
					style.rules.push(rule);
					// replace the webfolder parameter if present and add it to the styleSheetRule
					styleSheetRule += rule.replace("[[webfolder]]", "applications/" + _version.id + "/" + _version.version);
				}
			});
			// if there are rules
			if (style.rules.length > 0) {
				// add the style to the collection
				styles.push(style);
				// add the styleSheet rule
				addStyle(appliesTo, styleSheetRule);
			} // rules check
		} // check stylesheet
	});			
	// asign the collection to the control
	if (_selectedControl) _selectedControl.styles = styles;	
	// resize and reposition the selection as the geometry may have changed
	positionAndSizeBorder(_selectedControl);	
	// check the iFrame scroll bars for the same reason
	windowResize("rebuildStyles");
}

// applys the styles in the panel to the control
function applyStyles() {
	
	// only if a control is currently selected
	if (_selectedControl) {
				
		// if we lost the focus due to a style being selected with the mouse get the selection back
		if (_styleClicked) {
						
			// if we'd just chosen an attribute, now jump to the value
			if (!_styleSpan.hasClass("styleValue")) {
				// set the edit span to the value
				_styleSpan = _styleCell.children("span").last();
				// edit the style value
				styleEdit();
			}
			// mark the click over
			_styleClicked = false;
			
		} else {	
										
			// get the entry from the editable div
			var entry = _styleInput.text();
			// add a semi-colon to the end if there isn't one
			if (_styleSpan.is(".styleValue") && entry.lastIndexOf(";") != entry.length - 1) entry += ";";
			// replace any double semi-colons with a single
			entry = entry.replace(";;",";");
			// write the entry to the span
			_styleSpan.html(entry);
			// hide the input
			_styleInput.hide();
			// get the name
			var name = _styleCell.children("span").first().text();
			// get the value
			var value = _styleCell.children("span").last().text();
			// remove the semi-colon from the value for validation checking
			value = value.substr(0, value.length - 1);
			// trim it
			if (value) value = value.trim();
			// check the style for validity
			if (validateStyle(name, value)) {
				_styleCell.removeClass("validationFail");
			} else {
				_styleCell.addClass("validationFail");
			}									
			// if we've edited the attribute class remove if the doesn't exist
			if (!_styleSpan.hasClass("styleValue")) {
				var exists = false;
				for (var i in _styleRules) {
					// match the value to the style attribute (name)
					if (name == i) {
						exists = true;
						break;
					}
				}
				// remove the row if we failed to find the attribute in the list
				if (!exists) _styleCell.parent().remove();
			}
			// make sure reorder and delete are present				
			if (_styleCell.attr("colspan")) {
				_styleCell.removeAttr("colspan");
				_styleCell.after("<td><div class='iconsPanel'>" +
						"<div class='reorder fa-stack fa-sm' title='Reorder this action'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
						"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Delete this action'></i></div>" +
						"</div></td>");
				// attach a delete listener 
				addListener( _styleCell.parent().find("div.delete").click( function(ev) {
					// add an undo snapshot
					addUndo();
					// remove the row
					$(this).parent().parent().parent().remove();
					// rebuild the styles (and assign to control)
					rebuildStyles();										
				}));
				
				/////////////////////////////////////////////////////////////////////////////
				
				// Add reorder listener
				
				/////////////////////////////////////////////////////////////////////////////

			}
			
			// make sure there's always one empty row at the bottom
			if (_styleTable.find("td.styleCell").length == 0 || _styleTable.find("td.styleCell").last().text()) {
				_styleTable.append("<tr><td class='styleCell' colspan='2'></td></tr>");
				_styleTable.find("td.styleCell").last().click( function(ev) { 
					styleClick(ev); 
				});
			}

		} // style from list clicked
		
		// rebuild the styles (and assign to control)
		rebuildStyles();		
		
	}
	
	// remember we have applied the styles
	_stylesApplied = true;
	
}


$(document).ready( function() {
	
	// get a reference to the editable div we use to input the styles	
	_styleInput = $("#styleInput");
	
	// add a keyup listener
	_styleInput.keyup( function(ev) {
		// retain that the styles have not been reapplied yet
		_stylesApplied = false;
		// get the value
		var val = _styleInput.text();
		// remove any linebreaks, or tabs, if required
		if (val != val.replace(/(\r\n|\n|\r|\t)/gm,"")) {
			val = val.replace(/(\r\n|\n|\r|\t)/gm,"");
			_styleInput.text(val);
		}
		// remove any :
		val = val.replace(":","");
		// update the span with the value
		_styleSpan.html(val);
		// check which key was hit
		switch(ev.keyCode) {
		case 13: case 39 : case 59: case 186 :
			// enter, arrow right, ";", ":"  - get the value from the hint, only apply arrow right if hints are in use  
			if (ev.keyCode != 39 || _styleHint.is(":visible")) {
				var hinttext = _styleHint.text();
				// if we were using the hint
				if (hinttext) {
					// remove any linebreaks, or tabs, if required
					if (hinttext != hinttext.replace(/(\r\n|\n|\r|\t)/gm,"")) {
						hinttext = hinttext.replace(/(\r\n|\n|\r|\t)/gm,"");
					}
					// remove any :
					hinttext = hinttext.replace(":","");
					// assign hinttext to val
					val = hinttext;
					// assing to span
					_styleSpan.text(val);
				}
				// hide the hint and list
				_styleHint.hide();
				_styleList.hide();
				// if we've edited the attribute class move to the value
				if (_styleSpan.hasClass("styleValue")) {
					// semi-colon, or enter
					if (!ev.shiftKey && (ev.keyCode == 13 || ev.keyCode == 59 || ev.keyCode == 186)) {
						// fire the blur event to validate and apply the style in this row (also adds a new row)
						_styleInput.blur();
						// fire the click event in the last cell to set up our next style
						_styleTable.find("tr").last().find("td").first().click();
					}
				} else {
					// set the edit span to the value
					_styleSpan = _styleCell.children("span").last();
					// edit the style value
					styleEdit();
				}
				cursorToEnd(_styleInput[0]);	
			}
			break;
		case 40 :
			// arrow down
			_styleHint.show();
			if (!_styleList.is(":visible")) renderHints(val);
			if (_styleList.children(".styleHint").next()[0]) {
				var hint = _styleList.children(".styleHint").removeClass("styleHint").next().addClass("styleHint");
				_styleHint.html(hint.text());					
				_styleSpan.html(hint.text());
			}							
			cursorToEnd(_styleInput[0]);
			break;
		case 38 :
			// arrow up
			if (_styleList.children(".styleHint").prev()[0]) {
				var hint = _styleList.children(".styleHint").removeClass("styleHint").prev().addClass("styleHint");
				_styleHint.html(hint.text());	
				_styleSpan.html(hint.text());
			}
			cursorToEnd(_styleInput[0]);
			break;
		default :
						
			if (!_styleSpan.hasClass("styleValue")) {
				if (val) {
					renderHints(val);
				} else {
					_styleHint.hide();
					_styleList.hide();
					// hiding the hints could have lost a scroll bar so reposition
					positionHints();
				} // had a value
			} // style value				
		} // key switch		
	});
		
	// add a blur listener
	_styleInput.blur( function(ev) {		
		_styleHint.hide();
		_styleList.hide();
		applyStyles();		
	});
			
});

function showStyles(control) {
	
	// remove all showing style rules
	_stylesPanelDiv.html("");	
	
	// hide the input
	_styleInput.hide();
				
	// check there is a control and the class for any styling
	if (control) {
		
		// get the control class
		var controlClass = _controlTypes[control.type];
		
		// check there are styles
		if (controlClass.styles && controlClass.styles.style) {				
			// add a heading and table
			_stylesPanelDiv.append("<h2 id='stylesHeader' style='margin-top:5px;'>Styles  <i class='headerToggle fa' title='Hide controls' style='color:white;'></i><i id='helpStyles' class='headerHelp glyph fa hintIcon'></i></h2><div" + (_stylesHidden ? " style='display:none;'" : "") + "></div>");
			// add the help hint															
			addHelp("helpStyles",true);
			// add the header toggle hint
			_stylesPanelDiv.find("h2").click( toggleHeader );
			// get the panel
			var stylesTablesPanel = _stylesPanelDiv.find("div");
			// get the array of styles classes
			var styles = controlClass.styles.style;
			// make it an array if the xml to json didn't
			if (!$.isArray(styles)) styles = [ styles ];
			
			// loop styles
			for (var i in styles) {
				// get a reference to this style
				var style = styles[i];
				// get the applies to function
				var f = new Function(style.getAppliesToFunction);
				// invoke and return applies to with the control as the context
				var appliesTo = f.apply(control);
				// add a table for this rule
				stylesTablesPanel.append("<table class='stylesPanelTable'><tbody></tbody></table>");
				// grab a refrence to the table
				var stylesTable = _stylesPanelDiv.find("tbody").last();			
				// write it to the table;
				stylesTable.append("<tr><td  colspan='2' data-appliesTo='" + appliesTo + "'><b>" + style.name + "</b></td></tr><tr><td colspan='2'></td></tr>");
				// look for any style rules for this style in the control
				if (control.styles) {
					// loop them
					for (var j in control.styles) {
						// get a reference
						var controlStyle = control.styles[j];
						// is this the one we want, and are there rules?
						if (appliesTo == controlStyle.appliesTo && controlStyle.rules && controlStyle.rules.length > 0) {
							// loop the rules
							for (var k in controlStyle.rules) {
								// get a reference to the rule
								var rule = controlStyle.rules[k];
								// get the parts
								var parts = rule.split(":");
								// add this rule
								stylesTable.append("<tr><td class='styleCell'><span class='styleAttr'>" + parts[0] + "</span><span class='styleColon'>:</span><span class='styleValue'>" + parts[1] + "</span></td><td>" +
										"<div class='iconsPanel'>" +
										"<div class='reorder fa-stack fa-sm' title='Reorder this rule'><i class='fa fa-arrow-up fa-stack-1x'></i><i class='fa fa-arrow-down fa-stack-1x'></i></div>" +
										"<div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Delete this rule'></i></div>" +
										"</td></tr></div>");
					
							}		
							// add reorder listeners
							addReorder(controlStyle.rules, stylesTable.find("div.reorder"), function() { showStyles(control); });
							// we're done with this style
							break;
						}
					}
					// attach a delete listener to each row
					stylesTable.find("div.delete").each( function() {
						addListener( $(this).click( function(ev) {
							// add an undo snapshot
							addUndo();
							// remove the row
							$(this).closest("tr").remove();
							// rebuild the styles (and assign to control)
							rebuildStyles();				
						}));
					});
				}
				
				// add an empty row for adding a new style
				stylesTable.append("<tr><td class='styleCell' colspan='2'></td></tr>");
				// attach a click listener to each row's cell
				addListener( stylesTable.find("td.styleCell").click( function(ev) {
					styleClick(ev);
				}));								
			}
			
			// add a heading and table
			_stylesPanelDiv.append("<h2  id='styleClasssesHeader' style='margin-top:5px;'>Style classes  <i class='headerToggle fa' title='Hide controls' style='color:white;'></i><i id='helpStyleClasses' class='headerHelp glyph fa hintIcon'></i></h2><div" + (_styleClassesHidden ? " style='display:none;'" : "") + "></div>");
			// add the help hint																			
			addHelp("helpStyleClasses",true);
			// add the header toggle
			_stylesPanelDiv.find("h2").last().click( toggleHeader );
			
			// add a table for this rule
			_stylesPanelDiv.find("div").last().append("<table class='stylesPanelTable'><tbody></tbody></table>");
			// grab a refrence to the table
			var classesTable = _stylesPanelDiv.find("tbody").last();	
			
			// instantiate array if doesn't exist
			if (!control.classes) control.classes = [];
			// loop array
			for (var i in control.classes) {
				classesTable.append("<tr><td>" + control.classes[i] + "<div class='iconsPanel'><div class='delete fa-stack fa-sm'><i class='delete fa fa-trash' title='Remove this class'></i></div></div></td></tr>");
			}
			// find the delete
			var deletes = classesTable.find("div.delete");
			// add a listener
			addListener( deletes.click( {control: control}, function(ev) {
				// create an undo snapshot just before we apply the change
				addUndo();
				// get the del image
				var delImage = $(this);
				// get the index
				var index = delImage.closest("tr").index();
				// remove from collection
				ev.data.control.classes.splice(index,1);	
				// rebuild the styles
				showStyles(ev.data.control);
				// rebuild the html
				rebuildHtml(ev.data.control);				
			}));
			
			classesTable.append("<tr><td colspan='2' style='padding:0; border-bottom:none;'><select class='propertiesPanelTable'></select></td></tr>");
			// get a reference to the select
			var addClass = classesTable.find("select").last();
			// retain a string for the class options
			var classOptions = "<option>add...</option>" + getStyleClassesOptions();			
			// add the known options
			addClass.append(classOptions);
			// change listener
			addListener( addClass.change( {control : control}, function(ev) {			
				// get the potential new class
				var newClass = $(this).val();
				// get the classes
				var classes = ev.data.control.classes;
				// whether we have it already
				var gotClass = false;
				// check collection for this new one
				for (var i in classes) {
					if (classes[i] == newClass) {
						gotClass = true;
						break;
					}
				}
				// only if not got
				if (!gotClass) {
					// create an undo snapshot just before we apply the change
					addUndo();
					// add the selected class to the list
					classes.push(newClass);
					// sort the array
					classes.sort();				
					// rebuild the styles
					showStyles(ev.data.control);
				}
				// rebuild the html
				rebuildHtml(ev.data.control);
			}));
							
		} // styles check
		
	} // control check
	
	// retain that styles have been applied
	_stylesApplied = true;
	
}

// used by control getHtmlFunctions to return a space-seperated list of class names for inclusion in the classes attribute
function getStyleClasses(control) {
	// assume it's going to be an empty string
	var classes = "";	
	// check the control has a classes collections
	if (control.classes) {
		// loop it, building the space-seperated list
		for (var i in control.classes) {
			classes += " " + control.classes[i];
		}
	}	
	return classes;
}
