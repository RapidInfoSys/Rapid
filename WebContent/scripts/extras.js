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

AFTER MAKING CHANGES TO THIS FILE DELETE /scripts_min/extras.min.js IN ORDER TO REGENERATE IT

*/

// extend String to have a trim function for IE8
if (typeof String.prototype.trim !== 'function') {
	String.prototype.trim = function() {
		return this.replace(/^\s+|\s+$/g, ''); 
	};
}

// extend String to have a replaceAll function
String.prototype.replaceAll = function( find, replace ) {
    return this.split( find ).join( replace );        
};

// trigger attached show and hide functions when control is shown or hidden
// http://stackoverflow.com/questions/15232688/jquery-how-to-call-function-when-element-shows
(function ($) {
	  $.each(['show', 'hide'], function (i, ev) {
	    var el = $.fn[ev];
	    $.fn[ev] = function () {
	    	el.apply(this, arguments);
	    	this.trigger(ev);	 
	      return this;
	    };
	  });
	})(jQuery);

// extend JQuery to have functions for retrieving url parameter values
$.extend({
  getUrlVars: function(){
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++) {
      hash = hashes[i].split('=');
      vars.push(hash[0]);
      if (hash[1]) vars[hash[0]] = decodeURIComponent(hash[1].replace('#',''));
    }
    return vars;
  },
  getUrlVar: function(name){
    return $.getUrlVars()[name];
  }
});

// position loading cover - we can call this for controls with covers when the page morphs like when resizing or loading dialogues
function positionLoadingCover(control, loadingCover) {
	loadingCover.css({
		left: control.offset().left,
		top: control.offset().top,
		width: control.outerWidth(),
		height: control.outerHeight()
	}).show();
	loadingCover.children("div").css({
		height: control.outerHeight()
	});
	if (control.width() > 0) {
		var image = loadingCover.children("span");
		image.css({
			left: (control.outerWidth() - image.outerWidth())/2,
			top: (control.outerHeight() - image.outerHeight())/2
		}).show();
	}
}

// uses the above to position all visible loading covers
function positionLoadingCovers() {
	// get all visible loading covers and loop
	var loadingCovers = $("div.loadingCover:visible").each( function() {
		// get the cover
		var loadingCover = $(this);
		// get the control
		var control = $("#" + loadingCover.attr("data-id"));
		// position!
		positionLoadingCover(control, loadingCover);
	});	
}

// extend JQuery object methods
$.fn.extend({
  enable: function() {
	return this.removeAttr("disabled").find("button,input,select,textarea").removeAttr("disabled");  
  },
  disable: function() {
	return this.attr("disabled","disabled").find("button,input,select,textarea").attr("disabled","disabled");
  },
  focus: function() {
	var e = this[0];
	if (e) {
		e.focus();
		var c = this.find("button:visible,input:visible,select:visible,textarea:visible")[0];
		if (c) c.focus();
	}	
	return this;
  },
  showLoading: function() {
	 var id = this.attr("id");
	 var loadingCover = $("div.loadingCover[data-id=" + id + "]");
	 if (this[0] && this.is(":visible")) {		
		if (!loadingCover[0]) {
			$("body").after("<div class='loadingCover' data-id='" + id + "'><div class='loading'></div><span class='loading'></span></div>");
			loadingCover = $("div.loadingCover[data-id=" + id + "]");
		}				
		positionLoadingCover(this, loadingCover);
	 } else {
		loadingCover.hide();
	 }
	 return this;
  },
  hideLoading: function() {
	  if (this[0]) $("div.loadingCover[data-id=" + this.attr("id") + "]").hide();	
	  return this;
  },
  hideDialogue: function(reload, id) {
	  // get a reference to closest dialogue
	  var dialogue = $(this).closest("div.dialogue");
	  // if we didn't find one but we have an id, use that
	  if (dialogue.length == 0 && id) dialogue = $("#" + id + "dialogue")	  
	  // remove the cover
	  dialogue.prev("div.dialogueCover").remove();
	  // remove the dialogue
	  dialogue.remove(); 
	  // check reload
	  if (reload) {
		  var pageId = $("body").attr("id");
		  if (window["Event_pageload_" + pageId]) window["Event_pageload_" + pageId]();		  
	  } else {
		  // reset the tabs in the body
		  $("body").find("input, select, textarea, button, a").each( function() {
      		// get the element
      		var e = $(this);
      		// get any existing data tab index
      		var t = e.attr("data-tabindex");
      		// if there was one add a proper attribute for it
      		if (t > 0) {
	  			e.attr("tabindex",t);
	  			e.removeAttr("data-tabindex");
      		} else {
      			// remove negative tab index
      			e.removeAttr("tabindex");
      		}
      	});
	  }
	  return this;
  },
  hideAllDialogues: function(reload) {	  
	  $("div.dialogue").remove();
	  $("div.dialogueCover").remove();
	  if (reload) {
		  var pageId = $("body").attr("id");
		  if (window["Event_pageload_" + pageId]) window["Event_pageload_" + pageId]();		  
	  } else {
		// reset the tabs in the body
		  $("body").find("input, select, textarea, button, a").each( function() {
			  // get the element
			  var e = $(this);
			  // get any existing data tab index
			  var t = e.attr("data-tabindex");
			  // if there was one add a proper attribute for it
			  if (t > 0) {
				  e.attr("tabindex",t);
				  e.removeAttr("data-tabindex");
			  } else {
				  // remove negative tab index
				  e.removeAttr("tabindex");
			  }
		  });
	  }  
	  return this;
  },
  showError: function(server, status, message) {
	if (server) {
		var message = server.responseText||message;
		var b = message.indexOf("\n\n");
		if (b > 0) message = message.substring(0, b);
		alert(message);
	}
    return this;
  }
});

// this overrides the focus so that if focus is fired when the page is invisible, focus can be set once the page is made visible by Rapid
(function($) {
	// a reference to the original focus method
    var focus_orignal = $.fn.focus; // maintain a reference to the existing function
    // override the focus method
    $.fn.focus = function(type) {
    	// if there was an event of type DOMContentLoaded
    	if (type == "rapid") {
	    	// get a reference to the control
			var c = this[0];
			// if there is one
			if (c) {		  
				// if the page or dialogue are hidden
				if ($("body").css("visibility") == "hidden" || this.closest("div.dialogue").css("visibility") == "hidden" || this.closest("div.dialogue").css("display") == "none") {
					// mark the element with data-focus=true (the page display will then set the focus on this)
					this.attr("data-focus","true");			  
				}
			}		 
			// reset the arguments
			arguments = [];
    	}
		// apply and return the original method
        return focus_orignal.apply(this, arguments);
    };
})(jQuery);

// thanks to http://stackoverflow.com/questions/2200494/jquery-trigger-event-when-an-element-is-removed-from-the-dom/10172676#10172676
(function($) {
  $.event.special.destroyed = {
    remove: function(o) {
      if (o.handler) {
        o.handler()
      }
    }
  }
})(jQuery);

/*
 
 http://code.accursoft.com/caret
 
 Copyright (c) 2009, Gideon Sireling

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of Gideon Sireling nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
(function($) {
  $.fn.caret = function(pos) {
    var target = this[0];
	var isContentEditable = target.contentEditable === 'true';
    //get
    if (arguments.length == 0) {
      //HTML5
      if (window.getSelection) {
        //contenteditable
        if (isContentEditable) {
          target.focus();
          var range1 = window.getSelection().getRangeAt(0),
              range2 = range1.cloneRange();
          range2.selectNodeContents(target);
          range2.setEnd(range1.endContainer, range1.endOffset);
          return range2.toString().length;
        }
        //textarea
        return target.selectionStart;
      }
      //IE<9
      if (document.selection) {
        target.focus();
        //contenteditable
        if (isContentEditable) {
            var range1 = document.selection.createRange(),
                range2 = document.body.createTextRange();
            range2.moveToElementText(target);
            range2.setEndPoint('EndToEnd', range1);
            return range2.text.length;
        }
        //textarea
        var pos = 0,
            range = target.createTextRange(),
            range2 = document.selection.createRange().duplicate(),
            bookmark = range2.getBookmark();
        range.moveToBookmark(bookmark);
        while (range.moveStart('character', -1) !== 0) pos++;
        return pos;
      }
      //not supported
      return 0;
    }
    //set
    if (pos == -1)
      pos = this[isContentEditable? 'text' : 'val']().length;
    //HTML5
    if (window.getSelection) {
      //contenteditable
      if (isContentEditable) {
        target.focus();
        window.getSelection().collapse(target.firstChild, pos);
      }
      //textarea
      else
        target.setSelectionRange(pos, pos);
    }
    //IE<9
    else if (document.body.createTextRange) {
      var range = document.body.createTextRange();
      range.moveToElementText(target)
      range.moveStart('character', pos);
      range.collapse(true);
      range.select();
    }
    if (!isContentEditable)
      target.focus();
    return pos;
  }
})(jQuery);

// this function can be used on the keydown of textareas to prevent changing to the next control
function textareaOverride(ev) {
	// get the text area
	var textarea = $(ev.target);
	// if this is the tab key
	if (textarea.is("textarea") && ev.keyCode == 9) {
		// get the current cursor position
		var pos = textarea.caret();
		// get the current value
		var val = textarea.val();
		// add a tab at the current postion and re-assign to the textarea
		textarea.val(val.substr(0,pos) + "\t" + val.substr(pos));
		// advance the cursor position by 1
		textarea.caret(pos + 1);
		// stop any further events
		ev.stopPropagation();
		// stop any further behaviour from the tab key, like loosing focus
		return false;
	}
}

// function for applying the maxlength rules to a textarea (reused when dialogue loads)
function textarea_maxlength() {
	
	// ignore these keys
	var ignore = [8,9,13,33,34,35,36,37,38,39,40,46];
	
	// wrap the textarea
	$(this)
	
	// use keypress instead of keydown as that's the only
  	// place keystrokes could be canceled in Opera
    .on('keypress', function(event) {
      var self = $(this),
          maxlength = self.attr('maxlength'),
          code = $.data(this, 'keycode');

      // check if maxlength has a value.
      // The value must be greater than 0
      if (maxlength && maxlength > 0) {

        // continue with this keystroke if maxlength
        // not reached or one of the ignored keys were pressed.
        return ( self.val().length < maxlength || $.inArray(code, ignore) !== -1 );

      }
    })

    // store keyCode from keydown event for later use
    .on('keydown', function(event) {
      $.data(this, 'keycode', event.keyCode || event.which);
    });
	
}

// function 
function textarea_autoheight_size(textarea, resetHeight) {
	// reset height if required on setData and blur
	if (resetHeight) textarea.height(textarea.attr("data-height"));
	// if scroll height is already at the max
	if (textarea[0].scrollHeight > 1000) {
		// set height to max
		textarea.height(1000);
	} else {
		// max times
		var i = 0;
		// grow 
		while(i < 1000 && textarea.outerHeight() < textarea[0].scrollHeight + parseFloat(textarea.css("borderTopWidth")) + parseFloat(textarea.css("borderBottomWidth"))) {
			textarea.height(textarea.height() + 1);
			i ++;
	    };
	}
}

// function for apply the autoheight to a textarea  (reused when dialogue loads)
function textarea_autoheight(textarea) {
	
	// get a reference to the textarea if we weren't given one
	if (!textarea || !$(textarea).is("textarea")) textarea = $(this);
	
	// get the starting height
	var height = Math.max(textarea.height(), 10);
		
	// retain original height
	textarea.attr("data-height", height);
	
	// use keypress instead of keydown as that's the only place keystrokes could be cancelled in Opera
	textarea
	.on('keypress', function(ev) {
		textarea_autoheight_size(textarea);
    })
    .on('show', function(ev) {
    	// size it
		textarea_autoheight_size(textarea, true);
    })   
    .on('blur', function(ev) {
    	// small delay to allow any click events to fire
    	setTimeout(function(){ 
    		// get the body element
        	var b = $("body");
        	// get the html height
        	var h = b.height();
        	// set it in css (this stops the document resizing and bouncing us around)
        	b.css("height",h);        	
        	// do the sizing after resetting the height
        	textarea_autoheight_size(textarea, true);
            // set the body height back to auto
            b.css("height","auto");
    	}, 500);
    });
	
}

// this applies and enforces the maxlength attribute on textareas
jQuery(function($) {
  
	// handle textareas with maxlength attribute
	$('textarea[maxlength]').each( textarea_maxlength );
  
	// handle textareas with autoheight class
	$('textarea.autoheight').each( textarea_autoheight ); 
  
});

// override the much-used ajax call for both normal and Rapid Mobile 
if (window["_rapidmobile"]) {
	
	console.log("Rapid Mobile detected");
	
	// add save page html function for saving
	_rapidmobile.savePage = function() {
		// explicity push each input val in as an attribute
		$("input").each( function() {
			$(this).attr("value", $(this).val());
		});
		// explicitly push textarea value into it's html
		$("textarea").each( function() {
			$(this).html($(this).val());
		});
		// remove any script reference to the google maps api as they cause problems on reload
		$("head").find("script[src*='//maps.gstatic.com']").remove();
		// on this one we want to keep the first node so remove all but index = 0
		$("head").find("script[src*='googleapis.com/']").filter( function(idx) {
			return idx > 0;
		}).remove();		
								
		// get the html
		var html = $('html').html();
		// now call into the bridge
		_rapidmobile.savePageToDevice(html);		
	}
			
	// retain the original JQuery ajax function
	var ajax = $.ajax;
	
	// override it
	$.ajax = function(settings) {
		
		// only on old (unversioned clients with _rapidMobile), or with multithreaded local requests turned off
		if (!_rapidmobile.getVersion || !_rapidmobile.isMultiThreaded || !_rapidmobile.isMultiThreaded()) {
			// the shouldInterceptRequest method only works for GET, so if there is data add it to the url
			if (settings.data) {
				// add data to the url
				settings.url += "&data=" + encodeURIComponent(settings.data);
				// remove it from the body
				settings.data = null;
				// change the action to a GET
				settings.method = "GET";
			}
		}
		// retain the original success function
		var success = settings.success;
		// override it (and pass in the orginal)
		settings.success = function(data, textStatus, jqXHR) {
			// if there is a json object in the response
			if (jqXHR.responseJSON) {
				// if it contains an error object
				if (jqXHR.responseJSON.error) {					
					// get the error object
					var error = jqXHR.responseJSON.error;
					// check the status code
					switch (error.status) {
					case (401) :
						// the user failed authentication show them a message in the ui
						_rapidmobile.showMessage(error.responseText);
						// bail
						return false;
					default :
						// run the error function
						settings.error(error, error.status, error.responseText);
						// bail
						return false;
					}					
				}
			}
			// run the original function if all good
			if (success) success(data, textStatus, jqXHR);
		}
		// now run the original ajax with our modified settings
		ajax(settings);
	}
				
} else {
	
	// retain the original JQuery ajax function
	var ajax = $.ajax;
	// substitute our own
	$.ajax = function(url, settings) {
		// retain original error handler
		var error = url.error;
		// override error
		url.error = function(jqXHR, textStatus, errorThrown) {
			// if this is a 401 (unauthorised) redirect the user to the login page and set requestApp so we'll come straight back
			if (jqXHR.status == 401) {
				// start with a basic login page url
				var location = "login.jsp";
				// if an errorThrown was provided this might contain the location from a custom login
				if (errorThrown && errorThrown.indexOf("location=") == 0) location = errorThrown.substr(9);
				// if we're viewing an app we want to go back to it once logged in
				if (window.location.href.indexOf("/~?a=") > -1) {
					// look for an application parameter
					var appId = $.getUrlVar("a");
					// look for an application version parameter
					var appVersion = $.getUrlVar("v");
					// append escaped requestPath if there's an app, and version if it exists
					if (appId) location += "?requestApp=" + appId + (appVersion ? "&requestVersion=" + appVersion : "");
				}				
				// redirect to login page
				window.location = location;
			} else {
				// call the original error
				if (error) error(jqXHR, textStatus, errorThrown);
			}
		}
		// call the original
		ajax(url, settings);
	}
}


function showControlValidation(controlId, message) {
	var control = $("#" + controlId);
	control.addClass("validation");
	if (message) {
		var element = control.next("div.validation.validationMessage")[0];
		if (element) {
			$(element).html(message);
		} else {
			control.after("<div class='validation validationMessage'>" + message + "</div>");
		}
	}
}

function hideControlValidation(controlId) {
	var control = $("#" + controlId);
	control.removeClass("validation");
	control.next("div.validation.validationMessage").remove();
}

function getDataObjectFieldIndex(data, field, caseSensitive) {
	if (data && data.fields && data.fields.length > 0 && field) {
		for (var i in data.fields) {
			if (data.fields[i]) {
				if (caseSensitive) {
					if (data.fields[i] == field) return i;
				} else {
					if (data.fields[i].toLowerCase() == field.toLowerCase()) return i;
				}
			}
		}
		return -1;
	}
	return null;
}

function makeDataObject(data, field) {
	// check we were passed something to work with
	if (data != null && data !== undefined) {
		// convert any json strings to json
		if ($.type(data) == "string" && data.indexOf("{") == 0 && data.indexOf("}") == data.length - 1) data = JSON.parse(data);
		// return immediately if all well (we have rows and fields already and there is nothing to promote)
		if (data.rows && data.fields && !(field && (data[field]))) return data;		
		// initialise fields
		var fields = [];
		// initialise rows
		var rows = [];
		// initialise a fieldmap (as properties aren't always in the same position each time)
		var fieldMap = [];
		// if the field is what we're after move it into the data
		if (field && data[field] && !$.isFunction(data[field])) data = data[field];			
		// if the data is an array
		if ($.isArray(data)) {
			// loop the array
			for (var i in data) {
				// retrieve the item
				var item = data[i];
				// prepare a row
				var row = [];
				// if it's an object build data object from it's properties
				if ($.isPlainObject(item)) {				
					var fieldPos = 0;				
					for (var j in item) {
						// check for a field mapping				 
						if (fieldMap[fieldPos]) {
							// if the mapping is different 
							if (fieldMap[fieldPos] != j) {						
								// assume field isn't there
								fieldPos = -1;
								for (var k in fieldMap) {
									if (j == fieldMap[k]) {
										fieldPos =k;
										break;
									}
								}
								// field pos wasn't found
								if (fieldPos == -1) {
									fieldMap.push(j);
									fields.push(j);
									fieldPos = fields.length - 1;
								}
							}
						} else {
							// we don't have a mapping for this field (this is good, store field at this position in map and fields array)
							fieldMap.push(j);
							fields.push(j);
							fieldPos = fields.length - 1;
						}
						// store the data in the row at the field position 
						row[fieldPos] = item[j];
						// all being well the next property is in the next position, if it wraps it'll assume it's an unseen field
						if (fieldPos < fields.length - 1) fieldPos++;
					}								
				} else {
					// retain the field
					if (i == 0) fields.push(field);
					// make a row with the item
					row = [ item ];
				}
				// add the row
				rows.push(row);
			}				
		} else {
			var row = [];
			if ($.isPlainObject(data)) {
				for (var i in data) {
					fields.push(i);
					row.push(data[i]);
				}
			} else {
				fields.push(field);
				row.push(data);
			}		
			rows.push(row);
		}
		data = { fields: fields, rows: rows};
	}
	return data;
}

function makeObjectFromData(data, fields) {
	var object = data;
	if (data && data.rows && data.fields) {
		object = {};
		if (data.rows.length > 0 && data.fields.length > 0) {
			for (var i = 0; i < data.fields.length; i++) {
				if (fields && fields.length > 0) {
					for (var j = 0; j < fields.length; j++) {
						if (data.fields[i] == fields[j]) {
							object[data.fields[i]] = data.rows[0][i];
							break;
						}
					}
				} else {
					object[data.fields[i]] = data.rows[0][i];
				}
			}
		}
	}
	return object;
}

function mergeDataObjects(data1, data2, mergeType, field, maxRows, details) {
	var data = null;
	if (data1) {
		data1 = makeDataObject(data1);
		if (data2) {
			data2 = makeDataObject(data2);
			switch (mergeType) {
				case "append" : case "row" :
					var fields = [];
					for (var i in data1.fields) fields.push(data1.fields[i]);
					for (var i in data2.fields) {
						var gotField = false;
						for (var j in fields) {
							if (data2.fields[i] && fields[j] && data2.fields[i].toLowerCase() == fields[j].toLowerCase()) {
								gotField = true;
								break;
							}
						}
						if (!gotField) fields.push(data2.fields[i]);
					}
					data = {};
					data.fields = fields;
					if (mergeType == "append") {						
						if (data1.rows.length == 1 && data1.fields.length == 1 && data2.rows.length == 1 && data2.fields.length == 1 && !(details && details.type == "grid")) {
							data = data1.rows[0][0] + data2.rows[0][0]; 
						} else {
							data.rows = data1.rows;
							for (var i = 0; i < data2.rows.length; i++) {
								var row = [];
								for (var j in fields) {
									var value = null;
									for (var k in data2.fields) {
										if (fields[j] && data2.fields[k] && fields[j].toLowerCase() == data2.fields[k].toLowerCase()) {
											value = data2.rows[i][k];
											break;
										}
									}
									row.push(value);
								}
								data.rows.push(row);
							}
						}
					} else {
						data.rows = [];
						var totalRows = data2.rows.length;
						if (data1.rows.length > totalRows) totalRows = data1.rows.length;			
						for (var i = 0; i < totalRows; i++) {
							var row = [];
							for (var j in fields) {
								var value = null;
								if (i < data2.rows.length) {
									for (var k in data2.fields) {
										if (fields[j] && data2.fields[k] && fields[j].toLowerCase() == data2.fields[k].toLowerCase()) {
											value = data2.rows[i][k];
											break;
										}
									}
								}
								if (i < data1.rows.length && value == null) {
									for (var k in data1.fields) {
										if (fields[j] && data1.fields[k] && fields[j].toLowerCase() == data1.fields[k].toLowerCase()) {
											value = data1.rows[i][k];
											break;
										}
									}
								}
								row.push(value);
							}
							data.rows.push(row);
						}		
					}
				break;
				case "child" :
					var fieldMap = {};
					var fieldCount = 0;
					var fieldIndex = -1;
					// make a map of positions for fields common to parent and child data objects
					for (var i in data1.fields) {
						for (var j in data2.fields) {
							if (data1.fields[i] && data2.fields[j] && data1.fields[i].toLowerCase() == data2.fields[j].toLowerCase()) {
								fieldMap[i] = j;
								fieldCount ++;
							}
						}
						if (field && field.toLowerCase() == data1.fields[i].toLowerCase()) fieldIndex = i;
					}
					// if the field we want the child in is not present in the parent
					if (fieldIndex < 0) {
						// add the field
						data1.fields.push(field);
						// remember the position
						fieldIndex = data1.fields.length - 1;
					}
					// use the fields from the child
					var fields = data2.fields;											
					// loop all of the parent (destination) rows
					for (var i in data1.rows) {
						// get the parent row
						var r1 = data1.rows[i];
						// make sure we have as many cells in our row as there are fields
						while (r1.length < data1.fields.length)  r1.push(null);							
						// if there are matching fields between the parent (destination) and child (source)
						if (fieldCount > 0) {
							// create a child rows array 
							var childRows = []; 
							// loop the child (source) rows
							for (var j in data2.rows) {
								// get the row
								var r2 = data2.rows[j];
								// assume no matches
								var matches = 0;
								// loop the parent/child field map
								for (var k in fieldMap) {
									// if there is a match of values between the mapped fields in parent/child, count the match
									if (r1[k] == r2[fieldMap[k]]) matches ++; 
								}								
								// if values in all common fields between parent and child have been matched
								if (matches == fieldCount) {															
									// get the matched row from the source
									var row = data2.rows[j];
									// add the matched row to the child data object
									childRows.push(row);									
								}
							}
							// if data was put in the child rows use it to create a child data object in the parent
							if (childRows.length > 0) r1[fieldIndex] = {fields:fields,rows:childRows};
						} else {
							// assign the whole child data object to the parent row
							 r1[fieldIndex] = data2;
						} // has fields to match on
					} // parent row loop
					data = data1;					
				break;
				case "search" :
					var fieldIndexes = [];
					if (field) {
						var fields = field.split(",");
						for (var i in fields) {
							var f = fields[i].trim().toLowerCase();
							for (var i in data2.fields) {
								if (data2.fields[i] && data2.fields[i].toLowerCase() ==  f) {
									fieldIndexes.push(i);
								}
							}							
						}						
					}
					var data = {fields: data2.fields, rows: []};			
					var value = data1.rows[0][0];
					if (value && fieldIndexes.length > 0) {
						value = value.toLowerCase();								
						for (var i in data2.rows) {
							for (var j in fieldIndexes) {
								var v = data2.rows[i][fieldIndexes[j]];
								if (typeof v !== "undefined") {
									if (v.toLowerCase().indexOf(value) > -1) {
										data.rows.push(data2.rows[i]);
										break;
									}
								}
							}	
							if (data.rows.length >= maxRows) break;
						}
					}															
				break;
			}
							
		} else {
			data = data1;
		}
	} else {
		data = data2;
	}
	return data;
}

// assume css values are in pixels but if em is recognised convert (other units to follow)
function toPixels(size) {
	if (size) {
		if (size.indexOf("em") == size.length - 2) {
			var emSize = parseFloat($("body").css("font-size"));
		    return (emSize * parseFloat(size));
		} else {
			return parseFloat(size);
		}	
	} else {
		return 0;
	}
}

function getPageVariableValue(name, pageId) {
	var value = null;
	if (pageId && window["_pageVariables_" + pageId]) {
		value = window["_pageVariables_" + pageId][name];
	}
	if (!value) {
		value = $.getUrlVar(name);
	}
	return value;
}

function Event_initForm(id) {
	$("#" + id + "_form").submit( function() {		
		$("[id][disabled]").each( function () {
			$(this).removeAttr("disabled");
		});
		var hiddenControls = "";
		$(":hidden[id]:not([type=hidden])").each( function(i) {
			var id = $(this).attr("id");
			if (id.indexOf(_pageId) == 0) {
				if (hiddenControls) hiddenControls += ",";
				hiddenControls += id;
			}
		});
		$("#" + id + "_hiddenControls").val(hiddenControls);
	});
}

function Event_checkForm() {
	if (_formSubmitted) {
		$("input").disable();
		$("select").disable();
		$("textarea").disable();
		$("a:not([target])").unbind("click").click( function(ev){
			return false;
		}).addClass("disabled");
	}
}

function formatDatetime(format, date) {
	// check if format contains a time as well (split on " ")
	var formatParts = format.split(" ");
	var result = "";

	//if we have date and time parts
	if(formatParts.length > 1) {
		
		var dateFormatPart = formatParts[0];
		var timeFormatPart = formatParts[1];
		
		var dateString = $.datepicker.formatDate(dateFormatPart, date);
		var timeString = formatTime(timeFormatPart, date);
		
		result = dateString + " " + timeString;
		
	} else {	// we only have one part - either time or date
		
		if(formatParts[0].indexOf("yy") > -1) {
			result = $.datepicker.formatDate(formatParts[0], date);
			
		} else { // its just a time
			result = formatTime(formatParts[0], date);
		}
		
	}
	
	return result;
}

function formatTime(timeFormat, date) {
	var timePart = "";
	if(timeFormat == "24") {
		timePart = date.getTime() + ":" + date.getMinutes();
		
	} else {
		timePart = format12hour(date);
	} 
	
	return timePart;
}

function format12hour(date) {
	  var hours = date.getHours();
	  var minutes = date.getMinutes();
	  var ampmString = hours >= 12 ? 'PM' : 'AM';
	  hours = hours % 12;
	  hours = hours ? hours : 12; // the hour '0' should be '12'
	  minutes = minutes < 10 ? '0'+minutes : minutes;
	  var strTime = hours + ':' + minutes + ' ' + ampmString;
	  return strTime;
}

function padNumberWithZeros(number,length) {
	number += "";
	while (number.length < length) {
		number = "0" + number;
	}
	return number;
}