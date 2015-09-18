/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as 
published by the Free Software Foundation, either version 3 of the 
License, or (at your option) any later version. The terms require you 
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.security.SecurityAdapter;
import com.rapid.soa.AuthenticationWebservice;
import com.rapid.soa.SOAData;
import com.rapid.soa.SOADataReader;
import com.rapid.soa.Webservice;
import com.rapid.soa.SOADataReader.SOAJSONReader;
import com.rapid.soa.SOADataReader.SOAXMLReader;
import com.rapid.soa.SOADataWriter.SOAJSONWriter;
import com.rapid.soa.SOADataWriter.SOAXMLWriter;

public class SOA extends RapidHttpServlet {
			
	private static final long serialVersionUID = 3L;
	
	private Webservice _authenticationWebservice;
	private Logger _logger;
	
    public SOA() { 
    	super(); 
    	_logger = Logger.getLogger(SOA.class);
    	_authenticationWebservice = new AuthenticationWebservice();
    }
            
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
										
		// get a logger
		Logger logger = getLogger();
						
		// log!
		logger.debug("SOA GET request : " + request.getQueryString());
				
		PrintWriter out = response.getWriter();
		
		String appId = request.getParameter("a");
		
		if (appId == null) {
			
			response.setContentType("text/xml");
			
			URL url = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), request.getRequestURI());
			
			out.print(_authenticationWebservice.getWSDL(null, null, url.toString()));
			
		} else {
			
			String appVersion = request.getParameter("v");
			String serviceId = request.getParameter("s");
			
			Application app = getApplications().get(appId, appVersion);
							
			if (app == null) {
				
				// tell user app could not be found
				sendException(response, new Exception("Application could not be found"));
				
			} else if (serviceId == null) {
				
				out.print("<html><head><title>Rapid SOA Webservices</title></head><script type='text/javascript' src='scripts/jquery-1.10.2.js'></script><script type='text/javascript' src='scripts/soa.js'></script><body>Wsdls:<p/>");
				
				String dropDown = "<select id='action'>";
				
				// print a list of webservices for this app
				List<Webservice> webservices = app.getWebservices();
				if (webservices == null) {
					out.print("This application has no webservices");
				} else {
					for (Webservice webservice : webservices) {
						dropDown += "<option value='" + app.getId() + "." + webservice.getId() + "'>" + webservice.getName() + "</option>";
						out.print("<a href='soa?a=" + app.getId() + "&s=" + webservice.getId() + "'>" + webservice.getName() + "</a><p/>");
					}
				}
				
				dropDown += "</select>";
				
				out.print("<p>&nbsp;</p>Request:<p/>" + dropDown + "<input type='radio' name='contentType' value='text/xml' checked='checked'/>soap<input type='radio' name='contentType' value='application/json' />json<p/><textarea id='request' style='width:100%;height:200px;'></textarea><p/><button onclick='submitWebservice();'>Submit</button><p/>Response:<p/><div id='response' style='min-height:200px;border:1px solid grey;'></div>");
				
				out.print("</body></html>");
				
			} else {
				
				// try and avoid caching
				response.setHeader("Expires", "Sat, 15 March 1980 12:00:00 GMT");

				// Set standard HTTP/1.1 no-cache headers.
				response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

				// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
				response.addHeader("Cache-Control", "post-check=0, pre-check=0");

				// Set standard HTTP/1.0 no-cache header.
				response.setHeader("Pragma", "no-cache");
				
				Webservice webservice = app.getWebservice(serviceId);
				
				if (webservice == null) {
					
					sendException(response, new Exception("Webservice could not be found"));
					
				} else {
					
					response.setContentType("text/xml");
					
					URL url = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), request.getRequestURI());
					
					out.print(webservice.getWSDL(appId, appVersion, url.toString()));
									
				} // service check
				
			} // app and services exist check
			
		} // appId		
		
		out.close();
			
	}
		
	// get a suitable reader or throw an exception
	private SOADataReader getRequestReader(String contentType, Webservice webservice) throws Exception {
		
		// now get a suitable reader
		if (contentType.contains("xml")) {
			return  new SOAXMLReader(webservice.getRequestSchema()); 										
		} else if (contentType.contains("json")) {
			return new SOAJSONReader(webservice.getRequestSchema()); 	
		} else {
			throw new Exception("No suitable reader found");
		}
		
	}
	
	// write the response data
	private void writeResponseData(String contentType, HttpServletResponse response, PrintWriter out, SOAData soaResponseData, boolean soapAction) {
		
		// check the content type
		if (contentType.contains("xml")) {
														
			// get a suitable writer
			SOAXMLWriter xmlWriter = new SOAXMLWriter(soaResponseData);
			
			// assume no response
			String xmlResponse = null;
			
			// check 
			if (soapAction) {
				// soap
				
				 xmlResponse = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soa=\"http://soa.rapid-is.co.uk\"><soapenv:Body>";
						
				xmlResponse += xmlWriter.write();
				
				xmlResponse += "</soapenv:Body></soapenv:Envelope>";
																												
			} else {
				
				// resetful
				
				xmlResponse = xmlWriter.write();
																
			} // soap check
																									
			// set the response type
			response.setContentType("text/xml");
			
			// print the response
			out.print(xmlResponse);
			
			// log
			_logger.debug("SOA xml response : " + xmlResponse);
			
		} else if (contentType.contains("json")) {
			// json
											
			// get a suitable writer
			SOAJSONWriter jsonWriter = new SOAJSONWriter(soaResponseData);
			
			// write the data in json
			String jsonResponse = jsonWriter.write();
													
			// set the response type
			response.setContentType("application/json");
			
			// print the response
			out.print(jsonResponse);
			
			// log
			_logger.debug("SOA json response : " + jsonResponse);
			
		} // content type check
		
	}
	

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// get a logger
		Logger logger = getLogger();
						
		// log!
		logger.debug("SOA POST request : " + request.getContentType() + " "  + request.getContentLength() + " bytes" );
		
		// get the print writer
		PrintWriter out = response.getWriter();
		
		// get the content type
		String contentType = request.getContentType();
		
		// retieve the http session without creating a new one
		HttpSession session = request.getSession(false);
				
		// the reader, if an exception occurs we will try and get a session from it's authentication details
		SOADataReader soaReader = null;
											
		try {
			
			// check we got the content type
			if (contentType == null) {
				
				throw new Exception("No content type");
				
			} else {
				
				// put into lower case
				contentType = contentType.toLowerCase();
				
				// if this xml
				if (contentType.contains("xml") || contentType.contains("json")) {
					
					// assume not a SOAP action
					boolean soapAction = false;
					// get the action header
					String action =  request.getHeader("Action");
					// if not action
					if (action == null) {
						// try SOAPAction
						action =  request.getHeader("SOAPAction");
						// if we got something remember this is soap
						if (action != null) soapAction = true;
					}
					// if we got one clean up the action, removing "
					if (action != null) action = action.trim().replace("\"", "");
						
					// now check what we got
					if (action == null) {
						
						if (contentType.contains("xml")) {
							throw new Exception("Action or SOAPAction header must be provided");
						} else {
							throw new Exception("Action header must be provided");
						}
						
					} else if ("authenticate".equals(action)) {
						
						// now get a suitable reader
						soaReader = getRequestReader(contentType, _authenticationWebservice);
							
						// validate and read the data 
						SOAData requestData = soaReader.read(request.getInputStream());
						
						// make a suitable rapid request with the details the webservice might need
						RapidRequest rapidRequest = new RapidRequest(this, request);
												
						// call the webservice
						SOAData responseData = _authenticationWebservice.getResponseData(rapidRequest, requestData);
						
						// write the response (shared by authenticate)
						writeResponseData(contentType, response, out, responseData, soapAction);
				
					} else {
						
						// trim, clean, and split the action using forward slashes
						String[] actionParts = action.trim().replace("\"", "").replace("'","").split("/");
						// check we have enough parts
						if (actionParts.length == 3) {
							
							String appId = actionParts[0];
							String versionId = actionParts[1];
							String serviceId = actionParts[2];
							
							Application application = getApplications().get(appId, versionId);
							
							if (application == null) {
								
								throw new Exception("Application could not be found");
								
							} else {
								
								// get the webservice from the application
								Webservice webservice = application.getWebservice(serviceId);
								
								if (webservice == null) {
									
									throw new Exception("Webservice not found in application");
									
								} else {
																		
									// now get a suitable reader
									soaReader = getRequestReader(contentType, webservice);
										
									// validate and read the data 
									SOAData requestData = soaReader.read(request.getInputStream());
									
									// get the authentication
									String authentication = soaReader.getAuthentication();
									
									// if we have no session yet, use the authentication above to try and get one
									if (session == null) session = RapidSessionListener.getSession(authentication);
									
									// if no session could be found send an 
									if (session == null) {
										
										sendException(response, new Exception("Not authenticated"));
										
									} else {
										
										// make a suitable rapid request with the details the webservice might need
										RapidRequest rapidRequest = new RapidRequest(this, request, session, application);
										
										// get any securityAdapter
										SecurityAdapter security = application.getSecurityAdapter();
										
										// check the 
										boolean validUser = security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword());
										
										if (validUser) {
											
											// process the webservice and get its data
											SOAData soaResponseData = webservice.getResponseData(rapidRequest, requestData);
											
											// write the response (shared by authenticate)
											writeResponseData(contentType, response, out, soaResponseData, soapAction);
											
										} else {
											
											throw new Exception("You do not have permission to use this application");	
											
										} // valid user check
																																																						
									} // session check
																																						
								} // webservice check
								
							} // application check
							
						} else {
							
							if (soapAction) {							
								throw new Exception("SOAPAction must contain app id, version, and SOA webservice id, seperated by forward slashes. Received " + action);								
							} else {
								throw new Exception("Action must contain app id, version, and SOA webservice id, seperated by forward slashes. Received " + action);
							}
							
						} // action format check
						
					} // action header check
					
				} else {
					
					throw new Exception("Content type not recognised");
					
				}
				
			}
			
		} catch (Exception ex) {
			
			// if we got a reader
			if (soaReader != null) {
				// look for an authentication id
				String authentication = soaReader.getAuthentication();
				// if we got one, look for a session
				if (authentication != null) session = RapidSessionListener.getSession(authentication);
			}
			
			// check to see if we authenticated
			if (session == null) {
				// no authentication so send this instead
				sendException(response, new Exception("Not authenticated"));
			} else {
				// send the exception details as formatted 500 http response
				sendException(response, ex);
			}
			
			
		} 
		
		// close the output stream
		out.close();
					
	}

}
