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

function showDialogue(url, onShow) {

	// hide the control panel
	if (!_panelPinned) $("#controlPanel").hide();
	// remove any existing dialogues
	$("#dialogue").remove();
	// remove any existing dialogue covers
	$("#dialogueCover").remove();
	// add the dialogue div
	$("body").append("<div id='dialogue' class='dialogue'></div>");
	// add the dialogueCover div
	$("body").append("<div id='dialogueCover'></div>");
	
	// retain a reference to the window
	var win = $(window);
	// retain a reference to the document
	var doc = $(document);
	// retain a reference to the body
	var dialogue = $("#dialogue");
	// retain a reference to the dialogueCover div
	var dialogueCover = $("#dialogueCover");
	// hide it (but it must retain its geometry)
	dialogue.hide();

	// request the dialogue		
	$.ajax({
    	url: url,
    	type: "GET",          
        data: null,           
        async: false,
        error: function(server, status, error) { 
       		// if access denied
       		if (error && error.status == 403) {
       			// go to root to authenticate		       		
       			window.location.replace(".");
       		} else { 
       			alert("Error loading dialogue : " + error.responseText||message);
       		} 
        },
        success: function(page) {
        	
        	// if the page can't be found a blank response is sent, so only show if we got something
        	if (page) {
        		
        		// size and show the dialogueCover
        		dialogueCover.css({
            		width : doc.width(),
            		height : doc.height()
            	}).show();
            	
            	// retain any JavaScript
            	var javaScript = "";
            	// retain the body html
            	var bodyHtml = "";
            	
            	// loop the items
            	var items = $(page);
            	for (var i in items) {
            		// check for a script node
            		switch (items[i].nodeName) {
            		case "SCRIPT" :
              			// check there is some JavaScript inside (rather than a link), and it does not contain the design link comment
            			if (items[i].innerHTML && items[i].innerHTML.indexOf("/* designLink */") == -1) {
            				// retain the script in our array
            				javaScript += items[i].outerHTML;        				
            			}
            		break;
            		case "#text" : case "TITLE" : // ignore these two types
            		break;
            		default :
            			if (items[i].outerHTML) {
            				// retain the script in our body html
            				bodyHtml += items[i].outerHTML;        				
            			}
            		break;
            		}
            	}
            	
            	// if this is the login page go to the real thing, requesting to come back to this location
        		if (bodyHtml.indexOf("<form name=\"login\" id=\"RapidLogin\" method=\"post\">") > 0) {
            		            		
            		// stop the save changes onunload prompt
            		_dirty = false;
            		// go to the login page
            		window.location = "login.jsp?requestPath=" + window.location; 
            		
            	} else {
            	
	            	// inject the html and JavaScript
	            	dialogue.append(bodyHtml + javaScript);
	            	            	            	
	            	// size the dialogue
	            	dialogue.css({
	            		position : "fixed",
	            		left : (win.width() - dialogue.outerWidth()) / 2,
	            		top : (win.height() - dialogue.outerHeight()) / 3
	            	});
	            	
	            	// this seems to be the best way to avoid the resizing/flicker when showing
	            	window.setTimeout( function() {
	            		// show the dialogue
	            		dialogue.show();	        
	            		// set the focus now that we're visible 
	            		$('[data-focus]').focus();
	            	}, 200);
	            	
	            	// if any wait half a sec
                	if (onShow) {
                		window.setTimeout( function() {
    	            		// run the onshow
                			onShow();            	                			
    	            	}, 500);                		
                	}
            	
            	} // login page check
            	
        	} // page check   
        	
        } // success
        
	});
}

function hideDialogue() {
	
	// remove
	$("#dialogue").remove();
	// hide the dialogueCover
	$("#dialogueCover").remove();
	
}

