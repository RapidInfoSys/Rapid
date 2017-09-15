
/* This file is auto-generated on application load and save - it is minified when the application status is live */


/* Control and Action resource JavaScript */


/* Page control javascript resource JavaScript */

function Event_error(eventName, controlId, ex) {
	if (controlId) {
		alert("Error in " + eventName + " event for control " + controlId + "  " + ex);
	} else {
		alert("Error in " + eventName + " event for page " + ex);
	}
}

/* Data store control javascript resource JavaScript */

function getDataStoreData(id, details, field) {
	var data;
	switch (details.storageType) {
		case "L":
			// use localStorage
			if (!localStorage[_appId + "_" + id]) localStorage[_appId + "_" + id] = "{}";
			// get the data string
			var dataString = localStorage[_appId + "_" + id];
			// get data
			data = JSON.parse(dataString);				
		break;
		case "S": 
			// use sessionStorage
			if (!sessionStorage[_appId + "_" + id]) sessionStorage[_appId + "_" + id] = "{}";
			// get the data string
			var dataString = sessionStorage[_appId + "_" + id];
			// get data
			data = JSON.parse(dataString);
		break;
		case "P": 
			// instansiate an object in the page if there isn't one
			if (!window[id + "datastore"]) window[id + "datastore"] = {};
			// use the in page object
			data = window[id + "datastore"];
		break;
	}  
	// return it	
	return data;
}	                

function saveDataStoreData(id, details, data) {
	switch (details.storageType) {
		case "L":
			// use localStorage
			localStorage[_appId + "_" + id] = JSON.stringify(data);			
		break;
		case "S": 
			// use sessionStorage
			sessionStorage[_appId + "_" + id] = JSON.stringify(data);
		break;
		case "P": 
			// instansiate an object in the page if there isn't one
			window[id + "datastore"] = data;
		break;
	}
	var f = window["Event_change_" + id];
	if (f) f($.Event("change"));
}

/* Flow layout control javascript resource JavaScript */

$(document).ready( function() {
								
	$(window).resize(function(ex) {

		sizeFlowLayouts();

    });
    
});

function sizeFlowLayouts() {
	
	var layouts = [];
	
	// if we're in the designer
	if (window["_pageIframeWindow"]) {
		// get the layouts from the iFrame
		layouts = $(_pageIframeWindow[0].document).find("div.flowLayout")	
	} else {
		// get the layouts directly
		layouts = $("div.flowLayout");		
	}
	
	// loop the layouts
	layouts.each( function() {
		// get the layout
		var layout = $(this);
		// size it if visible (it'll have no size if not)
		if (layout.is(":visible")) sizeFlowLayout(layout);
	});
				
}

function sizeFlowLayout(layout) {

	// find the width (in pixels)
	var width = layout.width();
	// get all of the children
	var cells = layout.children(".flowLayoutCell");
	// get the details
	var details = window[layout.attr("id") + "details"];
	// set their width to the start width (however it was provided)
	cells.css("width", details.cellWidth);
	// get the first cell 
	var cell = layout.children(".flowLayoutCell").first();
	// get it's width in pixels
	var cellWidth = cell.width();
	// get it's width-affecting margin
	var cellMarginWidth = toPixels(cell.css("margin-left")) + toPixels(cell.css("margin-right"));
	// get it's with-affecting padding 
	var cellPaddingWidth = toPixels(cell.css("padding-left")) + toPixels(cell.css("padding-right")); 
	// get it's width-affecting borders
	var cellBorderWidth = toPixels(cell.css("border-left-width")) + toPixels(cell.css("border-right-width")); 		 
	// calculate the max number of whole cells across (can't be more than cells we have)
	var cellsWide = Math.min(Math.floor(width / cellWidth), cells.length);	
	// adjust the cell width to include equal amounts of the white space, and adjust for padding and border
	cellWidth += (width - cellsWide * cellWidth) / cellsWide - cellMarginWidth - cellPaddingWidth - cellBorderWidth;
	// if there is a transform in place
	if ($("body").css("transform") && $("body").css("transform") != "auto") {
		// if the new size exceeds (or equals) the available width reduce by 1 pixel divided by number of cells
		if (width <= (cellWidth + cellMarginWidth + cellPaddingWidth + cellBorderWidth) * cellsWide) cellWidth -= 1 / cellsWide;
	}
	// update the widths
	layout.children(".flowLayoutCell").css("width", cellWidth);			
	
}

/* Gallery control javascript resource JavaScript */

function Gallery_removeImage(ev, id) {
	// confirm
	if (confirm("Remove image?")) {
		// get the image
		var img = $(ev.target);
		// tell Rapid Mobile an image has been removed
		if (typeof _rapidmobile != "undefined") _rapidmobile.removeImage(id, img.attr("src")); 
		// remove it
		img.remove();
		// look for our custom imageRemoved handler for this control
		var imageRemoved = window["Event_imageRemoved_" + id];
		// fire it if we found it
		if (imageRemoved) window["Event_imageRemoved_" + id]();
	}
}              	

function Gallery_showImage(ev, id) {	
	// look for the big gallery div
	var gallery = $("div.bigGallery");
	// if we couldn't find one add, and 
	if (!gallery[0]) gallery = $("body").append("<div class='bigGallery'></div>").find("div.bigGallery");
	// hide the gallery
	gallery.hide();
	// get the image
	var img = $(ev.target);
	// add the image to the gallery
	gallery.html("<img src='" + img.attr("src") + "'/>");
	// get the index
	var index = img.index();
	// get the details
	var details = window[id + "details"];	
	// if we've got the details
	if (details) {					
	// show the caption
	gallery.append(details.captions[index]);
	}
}

/* Grid control javascript resource JavaScript */

function getGridDataStoreData(id, details) {
	data = null;
	dataString = null;
	switch (details.dataStorageType) {
		case "L" :
			dataString = localStorage[_appId + id];
		break;
		case "S" :
			dataString = sessionStorage[_appId + id];
		break;
		case "P" :
			dataString = window[_appId + "datastore"];			
		break;
	}
	if (dataString) {
		data = JSON.parse(dataString);
		if (data) {
			if (!data.fields) data.fields = [];
			if (!data.rows) data.rows = [];
		}
	}
	return data;
}

function saveGridDataStoreData(id, details, data) {
	switch (details.dataStorageType) {
		case "L" :
			localStorage[_appId + id] = JSON.stringify(data);
		break;
		case "S" :		
			sessionStorage[_appId + id] = JSON.stringify(data);
		break;
		case "P" :
			window[_appId + "datastore"] = JSON.stringify(data);
		break;
	}
}	

function sortGridByColumn(ev, id, details, column, asc) {

	// check we have details and this column
	if (details && details.columns && details.columns[column]) {

		// get the grid data
		var data = getData_grid(ev, id, null, details);
			
		// get the field
		var field = details.columns[column].field;
		
		// assume data and details columns are the same
		var dataColumn = column;
		
		// check the column matches the field
		if (data.fields[column] != field) {
			// didn't match so find what it should be
			for (var i in data.fields) {
				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
					dataColumn = i;
					break;
				}
			}	
		}
	
		// check there is data for the column
		if (data && data.rows && data.fields[dataColumn]) {
		
			// an array for all values in this column
			var sortedValues = [];
			// populate the array for each row with an object with the original index and value
			for (var i in data.rows) sortedValues.push( {index:i, value:data.rows[i][dataColumn]} );
			// determin the sort type
			switch (details.columns[column].sort) {
				// for n, turn both values into numbers and use that
				case "n" : sortedValues.sort( function(i1, i2) {
					var v1 = i1.value;
					if (v1) v1 = parseFloat(v1.replace(",",""));
					var v2 = i2.value;
					if (v2) v2 = parseFloat(v2.replace(",",""));
					return v1 - v2; 
				});			
				break;
				// d1 is uk date format
				case "d1": sortedValues.sort( function(i1, i2) {
					// get the parts of the dates
					var d1Parts = i1.value.split("/");
					var d2Parts = i2.value.split("/");
					// easy one's first (null or not enough parts to contruct a date)
					if (!d1Parts || d1Parts.length < 3) {
						return 1;
					} else if (!d2Parts || d2Parts.length < 3) {
						return -1;
					} else {
						var d1 = new Date(d1Parts[2], d1Parts[1]-1, d1Parts[0]);
						var d2 = new Date(d2Parts[2], d2Parts[1]-1, d2Parts[0]);
						return d1.getTime() - d2.getTime();
					}
				});					
				break;
				// d2 is uk month-word date format
				case "d2": sortedValues.sort( function(i1, i2) {
					// get the parts of the dates
					var d1Parts = i1.value.split("-");
					var d2Parts = i2.value.split("-");
					// easy one's first (null or not enough parts to contruct a date)
					if (!d1Parts || d1Parts.length < 3) {
						return 1;
					} else if (!d2Parts || d2Parts.length < 3) {
						return -1;
					} else {
						var months = ["jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"];
						for (m1 in months) if (d1Parts[1].toLowerCase().substr(0,3) == months[m1]) break;
						for (m2 in months) if (d2Parts[1].toLowerCase().substr(0,3) == months[m2]) break;
						var d1 = new Date(d1Parts[2], m1, d1Parts[0]);
						var d2 = new Date(d2Parts[2], m2, d2Parts[0]);
						return d1.getTime() - d2.getTime();
					}
				});					
				break;
				// d3 is us date format
				case "d3": sortedValues.sort( function(i1, i2) {
					// get the parts of the dates
					var d1Parts = i1.value.split("/");
					var d2Parts = i2.value.split("/");
					// easy one's first (null or not enough parts to contruct a date)
					if (!d1Parts || d1Parts.length < 3) {
						return 1;
					} else if (!d2Parts || d2Parts.length < 3) {
						return -1;
					} else {
						var d1 = new Date(d1Parts[2], d1Parts[0]-1, d1Parts[1]);
						var d2 = new Date(d2Parts[2], d2Parts[0]-1, d2Parts[1]);
						return d1.getTime() - d2.getTime();
					}
				});					
				break;
				// d4 is xml date format
				case "d1": sortedValues.sort( function(i1, i2) {
					// get the parts of the dates
					var d1Parts = i1.value.split("-");
					var d2Parts = i2.value.split("-");
					// easy one's first (null or not enough parts to contruct a date)
					if (!d1Parts || d1Parts.length < 3) {
						return 1;
					} else if (!d2Parts || d2Parts.length < 3) {
						return -1;
					} else {
						var d1 = new Date(d1Parts[0], d1Parts[1]-1, d1Parts[1]);
						var d2 = new Date(d2Parts[0], d2Parts[1]-1, d2Parts[1]);
						return d1.getTime() - d2.getTime();
					}
				});					
				break;
				// for c, make a new function from the sortFunction property and use that
				case "c":
					var f = new Function("item1","item2",details.columns[column].sortFunction);
					sortedValues.sort(f);				
				break;
				default : sortedValues.sort( function(i1, i2) {				
					// easy one's first
					if (i1.value == null) {				
						return 1;
					} else if (i2.value == null) {
						return -1;
					} else {
						// assume the values match
						var s = 0;
						// get both values in lower case
						v1 = i1.value.toLowerCase();
						v2 = i2.value.toLowerCase();
						// only if the two are different
						if (v1 != v2) {			
								// get the shortest length
								var l = Math.min(v1.length, v2.length);
								// loop the shortest length
								for (var i = 0; i < l; i++) {
									// check the different in ascii values
									if (v1.charCodeAt(i) > v2.charCodeAt(i)) {
										s = 1;
										break;
									} else if (v1.charCodeAt(i) < v2.charCodeAt(i)) {
										s = -1;
										break;
									}					
								}
								// if s is still zero use the length of the strings
								if (s == 0) s = v1.length - v2.length;			
						} // same check
						return s
					} // null checks					
				}); 
			}
					
			// create a new rows array
			var rows = [];
			// if asc
			if (asc) {
				// loop the returned sorted column values and add into the rows array from their original position
				for (var i in sortedValues)  rows.push(data.rows[sortedValues[i].index]);
			} else {
				// loop the returned sorted column values in reverse  and add into the rows array from their original position
				for (var i in sortedValues)  rows.push(data.rows[sortedValues[sortedValues.length - i - 1].index]);
			}
			// replace the data rows with the placed rows
			data.rows = rows;
			// set the sorted data
			setData_grid(ev, id, null, details, data);
		}
	}
} 

// get the index of the selected row, ignoring rows without rowStyle1 and rowStyle2 which can be introduced by custom code
function getGridSelectedRowNumber(id, ev) {
	// assume we're not going to find it
	var index = -1;
	// get the target of the event
	var target = $(ev.target);
	// look for whether the target was in a grid row
	var row = target.closest("tr.rowStyle1,tr.rowStyle2");
	// check if in a row
	if (row[0]) {
		// the event occured from within a row so find the table, and the index of the row from the official rows
		var rows = row.closest("table").find("tr.rowStyle1,tr.rowStyle2").not("[data-ignore]");
		// if this was a row to be ignored, find the previous most non-ignored row
		if (row.is("[data-ignore]")) row = row.prevAll("tr.rowStyle1,tr.rowStyle2").not("[data-ignore]").first();
		// get the position of relevant row
		index = rows.index(row);		
	} else {
		// the event occured outside a row so get the table using the id
		var grid = $("#" + id);
		// index is the row with the selected style within the grid
		index = grid.find("tr.rowStyle1,tr.rowStyle2").not("[data-ignore]").index(grid.find("tr.rowSelect"));			
	}
	if (index > -1) index ++;
	return index;
}

// gets the selected row, ignoring rows without rowStyle1 and rowStyle2 which can be introduced by custom code
function getGridSelectedRow(id, ev) {
	// assume we're not going to find it
	var row = null;
	// get the target of the event
	var target = $(ev.target);
	// look for whether the target was in a grid row
	var row = target.closest("tr.rowStyle1,tr.rowStyle2");
	// check if in a row
	if (row[0]) {
		// the event occured from within a row so find the table, and the index of the row from the official rows
		var rows = row.closest("table").find("tr.rowStyle1,tr.rowStyle2").not("[data-ignore]");
		// if this was a row to be ignored, find the previous most non-ignored row
		if (row.is("[data-ignore]")) row = row.prevAll("tr.rowStyle1,tr.rowStyle2").not("[data-ignore]").first();
	} else {
		// the event occured outside a row so get the table using the id
		var grid = $("#" + id);
		// row is .rowSelect
		row = grid.find("tr.rowSelect").not("[data-ignore]");			
	}
	return row;
}

/* Link control javascript resource JavaScript */

function linkClick(url, sessionVariablesString, popup) {
	
	var sessionVariables = JSON.parse(sessionVariablesString);
	
	for (var i in sessionVariables) {
	
		var value = null;
	
		var item = sessionVariables[i];
		
		var itemId = item.itemId;
		
		if (itemId) {
		
			if (item.type) {
		
				var idParts = itemId.split(".");
				
				var ev = $.Event("linkClick");
				
				if (idParts.length == 1) {
			
					value = window["getData_" + item.type](ev, itemId, item.field, item.details);
					
				} else if (idParts.length == 2) {
							
					value = window["getProperty_" + item.type + "_" + idParts[1]](ev, idParts[0], item.field, item.details);
									
				}
				
			} else if (itemId.indexOf("System") == 0) {
			
				var idParts = itemId.split(".");
			
				switch (idParts[1]) {
					case "app id" : value = _appId;
					break;
					case "app version" : value = _appVersion;
					break;
					case "page id" : value = _pageId;
					break;
					case "user name" : value = _userName;
					break;
					case "online" : value = (typeof _rapidmobile == 'undefined' ? true : _rapidmobile.isOnline() == true || _rapidmobile.isOnline() == 'true');
					break;
					case "mobile" : value = (typeof _rapidmobile == 'undefined' ? false : true);
					break;
					case "mobile version" : value = (typeof _rapidmobile == 'undefined' ? -1 : (_rapidmobile.getVersion ? _rapidmobile.getVersion() : 0));
					break;
					case "true" : value = "true";
					break;
					case "false" : value = "false";
					break;
					case "field" : value = item.field;
					break;
				}
				
			}
		
		} else {
		
			value = $.getUrlVar(item.itemId);
		
		}
	
		if (value != null && value !== undefined) url += "&" + item.name + "=" + value;
	}
	
	if (popup) {
		window.open(url, "_blank");
	} else {
		window.location = url;
	}
}

/* Page panel control javascript resource JavaScript */

var _stickyFooterHeight = 0;

function checkPagePanelFooter() {
	var footer = $("div.pagePanelFooter");
	if (footer[0]) {
		window.setTimeout( function(ev) {
			// set html height back to auto
			$("html").css("height","auto");
			$("body").css("min-height","0");
			var fh = footer.height();
			if (fh > 0) _stickyFooterHeight = fh;
			var body = $(document.body);			
			var bh = 0;
			body.children("div.r-panel:visible").each(function(){
 				bh += $(this).height();
			});
			bh = Math.max(bh,body.height());			
			if (bh + body.offset().top + _stickyFooterHeight < $(window).height()) {
				// Add sticky footer
				footer.addClass("pagePanelStickyFooter");
			}	else {
				// Remove sticky footer
				footer.removeClass("pagePanelStickyFooter");
			}
		}, 200);
	}
}

$(document).ready( function() {	
	$("button").click(checkPagePanelFooter);	
	$("input").change(checkPagePanelFooter);
	$("select").change(checkPagePanelFooter);
	$("textarea").change(checkPagePanelFooter);	
	$(window).resize(function() {
		checkPagePanelFooter();
		positionLoadingCovers();
	});
	checkPagePanelFooter();
});

/* Responsive date control javascript resource JavaScript */

var date_quickISORegex = /^\d{4}-\d{2}-\d{2}/;
var date_quickOracleRegex = /^\d{1,2}-[a-zA-Z]{3}-\d{4}/;

/* Signature control javascript resource JavaScript */

var _signatureControls = {};
var _signatureCanvases = [];
	                	                
// JQuery is ready! 
$(document).ready( function() {

	$(window).resize(function(ex) {
	
		var ratio =  Math.max(window.devicePixelRatio || 1, 1);
	
		for (var i in _signatureCanvases) {
			canvas = _signatureCanvases[i];
			var context = canvas.getContext("2d");
			var data = context.getImageData(0,0,canvas.width, canvas.height);
	    	canvas.width = canvas.offsetWidth * ratio;
	    	canvas.height = canvas.offsetHeight * ratio;
	    	context.scale(ratio, ratio);
	    	context.putImageData(data, 0, 0);
	    }
	    
	});
	
});

/* Slide panel control javascript resource JavaScript */

function sizeSlidePanelCover() {
	
	if (typeof(window.parent._pageIframe) === "undefined") {
	
		// get a reference to the document for the full height
		var doc = $(document);
		// get a reference to the slidePanelPane if it's there yet
		var panel = $(".slidePanelPane");
		// if we got a panel resize it
		if (panel[0]) panel.css("height",doc.height() - panel.offset().top);
		
		// get a reference to the window for the viewport size
		var win = $(window);			
		// resize the page cover
		$("div.slidePanelCover").css({
       		width : win.width(),
       		height : win.height()
       	});
       	
    } else {
    
    	// get the page iframe
    	var _pageIframe = window.parent._pageIframe;
    	// get the scale
    	var _scale = window.parent._scale;
    		    		    
    	// resize the page cover
    	$(_pageIframe[0].contentDocument).find("div.slidePanelCover").css({
       		width : _pageIframe.width() / _scale,
       		height : _pageIframe.height() / _scale
       	});
    
    }
}	                
	                
//JQuery is ready! 
$(document).ready( function() {	
	$(window).resize(function(ex) {	
		sizeSlidePanelCover();       	      		
	});	
});

/* Database action javascript resource JavaScript */

// this global associative array tracks the databaseAction call sequences for each action	    			
var _databaseActionSequence = [];	    

// this global associative array holds the greates sequence received back     			
var _databaseActionMaxSequence = [];	

// this function returns an incrementing sequence for each database action call so long-running slow queries don't overrwrite fast later queries
function getDatabaseActionSequence(actionId) {
	// retrieve the current sequence for the action
	var sequence = _databaseActionSequence[actionId];
	// if null set to 0
	if (!sequence) sequence = 0
	// increment
	sequence++;
	// store
	_databaseActionSequence[actionId] = sequence;
	// pass back
	return sequence;
}		

// this function sets the max to 0 if null
function getDatabaseActionMaxSequence(actionId) {
	// retrieve the current sequence for the action
	var sequence = _databaseActionMaxSequence[actionId];
	// if undefined
	if (sequence === undefined) {
		// set to 0
		sequence = 0;
		// retain for next time
		_databaseActionMaxSequence[actionId] = sequence;
	}
	// pass back
	return sequence;
}	

// this function creates input data for the database action
function getDatabaseActionInputData(multiRow, inputs, sourceId, sourceData) {
	// start data object
	var data = {};
	// check multirow
	if (multiRow) {
		// check there are sourceData rows
		if (sourceData && sourceData.fields && sourceData.rows && sourceData.fields.length > 0 && sourceData.rows.length > 0) {
			// add a fields collection
			data.fields = [];
			// loop the inputs
			for (var i in inputs) {
				// the field we want to send is the source id plus the field, this matches how we do non multi row queries
				data.fields.push(sourceId + "." + inputs[i]);
			}
			// add a rows collection
			data.rows = [];
			// loop the sourceData rows
			for (var i in sourceData.rows) {
				// get the source row
				var sourceRow = sourceData.rows[i];
				// make a row for our return
				var row = [];
				// now loop the inputs
				for (var j in inputs) {
					// get the input field
					var field = inputs[j];
					// assume we can't find the field we want
					var fieldIndex = -1;
					// loop the source fields looking for the position of the field we want
					for (var k in sourceData.fields) {
						if (field.toLowerCase() == sourceData.fields[k].toLowerCase()) {
							// set the fieldIndex
							fieldIndex = k;
							// we're done
							break;
						}
					}
					// if we found the field
					if (fieldIndex > -1) {
						row.push(sourceRow[fieldIndex]);
					} else {
						row.push(null);
					}
				}
				// add the row
				data.rows.push(row);
			}
		} else {
			// add a dummy row 
			data.rows = [];
			data.rows.push([]);
		}
	} else {
		// not multirow so add fields 
		data.fields = [];
		// add a single row for the values
		data.rows = [];
		data.rows.push([]);
		// loop the inputs and add id as field, value as row
		for (var i in inputs) {
			var input = inputs[i];
			data.fields.push(input.id);
			data.rows[0].push(input.value);
		}
	}
	// return data
	return data
}

/* Webservice action javascript resource JavaScript */

// this global associative array tracks the webserviceAction call sequences for each action	    			
var _webserviceActionSequence = [];	    

// this global associative array holds the greates sequence received back     			
var _webserviceActionMaxSequence = [];	

// this function returns an incrementing sequence for each database action call so long-running slow queries don't overrwrite fast later queries
function getWebserviceActionSequence(actionId) {
	// retrieve the current sequence for the action
	var sequence = _webserviceActionSequence[actionId];
	// if null set to 0
	if (!sequence) sequence = 0
	// increment
	sequence++;
	// store
	_webserviceActionSequence[actionId] = sequence;
	// pass back
	return sequence;
}	

// this function sets the max to 0 if null
function getWebserviceActionMaxSequence(actionId) {
	// retrieve the current sequence for the action
	var sequence = _webserviceActionMaxSequence[actionId];
	// if undefined
	if (sequence === undefined) {
		// set to 0
		sequence = 0;
		// retain for next time
		_webserviceActionMaxSequence[actionId] = sequence;
	}
	// pass back
	return sequence;
}


/* Control initialisation methods */


function Init_calendar(id, details) {
  var calendar = $("#" + id);
  var date = new Date();
  calendar.attr("data-year",date.getFullYear());
  calendar.attr("data-month",date.getMonth());
  calendarUpdate(id);
}

function Init_flowLayout(id, details) {
  $("#" + id).children("div").click( function(ev) {
  	// get a reference to the flow layout
  	var layout = $("#" + id);
  	// remove selected from all child divs
  	layout.children("div").removeClass("selected");
  	// add selected to the div we just clicked on, also get it's index, plus 1, to go from zero to 1 based
  	var index = $(this).addClass("selected").index() + 1;
  	// apply selected to the correct body
  	layout.children("div:nth-child(" + index + ")").addClass("selected");
  });
}

function Init_gallery(id, details) {
  $("#" + id).children("img").click( function(ev) {
  	var details = window[id + "details"];
  	if (details) {
  		if (details.onImageClick == "large") {
  			Gallery_showImage(ev,id);
  		} else if (details.onImageClick == "caption") {
  			var index = $(ev.target).index();
  			if (details.captions && details.captions[index]) alert(details.captions[index]);
  		} else {
  			Gallery_removeImage(ev, id);			
  		}
  	}		
  });
}

function Init_grid(id, details) {
  // get the control
  var control = $("#" + id);
  
  // make sure it's a table
  if (!control.is("table")) control = control.find("table").last();
         
  // check there are details	        
  if (details) {
  	// if there are columns
  	if (details.columns) {				
  		// get the header cells
  		var headers = control.find("tr:first-child td");
  		// loop the columns
  		for (var i in details.columns) {
  			// if it has a sort
  			if (details.columns[i].sort) {
  				// get the header cell
  				var cell = $(headers.get(i));
  				// change the cursor
  				cell.css("cursor","pointer");
  				// add a listener
  				cell.click( {index:i},function(ev) {
  					// get the span
  					var span = $(ev.target);
  					// drill down if need be
  					if (!span.is("span")) span = span.find("span");
  					// if ascending or not set (note the unicode escape)
  					if (span.html() == "\uf0de") {
  						sortGridByColumn(ev, id, details, ev.data.index, false);
  						span.html("&#xf0dd;");
  					} else {
  						sortGridByColumn(ev, id, details, ev.data.index, true);
  						span.html("&#xf0de;");
  					}
  				});
  			}
  		}
  	}
  	// if there is a data store
  	if (details.dataStorageType) {
  		var data = getGridDataStoreData(id, details);
  		if (data) setData_grid($.Event('gridinit'), id, null, details, data);
  		$("#" + id).click( function(ev) {
  			var data = getGridDataStoreData(id, details);
  			data.selectedRowNumber = getGridSelectedRowNumber(id, ev);
  			saveGridDataStoreData(id, details, data);
  		});
  	} // datastorage type check
  }
}

function Init_map(id, details) {
  loadMapJavaScript(id, details);
}

function Init_pagePanel(id, details) {
  // if this page is not static reload it
  if (details.simple === undefined || !details.simple) {
  	        
  	var bodyHtml = "<center><h1>Page</h1></center>";
  	
  	// request the page
  	$.ajax({
  	   	url: "~?a=" + _appId + "&v=" + _appVersion + "&p=" + details.pageId + "&action=dialogue",  // reuse the design link hiding from dialogues
  	   	type: "GET",
  	       data: null,
  	       error: function(server, status, error) {
  	       	var bodyHtml = "Error loading page : " + error;
  	       },
  	       success: function(page) {
  	       	
  	       // if the page can't be found a blank response is sent, so only show if we got something
  	       if (page) {
  	       	
  	       		// get the document head
  	        	var head = $("head");
  	       		// empty the body html
  	       		bodyHtml = "";
  	       		// assume the script is empty
  	       		script = "";
  	       		       		
  	           	// loop the items
  	           	var items = $(page);
  	           	for (var i in items) {
  	           		// check for a script node
  	           		switch (items[i].nodeName) {
  	           		case "#text" : case "TITLE" : case "META" : // ignore these types
  	           		break;
  	           		case "SCRIPT" :
  	           			// check whether a script block or link
  	           			if (items[i].innerHTML) {
  	           				var s = items[i].outerHTML;
  	           				// exclude the app id, version, page, mobile resume link
  	           				if (s.indexOf("var _appId =") == -1 && s.indexOf("var _appVersion =") == -1 && s.indexOf("var _pageId =") == -1 && s.indexOf("var _mobileResume =") == -1)
  	           					script += s;
  	           			} else {
  	           				// fetch the text
  	           				var text = items[i].outerHTML;
  	           				// look for a src
  	           				if (text.indexOf("src=\"") > 0) {
  	           					var startPos = text.indexOf("src=\"")+5;
  	           					var src = text.substr(startPos,text.indexOf("\"", startPos) - startPos);
  	           					// add if not in the head already
  	           					if (!head.find("script[src='" + src + "']")[0]) head.append(text);
  	           				}
  	           			}
  	           		break;
  	           		case "LINK" :
  	           			// fetch the text
  	           			var text = items[i].outerHTML;
  	           			// look for an href="
  	           			if (!items[i].innerHTML && text.indexOf("href=\"") > 0) {
  	           				var startPos = text.indexOf("href=\"")+6;
  	           				var href = text.substr(startPos,text.indexOf("\"", startPos) - startPos);
  	           				// add this link to the page if not there already
  	           				if (!head.find("link[href='" + href + "']")[0]) head.append(text);
  	           			}
  	           		break;
  	           		default :
  	           			if (items[i].outerHTML) {
  	           				// retain the script in our body html
  	           				bodyHtml += items[i].outerHTML;
  	           			}
  	           		break;
  	           		}
  	           	}
  	           	// wrap in sticky footer if need be
  	           	if (details.footer) bodyHtml = "<div class='pagePanelFooter'>" + bodyHtml + "</div>";
  	           	// apply the injected html
  	           	$("#" + id).html(bodyHtml);
  	           	// add script into the page (if applicable)
  	           	if (script) $("#" + id).append(script);
  	           	// fire the window resize event
  	           	$(window).resize();
  	    	}
  	    }
  	});
  }
}

function Init_responsivedate(id, details) {
  var format = details.format;
  if (!format) format = "dd/mm/yy";
  $( "#" + id ).find("input").datepicker({
  	changeMonth: true,
      changeYear: true,
      dateFormat: format,
      maxDate: details.maxDate,
      minDate: details.minDate,
      yearRange: details.yearRange,
      showOtherMonths: details.showOtherMonths,
      selectOtherMonths: details.selectOtherMonths,
      constrainInput: true,
      onChangeMonthYear: function(year, month, inst) {
      	// this changes the default behaviour of the month and year change drop down and arrows to select the previously selected day in the new month or year
      	try {
      		// if we have an instance of the date picker and it has a value we can work on
      		if (inst && inst.currentDay && inst.input.val()) {
      			// get the currently selected day - this can change as soon as we call getDate below
      			var dy = inst.selectedDay;
      			// get an jQuery wrapped instance of the date picker control
      			var dp = $(this);
      			// get the current date - the day can be from the initial or previous value, different from the currently selected
  	    		var d = dp.datepicker("getDate");
  	    		// if we got a date
  	    		if (d) {
  	    			// if we had a selected day use that
  	    			if (dy) d.setDate(dy);
  	    			// set the month to the new one
  		      		d.setMonth(month-1);
  		      		// set the year to the new one
  		      		d.setFullYear(year);
  		      		// set the new date
  		      		dp.datepicker("setDate", d);
  		      		// trigger a change in the control unless these have been temporarily turned off in the setData method 
  		      		if (!details.dontTriggerChange) inst.input.trigger("change");
  				}
  			}
  		} catch(ex){}
      }
  }).change( function (ev) {
  	var datePicker = $(this);
  	var date = datePicker.val();
  	if (date) {
  		var parts = date.split("/");
  		if (parts.length == 3) {
  			if (parts[2].length < 4) {
  				try {
  					var year = parseInt(parts[2]);
  					var now = new Date();
  					var y = now.getFullYear() + "";
  					var y1 = y.substr(0,2) * 1;
  					var y2 = y.substr(2) * 1;
  					if (year > y2 + 10) y1--;
  					if (parts[2].length == 1) date = parts[0] + "/" + parts[1] + "/" + y1 + "0" + parts[2];
  					if (parts[2].length == 2) date = parts[0] + "/" + parts[1] + "/" + y1 + parts[2];
  					if (parts[2].length == 3) date = parts[0] + "/" + parts[1] + "/" + (y1 + "").substr(0,1) + parts[2];
  					datePicker.val(date);
  				} catch (ex) {}
  			}
  		}
  		if (details.format == "dd-M-yy") datePicker.val(date.toUpperCase());
  	}
  }).mouseup( function (ev){
  	if ((details.readonly || details.readonly === undefined)) {
  		var datePicker = $(this);
  		var widget = datePicker.datepicker("widget");
  		if (widget.is(":visible") && widget.css("opacity") == 1) {
  			datePicker.datepicker("hide");
  		} else {
  			datePicker.datepicker("show");
  		}
  	}
  });
  // now set any value
  $( "#" + id ).find("input").datepicker("setDate", details.value);
}

function Init_signature(id, details) {
  var canvas = $("#" + id)[0];
  if (canvas) {
  	var minWidth = 2;
  	var maxWidth = 2;
  	switch (details.lineThickness) {
  	  case "thin" :
  	    minWidth = 0.5;
  	    maxWidth = 1.5;
  	  break;
  	  case "thick" :
  	    minWidth = 5;
  	    maxWidth = 7.5;
  	  break;
  	}
  	var signature = new SignaturePad(canvas,{
  	  backgroundColor : "rgb(255,255,255)",
  	  penColor: details.lineColour,
  	  minWidth: minWidth,
  	  maxWidth: maxWidth
  	});
  	_signatureControls[id] = signature;
  	var ratio =  Math.max(window.devicePixelRatio || 1, 1);
  	canvas.width = canvas.offsetWidth * ratio;
  	canvas.height = canvas.offsetHeight * ratio;
  	canvas.getContext("2d").scale(ratio, ratio);
  	_signatureCanvases.push(canvas);
  }
}

function Init_slidePanel(id, details) {
  // get a reference to the body
  var body = $("body");
  // get the pageCover
  var pageCover = body.find("div.slidePanelCover");
  // if we don't have one
  if (!pageCover[0]) {
  	// add one
  	body.append("<div class='slidePanelCover'></div>");
  	// set the reference
  	pageCover = body.find("div.slidePanelCover");
  	// get a reference to the document
  	var doc = $(document);	
  	// size the cover
  	pageCover.css({
      	width : doc.width(),
         	height : doc.height()
      });
  }
  
  // get a reference to the slidePanel
  var slidePanel = $("#" + id);
  // get the slidePanelPaneId
  var slidePanelPaneId = slidePanel.attr("data-pane");
  // get the pane
  var slidePanelPane = $("#" + slidePanelPaneId);
  
  // show or hide the page cover if panel is visible
  if (slidePanelPane.is(":visible")) {
  	pageCover.show();
  } else {
  	pageCover.hide();
  }
  
  // add the opener listener	       
  slidePanel.click({width: slidePanelPane.css("width"),left: slidePanelPane.css("margin-left")}, function(ev) {
  	// get the stored width
  	var width = ev.data.width
  	// get any existing left margin
  	var left = ev.data.left;
  	// check visibility
  	if (slidePanelPane.is(":visible")) {
  		// animate off-screen
  		slidePanelPane.animate({"margin-left": "-" + width}, 500, function() {
  			// hide when complete
  			slidePanelPane.hide();
  			// toggle open closed
  			slidePanel.removeClass("slidePanelOpen");
  			slidePanel.addClass("slidePanelClosed");	
  			// hide the page cover
  			pageCover.hide();		
  		});		
  	} else {
  		// set off screen
  		slidePanelPane.css({"margin-left": "-" + width}).show();
  		// animate to full width
  		slidePanelPane.animate({"margin-left": 0}, 500);		
  		// toggle open closed
  		slidePanel.removeClass("slidePanelClosed");
  		slidePanel.addClass("slidePanelOpen");
  		// show the page cover	
  		pageCover.show();
  	}
  });	        
  
  // add the cover listener
  pageCover.click({width: slidePanelPane.css("width"),left: slidePanelPane.css("margin-left")}, function(ev){
  	// get the stored width
  	var width = ev.data.width
  	// get any existing left margin
  	var left = ev.data.left;
  	// animate off-screen
  	slidePanelPane.animate({"margin-left": "-" + width}, 500, function() {
  		// hide when complete
  		slidePanelPane.hide();
  		// toggle open closed
  		slidePanel.removeClass("slidePanelOpen");
  		slidePanel.addClass("slidePanelClosed");	
  		// hide the page cover
  		pageCover.hide();		
  	});
  });
}


/* Control getData and setData methods */


function getData_calendar(ev, id, field, details) {
  var calendar = $("#" + id);
  var dateString = calendar.attr("data-date");
  if (dateString) {
  	var date = new Date(dateString);
  	return cal_f_tcalGenerateDate(date, details.dateFormat);
  } else {
  	return null;
  }
}

function setData_calendar(ev, id, field, details, data, changeEvents) {
  var calendar = $("#" + id);
  var date = null;
  var dateData = makeDataObject(data,field);
  if (dateData) {
  	if (dateData.rows && dateData.rows.length > 0) {
  		var dateString = dateData.rows[0][0];
  		if (dateData.fields && dateData.fields.length >0 && field) {
  			for (var i in date = dateData.fields) {
  				var dataField = dateData.fields[i];
  				if (dataField && dataField.toLowerCase() == field.toLowerCase()) {
  					dateString = dateData.rows[0][i];
  					break;
  				}
  			}
  		}		
  		date = cal_f_tcalParseDate(dateString, details.dateFormat);
  	}
  }
  if (date) {
  	calendar.attr("data-date",date.getFullYear() + "-" + (date.getMonth() + 1) + "-" + date.getDate());
  	calendar.attr("data-year",date.getFullYear());
  	calendar.attr("data-month",date.getMonth());
  } else {
  	calendar.removeAttr("data-date");
  }	
  
  calendarUpdate(id);
}

function getData_dataStore(ev, id, field, details) {
  var data = getDataStoreData(id, details, field);
  if (data) {		
  	if (data.rows && data.fields) {
  		if (data.rows[0] && field) {
  			for (var i in data.fields) {
  				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  					var value = data.rows[0][i];
  					if (value !== undefined) {
  						return value;
  					} else {
  						break;
  					}					
  				}
  			}
  			return null;
  		} else {
  			return data;
  		}
  	} else if (field && data[field]) {
  		return data[field];
  	} else {
  		return null;
  	}
  }	 
  return data;
}

function setData_dataStore(ev, id, field, details, data, changeEvents) {
  if (details.id) id = details.id;
  if (data != null && data !== undefined) {
  	data = makeDataObject(data, field);
  	saveDataStoreData(id, details, data);
  } else {
  	saveDataStoreData(id, details, null);
  }
}

function getProperty_dataStore_selectedRowData(ev, id, field, details) {
  var data = getDataStoreData(id, details);
  if (data && data.selectedRowNumber && data.rows && data.rows[data.selectedRowNumber - 1]) {
  	return {fields:data.fields,rows:[data.rows[data.selectedRowNumber - 1]]};
  }
}

function setProperty_dataStore_selectedRowData(ev, id, field, details, data, changeEvents) {
  var dataStoreData = getDataStoreData(id, details);	                
  if (dataStoreData && dataStoreData.selectedRowNumber && dataStoreData.rows && dataStoreData.rows.length >= dataStoreData.selectedRowNumber) {	
  	data = makeDataObject(data, field);
  	if (data && data.rows) {
  		dataStoreData.rows[dataStoreData.selectedRowNumber - 1] = data.rows[0][0];		
  	} else {
  		dataStoreData.rows.splice(dataStoreData.selectedRowNumber - 1,1);
  	}
  	saveDataStoreData(id, details, dataStoreData);
  }
}

function getProperty_dataStore_selectedRowNumber(ev, id, field, details) {
  var data = getDataStoreData(id, details);
  if (data) return data.selectedRowNumber;
}

function setProperty_dataStore_selectedRowNumber(ev, id, field, details, data, changeEvents) {
  var dataStoreData = getDataStoreData(id, details);	                
  if (dataStoreData) {	
  	data = makeDataObject(data, field);
  	if (data && data.rows) {
  		dataStoreData.selectedRowNumber = data.rows[0][0];
  		saveDataStoreData(id, details, dataStoreData);
  	}
  }
}

function getProperty_dataStore_rowCount(ev, id, field, details) {
  var dataStoreData = getDataStoreData(id, details);
  if (dataStoreData && dataStoreData.rows) return dataStoreData.rows.length;
}

function setProperty_dataStore_append(ev, id, field, details, data, changeEvents) {
  var dataStoreData = getDataStoreData(id, details);
  if (dataStoreData) {
  	data = makeDataObject(data, field);
  	if (data && data.rows) {
  		if (!dataStoreData.fields) dataStoreData.fields = data.fields;
  		if (!dataStoreData.rows) dataStoreData.rows = []; 
  		for (var i in data.rows) dataStoreData.rows.push(data.rows[i]);
  		saveDataStoreData(id, details, dataStoreData);
  	}	
  }
}

function getData_gallery(ev, id, field, details) {
  var data = {fields:["url"],rows:[]};
  var control = $("#" + id);
  var images = control.children("img");
  images.each( function(i) {
  	var url = $(this).attr("src");
  	data.rows.push([url]);
  });
  return data;
}

function setData_gallery(ev, id, field, details, data, changeEvents) {
  if (data != null && data !== undefined) {
  	var control = $("#" + id);
  	data = makeDataObject(data, field);
  	if (data.rows) {	
  		// remove the no pictures message
  		control.find("span").remove();
  		// look for url or urls in the fields or use the first column if not found	
  		var urlIndex = 0;
  		if (data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i] == "url" || data.fields[i] == "urls") {
  					urlIndex = i;
  					break;
  				}
  			}
  		}			
  		// loop the rows
  		for (var i in data.rows) {
  			// allow comma seperated list of urls in single field too
  			var urls = data.rows[i][urlIndex].split(",");
  			// loop the urls
  			for (var j in urls) {
  				// get the url
  				var url = urls[j];
  				// if it doesn't start with http add the sudo start for Rapid Mobile
  				if (url.indexOf("http") != 0) url = "http://images/" + url;
  				control.append("<img src='" + url  + "'></img>");
  				control.find("img").last().click( function(ev) {
  					Gallery_removeImage(ev, id);				
  				});
  				// look for our custom imageAddedEvent handler for this control
  				var imageAdded = window["Event_imageAdded_" + id];
  				// fire it if we found it
  				if (imageAdded) window["Event_imageAdded_" + id]();
  			}			
  		}
  	} 
  }
}

function getProperty_gallery_imageCount(ev, id, field, details) {
  return ($("#" + id).children("img").size());
}

function getProperty_gallery_urls(ev, id, field, details) {
  var urls = "";
  var control = $("#" + id);
  var images = control.children("img");
  images.each( function(i) {
  	var url = $(this).attr("src");
  	if (url) {
  		urls += url.replace("http://cache/","").replace("http://images/","");
  		if (i < images.length - 1) urls += ",";
  	}
  });
  if (urls) {
  	return urls;
  } else {
  	return null;
  }
}

function getData_grid(ev, id, field, details) {
  var data = null;
  if (details) {
  	// if there is a datastore and the grid is not present in the page    
  	if (details.dataStorageType && !$("#" + id)[0]) {
  		data = getGridDataStoreData(id, details);
  		if (field && data.fields && data.rows && data.rows.length > 0) {
  			for (var i in data.fields) {
  				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  					data = data.rows[0][i];
  					break;
  				}
  			}
  		}
  	} else if (details.columns) {
  		if (field) {		
  			var row = getGridSelectedRow(id, ev);
  			if (row[0]) {
  				for (var i in details.columns) {
  					if (details.columns[i].field && details.columns[i].field.toLowerCase() == field.toLowerCase()) {
  						data = row.children(":nth(" + i + ")").html();
  						break;
  					}
  				}
  			}	    
  			if (data == null && details.dataStorageType) {
  				var dataStore = getGridDataStoreData(id, details);
  				var rowIndex = getGridSelectedRowNumber(id, ev);
  				rowIndex --;
  				if (rowIndex > -1 && dataStore.rows && dataStore.rows.length > rowIndex) {
  					for (var i in dataStore.fields) {
  						if (dataStore.fields[i] && dataStore.fields[i].toLowerCase() == field.toLowerCase()) {
  							data = dataStore.rows[rowIndex][i];
  							break;
  						}
  					}
  				}
  			}
  		} else {
  			var data = {};
  			data.fields = [];		
  			for (var i in details.columns) {
  				var field = details.columns[i].field;
  				if (field) data.fields.push(details.columns[i].field);		
  			}
  			data.rows = [];
  			var table = $("#" + id);
  			if (!table.is("table")) table = table.find("table").last();
  			table.find("tr:not(:first):not([data-ignore])").each(function(i) {
  				var row = [];
  				$(this).children().each(function(i) {
  					if (details.columns[i].field) row.push($(this).html());
  				});
  				data.rows.push(row);
  			});
  			// if there is a data store
  			if (details.dataStorageType) {
  			    // get the data from the data store
  				var dataStore = getGridDataStoreData(id, details);
  				// if we got one
  				if (dataStore) {
  					// make a field map of data from the grid to data from the dataStore
  					var fieldMap = {};
  					// loop the fields from the control in the page
  					for (var i in data.fields) {
  						// assume we can't find this field
  						var found = false;
  						// loop the fields from the dataStore
  						for (var j in dataStore.fields) {
  							// if this is the field they both have
  							if (data.fields[i] && dataStore.fields[j] && data.fields[i].toLowerCase() == dataStore.fields[j].toLowerCase()) {
  								fieldMap[i] = j;
  								found = true;
  								break;
  							}							
  						}
  						// if not found 
  						if (!found) {
  							//add to dataStore fields
  							dataStore.fields.push(data.fields[i]);
  							// add to map
  							fieldMap[i] = dataStore.fields.length - 1;
  						}	
  					}
  					// loop the rows in the data from the grid
  					for (var i in data.rows) {
  						// add an empty row to the dataStore rows if there are more in the page
  						if (dataStore.rows.length < i - 1) dataStore.rows.push([]);
  						// if there are less cells in the rows than it's fields add more
  						while (dataStore.rows[i].length < dataStore.fields.length) dataStore.rows[i].push(null);						
  						// loop the fieldMap and update dataStore with values from page
  						for (j in fieldMap) dataStore.rows[i][fieldMap[j]] = data.rows[i][j];						
  					}
  					// set data to our merged dataStore
  					data = dataStore;	
  				}							
  			}						
  		}	
  	}
  }
  return data;
}

function setData_grid(ev, id, field, details, data, changeEvents) {
  // get the grid control	        
  var control = $("#" + id);
  // make sure it's a table
  if (!control.is("table")) control = control.find("table").last();
  // remove all rows except for the first one
  control.find("tr:not(:first-child)").remove();	        
  // reset any sort indicators if not being used for sorting
  control.find("span.sort").html("&#xf0dc");
  // if we have data
  if (data !== undefined) {
  
  	data = makeDataObject(data, field);
  	if (data && data.rows) {	        		
  		if (details && details.columns && data.fields) {
  			var columnMap = [];
  			for (var i in details.columns) {				
  				for (var j in data.fields) {
  					var found = false;
  					if (details.columns[i].field && data.fields[j]) {
  						if (details.columns[i].field.toLowerCase() == data.fields[j].toLowerCase()) found = true;
  					} else {
  						if (!data.fields[j]) found = true;
  					}
  					if (found) {
  						columnMap.push(j);
  						break;
  					}
  				}
  				// added the column to the map
  				if (columnMap.length == i) columnMap.push("");
  				// if we have cellFunction JavaScript, and it hasn't been turned into a function object yet
  				if (details.columns[i].cellFunction && !details.columns[i].f) details.columns[i].f = new Function(["id", "data", "field", "details", "value"],details.columns[i].cellFunction);
  			}
  			for (var i in data.rows) {
  				var row = data.rows[i];
  				var rowObject = control.append("<tr class='rowStyle" + (i % 2 + 1) + "'></tr>").find("tr:last");
  				for (var j in details.columns) {
  					var style = "";
  					if (!details.columns[j].visible) style += "display:none;";
  					if (details.columns[j].style) style += details.columns[j].style;
  					if (style) style = " style='" + style + "'";
  					// assume the cell has no value
  					var value = "";				
  					// get the position of this column in the data object
  					var mappedCol = columnMap[j];
  					// if we can find the column and it has data use it as the value
  					if (mappedCol && row[mappedCol]) value = row[columnMap[j]];
  					// add the cell with the value and return a reference
  					var cellObject = rowObject.append("<td" + style + ">" + value + "</td>").find("td:last");
  					// apply any cell function
  					if (details.columns[j].f) {
  						details.columns[j].f.apply(cellObject,[id, data, field, details, value]);
  					}
  				}		
  				// if there is a rowValidation collection
  				if (data.rowValidation && i < data.rowValidation.length) {
  					if (!data.rowValidation[i]) rowObject.addClass("validation");
  				}		
  			}
  			// if there is a selected row
  			if (data.selectedRowNumber) control.find("tr:nth(" + data.selectedRowNumber + ")").addClass("rowSelect");
  		} else {
  			for (var i in data.rows) {
  				var row = data.rows[i];
  				var rowHtml = "<tr>";
  				for (var j in row) {
  					rowHtml += "<td>" + row[j] + "</td>";
  				}
  				rowHtml += "</tr>";
  				control.append(rowHtml);
  			}
  		} // got details and columns and data fields							
  	} // got data
  	if (details && details.dataStorageType) saveGridDataStoreData(id, details, data);
  		
  	// if we vertical scrolling
  	if (details && details.scrollV && details.scrollHeight) {
  	
  		// if we have a fixed header
  		if (details.fixedHeader) {
  		
  			// if we haven't applied it yet
  			if (!control.parent().is(".gridScrollBody")) {
  		
  			    // create and style the wrapper
  			    var oTblDiv = $("<div class='gridScrollBody' />");
  			    oTblDiv.css({
  			    	"max-height": details.scrollHeight,
  			    	"overflow-y": details.scrollV
  			    });
  			    // wrap it
  			    control.wrap(oTblDiv);
  
  			    // save original column widths			    			    
  			    control.find('thead tr td').each(function(){
  			        $(this).attr("data-item-original-width",$(this).width());
  			    }); 
  			    control.find('tbody tr:eq(0) td').each(function(){
  			        $(this).attr("data-item-original-width",$(this).width());
  			    });                 
  			
  			    // clone the original table
  			    var newTbl = control.clone();
  			    // update it's id
  			    newTbl.attr("id",id + "header");
  			    				
  				// set the class on the parent on the new table (this is the control root)
  			    newTbl.parent().addClass("gridScroll");	      
  				// get the old table parent
  			    var parent = control.parent();
  				// add the new table
  			    parent.parent().prepend(newTbl);			    
  			    // move the old table parent after the new table
  			    parent.insertAfter(newTbl);
  			    // wrap the new table
  			    newTbl.wrap("<div class='gridScrollHeader' />")
  			    				    			
  			    // replace original column widths
  			    newTbl.find('thead tr td').each(function(){
  			        $(this).width($(this).attr("data-item-original-width"));
  			    });     
  			    control.width(control.attr('data-item-original-width'));      
  			    control.find('tbody tr:eq(0) td').each(function(){
  			        $(this).width($(this).attr("data-item-original-width"));
  			    });
  			    
  			    // get the header hight
  			    var headerHight = newTbl.find("tr:first-child").height();
  			    // set height of the header table wrapper
  			    newTbl.parent().css({"height":headerHight});
  			    // shift the new table up behind the fixed header
  			    control.css("margin-top",-headerHight);
  			    
  			} // fixed header applied
  		    
  		} else {
  		
  			// check not applied already
  			if (!control.parent().is(".gridScrollApplied")) {
  
  				// remove the table id
  				control.removeAttr("id")
  				// create and style the wrapper
  			    var oTblDiv = $("<div class='gridScroll' id='" + id + "' />");
  			    oTblDiv.css({
  			    	"height": details.scrollHeight,
  			    	"overflow-y": details.scrollV
  			    });
  			    // retain that we have applied the scroll
  			    oTblDiv.addClass("gridScrollApplied");
  			    // wrap it
  			    control.wrap(oTblDiv);
  			    
  			} // v scroll applied check
  		
  		} // fixed header
  				
  	} // vertical scrolling
  	
  	// if we click on a row that isn't the first one
  	control.children().last().children("tr:not(:first)").click( function() {
  		// get the row 
  		var row = $(this);
  		// remove rowSelect class from all rows in the table
  		row.parent().find("tr.rowSelect").removeClass("rowSelect");
  		// add rowSelect to this row
  		row.addClass("rowSelect"); 
  	});
  	
  	// apply the resizing	
  	$(window).resize(); 
  	
  }
}

function getProperty_grid_selectedRowData(ev, id, field, details) {
  var data = null;
  if (details.dataStorageType) {
  	data = getGridDataStoreData(id, details);	
  	var grid = $("#" + id);
  	if (grid[0]) {
  		var rowNumber = getGridSelectedRowNumber(id, ev);
  		if (rowNumber != data.selectedRowNumber) {
  			data.selectedRowNumber = rowNumber;
  			saveGridDataStoreData(id, details, data);
  		}
  	}
  	if (data.selectedRowNumber) {
  		data.rows = [data.rows[data.selectedRowNumber - 1]];
  	} else {
  		data.rows = [];
  	}
  } else {
  	var row = getGridSelectedRow(id, ev);
  	if (row[0]) {
  		data = {fields:[],rows:[[]]};
  		for (var i in details.columns) {
  			data.fields.push(details.columns[i].field);
  			data.rows[0].push(row.children(":nth(" + i + ")").html());
  		}
  	}
  }
  if (field && data && data.fields && data.rows && data.rows.length > 0) {
  	var found = false;
  	for (var i in data.fields) {
  		if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  			data = data.rows[0][i];
  			found = true;
  			break;
  		}
  	}
  	if (!found) data = null;
  }
  return data;
}

function setProperty_grid_selectedRowData(ev, id, field, details, data, changeEvents) {
  gridData = getGridDataStoreData(id, details);
  if (!gridData) gridData = {};
  var grid = $("#" + id);
  if (grid[0]) {
  		var rowNumber = getGridSelectedRowNumber(id, ev);
  		if (rowNumber != gridData.selectedRowNumber) {
  			gridData.selectedRowNumber = rowNumber;
  			saveGridDataStoreData(id, details, gridData);
  		}
  	}
  data = makeDataObject(data, field);
  if (gridData.selectedRowNumber && gridData.selectedRowNumber > 0) {
  	// replace or remove selected row	
  	if (data && data.fields && data.rows && data.rows.length > 0) {
  		// get the gridData fields
  		var gridDataFields = gridData.fields;
  		// check there are some
  		if (gridDataFields) {
  			
  			// set up a field map which will hold the location of the incoming fields in the grid fields
  			var fieldMap = {};
  			// loop the incoming fields
  			for (var i in data.fields) {
  				// get it's name
  				var field = data.fields[i];
  				// assume it wasn't found in the grid's list of fields	
  				var foundField = false;							
  				// loop the grid's fields
  				for (var j in gridData.fields) {
  					// if there's a match					
  					if (field.toLowerCase() == gridData.fields[j].toLowerCase()) {
  						// store it's position
  						fieldMap[field] = j;
  						foundField = true;
  						break;
  					}
  				}
  				// if the field wasn't found in the grid
  				if (!foundField) {
  					// add it to the grid's fields
  					gridData.fields.push(field);
  					// retain the position
  					fieldMap[field] = gridData.fields.length - 1;
  				}				
  			}			
  			// loop the incoming fields one more time
  			for (var i in data.fields) {
  				// add an entry at this position if we need one
  				if (gridData.rows[gridData.selectedRowNumber - 1].length < data.fields.length) gridData.rows[gridData.selectedRowNumber - 1].push(null);
  				// update the selected row field from the map
  				gridData.rows[gridData.selectedRowNumber - 1][fieldMap[data.fields[i]]] = data.rows[0][i];
  			}			
  		} else {
  			// take them from the incoming data object
  			gridData.fields = data.fields;
  			// put top row of what we were passed into selected row
  			gridData.rows[gridData.selectedRowNumber - 1] = data.rows[0];
  		}		
  	} else {
  		// remove the selected row 
  		gridData.rows.splice(gridData.selectedRowNumber - 1, 1);
  		// remove the validation entry if present
  		if (gridData.rowValidation) gridData.rowValidation.splice(gridData.selectedRowNumber - 1, 1); 
  		// remove the selection
  		gridData.selectedRowNumber = null;
  	}
  	// if there is a datastore to save the data to
  	if (details && details.dataStorageType) {
  		// if the grid is visible in the page
  		if ($("#" + id)[0]) {
  	  		// update the grid with the gridData (which will save as well)
  	  		setData_grid(ev, id, field, details, gridData, changeEvents);
  	  	} else {
  	  		// save the grid
  	  		saveGridDataStoreData(id, details, gridData);
  	  	}
  	}
  } else {
  	// append if no selected row
  	if (data) {
  		// create a rows array if not present
  		if (!gridData.rows) gridData.rows = [];
  		// look for existing fields
  		if (gridData.fields) {
  		
  			// set up a field map which will hold the location of the incoming fields in the grid fields
  			var fieldMap = {};
  			// loop the incoming fields
  			for (var i in data.fields) {
  				// get it's name
  				var field = data.fields[i];
  				// assume it wasn't found in the grid's list of fields	
  				var foundField = false;							
  				// loop the grid's fields
  				for (var j in gridData.fields) {
  					// if there's a match					
  					if (field.toLowerCase() == gridData.fields[j].toLowerCase()) {
  						// store it's position
  						fieldMap[field] = j;
  						foundField = true;
  						break;
  					}
  				}
  				// if the field wasn't found in the grid
  				if (!foundField) {
  					// add it to the grid's fields
  					gridData.fields.push(field);
  					// retain the position
  					fieldMap[field] = gridData.fields.length - 1;
  				}				
  			}
  			
  			// make a row
  			var row = [];
  			// add the cells
  			while (row.length < gridData.fields.length) row.push(null);
  			// loop the data fields and use the map to get the correct position
  			for (var i in data.fields) {
  				// get the field
  				var field = data.fields[i];
  				// get the position
  				var pos = fieldMap[field];
  				// add it to the correct place in the row
  				row[pos] = data.rows[0][i];
  			}
  			// add the row to the gridData
  			gridData.rows.push(row);
  		
  		} else {
  			
  			// assume the incoming fields		
  			gridData.fields = data.fields;
  			// add the top row of what we were given
  			gridData.rows.push(data.rows[0]);
  		
  		}
  		
  		// set the selected row number
  		gridData.selectedRowNumber = gridData.rows.length;
  		// save the grid
  		saveGridDataStoreData(id, details, gridData);
  		// if the grid is visible, refresh it
  		if (grid[0]) setData_grid(ev, id, null, details, gridData, changeEvents);
  	}	
  }
}

function getProperty_grid_selectedRowValidation(ev, id, field, details) {
  if (details && details.dataStorageType) {
  	var gridData = getGridDataStoreData(id, details);
  	if (gridData.rowValidation && gridData.selectedRowNumber) {
  		return gridData.rowValidation[gridData.selectedRowNumber - 1];
  	} 
  } else {
  	return getGridSelectedRowNumber(id, ev);
  }
}

function setProperty_grid_selectedRowValidation(ev, id, field, details, data, changeEvents) {
  var data = makeDataObject(data, field);
  var selectedRowNumber = null;
  
  // look for the grid on this page
  var grid = $("#" + id);
  // if it's present
  if (grid[0]) {
  	// get the selected row number
  	selectedRowNumber = getGridSelectedRowNumber(id, ev);
  	// check the incoming data for a positive
  	if (data[0][0]) {
  		// remove validation class from row if true
  		grid.find("tr.rowSelect").removeClass("validation");
  	} else {
  		// add validation class to row if false
  		grid.find("tr.rowSelect").addClass("validation");
  	}
  }
  	            	   	            
  if (details && details.dataStorageType) {
  	// get the grid data
  	gridData = getGridDataStoreData(id, details);
  	// make an object if there isn't one
  	if (!gridData) gridData = {};
  	// if we got a row number from the grid put it into the grid now
  	if (selectedRowNumber) gridData.selectedRowNumber = selectedRowNumber;
  	// if there are rows and one is selected
  	if (gridData.rows && gridData.selectedRowNumber) {
  		// add a rowValidation collection if there isn't one
  		if (!gridData.rowValidation) gridData.rowValidation = [];
  		// ensure the rowValidation collection is the same size as the row collection
  		while (gridData.rows.length > gridData.rowValidation) gridData.rowValidation.push(true);
  		// set the validation
  		gridData.rowValidation[gridData.selectedRowNumber - 1] = data.rows[0][0];
  		// save the grid
  		saveGridDataStoreData(id, details, gridData);
  	}
  }
}

function getProperty_grid_selectedRowNumber(ev, id, field, details) {
  if (details && details.dataStorageType) {
  	var gridData = getGridDataStoreData(id, details);
  	if (gridData && gridData.selectedRowNumber) {
  		return gridData.selectedRowNumber;
  	} else {
  		return null;
  	}
  } else {
  	return getGridSelectedRowNumber(id, ev);
  }
}

function setProperty_grid_selectedRowNumber(ev, id, field, details, data, changeEvents) {
  gridData = getGridDataStoreData(id, details);
  if (!gridData) gridData = {fields:[],rows:[]};
  data = makeDataObject(data, field);
  var selectedRowNumber = null;
  var grid = $("#" + id);
  if (grid) grid.find("tr.rowSelect").removeClass("rowSelect");
  if (data) {
  	selectedRowNumber = data.rows[0][0];
  	if (grid) grid.find("tr:eq(" + selectedRowNumber + ")").addClass("rowSelect");
  }
  gridData.selectedRowNumber = selectedRowNumber;	
  saveGridDataStoreData(id, details, gridData);
}

function getProperty_grid_rowCount(ev, id, field, details) {
  if (details && details.dataStorageType) {
  	var gridData = getGridDataStoreData(id, details);
  	if (gridData && gridData.rows) {
  		return gridData.rows.length;
  	} else {
  		return 0;
  	}
  } else {
  	return $("#" + id).find("tr").size() - 1;
  }
}

function getData_input(ev, id, field, details) {
  return $("#" + id).val();
}

function setData_input(ev, id, field, details, data, changeEvents) {
  var control = $("#" + id);
  var value = "";
  if (data != null && data !== undefined) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields && data.fields.length > 0) {
  			for (var i in data.fields) {
  				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];
  					break;
  				}
  			}
  		} else {
  			if (data.rows[0][0] != null && data.rows[0][0] !== undefined) {
  				value = data.rows[0][0];
  			} 			
  		}
  	} 
  } 
  control.val(value);
  if (control.is(".autoheight")) textarea_autoheight_size(control, true);
  if (changeEvents) control.trigger("change");
}

function setData_link(ev, id, field, details, data, changeEvents) {
  var control = $("#" + id);
  var value = "";
  if (data != null && data !== undefined) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields && data.fields.length > 0) {
  			for (var i in data.fields) {
  				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];
  					break;
  				}
  			}
  		} else {
  			if (data.rows[0][0] != null && data.rows[0][0] !== undefined) {
  				value = data.rows[0][0];
  			} 			
  		}
  	} 
  } 
  control.attr("href",value);
}

function getProperty_map_mapCentre(ev, id, field, details) {
  // make the data object
  var data = null;
  // get the map
  var map = _maps[id];
  // if there was a map
  if (map) {
  	// make a data object
  	data = {fields:["lat","latitude","lng","longitude"],rows:[[]]};
  	// get the centre
  	var centre = map.getCenter();
  	// get lat
  	var lat = centre.lat();
  	// get lng
  	var lng = centre.lng();
  	// add lat to the data object
  	data.rows[0].push(lat);
  	// add latitude to the data object
  	data.rows[0].push(lat);
  	// add lng to the data object
  	data.rows[0].push(lng);
  	// add longitude to the data object
  	data.rows[0].push(lng);
  	// make a new easting / northing
  	var pIn = new GT_WGS84();
  	// add lat/lng
  	pIn.setDegrees(lat,lng);
  	// if in Great Britiain
  	if (pIn.isGreatBritain()) {
  		// get the easting / northing
  		var pOut = pIn.getOSGB();
  		// add east / north fields
  		data.fields.push("east");
  		data.fields.push("north");
  		// set the east
  		data.rows[0].push(pOut.eastings);
  		// set the north
  		data.rows[0].push(pOut.northings);
  	}
  	// if there was a field
  	if (field) {
  		var fieldIndex = -1;
  		for (var i in data.fields) {
  			if (data.fields[i] && field.toLowerCase() == data.fields[i].toLowerCase()) {
  				return data.rows[0][i];
  			}
  		}	
  		return null;
  	} else {
  		return data;
  	}
  }
  // return what we got
  return data;
}

function setProperty_map_mapCentre(ev, id, field, details, data, changeEvents) {
  // get the map
  var map = _maps[id];
  // get the data object
  var data = makeDataObject(data, field);
  // get the position for the first row
  var pos = getMapPosition(data, 0, setMapCentre, map);
  // if we got a map and data
  if (map && pos && pos.lat && pos.lng) {
  	setMapCentre(map, pos);
  }
}

function setProperty_map_addMarkers(ev, id, field, details, data, changeEvents) {
  // get the map
  var map = _maps[id];
  // get the data object
  var data = makeDataObject(data, field);
  // if we got a map and data
  if (map && data && data.rows && data.rows.length > 0) {
  	// create a markers array if there isn't one
  	if (!map.markers) map.markers = [];
  	// add the markers
  	addMapMarkers(map, data, details);
  }
}

function setProperty_map_replaceMarkers(ev, id, field, details, data, changeEvents) {
  // get the map
  var map = _maps[id];
  // redraw the map in case it was hidden (this will mean the centre hasn't been set correctly but the zoomExtents for the markers will fix this)
  google.maps.event.trigger(map, "resize");
  // get the data object
  var data = makeDataObject(data, field);
  // if there are any current markers
  if (map.markers) {
  	// loop them
  	for (var i in map.markers) {
  		map.markers[i].setMap(null);
  	}
  } 
  // empty markers array
  map.markers = [];
  // if we got a map and data
  if (map && data && data.rows && data.rows.length > 0) {		
  	// add the markers, and zoom their extents and centre the map once all markers are resolved
  	addMapMarkers(map, data, details, data.rows.length - 1);
  }
}

function getProperty_map_selectedMarker(ev, id, field, details) {
  // get the map
  var map = _maps[id];
  // get the selectedIndex
  var selectedIndex = map.markerSelectedIndex;
  // if we got a map and data
  if (map && selectedIndex > -1 && map.markers.length > selectedIndex) {
  	var marker = map.markers[selectedIndex];
  	var data = marker.data;
  	if (field) {
  		var fieldIndex = -1;
  		for (var i in data.fields) {
  			if (data.fields[i] && field.toLowerCase() == data.fields[i].toLowerCase()) {
  				return data.rows[0][i];
  			}
  		}	
  		return null;
  	} else {
  		return data;
  	}	
  } else {
  	return null;
  }
}

function getData_responsivecheckbox(ev, id, field, details) {
  var control = $("#" + id);
  var input = control.find("input:not([type=hidden])");
  var value = null;
  if (ev.validation) {
  	var container = control.closest(".validated");
  	if (!container[0]) container = control.parent();
  	container.find("input:not([type=hidden])").each( function() {
  		if ($(this).prop("checked")) {
  			value = "true";
  		}
  	});
  } else {
  	var attr = input.attr("value");
  	if (attr) {
  		if (input.prop("checked")) value = attr;
  	} else {
  		if (input.prop("checked")) {
  			value = "true";
  		} else {
  			value = "false";
  		}
  	}
  }
  return value;
}

function setData_responsivecheckbox(ev, id, field, details, data, changeEvents) {
  var value = false;
  if (data != null && data !== undefined) {
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];
  					break;
  				}
  			}
  		} else {
  			value = data.rows[0][0];
  		}
  	}
  }
  var control = $("#" + id);
  if (!control.is("input")) control = control.find("#" + id + "checkbox");
  var attr = control.attr("value");
  var checked = false;
  if (attr) {
  	if (value == attr || (value == true && attr == "true")) checked = true;
  } else {
  	if (value && !value == "false") checked = true;
  }
  control.prop('checked', checked);
  if (changeEvents) control.trigger("change");
}

function getData_responsivedate(ev, id, field, details) {
  return $("#" + id).find("input").val();
}

function setData_responsivedate(ev, id, field, details, data, changeEvents) {
  var value = "";
  if (data != null && data !== undefined) {
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {
  		if (field && data.fields && data.fields.length > 0) {
  			for (var i in data.fields) {
  				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];
  					break;
  				}
  			}
  		} else {
  			if (data.rows[0][0] != null && data.rows[0][0] !== undefined) {
  				value = data.rows[0][0];
  			}
  		}
  	}
  }
  // get control container
  var control = $("#" + id);
  // trigger the change here
  if (changeEvents) control.trigger("change");
  // update control to input
  control = control.find("input");
  
  // record on the details whether we want change events triggered - this is because we are about to update and the month/year change handler will read this
  details.dontTriggerChange = !changeEvents;
  
  // look for simple, unambiguous date formats
  try {
  	// retain current value
  	var d = value;
  	// if this looks like an ISO date format
  	if (date_quickISORegex.test(value)) {
  		// parse date from ISO format
  		d = $.datepicker.parseDate("yy-mm-dd", value.substr(0, 10));
  	// if this looks like an Oracle date format
  	} else if (date_quickOracleRegex.test(value)) {
  		// parse date from Oracle format
  		d = $.datepicker.parseDate("dd-M-yy", value.substr(0, 11));
  	}
  	// set the date into the control
  	control.datepicker("setDate", d);
  } catch (ex) {
  	control.datepicker("setDate",value);
  }
  
  // set this back again so default behaviour resumes
  details.dontTriggerChange = false;
}

function getProperty_responsivedate_age(ev, id, field, details) {
  var dp = $("#" + id).find("input");
  if (dp.val()) {
  	var d = dp.datepicker( "getDate" );
  	var age = (Date.now() - d) / 31557600000;
  	return age;
  } else {
  	return null;
  }
}

function getData_responsivedropdown(ev, id, field, details) {
  return $("#" + id).find("select").val();
}

function setData_responsivedropdown(ev, id, field, details, data, changeEvents) {
  if (data !== undefined) {
  	var control = $("#" + id).find("select");
  	if (data == null) {
  		if (!field) control.children().remove();
  	} else {
  		data = makeDataObject(data, field);
  		if (data.rows && data.fields) {
  			if (field && data.rows[0]) {
  				var foundField = false;
  				for (var i in data.fields) {
  					if (data.fields[i] && field.toLowerCase() == data.fields[i].toLowerCase()) {
  						control.val(data.rows[0][i]);
  						if (changeEvents) control.trigger("change");
  						foundField = true;
  						break;
  					}
  				}
  				if (!foundField) control.val(data.rows[0][0]);
  			} else {
  				for (var i in data.rows) {
  					var row = data.rows[i];
  					var text = "";
  					var value = null;
  					if (data.fields) {
  						for (var j in data.fields) {
  							if (data.fields[j]) {
  								if (data.fields[j].toLowerCase() == "text") text = data.rows[i][j];
  								if (data.fields[j].toLowerCase() == "value") value = data.rows[i][j];
  							}
  						}
  					}
  					if (!text) text = row[0];
  					if (value == null && row[1]) value = row[1];
  					if (value != null) value = "value='" + value + "'";
  					control.append("<option " + value + ">" + text + "</option>");
  				}
  			}
  		}
  	}
  }
}

function setProperty_responsivedropdown_value(ev, id, field, details, data, changeEvents) {
  var	data = makeDataObject(data, field);
  var control = $("#" + id).find("select");
  if (data && data.rows) {
  	var foundField = false;
  	if (field) {
  		for (var i in data.fields) {
  			if (data.fields[i] && field.toLowerCase() == data.fields[i].toLowerCase()) {
  				control.val(data.rows[0][i]);
  				foundField = true;
  				break;
  			}
  		}
  	}
  	if (!foundField) control.val(data.rows[0][0]);
  } else {
  	control.val("");
  }
  if (changeEvents) control.trigger("change");
}

function getData_responsiveinput(ev, id, field, details) {
  return $("#" + id).find("input,textarea").val();
}

function setData_responsiveinput(ev, id, field, details, data, changeEvents) {
  var value = "";
  if (data != null && data !== undefined) {
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {
  		if (field && data.fields && data.fields.length > 0) {
  			for (var i in data.fields) {
  				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];
  					break;
  				}
  			}
  		} else {
  			if (data.rows[0][0] != null && data.rows[0][0] !== undefined) {
  				value = data.rows[0][0];
  			}
  		}
  	}
  }
  // get control container
  var control = $("#" + id);
  if (changeEvents) control.trigger("change");
  // update control to input
  control = control.find("input,textarea");
  control.val(value);
  if (control.is(".autoheight")) textarea_autoheight_size(control, true);
}

function getData_responsiveradiobuttons(ev, id, field, details) {
  return $("#" + id).find("input[type=radio]:checked").val();
}

function setData_responsiveradiobuttons(ev, id, field, details, data, changeEvents) {
  if (data != null && data !== undefined) {
  	var radiobuttons = $("#" + id);
  	radiobuttons.find("input[type=radio]").prop('checked',false);
  	data = makeDataObject(data, field);
  	var value = null;
  	if (data.rows && data.rows[0]) {
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];
  					break;
  				}
  			}
  		} else {
  			value = data.rows[0][0];
  		}
  	}
  	if (value !== undefined) {
  		var button = radiobuttons.find("input[type=radio][value='" + value + "']");
  		if (button[0]) {
  			button.prop('checked',true);
  		} else {
  			radiobuttons.find("input[type=radio]").prop('checked',false);
  		}
  		if (changeEvents) button.trigger("change");
  	}
  }
}

function getData_responsivetext(ev, id, field, details) {
  // get the control	        
  var control = $("#" + id);
  // if the control is not the text itself find the element we want
  if (!control.is("[data-for=text]")) control = control.find("[data-for=text]");
  // get the value
  var val = control.html();
  // update to empty if &nbsp; (from set)
  if (val == "&nbsp;") val = "";
  // return
  return val;
}

function setData_responsivetext(ev, id, field, details, data, changeEvents) {
  // get the control	        
  var control = $("#" + id);
  // assume no value
  var value = "";
  // get the value from the data
  if (data != null && data !== undefined) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];
  					break;
  				}
  			}
  		} else {
  			value = data.rows[0][0];
  		}
  	}
  }
  // if the control is not the text itself find the element we want
  if (!control.is("[data-for=text]")) control = control.find("[data-for=text]");
  // if no value make nbsp
  if (!value) value = "&nbsp;";
  // set value
  control.html(value);
}

function getData_signature(ev, id, field, details) {
  var signature = _signatureControls[id];
  return signature.toDataURL();
}

function setData_signature(ev, id, field, details, data, changeEvents) {
  var signature = _signatureControls[id];
  var data = makeDataObject(data, field);
  if (data && data.rows) {
  	signature.fromDataURL(data.rows[0][0]);
  } else {
  	signature.clear();
  }
}

function getData_text(ev, id, field, details) {
  return $("#" + id).html();
}

function setData_text(ev, id, field, details, data, changeEvents) {
  var control = $("#" + id);	        
  if (data != null && data !== undefined) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i] && data.fields[i].toLowerCase() == field.toLowerCase()) {
  					control.html(data.rows[0][i]);
  					break;
  				}
  			}
  		} else {
  			control.html(data.rows[0][0]);
  		}
  	} else {
  		control.html("");
  	}
  } else {
  	control.html("");
  }
}

function getProperty_flowLayoutCell_cellIndex(ev, id, field, details) {
  var selectedCell = $("#" + id).children("div.selected");
  if (selectedCell[0]) {
  	return selectedCell.index();
  } else {		        
  	return -1;
  }
}


/* Action methods */


function Action_control(actions) {
	for (var i in actions) {
		var action = actions[i];
		$("#" + action.id)[action["function"]](action.value);
	}	
}

function Action_datacopy(ev, data, outputs, changeEvents, copyType, copyData, field, details) {
	if (data !== undefined && outputs) {
		for (var i in outputs) {
			var output = outputs[i];
			var outputData = null;
			switch (copyType) {
				case "append" :	case "row" :
					var mergeData = window["getData_" + output.type](ev, output.id, null, output.details);
					data = makeDataObject(data, output.field);
					outputData = mergeDataObjects(mergeData, data, copyType, field);
				break;
				case "child" :
					var mergeData = window["getData_" + output.type](ev, output.id, null, output.details);	
					if (data && data.rows) {
						outputData = mergeDataObjects(mergeData, data, copyType, field);
					} else {
						outputData = mergeData;
					}
				break;
				case "search" :
					if (data) outputData = mergeDataObjects(copyData, data, copyType, field, details);
				break;
				case "trans" :
					if (data && data.fields && data.rows) {
												
						var downtoside = false;
						// checking for down to side transpose is quite expensive so get the easy stuff out the way first
						if (data.rows.length >= details.keyFields.length + 2) {
							var keyFieldsMap = {};
							for (var i in data.fields) {
								for (var j in details.keyFields) {
									if (data.fields[i] && details.keyFields[j] && data.fields[i].toLowerCase() == details.keyFields[j].toLowerCase()) {
										keyFieldsMap[i] = j;
										break;
									}
								}
							}
							if (Object.keys(keyFieldsMap).length == details.keyFields.length && data.fields.length >= details.keyFields.length + 2) {
								var keyValueCounts = {};
								for (var i in data.rows) {
									var keyValue = "";
									for (var j in keyFieldsMap) {
										keyValue += data.rows[i][keyFieldsMap[j]];
									}
									if (keyValueCounts[keyValue]) {
										keyValueCounts[keyValue] ++;
									} else {
										keyValueCounts[keyValue] = 1;
									}
									if (keyValueCounts[keyValue] > 1) {
										downtoside = true;
										break;
									}
								}
							}						
						}
												
						if (downtoside) {
							// transpose down to side
							var fieldPos = 0;
							var valuePos = 1;
							// check the map for whether the key fields are before or after the field/value pair
							for (i in keyFieldsMap) {
								// if any of the key fields are in the first column
								if (i*1 == 0) {
									// change field and value columns from 1st and 2nd to those following the keys
									fieldPos = details.keyFields.length;
									valuePos = details.keyFields.length + 1;									
								}
							}
							var outputFields = [];
							for (var i in details.keyFields) outputFields.push(details.keyFields[i]);
							var keyValueRows = {};
							for (var i in data.rows) {
								var row = data.rows[i];
								var field = row[fieldPos];
								var value = row[valuePos];
								// determin the key value for this row
								var keyValue = "";
								for (var j in keyFieldsMap) {
									keyValue += row[j];
								}
								// fetch the row we are creating for this key
								var keyRow = keyValueRows[keyValue];
								if (!keyRow) {
									keyRow = [];
									for (var j in keyFieldsMap) keyRow.push(row[j]);
									keyValueRows[keyValue] = keyRow;
								}
								// if there is a field (can be null on an outer join)
								if (field) {
									// find the position of this field in the output fields
									var outputFieldPos = -1;
									for (var j in outputFields) {
										if (outputFields[j] == field) {
											outputFieldPos = j;
											break;
										}
									}
									// if the field couldn't be found
									if (outputFieldPos < 0) {
										outputFields.push(field);
										outputFieldPos = outputFields.length - 1;
									}
								}
								// ensure the row is the right length as we discover more fields								
								while (keyRow.length < outputFields.length) keyRow.push(null);
								// set the value in the correct position
								keyRow[outputFieldPos] = value;								
							}
							// create the output data object
							outputData = {fields:outputFields,rows:[]};
							// loop the unique rows		
							for (var i in keyValueRows) {
								// get the row
								var row = keyValueRows[i];
								// ensure the row is the right length
								while (row.length < outputFields.length) row.push(null);
								// add the row
								outputData.rows.push(row);
							}
						} else {
							// transpose side to down				
							outputData = {fields:["field","value"],rows:[]};						
							if (details && details.keyFields && details.keyFields.length > 0) {
								for (var j in details.keyFields) {
									outputData.fields.push(details.keyFields[j]);
								}
							}						
							for (var i in data.rows) {						
								var keyFieldValues = [];						
								// key fields
								if (details && details.keyFields && details.keyFields.length > 0) {
									for (var j in details.keyFields) {
										for (var k in data.fields) {
											if (data.fields[k] && details.keyFields[j].toLowerCase() == data.fields[k].toLowerCase()) {
												keyFieldValues.push(data.rows[i][k]);
												break;
											}
										}
									}								
								}
																																		
								// values
								for (var j in data.fields) {
									var ignore = false;
									if (details && details.ignoreFields && details.ignoreFields.length > 0) {
										for (var k in details.ignoreFields) {
											if (!data.fields[j] || data.fields[j].toLowerCase() == details.ignoreFields[k].toLowerCase()) {
												ignore = true;
												break;
											}
										}
									}
									if (!ignore) {
										var row = [data.fields[j],data.rows[i][j]];
										for (var k in keyFieldValues) row.push(keyFieldValues[k]);
										outputData.rows.push(row);
									}
								}																											
							}
						}
					}
				break;
				default:
					outputData = data;
			}	
			window["setData_" + output.type](ev, output.id, output.field, output.details, outputData, changeEvents);
		}
	}
}

function Action_database(ev, actionId, data, outputs, childField) {
	// check we got data and somewhere to put it
	if (data && outputs) {		
		// if there is a child field
		if (childField && data.fields) {
			// loop fields
			for (var i in data.fields) {
				// look for a field in the parent matching the childField
				if (data.fields[i] && data.fields[i].toLowerCase() == childField.toLowerCase()) {
					// promote the data from child
					data = data.rows[0][i];
					// we're done
					break;
				}
			}
		}
		// check we have data
		if (data) {
			// check the returned sequence is higher than any others received so far
			if (!data.sequence || data.sequence > getDatabaseActionMaxSequence(actionId)) {
				// retain this sequence as the new highest
				if (data.sequence) _databaseActionMaxSequence[actionId] = data.sequence;
				for (var i in outputs) {
					var output = outputs[i];			
					window["setData_" + output.type](ev, output.id, output.field, output.details, data);
				}
			}
		}
	}
}

function Action_mobile(actionId, type) {
	// action callback
	alert("Callback for " + actionId + "." + type);
}

//JQuery is ready! 
$(document).ready( function() {
	
	$(window).resize(function(ex) {
	
		var doc = $(document);
		var win = $(window);
				
		// resize the cover
		$(".dialogueCover").css({
       		width : doc.width(),
       		height : doc.height()
       	});
       	      	
       	// resize the dialogues
       	$(".dialogue").each(function() {
       		var dialogue = $(this);
       		var w = dialogue.outerWidth(true);
       		var h = dialogue.outerHeight(true);
       		if (w > 0 && h > 0) {
		       	dialogue.css({
		       		left : ((window.innerWidth ? window.innerWidth : win.width()) - w) / 2,
		       		top : ((window.innerHeight ? window.innerHeight : win.height()) - h) / 2
		       	}); 
		    }
	    });
	
	});
	
});		        
	        
function Action_navigate(url, dialogue, id) {

	if (dialogue) {
	
		var bodyHtml = "<div><h1 style='margin-left:auto;margin-right:auto;'>Page</h1></div>";
		
		// request the page		
		$.ajax({
		   	url: url,
		   	type: "GET",          
		       data: null,        
		       async: false,
		       error: function(error, status, message) { 
		       		alert("Error loading dialogue : " + error.responseText||message); 
		       },
		       success: function(page) {
		       	
		       // if the page can't be found a blank response is sent, so only show if we got something
		       if (page) {
		       	
		       		// empty the body html
		       		bodyHtml = "";
		       		script = "";
		       		links = "";
		       		
		       		// get the page document head
		       		var head = $("head");
		       		
		           	// loop the items
		           	var items = $(page);
		           	for (var i in items) {
		           		// check for a script node
		           		switch (items[i].nodeName) {
		           		case "#text" : case "TITLE" : // ignore these types
		           		break;
		           		case "SCRIPT" :
		           			// check whether a script block or link
		           			if (items[i].innerHTML) {
		           				var s = items[i].outerHTML;
		           				// exclude the app id, version, page, mobile resume link
		           				if (s.indexOf("var _appId =") == -1 && s.indexOf("var _appVersion =") == -1 && s.indexOf("var _pageId =") == -1 && s.indexOf("var _mobileResume =") == -1)
		           					script += s;
		           			} else {
		           				// fetch the text
		           				var text = items[i].outerHTML;
		           				// look for a src
		           				if (text.indexOf("src=\"") > 0) {
		           					var startPos = text.indexOf("src=\"")+5;
		           					var src = text.substr(startPos,text.indexOf("\"", startPos) - startPos);		           					
		           					// add this to the page if not there already
		           					if (!head.find("script[src='" + src + "']")[0]) {
		           						// append to the head section of the page and fail silently if any issue - for some reason Rapid Mobile throws a TypeError: undefined is not a function for calendar.js
		           						try { head.append(text); } catch(ex) {}
		           					}
		           				}
		           			}          			
		           		break;
		           		case "LINK" :
		           			// fetch the text
		           			var text = items[i].outerHTML;	           			
		           			// look for an href="
		           			if (!items[i].innerHTML && text.indexOf("href=\"") > 0) {
		           				var startPos = text.indexOf("href=\"")+6;
		           				var href = text.substr(startPos,text.indexOf("\"", startPos) - startPos);
		           				// add this link to the page if not there already
		           				if (!head.find("link[href='" + href + "']")[0]) 
		           					head.append(text);
		           			}		           				           			
		           		break;
		           		case "META" :
		           			// meta tags can be ignored
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
            		if (bodyHtml.indexOf("<form name=\"login\" id=\"RapidLogin\" method=\"post\">") > 0) window.location = "login.jsp?requestPath=" + window.location; 
            	
            		// get a reference to the document for the entire height and width     	
		           	var doc = $(document);
		           	// get a reference to the body		           	
		           	var body = $("body");
		           	
		           	// remove any existing dialogue cover for this action
		           	$("#" + id + "cover").remove();
		           	// add the cover and return reference
		           	dialogueCover = body.append("<div id='" + id + "cover' class='dialogueCover' style='position:absolute;left:0px;top:0px;z-index:1000;'></div>").children().last();
		           			      		           			           		            	
	            	// remove any existing dialogue container for this action
		           	$("#" + id + "dialogue").remove();
		           	// add the dialogue container and remove the reference
		           	dialogue = body.append("<div id='" + id + "dialogue' class='dialogue' style='position:fixed;z-index:1001;'></div>").children().last(); 
		           	
		           	// make sure it's hidden
		           	dialogue.css("visibility","hidden");
		           			           						
					// add any links into the page (if applicable)
		           	if (links) dialogue.append(links);		           			           	
		           	
		           	// append the injected html
		           	dialogue.append(bodyHtml);
		           	
					dialogue.children().each( function(i) {
						// thanks to http://viralpatel.net/blogs/jquery-trigger-custom-event-show-hide-element/	
						var el = $.fn["hide"];
						$.fn["hide"] = function () {							
		           			// if we're hiding an immediate child of the dialogue
		           			if (this.parent().is(dialogue)) {
		           				dialogue.prev("div.dialogueCover").remove();
								dialogue.remove();
		           				return false;
		           			} else {
		           				// trigger the regular hide 								
								return el.apply(this, arguments);
							}
						};
					});
							           	
		           	// add any scripts into the page (if applicable)
		           	if (script) dialogue.append(script);
		           	
		           	// handle textareas with maxlength attribute
					dialogue.find('textarea[maxlength]').each( textarea_maxlength );
  
					// handle textareas with autoheight class
					dialogue.find('textarea.autoheight').each( textarea_autoheight ).trigger('keypress'); 
		           			           	
		           	// apply the resizing	
	            	$(window).resize(); 
	            	
	            	// find all tabbable elements in the body excluding those in the dialogue 
	            	body.find("input, select, textarea, button, a").not(dialogue.find("input, select, textarea, button, a")).each( function() {
	            		// get the element
	            		var e = $(this);
	            		// get any existing tab index
	            		var t = e.attr("tabindex");
	            		// if there was one add a data attribute for it
	            		if (t > 0) e.attr("data-tabindex",t);
	            		// set their tab index to -1
	            		$(this).attr("tabindex", -1);
	            	});
	            	
	            	// this seems to be the best way to avoid the resizing/flicker when showing
	            	window.setTimeout( function() {
	            		// make the dialogue visible
	            		dialogue.css("visibility","visible");
	            		// apply the resizing again (for mobile)	
	            		if (window["_rapidmobile"]) $(window).resize();
	            		// set the focus now that we're visible 
	            		$('[data-focus]').focus();
	            	}, 200);
		           	           	        	            	            	            
		    	}
		    	        	       	        	        	        	        		
		    }       
		    		    		    	        	        
		});	      
	
	} else {
		window.location = url;
	}
		
}

function Action_validation(ev, validations, showMessages) {
	var valid = true;
	ev.validation = true;
	for (var i in validations) {
		var validation = validations[i];
		var validationControl = $("#" + validation.controlId);
		if (validationControl[0]) {
			var getValueFunction = null;
			if (validation.validationProperty) {
				getValueFunction = window["getProperty_" + validation.controlType + "_" + validation.validationProperty];
			} else {
				getValueFunction = window["getData_" + validation.controlType];
			}
			if (getValueFunction) {
				var details = null;
				if (validation.details) {
					if (validation.details.indexOf("{") == 0) {
						details = JSON.parse(details);
					} else {
						details = window[validation.details];
					}
				}
				var value = getValueFunction(ev, validation.controlId, validation.field, details);
				if (validation.validationType == "javascript") {
					var validationFunction = new Function(["ev","id","value"], validation.javaScript);
					var failMessage = validationFunction.apply(this, [ev,validation.controlId,value]);
					if (failMessage != null && failMessage !== false && failMessage !== undefined) {
						if (showMessages) showControlValidation(validation.controlId, failMessage);
						valid = false;
					} else {
						if (showMessages) hideControlValidation(validation.controlId);
					}
				} else {
					// assume no pass
					var pass = false;
					// start with some basic checks
					if (value && value != "false") {
						// get the regex expression
						var exp = validation.regEx.trim();
						// make a regex
						var regex = new RegExp(exp);
						// if the expression ends with /i
						if (exp.indexOf("/i") == exp.length - 2) {
							// update the exp removing the /i
							exp = exp.substr(0, exp.length - 2);
							// remake the regex with the case insensitivity
							regex = new RegExp(exp, "i")
						}
						// get the matches from the reg ex
						var matches = value.match(regex);
						// ensure the first match is for the whole value
						if (matches) {
							// got some matches now groups must match too
							if (matches.length == 1) {
								// these are the easy ones
								pass = true;
							} else {
								// we now need to loop all of the groups
								for (var i in matches) {
									if (i != "index" && i != "input" && matches[i] == value) {
										// we're good
										pass = true;
										break;
									}
								}
							}
						}
					} else if (validation.allowNulls) {
						// no value but this is allowed
						pass = true;
					}
					// pass, disabled, or hidden and allowed this way
					if (pass || validationControl.is(":disabled") || (validationControl.is(":hidden") && validation.passHidden)) {
						// passed
						if (showMessages) hideControlValidation(validation.controlId);
					} else {
						// failed, and there is a message to show, but not on tab keyup
						if (showMessages && !(ev.type == "keyup" && ev.keyCode == 9)) showControlValidation(validation.controlId, validation.message);
						valid = false;
					}
				}
			} else {
				if (showMessages) showControlValidation(validation.controlId, validation.message);
				valid = false;
			}
		}
	}
	ev.validation = false;
	return valid;
}

function Action_webservice(ev, actionId, data, outputs) {
	// only if there are data and outputs
	if (data && outputs) {
		// only if this is the latest sequence
		if (data.sequence > getWebserviceActionMaxSequence(actionId)) {
			// retain this as the lastest sequence
			_webserviceActionMaxSequence[actionId] = data.sequence;
			// loop the outputs
			for (var i in outputs) {
				var output = outputs[i];			
				window["setData_" + output.type](ev, output.id, output.field, output.details, data);
			}
		}
	}
}
