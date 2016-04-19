/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
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

package com.rapid.soa;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlType;

import com.rapid.server.RapidRequest;
import com.rapid.soa.SOAElementRestriction.*;
import com.rapid.soa.SOASchema.SOASchemaElement;
import com.rapid.utils.Files;

@XmlType(namespace="http://rapid-is.co.uk/soa")
public abstract class Webservice {
	
	// exception class
	public static class WebserviceException extends Exception {
		
		private static final long serialVersionUID = 1010L;
		
		String _message;
		Exception _ex;
		
		public WebserviceException(String message) {
			_message = message;
		}
		
		public WebserviceException(Exception ex) {
			_ex = ex;
			_message = _ex.getMessage();
		}

		@Override
		public String getLocalizedMessage() {
			if (_ex == null) {
				return "";
			} else {
				return _ex.getLocalizedMessage();
			}
		}

		@Override
		public String getMessage() {
			return _message;
		}

		@Override
		public Throwable getCause() {
			if (_ex == null) return super.getCause();
			return _ex.getCause();
		}

		@Override
		public StackTraceElement[] getStackTrace() {
			if (_ex == null) return super.getStackTrace();
			return _ex.getStackTrace();
		}
	
	}
	
	// final static (for wsdl production)
	
	private final static String wsdlNameSpace = "http://soa.rapid-is.co.uk";
		
	// instance variables
	
	protected String _id, _name;
	protected boolean _isAuthenticate;
	protected SOASchema _requestSchema, _responseSchema;
	private StringBuilder _wsdl;
	
	// properties 
	
	public String getId() { return _id; }
	public void setId(String id) { _id = id; }
	
	public String getName() { return _name; }
	public void setName(String name) { 
		_name = name;
		_id = Files.safeName(_name);
	}
		
	public SOASchema getRequestSchema() { return _requestSchema; }
	public void setRequestSchema(SOASchema requestSchema) { _requestSchema = requestSchema; }
	
	public SOASchema getResponseSchema() { return _responseSchema; }
	public void setResponseSchema(SOASchema responseSchema) { _responseSchema = responseSchema; }
			
	// constructor
	
	public Webservice() {}
	
	// specific child classes can be identified with this method
		
	public String getType() { return this.getClass().getSimpleName(); }
		
	private void appendElement(SOASchemaElement element, StringBuilder elementStringBuilder, boolean arrayNode) {
						
		// assume we are appending into the provided stringbuilder inside an element 
		StringBuilder stringBuilder = elementStringBuilder;
		// if not append to the root of the schema
		if (elementStringBuilder == null) stringBuilder = _wsdl;
		
		// these important restrictions go into the parent element
		List<SOAElementRestriction> parentRestrictions = null;
		// child restrictions get a special simpleType restriction
		List<SOAElementRestriction> childRestrictions = null;
		
		// get a reference to any restrictions
		List<SOAElementRestriction> restrictions = element.getRestrictions();
		// if we got some restrictions pick out the important ones
		if (restrictions != null) {
			for (SOAElementRestriction restriction : restrictions) {
				// add to parent or child restrictions list
				if (restriction.checkAtParent()) {
					// instantiate if we need to do so
					if (parentRestrictions == null) parentRestrictions = new ArrayList<SOAElementRestriction>();
					// add this restriction to the collection
					parentRestrictions.add(restriction);
				} else {
					// instantiate if we need to do so
					if (childRestrictions == null) childRestrictions = new ArrayList<SOAElementRestriction>();
					// add this restriction to the collection
					childRestrictions.add(restriction);
				}
			}
		}
						
		if (element.getIsArray() && arrayNode) {
									
			stringBuilder.append("<xs:complexType>");
			stringBuilder.append("<xs:sequence>");
			stringBuilder.append("<xs:element name=\"" + element.getName() + "\" ");
			
			// retain whether a maxOccurs was set
			boolean maxOccursSet = false;
			// add any parent restrictions
			if (parentRestrictions != null) {
				for (SOAElementRestriction restriction : parentRestrictions) {
					// check and set maxOccurs
					if (restriction.getClass() == MaxOccursRestriction.class) maxOccursSet = true;
					// add restriction
					stringBuilder.append(restriction.getSchemaRule() + " ");
				}
			}
			// add unbounded maxOccurs if no explicit one set
			if (!maxOccursSet) stringBuilder.append("maxOccurs=\"unbounded\" ");
			
			stringBuilder.append(" type=\"xsd:" + element.getName() + "\" />");
			
			stringBuilder.append("</xs:sequence>");
			stringBuilder.append("</xs:complexType>");
			
			appendElement(element, null,  false);
			
		} else if (element.getDataType() == 0) {
					
			// complex type
			stringBuilder.append("<xs:complexType");			
			if (elementStringBuilder == null) stringBuilder.append(" name=\"" + element.getName() + "\"");
			stringBuilder.append(">");
			
			stringBuilder.append("<xs:sequence>");
			
			if (element.getChildElements() != null) {
				
				for (SOASchemaElement childElement : element.getChildElements()) {
					
					appendElement(childElement, stringBuilder, childElement.getIsArray());
					
				}
				
			}
			
			stringBuilder.append("</xs:sequence>");
			stringBuilder.append("</xs:complexType>");
			
		} else {
			
			// simple type
			stringBuilder.append("<xs:element name=\"" + element.getName() + "\" ");
			// add any parent restrictions
			if (parentRestrictions != null) {
				for (SOAElementRestriction restriction : parentRestrictions) {
					stringBuilder.append(restriction.getSchemaRule() + " ");
				}
			}
			// check if child restrictions
			if (childRestrictions != null) {
				// finish opening tag
				stringBuilder.append(">");
				// open simple type
				stringBuilder.append("<xs:simpleType>");
				stringBuilder.append("<xs:restriction base=\"xs:" + SOASchema.getDataTypeName(element.getDataType()) + "\">");
				for (SOAElementRestriction restriction : childRestrictions) {
					stringBuilder.append(restriction.getSchemaRule() + " ");
				}
				stringBuilder.append("</xs:restriction>");
				stringBuilder.append("</xs:simpleType>");
				// close element
				stringBuilder.append("</xs:element>");
			} else {
				// add type and self terminate if no child restrictions
				stringBuilder.append("type=\"xs:" + SOASchema.getDataTypeName(element.getDataType()) + "\"/>");
			}			
			
		}
						
	}
	
	private void appendRootElement(SOASchemaElement element) {
		
		StringBuilder elementStringBuilder = new StringBuilder(); 
		
		elementStringBuilder.append("<xs:element name=\"" + element.getNameArrayCheck() + "\">");
		appendElement(element, elementStringBuilder, element.getIsArray());
		elementStringBuilder.append("</xs:element>");
		
		_wsdl.append(elementStringBuilder.toString());
							
	}
			
	// produce the WSDL for this webservice
	
	public String getWSDL(String appId, String appVersion, String endPoint) {
		
		_wsdl = new StringBuilder();
		
		// this is the root node of the schema - not the various namespaces and that our own namespace is the target
		_wsdl.append("<wsdl:definitions xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:tns=\"" + wsdlNameSpace + ".wsdl\" xmlns:xsd=\"" + wsdlNameSpace + "\" targetNamespace=\"" + wsdlNameSpace + ".wsdl\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">");
		
		// this starts all the schemas used by this webservice
		_wsdl.append("<wsdl:types>");
		_wsdl.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" targetNamespace=\"" + wsdlNameSpace + "\">");
		
		// get the id just once as authenticate will override it
		String id = getId();
		
		// header - not required to establish authentication, but is used thereafter  
		if (_isAuthenticate) {
			id = "authenticate";
		} else {
			_wsdl.append("<xs:element name=\"authentication\" type=\"xs:string\"/>");
		}
		
		// request element
		SOASchemaElement requestRootElement = null;
		// request schema - use the method to allow overrides to populate it
		SOASchema requestSchema = getRequestSchema();
		if (requestSchema != null) {
			requestRootElement = requestSchema.getRootElement(); 
			if (requestRootElement != null) appendRootElement(requestRootElement);
		}
		// response
		SOASchemaElement responseRootElement = null;
		// response schema - use the method to allow overrides to populate it
		SOASchema responseSchema = getResponseSchema();
		if (responseSchema != null) {
			responseRootElement = responseSchema.getRootElement(); 
			if (responseRootElement != null) appendRootElement(responseRootElement);
		}

		_wsdl.append("</xs:schema>");
		_wsdl.append("</wsdl:types>");
		
		// this is the request header - a property of SOAP is that it is optional
		if (!_isAuthenticate) {		 
			_wsdl.append("<wsdl:message name=\"Header\">");
		 	_wsdl.append("<wsdl:part element=\"xsd:authentication\" name=\"header\" />");
		 	_wsdl.append("</wsdl:message>");
		}
		
		// this is the request body - note the use of getNameArrayCheck which adds the "Array" prefix to the name should it be required
		_wsdl.append("<wsdl:message name=\"Input\">");
		if (_requestSchema != null && requestRootElement != null) _wsdl.append("<wsdl:part element=\"xsd:" + requestRootElement.getNameArrayCheck() + "\" name=\"body\"/>");
		_wsdl.append("</wsdl:message>");
								
		// this is the response body
		_wsdl.append("<wsdl:message name=\"Output\">");
		if (_responseSchema != null && responseRootElement != null) _wsdl.append("<wsdl:part element=\"xsd:" + responseRootElement.getNameArrayCheck() + "\" name=\"body\"/>");
		_wsdl.append("</wsdl:message>");
	
		
		_wsdl.append("<wsdl:portType name=\"PortType\">");
		_wsdl.append("<wsdl:operation name=\"" + id + "\">");
		_wsdl.append("<wsdl:input message=\"tns:Input\"/>");
		_wsdl.append("<wsdl:output message=\"tns:Output\"/>");
		_wsdl.append("</wsdl:operation>");
		_wsdl.append("</wsdl:portType>");
		
		// this specifies the binding, including the soapAction which is the appId . webserviceId
		_wsdl.append("<wsdl:binding name=\"" + id + "Binding\" type=\"tns:PortType\">");
		_wsdl.append("<soap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>");
		_wsdl.append("<wsdl:operation name=\"" + id + "\">");
		if (_isAuthenticate) {
			_wsdl.append("<soap:operation soapAction=\"authenticate\"/>");
		} else {
			_wsdl.append("<soap:operation soapAction=\"" + appId + "/" + appVersion + "/" + _name + "\"/>");
		}
		
		_wsdl.append("<wsdl:input>");
		_wsdl.append("<soap:body use=\"literal\"/>");
		if (!_isAuthenticate) _wsdl.append("<soap:header message=\"tns:Header\" part=\"header\"/>");
		_wsdl.append("</wsdl:input>");
		
		_wsdl.append("<wsdl:output>");
		_wsdl.append("<soap:body use=\"literal\"/>");
		_wsdl.append("</wsdl:output>");
		
		_wsdl.append("</wsdl:operation>");
		_wsdl.append("</wsdl:binding>");
		
		// this specifies the end point which we get from the servelet
		_wsdl.append("<wsdl:service name=\"Service\">");
		_wsdl.append("<wsdl:port binding=\"tns:" + id + "Binding\" name=\"Port\">");
		_wsdl.append("<soap:address location=\"" + endPoint + "\"/>");
		_wsdl.append("</wsdl:port>");		
		_wsdl.append("</wsdl:service>");
				
		_wsdl.append("</wsdl:definitions>");
		
		return _wsdl.toString();
		
	}
		
	// every child class is expected to return a response of SOAData
	
	public abstract SOAData getResponseData(RapidRequest rapidRequest, SOAData requestData) throws WebserviceException;
	
}
