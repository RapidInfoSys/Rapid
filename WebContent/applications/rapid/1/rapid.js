
/* This file is auto-generated on application load and save - it is minified when in production */


/* Action methods */


function Action_control(actions) {
	for (var i in actions) {
		var action = actions[i];
		$("#" + action.id)[action["function"]](action.value);
	}	
}

function Action_datacopy(data, outputs) {
	if (data != null && data !== undefined && outputs) {
		for (var i in outputs) {
			var output = outputs[i];			
			window["setData_" + output.type](output.id, data, output.field, output.details);
		}
	}
}

function Action_database(actionId, data, outputs) {
	// check we got data and somewhere to put it
	if (data && outputs) {
		// check the returned sequence is higher than any others received so far
		if (data.sequence > getDatabaseActionMaxSequence(actionId)) {
			// retain this sequence as the new highest
			_databaseActionMaxSequence[actionId] = data.sequence;
			for (var i in outputs) {
				var output = outputs[i];			
				window["setData_" + output.type](output.id, data, output.field, output.details);
			}
		}
	}
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
	       	dialogue.css({
	       		left : (win.width() - dialogue.outerWidth()) / 2,
	       		top : (win.height() - dialogue.outerHeight()) / 3
	       	}); 
	    });
	
	});
	
});		        
	        
function Action_navigate(url, dialogue) {
	if (dialogue) {
	
		var bodyHtml = "<center><h1>Page</h1></center>";
		
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
		       		
		           	// loop the items
		           	var items = $(page);
		           	for (var i in items) {
		           		// check for a script node
		           		switch (items[i].nodeName) {
		           		case "#text" : case "TITLE" : // ignore these types
		           		break;
		           		case "SCRIPT" :
		           			// exclude the design link
		           			if (items[i].innerHTML && items[i].innerHTML.indexOf("/* designLink */") < 0) {
		           				script += items[i].outerHTML;
		           			}		           			
		           		break;
		           		case "LINK" :
		           			// assume we can include this
		           			var include = true;
		           			// fetch the text
		           			var text = items[i].outerHTML;	           			
		           			// look for an href="
		           			if (!items[i].innerHTML && text.indexOf("href=\"") > 0) {
		           				var startPos = text.indexOf("href=\"")+6;
		           				var href = text.substr(startPos,text.indexOf("\"", startPos) - startPos);
		           				// exclude if we already have an element in the head with with href
		           				if ($("head").find("link[href='" + href + "']")[0]) include = false;
		           			}		           			
		           			// if still safe to include
		           			if (include) links += text;		           			
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
            		if (bodyHtml.indexOf("<input type=\"submit\" value=\"log in\">") > 0) window.location = "login.jsp?requestPath=" + window.location; 
            	
		           	// get a reference to the body		           	
		           	var body = $("body");
		           	// add the cover and return reference
		           	var dialogueCover = body.append("<div class='dialogueCover' style='position:absolute;left:0px;top:0px;z-index:100;'></div>").children().last();
		           			      
		           	// get a reference to the document for the entire height and width     	
		           	var doc = $(document);
		           		            	
	            	// find the dialogue container div
		           	var dialogue = body.append("<div class='dialogue' style='position:absolute;z-index:101;'></div>").children().last();
		           	// make sure it's hidden
		           	dialogue.css("visibility","hidden");
					// add any links into the page (if applicable)
		           	if (links) dialogue.append(links);		           			           	
		           	// append the injected html
		           	dialogue.append(bodyHtml);
		           	// add any scripts into the page (if applicable)
		           	if (script) dialogue.append(script);
		           			           	
		           	// get a reference to the window for the visible area
		           	var win = $(window);
		           	
		           	// apply the resizing	
	            	$(window).resize(); 
	            	
	            	// this seems to be the best way to avoid the resizing/flicker when showing
	            	window.setTimeout( function() {
	            		// make the dialogue visible
	            		dialogue.css("visibility","visible");
	            		// apply the resizing again (for mobile)	
	            		if (window["_rapidmobile"]) $(window).resize(); 
	            	}, 200);
		           	           	        	            	            	            
		    	}        	       	        	        	        	        		
		    }       	        	        
		});	      
	
	} else {
		window.location = url;
	}
}

// a global for holding the userName which we get when calling GETAPPS	        
var _userName = "";	        
	        
function Action_rapid(ev, appId, pageId, controlId, actionId, actionType, rapidApp, successCallback, errorCallback) {

	var type = "GET";
	
	var data = null;
	var callback = null;

	// some special types require data and callbacks	
	switch (actionType) {
		case "GETAPPS" :		
			data = { actionType: actionType, appId: "rapid" };	
			callback = function(data) {			
				setData_dataStore('rapid_P0_C210', data, null, {storageType:"S"});									
			};
		break;
		case "GETVERSIONS" :	
			var appId = $("#rapid_P0_C43").val();
			if (appId) {
				data = { actionType: actionType, appId: appId  };	
				callback = function(data) {
					setData_dataStore('rapid_P0_C1063_', data, "versions", {storageType:"S"});
				};
			} else {
				return false;
			}
		break;
		case "GETVERSION" :		
			var appId = $("#rapid_P0_C43").val();
			var version = $("#rapid_P0_C1044_").val();
			if (appId && version) {
				data = { actionType: actionType, appId: appId, version: version };	
				callback = function(data) {
					setData_dataStore('rapid_P0_C1072_', data, null, {storageType:"S"});
				};
			} else {
				return false;
			}
		break;	
		case "GETDBCONN" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), index: $("#rapid_P0_C311").find("tr.rowSelect").index()-1 };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C361', data, "databaseConnection", {storageType:"S"});
			};
		break;
		case "GETSOA" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), index: $("#rapid_P0_C483_").find("tr.rowSelect").index()-1 };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C528_', data, "webservice", {storageType:"S"});
				loadSOA(data.webservice);
			};
		break;
		case "GETSEC" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), securityAdapter: $("#rapid_P0_C81").val() };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C469_', data, "security", {storageType:"S"});
			};
		break;					
		case "GETUSERS" :		
			data = { actionType: actionType, appId: "rapid" };	
			callback = function(data) {			    				
				setData_grid('rapid_P0_C823_', data, 'users', {"rowSelect":true,"columns":[{"field":"name","visible":true,"style":"text-align:left;padding-left:10px;"},{"field":"description","visible":true,"style":"text-align:left;padding-left:10px;padding-right:10px;"},{"cellFunction":"","field":"","visible":true,"style":""}]});
				var rapidUserRows = $("#rapid_P0_C823_").find("tr.rowStyle1,tr.rowStyle2");
				rapidUserRows.each( function() {
				  var children = $(this).children("td");
				  var user = children.first().html();
				  if (data.currentUser.toLowerCase() != user.toLowerCase()) {
					  var cell = children.last();
					  cell.html("<button>delete...</button>");
					  cell.find("button").click( function(ev) {
					    // confirm
					    if (confirm("Are you sure?")) {    
					      Action_rapid(ev, 'rapid', 'P0', null, 'P0_A904_', 'DELUSER', true);					      
					    }
					    // stop bubbling
					    ev.stopPropagation();
					  });
				   }
				});
			};
		break;
		case "GETUSER" :	
			if (rapidApp) {
				data = { actionType: actionType, appId: "rapid", version: _appVersion, userName: getData_grid(ev, "rapid_P0_C823_", "name", {columns:[{field:"name"}]}) };
			} else {
				data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), userName: getData_grid(ev, "rapid_P0_C216", "userName", {columns:[{field:"userName"}]}) };	
			}
			callback = function(data) {
				setData_dataStore('rapid_P0_C243', data, "user", {storageType:"S"});
			};
		break;	
		case "GETPARAM" :	
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), index: $("#rapid_P0_C1108_").find("tr.rowSelect").index()-1 };
			callback = function(data) {
				setData_dataStore('rapid_P0_C1145_', data, "parameter", {storageType:"S"});
			};
		break;
		case "GETDEVICE" :	
			data = { actionType: actionType, appId: "rapid", version: _appVersion, index: $("#rapid_P0_C1199_").find("tr.rowSelect").index()-1 };
			callback = function(data) {
				setData_dataStore('rapid_P0_C1269_', data, "device", {storageType:"S"});
			};
		break;
		case "SAVEAPP" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				status: $("#rapid_P0_C1098_").val(),					
				name: $("#rapid_P0_C392_").val(),
				saveVersion: $("#rapid_P0_C1077_").val(),
				title: $("#rapid_P0_C394_").val(),
				description: $("#rapid_P0_C393_").val(),
				showControlIds: $("#rapid_P0_C381_").prop("checked"),
				showActionIds: $("#rapid_P0_C382_").prop("checked"),
				startPageId: $("#rapid_P0_C644_").val()
			};	
		break;
		case "SAVESTYLES" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), styles: $("#rapid_P0_C116").val() };	
		break;		
		case "SAVEDBCONN" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				index: $("#rapid_P0_C311").find("tr.rowSelect").index()-1,
				name: $("#rapid_P0_C360").val(),
				driver: $("#rapid_P0_C338").val(),
				connectionString: $("#rapid_P0_C374").val(),
				connectionAdapter: $("#rapid_P0_C339").val(),
				userName: $("#rapid_P0_C340").val(),
				password: $("#rapid_P0_C341").val()
			};	
			callback = function() {
				Event_click_rapid_P0_C311({target: $("#rapid_P0_C311").find("tr.rowSelect").children().first()[0] });
			};
		break;
		case "SAVESOASQL" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				index: $("#rapid_P0_C483_").find("tr.rowSelect").index()-1,
				name: $("#rapid_P0_C496_").val(), 
				databaseConnectionIndex: $("#rapid_P0_C536_")[0].selectedIndex,
				details: _soaDetails,
				type: "SQLWebservice"
			};	
		break;
		case "SAVESOAJAVA" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				index: $("#rapid_P0_C483_").find("tr.rowSelect").index()-1,
				name: $("#rapid_P0_C944_").val(), 
				className: $("#rapid_P0_C989_").val(),
				type: "JavaWebservice"
			};	
		break;
		case "SAVESECURITYADAPT" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				securityAdapter: $("#rapid_P0_C81").val()
			};	
		break;
		case "SAVEACTIONS" :
			var actionTypes = [];
			$("#rapid_P0_C288").find("input:checked").each( function(){
				actionTypes.push($(this).closest("tr").children().first().html());
			});		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), actionTypes: actionTypes };	
		break;
		case "SAVECONTROLS" :	
			var controlTypes = [];
			$("#rapid_P0_C289").find("input:checked").each( function(){
				controlTypes.push($(this).closest("tr").children().first().html());
			});		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), controlTypes: controlTypes };	
		break;
		case "REBUILDPAGES" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val() };	
		break;
		case "NEWAPP" :
			data = {
				actionType: actionType,
				appId: "rapid",
				name: $("#rapid_P1_C7").val(),
				version: $("#rapid_P1_C48_").val(),
				title: $("#rapid_P1_C11").val(),
				description: $("#rapid_P1_C15").val()
			}
			callback = function(response) {
				window.location = "~?a=rapid&p=P0&appId=" + response.appId + "&version=" + response.version;
			}; 
		break;
		case "DELAPP" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val() 
			};	
			callback = function() {
				window.location = "~?a=rapid&p=P0";
			}; 			
		break;		
		case "DUPAPP" :		
			data = {
				actionType: actionType,
				appId: $("#rapid_P0_C43").val(),
				version: $("#rapid_P0_C1044_").val(),
				newVersion: $("#rapid_P8_C7_").val(),
				title: $("#rapid_P8_C12_").val(),
				description: $("#rapid_P8_C17_").val()
			}
			callback = function(response) {
				location.reload();
			}; 			
		break;				
		case "NEWVERSION" :		
			data = {
				actionType: actionType,
				appId: $("#rapid_P0_C43").val(),
				version: $("#rapid_P0_C1044_").val(),
				newVersion: $("#rapid_P17_C7_").val(),
				title: $("#rapid_P17_C12_").val(),
				description: $("#rapid_P17_C17_").val()
			}
			callback = function(response) {
				location.reload();
			}; 			
		break;		
		case "DELVERSION" :		
			data = {
				actionType: actionType,
				appId: $("#rapid_P0_C43").val(),
				version: $("#rapid_P0_C1044_").val()
			}
			callback = function(response) {
				location.reload();
			}; 			
		break;				
		case "NEWPAGE" :	
			data = {
				actionType: actionType,
				appId: _version.id,
				version: _version.version,
				id: "P" + _nextPageId, 
				name: $("#rapid_P3_C17").val(),
				title: $("#rapid_P3_C18").val(),
				description: $("#rapid_P3_C19").val()
			}
			callback = function(data) {
				hideDialogue();
				loadPages(data.id, true);
			}; 
		break;
		case "DELPAGE" :
			data = { actionType: actionType, appId: _version.id, version: _version.version, id: _page.id }; 
			callback = function() {
				hideDialogue();
				loadPages(null, true);
			}; 
		break;	
		case "NEWDBCONN" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				name: $("#rapid_P7_C7").val(),
				driver: $("#rapid_P7_C37").val(),
				connectionString: $("#rapid_P7_C44").val(),
				connectionAdapter: $("#rapid_P7_C38").val(),
				userName: $("#rapid_P7_C30").val(),
				password: $("#rapid_P7_C35").val()
			};	
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};	
		break;
		case "DELDBCONN" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), index: $(ev.target).parent().parent().index()-1 };
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};	
		break;
		case "NEWSOA" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				name: $("#rapid_P10_C8_").val(),
				type: $("#rapid_P10_C23_").val()
			};	
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};	
		break;
		case "DELSOA" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), index: $(ev.target).parent().parent().index()-1 };
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};	
		break;
		case "NEWROLE" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), role: $("#rapid_P5_C7").val(), description: $("#rapid_P5_C41_").val() };
			callback = function() {
				// fake an adapter change
				$("#rapid_P0_C81").change();
				// fake a tab click
				$("#rapid_P0_C74").click(); 
			};								
		break;
		case "DELROLE" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), role: $(ev.target).closest("tr").find("td").first().html() };
			callback = function() {
				// fake an adapter change
				$("#rapid_P0_C81").change(); 
				// fake a tab click
				$("#rapid_P0_C74").click(); 
			};	
		break;
		case "SAVEROLE" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), role: $("#rapid_P0_C222").find("tr.rowSelect").children().first().html(), description: $("#rapid_P0_C735_").val() };
			callback = function() {
				// fake an adapter change
				$("#rapid_P0_C81").change(); 
				// fake a tab click
				$("#rapid_P0_C74").click(); 
			};							
		break;
		case "NEWUSER" :	
			if (rapidApp) {
				data = { actionType: actionType, appId: "rapid", version : _appVersion, userName: $("#rapid_P16_C8_").val(), description: $("#rapid_P16_C13_").val() , password: $("#rapid_P16_C17_").val(), useAdmin: $("#rapid_P16_C38_").prop("checked"), useDesign: $("#rapid_P16_C39_").prop("checked")};
			} else {	
				data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), userName: $("#rapid_P6_C7").val(), description: $("#rapid_P6_C42_").val() , password: $("#rapid_P6_C18").val() };
				callback = function() {
					Event_change_rapid_P0_C43(ev);
				};
			}								
		break;
		case "DELUSER" :		
			if (rapidApp) {
				data = { actionType: actionType, appId: "rapid", userName: $(ev.target).closest("tr").find("td").first().html() };
				callback = function() {			
					// hide any currently displayed user details
					$("#rapid_P0_C827_").hide();	
					// reload users
					Action_rapid(ev, 'rapid', 'P0', null, 'P0_A901_', 'GETUSERS', true);
				};
			} else {
				data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), userName: $(ev.target).closest("tr").find("td").first().html() };
				callback = function() {
					// fake an adapter change
					$("#rapid_P0_C81").change(); 
					// fake a tab click
					$("#rapid_P0_C74").click();    
				};
			} 											
		break;
		case "NEWUSERROLE" : 
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), userName: $("#rapid_P0_C216").find("tr.rowSelect").children().first().html(), role: $("#rapid_P0_C254").val() };
			callback = function() {
				Event_click_rapid_P0_C216({target: $("#rapid_P0_C216").find("tr.rowSelect").children().first()[0] });
			};																
		break;
		case "DELUSERROLE" :	
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), userName: $("#rapid_P0_C216").find("tr.rowSelect").children().first().html(), role: $(ev.target).parent().prev().html() };
			callback = function() {
				Event_click_rapid_P0_C216({target: $("#rapid_P0_C216").find("tr.rowSelect").children().first()[0] });
			};																
		break;
		case "SAVEUSER" :	
			if (rapidApp) {
				data = { actionType: actionType, appId: "rapid", version: _appVersion, userName: $("#rapid_P0_C823_").find("tr.rowSelect").children().first().html(), description: $("#rapid_P0_C838_").val(), password: $("#rapid_P0_C843_").val(), useAdmin: $("#rapid_P0_C879_").prop('checked'), useDesign: $("#rapid_P0_C880_").prop('checked') }; 
			} else {
				data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), userName: $("#rapid_P0_C216").find("tr.rowSelect").children().first().html(), description: $("#rapid_P0_C717_").val(), password: $("#rapid_P0_C231").val() };
				callback = function() {
					// fake an adapter change
					$("#rapid_P0_C81").change(); 
					// fake a tab click
					$("#rapid_P0_C74").click();    
				};	
			}															
		break;
		case "NEWPARAM" :
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val() };
		break;
		case "DELPARAM" :
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), index: $(ev.target).closest("tr").index()-1 };
		break;
		case "SAVEPARAM" :
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), version: $("#rapid_P0_C1044_").val(), index: $("#rapid_P0_C1108_").find("tr.rowSelect").index()-1, name: $("#rapid_P0_C1134_").val(), value: $("#rapid_P0_C1123_").val()};
		break;
		case "NEWDEVICE" :
			data = { actionType: actionType, appId: "rapid", version: _appVersion };
		break;
		case "DELDEVICE" :
			data = { actionType: actionType, appId: "rapid", version: _appVersion, index: $(ev.target).closest("tr").index()-1 };
		break;
		case "SAVEDEVICE" :
			data = { actionType: actionType, appId: "rapid", version: _appVersion, index: $("#rapid_P0_C1199_").find("tr.rowSelect").index()-1, 
			name: $("#rapid_P0_C1243_").val(), 
			width: $("#rapid_P0_C1217_").val(),
			height: $("#rapid_P0_C1248_").val(),
			ppi: $("#rapid_P0_C1253_").val(),
			scale: $("#rapid_P0_C1258_").val()
		};
		break;
		case "TESTDBCONN" :	
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				index: $("#rapid_P0_C311").find("tr.rowSelect").index()-1,
				driver: $("#rapid_P0_C338").val(),
				connectionString: $("#rapid_P0_C374").val(),
				connectionAdapter: $("#rapid_P0_C339").val(),
				userName: $("#rapid_P0_C340").val(),
				password: $("#rapid_P0_C341").val()
			};	
			callback = function(data) {
				alert(data.message);
			};															
		break;
		case "DELAPPBACKUP" : case "RESTOREAPPBACKUP" : 
			data = { actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				backupId: $("#rapid_P0_C663_").find("tr.rowSelect").children("td").first().text() 
			};	
			callback = function(data) {
				$(data.controlId).hideDialogue();
				Event_change_rapid_P0_C43();
				$("#rapid_P0_C662_").click();
			};														
		break;
		case "DELPAGEBACKUP" : case "RESTOREPAGEBACKUP" :	
			data = { actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				backupId: $("#rapid_P0_C678_").find("tr.rowSelect").children("td").first().text()  
			};	
			callback = function(data) {
				$(data.controlId).hideDialogue();
				Event_change_rapid_P0_C43();
				$("#rapid_P0_C662_").click();
			};															
		break;
		case "SAVEAPPBACKUPSIZE" :			
			data = { actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				backupMaxSize: $("#rapid_P0_C685_").val()  
			};
		break;
		case "SAVEPAGEBACKUPSIZE" :
			data = { actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				version: $("#rapid_P0_C1044_").val(),
				backupMaxSize: $("#rapid_P0_C686_").val()  
			};
		break;
		default :
			data = { actionType: actionType, appId: "rapid" };
	}
	
	// stringify any data
	if (data) data = JSON.stringify(data);
	
	// run the action on the server	
	$.ajax({
    	url: "~?a=rapid&p=" + pageId + "&act=" + actionId,
    	type: "POST",          
    	dataType: "json",
        data: data,            
        error: function(server, status, error) { 
        	if (server && server.status && server.status == 401) {
        		window.location = "login.jsp";
        	} else if (errorCallback) {
        		errorCallback(server, status, error);
        	} else {
        		alert(server.responseText || "Error : " + status);
        	}
        },
        success: function(data) {
       		if (callback) callback(data);
       		if (successCallback) successCallback(data);        	
        }
	});
}

function Action_validation(ev, validations, showMessages) {
	var valid = true;
	for (var i in validations) {
		var validation = validations[i];
		var value = window["getData_" + validation.controlType](ev, validation.controlId, validation.field, validation.details);
		if (validation.validationType == "javascript") {						
			var validationFunction = new Function("value", validation.javaScript);
			var failMessage = validationFunction.apply(this, [value]);
			if (failMessage) {
				if (showMessages) showValidationMessage(validation.controlId, failMessage);
				valid = false;
			} else {
				if (showMessages) hideValidationMessage(validation.controlId);
			}
		} else {
			if ((value && value.match(new RegExp(validation.regEx)))||(!value && validation.allowNulls)) {
				// passed
				if (showMessages) hideValidationMessage(validation.controlId);				
			} else {
				// failed
				if (showMessages) showValidationMessage(validation.controlId, validation.message);
				valid = false;					
			}	
		}	
	}	
	return valid;
}

function Action_webservice(actionId, data, outputs) {
	// only if there are data and outputs
	if (data && outputs) {
		// only if this is the latest sequence
		if (data.sequence > getWebserviceActionMaxSequence(actionId)) {
			// retain this as the lastest sequence
			_webserviceActionMaxSequence[actionId] = data.sequence;
			// loop the outputs
			for (var i in outputs) {
				var output = outputs[i];			
				window["setData_" + output.type](output.id, data, output.field, output.details);
			}
		}
	}
}


/* Control initialise methods */


function Init_hints(id, details) {
  var body = $("body");
  	    	
  for (var i in details.controlHints) {
  
  	var controlHint = details.controlHints[i];
  	
  	if (!$("#" + controlHint.controlId + "hint")[0]) {
  	
  		var style = controlHint.style;
  		if (style) {
  			style = " style='" + style + "'";
  		} else {
  			style = "";
  		}
  		
  		body.append("<span class='hint' id='" + controlHint.controlId + "hint'" + style + ">" + controlHint.text + "</span>");
  		
  		$("#" + controlHint.controlId + "hint").hide();
  		
  	}
  		
  	$("#" + controlHint.controlId).mouseout({controlId: controlHint.controlId}, function(ev) {
  		$("#" + ev.data.controlId + "hint").hide();
  	});
  		
  	switch (controlHint.type) {		
  		case "click" :
  			$("#" + controlHint.controlId).click({controlId: controlHint.controlId}, function(ev) { 
  				$("#" + ev.data.controlId + "hint").css({
  					left: ev.pageX + 5,
  					top: ev.pageY + 5
  				}).show(); 
  			});
  			break;
  		case "hover" :
  			$("#" + controlHint.controlId).mouseover({controlId: controlHint.controlId}, function(ev) { 
  				$("#" + ev.data.controlId + "hint").css({
  					left: ev.pageX + 5,
  					top: ev.pageY + 5
  				}).show();  
  			});
  		break;
  	}
  	
  }
}

function Init_pagePanel(id, details) {
  var bodyHtml = "<center><h1>Page</h1></center>";
  
  // request the page		
  $.ajax({
     	url: "~?a=" + details.appId + "&p=" + details.pageId + "&v=" + details.version,
     	type: "GET",          
         data: null,        
         error: function(server, status, error) { 
         	var bodyHtml = "Error loading page : " + error; 
         },
         success: function(page) {
         	
         // if the page can't be found a blank response is sent, so only show if we got something
         if (page) {
         	
         		// empty the body html
         		bodyHtml = "";
         		script = "";
         		
             	// loop the items
             	var items = $(page);
             	for (var i in items) {
             		// check for a script node
             		switch (items[i].nodeName) {
             		case "#text" : case "TITLE" : // ignore these types
             		break;
             		case "SCRIPT" :
             			if (items[i].innerHTML) {
             				script += items[i].outerHTML;
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

function Init_tabGroup(id, details) {
  $("#" + id).children("ul").children("li").each( function() {
  	$(this).click( function(ev, index) {
  		// get a reference to the tabs group
  		var tabs = $("#" + id);
  		// remove selected from all tab header items
  		tabs.children("ul").children("li").removeClass("selected");
  		// remove selected from all tab body items
  		tabs.children("div").removeClass("selected");
  		// add selected to the li we just clicked on, also get it's index, plus 2, 1 to go from zero to 1 based, the other 1 because of the headers
  		var index = $(this).addClass("selected").index() + 2;
  		// apply selected to the correct body
  		tabs.children("div:nth-child(" + index + ")").addClass("selected");
  	});
  });
}


/* Control getData and setData methods */


function getData_checkbox(ev, id, field, details) {
  return $("#" + id).prop("checked") ? "true" : "false";
}

function setData_checkbox(id, data, field, details) {
  var control = $("#" + id);	        
  if (data != null && data !== undefined) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					control.prop('checked', data.rows[0][i]);
  					break;
  				}
  			}
  		} else {
  			control.prop('checked', data.rows[0][0]);
  		}
  	} else {
  		control.prop('checked', false);
  	}
  } else {
  	control.prop('checked', false);
  }
}

function getData_dataStore(ev, id, field, details) {
  var dataStore;
  switch (details.storageType) {
  	case "S": 
  	dataStore = sessionStorage;
  	break;
  	case "L":
  	dataStore = localStorage;
  	break;
  }  
  if (dataStore) {
  	if (details.id) id = details.id;
  	var data = null;
  	var dataString = dataStore[id];
  	if (dataString) {
  		var data = JSON.parse(dataString);
  		if (data) {		
  			if (data.rows && data.fields) {
  				if (data.rows[0] && field) {
  					for (var i in data.fields) {
  						if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  							return data.rows[0][i];
  						}
  					}
  				} else {
  					return data;
  				}
  			} else if (field && data[field]) {
  				return data[field];
  			}
  		}	 
  	} 
  	return data;		
  }
}

function setData_dataStore(id, data, field, details) {
  var dataStore;
  switch (details.storageType) {
  	case "S": 
  	dataStore = sessionStorage;
  	break;
  	case "L":
  	dataStore = localStorage;
  	break;
  } 	   
  if (dataStore) {
  	if (details.id) id = details.id;
  	if (data != null && data !== undefined) {
  		data = makeDataObject(data, field);
  		if (details.merge && dataStore[id]) data = mergeDataObjects(data, JSON.parse(dataStore[id]));		
  		dataStore[id] = JSON.stringify(data);
  	} else {
  		dataStore[id] = null;
  	}
  }
}

function getData_dropdown(ev, id, field, details) {
  return $("#" + id).val();
}

function setData_dropdown(id, data, field, details) {
  if (data != null && data !== undefined) {
  	var control = $("#" + id);
  	data = makeDataObject(data, field);
  	if (data.rows && data.fields) {
  		if (field && data.rows[0]) {	      
  			var foundField = false;  	
  			for (var i in data.fields) {
  				if (field.toLowerCase() == data.fields[i].toLowerCase()) {
  					control.val(data.rows[0][i]);
  					foundField = true;
  					break;
  				}
  			}				
  			if (!foundField) control.val(data.rows[0][0]);
  		} else {
  			for (var i in data.rows) {
  				var row = data.rows[i];		
  				var text = "";
  				var value = "";
  				if (data.fields) {
  					for (var j in data.fields) {
  						if (data.fields[j].toLowerCase() == "text") text = data.rows[i][j];
  						if (data.fields[j].toLowerCase() == "value") value = data.rows[i][j];
  					}
  				}
  				if (!text) text = row[0];
  				if (!value && row[1]) value = row[1];
  				if (value || value == "0") value = 	" value='" + value + "'";
  				control.append("<option " + value + ">" + text + "</option>");
  			}	
  		}
  	} 
  }
}

function getData_grid(ev, id, field, details) {
  var data = null;
  if (details && details.columns) {
  	if (field) {		
  		var row = $(ev.target).closest("tr");
  		var rowIndex = row.index() - 1;
  		if (rowIndex >= 0) {
  			for (var i in details.columns) {
  				if (details.columns[i].field.toLowerCase() == field.toLowerCase()) {
  					data = row.children(":nth(" + i + ")").html();
  					break;
  				}
  			}
  		}	    
  	} else {
  		var data = {};
  		data.fields = [];		
  		for (var i in details.columns) {	
  			data.fields.push(details.columns[i].field);		
  		}
  		data.rows = [];
  		$("#" + id).find("tr:not(:first)").each(function(i) {
  			var row = [];
  			$(this).children().each(function(i) {
  				row.push($(this).html());
  			});
  			data.rows.push(row);
  		});
  	}	
  }
  return data;
}

function setData_grid(id, data, field, details) {
  var control = $("#" + id);
  control.find("tr:not(:first)").remove();	        
  if (data != null && data !== undefined) {	
  	data = makeDataObject(data, field);
  	if (data.rows) {	        		
  		if (details && details.columns && data.fields) {
  			var columnMap = [];
  			for (var i in details.columns) {				
  				for (var j in data.fields) {
  					var found = false;
  					if (details.columns[i].field) {
  						if (details.columns[i].field.toLowerCase() == data.fields[j].toLowerCase()) found = true;
  					} else {
  						if (!data.fields[j]) found = true;
  					}
  					if (found) {
  						columnMap.push(j);
  						break;
  					}
  				}
  				if (columnMap.length == i)
  					columnMap.push("");
  				// if we have cellFunction JavaScript, and it hasn't been turned into a function object yet
  				if (details.columns[i].cellFunction && !details.columns[i].f) 
  					details.columns[i].f = new Function(details.columns[i].cellFunction);
  			}
  			for (var i in data.rows) {
  				var row = data.rows[i];
  				var rowObject = control.append("<tr class='rowStyle" + (i % 2 + 1) + "'></tr>").find("tr:last");
  				for (var j in details.columns) {
  					var style = "";
  					if (!details.columns[j].visible) style += "display:none;";
  					if (details.columns[j].style) style += details.columns[j].style;
  					if (style) style = " style='" + style + "'";				
  					var cellObject = rowObject.append("<td" + style + ">" + ((columnMap[j]) ? row[columnMap[j]] : "") + "</td>").find("td:last");
  					if (details.columns[j].f) 
  						details.columns[j].f.apply(cellObject,[id, data, field, details]);
  				}				
  			}
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
  		}	
  	} 
  	
  	control.children().last().children("tr:not(:first)").click( function() { 
  		var row = $(this);
  		row.parent().find("tr.rowSelect").each( function() {
  			var row = $(this);
  			row.removeClass("rowSelect");
  		});
  		row.addClass("rowSelect"); 
  	});
  	
  }
}

function getProperty_grid_rowCount(ev, id, field, details) {
  return ($("#" + id).find("tr").size() - 1);
}

function getData_input(ev, id, field, details) {
  return $("#" + id).val();
}

function setData_input(id, data, field, details) {
  var control = $("#" + id);
  if (data != null && data !== undefined) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields && data.fields.length > 0) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					control.val(data.rows[0][i]);
  					break;
  				}
  			}
  		} else {
  			if (data.rows[0][0] != null && data.rows[0][0] !== undefined) {
  				control.val(data.rows[0][0]);
  			} else {
  				control.val("");
  			}
  		}
  	} else {
  		control.val("");
  	}
  } else {
  	control.val("");
  }
}

function getData_radiobuttons(ev, id, field, details) {
  return $("#" + id).children("input[type=radio]:checked").val();
}

function setData_radiobuttons(id, data, field, details) {
  if (data != null && data !== undefined) {
  	var radiobuttons = $("#" + id);
  	radiobuttons.children("input[type=radio]").prop('checked',false);
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
  	if (value) {
  		radiobuttons.children("input[type=radio][value=" + value + "]").prop('checked',true);
  	}
  }
}

function getData_text(ev, id, field, details) {
  return $("#" + id).html();
}

function setData_text(id, data, field, details) {
  var control = $("#" + id);	        
  if (data != null && data !== undefined) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
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


/* Control and Action resource JavaScript */


/* Link control resource JavaScript */

function linkClick(url, sessionVariablesString) {
	
	var sessionVariables = JSON.parse(sessionVariablesString);
	
	for (var i in sessionVariables) {
	
		var item = sessionVariables[i];
		
		if (item.type) {
		
			var value = window["getData_" + item.type](null, item.itemId, item.field, item.details);
			
		} else {
		
			var value = $.getUrlVar(item.itemId);
		
		}
	
		if (value !== undefined) url += "&" + item.name + "=" + value;
	}
	
	window.location = url;
	
}

/* Database action resource JavaScript */

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

/* Webservice action resource JavaScript */

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
