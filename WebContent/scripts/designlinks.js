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

function showDesignData(link) {
	
	$(".designData").hide();
	
	var id = link.attr("data-id");
	var name = link.find("img").attr("title");
	var details = window[id + "details"];
	var div = $("#" + id + "designData");
	if (!div[0]) {
		div = $("body").append("<div id='" + id + "designData' style='display:none;' class='designData'></div>").find("#" + id + "designData");
		div.css({
			"left": link.offset().left
		});
		div.mouseover( function(ev) {  
			_onDesignTable = true;
		});
		div.mouseleave( function(ev) {  
			_onDesignTable = false;
		});
	}
	if (details) {		
		var data = null;
		switch (details.type) {
			case "grid" :
				data = getGridDataStoreData(id, details);
			break;
			case "dataStore" :
				data = getDataStoreData(id, details);
			break;
			default :
				data = $("#" + id).val();
		}
		renderDesignData(name, data, div);
	} else {
		renderDesignData(name, $("#" + id).val(), div);
	}
}

function getDesignDataTable(data) {
	var table = "<table>";
	if (data && data.fields && data.rows) {
		// open the table
		table += "<tr>";
		// loop the fields, adding the field name
		for (var i in data.fields) table += "<td>" + data.fields[i] + "</td>";
		// close the table
		table += "</tr>";
		// loop the rows
		for (var j in data.rows) {
			// open  the row
			table += "<tr>";
			// loop the 
			for (var k = 0; k <= i; k++) {
				// get the contents at this position
				var d = data.rows[j][k];
				// if it's an object with it's own fields and rows, get it's own table!
				if (d && d.fields && d.rows) d = getDesignDataTable(d);
				// add the cell contents
				table += "<td>" +  d + "</td>";
			}
			// close the table			
			table += "</tr>";
		}		
	} else if ($.isEmptyObject(data)) {
			table += "<tr><td>(empty)</td></tr>";
	} else {
		table += "<tr><td>" + data + "</td></tr>";
	}
	table += "</table>";
	// return it
	return table;
}

function renderDesignData(name, data, div) {
	var table = getDesignDataTable(data);
	div.html(name + "<br/>" + table).slideDown(500);
}