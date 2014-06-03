
function showDialogue(url) {

	// hide the control panel
	if (!_panelPinned) $("#controlPanel").hide();
	// add the dialogue div
	$("body").append("<div id='dialogue'></div>");
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
        error: function(server, status, error) { 
        	alert("Error loading dialogue : " + error); 
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
            		dialogue.show();
            	}, 10);
            	
        	}        	        	
        }
	});
}

function hideDialogue() {
	
	// remove
	$("#dialogue").remove();
	// hide the dialogueCover
	$("#dialogueCover").remove();
	
}

