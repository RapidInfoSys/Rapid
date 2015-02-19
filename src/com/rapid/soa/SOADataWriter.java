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

package com.rapid.soa;

import java.util.List;

public abstract class SOADataWriter {
	
	// private instance variables
	
	protected SOAData _soaData;
	
	// properties
	
	public SOAData getSOAData() { return _soaData; }
	
	// constructor
	
	public SOADataWriter(SOAData soaData) {
		_soaData = soaData;
	}
		
	// abstract methods
	
	public abstract String write();
	
					
	// implementing classes
	
	/*
	 *  SOAXMLWriter
	 * 
	 *  Writes SOAData as XML
	 *  
	 */
	
	public static class SOAXMLWriter extends SOADataWriter {

		private static final String NAMESPACE_PREFIX = "soa:";
		
		StringBuilder _stringBuilder;
		
		public SOAXMLWriter(SOAData soaData) {
			super(soaData);			
		}
		
		private String xmlEscape(String value) {
			return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
		}
		
		private void appendElement(SOAElement element) {
			
			// get the value
			String value = element.getValue();
			
			// elements require either children or a value to be printed
			if (element.getChildElements() != null || value != null) {
												
				// check whether array element
				if (element.getIsArray()) {
					// open array 
					_stringBuilder.append("<" + NAMESPACE_PREFIX + element.getName() + "Array>");
				} else {
					// open the element
					_stringBuilder.append("<" + NAMESPACE_PREFIX + element.getName() + ">");
				}
									
				// if we got a value print it into the element
				if (value != null) _stringBuilder.append(xmlEscape(value.trim()));
				
				// check for any child elements
				List<SOAElement> childElements = element.getChildElements();
										
				// if we have child elements
				if (childElements != null) {
					// loop and write the child elements
					for (SOAElement childElement : childElements) {
						appendElement(childElement);
					}
				}
																
				// check whether the array
				if (element.getIsArray()) {
					// close array (if need be)
					_stringBuilder.append("</" + NAMESPACE_PREFIX + element.getName() + "Array>");
				} else {
					// close the element
					_stringBuilder.append("</" + NAMESPACE_PREFIX + element.getName() + ">");
				}
			}
		}

		@Override
		public String write() {
			_stringBuilder = new StringBuilder();			
			appendElement(_soaData.getRootElement());			
			return _stringBuilder.toString();
		}
		
	}
	
	/*
	 *  SOAJSONReader 
	 * 
	 *  Writes SOAData as JSON
	 *  
	 */
		
	public static class SOAJSONWriter extends SOADataWriter {

		StringBuilder _stringBuilder;
		
		public SOAJSONWriter(SOAData soaData) {
			super(soaData);
		}	
		
		private String jsonEscape(String value) {
			return value.replace("'", "\\'").replace("\"", "\\\"");
		}
				
		private void append(SOAElement element) {
			
			if (element.getIsArray()) {
				
				_stringBuilder.append("{\"" + element.getName() + "\":[");
				
				List<SOAElement> childElements = element.getChildElements();
				
				if (childElements != null) {
					
					for (int i = 0; i < childElements.size(); i ++) {
						
						append(childElements.get(i));
						
						if (i < childElements.size() - 1) _stringBuilder.append(",");
						
					}
					
				}
				
				_stringBuilder.append("]}");
				
			} else {
												
				if (element.getParentElement() == null) {
					
					// only the root element has no parent and we have already established that it is not an array
					
					List<SOAElement> childElements = element.getChildElements();
					
					if (childElements != null) {
						
						_stringBuilder.append("{");
						
						for (int i = 0; i < childElements.size(); i ++) {
							
							append(childElements.get(i));
							
							if (i < childElements.size() - 1) _stringBuilder.append(",");
							
						}
						
						_stringBuilder.append("}");
						
					}
					
				} else {
					
										
					if (element.getParentElement().getIsArray()) {
						
						List<SOAElement> childElements = element.getChildElements();
						
						if (childElements != null) {
							
							_stringBuilder.append("{");
							
							for (int i = 0; i < childElements.size(); i ++) {
								
								append(childElements.get(i));
								
								// if we're not on the final element
								if (i < childElements.size() - 1) {
									// only add a separating comma if the next element has a value
									if (element.getChildElements().get(i + 1).getValue() != null) _stringBuilder.append(",");
									
								} 
								
							}
							
							_stringBuilder.append("}");
														
						}
																														
					} else {
																		
						String value = element.getValue();
						
						if (value != null) {
							
							_stringBuilder.append("\"" + element.getName() + "\":\"" + jsonEscape(value) + "\"");
							
						}
						
					}
					
				}
				
			}
				
		}

		@Override
		public String write() {
			SOAElement rootElement = _soaData.getRootElement();
			if (rootElement == null) {
				return "{}";
			} else {
				_stringBuilder = new StringBuilder();			
				append(rootElement);
				return _stringBuilder.toString();
			}
			
		}
				
	}
	
	public static class SOARapidWriter extends SOADataWriter {

		StringBuilder _stringBuilder;
		
		public SOARapidWriter(SOAData soaData) {
			super(soaData);
		}
		
		private String jsonEscape(String value) {
			if (value == null) {
				return value;
			} else {
				return value.replace("'", "\\'").replace("\"", "\\\"");
			}
		}
		
		private void append(SOAElement element) {
			
			if (element.getIsArray()) {
								
				_stringBuilder.append("{");
				
				List<SOAElement> collectionElements = element.getChildElements();
				
				if (collectionElements != null) {
					
					for (int i = 0; i < collectionElements.size(); i ++) {
																
						SOAElement collectionElement = collectionElements.get(i);
						
						List<SOAElement> childElements = collectionElement.getChildElements();
						
						if (i == 0) {
							
							_stringBuilder.append("'fields':[");
							
							for (int j = 0; j < childElements.size(); j++) {
								
								SOAElement childElement = childElements.get(j);
																									
								_stringBuilder.append("'" + jsonEscape(childElement.getName()) + "'");
									
								if (j < childElements.size() - 1) _stringBuilder.append(",");
																									
							}
							
							_stringBuilder.append("],rows:[");
																
						}
												
						_stringBuilder.append("[");
							
						for (int j = 0; j < childElements.size(); j++) {
							
							SOAElement childElement = childElements.get(j);
							
							if (childElement.getChildElements() == null || childElement.getChildElements().size() == 0) {
							
								_stringBuilder.append("'" + jsonEscape(childElement.getValue()) + "'");
								
							} else {
								
								append(childElement);
								
							}
																											
							if (j < childElements.size() - 1) _stringBuilder.append(",");
							
						}
						
						_stringBuilder.append("]");
						
						if (i < collectionElements.size() - 1) _stringBuilder.append(",");
						
					}
					
					_stringBuilder.append("]");
					
				}
				
				_stringBuilder.append("}");
				
			} else {
												
				_stringBuilder.append("{");
				
				List<SOAElement> childElements = element.getChildElements();
				
				_stringBuilder.append("'fields':[");
				
				for (int i = 0; i < childElements.size(); i++) {
					
					SOAElement childElement = childElements.get(i);
																						
					_stringBuilder.append("'" + childElement.getName() + "'");
						
					if (i < childElements.size() - 1) _stringBuilder.append(",");
																						
				}
				
				_stringBuilder.append("],rows:[[");
														
					
				for (int i = 0; i < childElements.size(); i++) {
					
					SOAElement childElement = childElements.get(i);
					
					if (childElement.getChildElements() == null || childElement.getChildElements().size() == 0) {
					
						_stringBuilder.append("'" + jsonEscape(childElement.getValue()) + "'");
						
					} else {
						
						append(childElement);
						
					}
																									
					if (i < childElements.size() - 1) _stringBuilder.append(",");
					
				}
				
				_stringBuilder.append("]]}");
								
			}
			
		}

		@Override
		public String write() {
			SOAElement rootElement = _soaData.getRootElement();
			if (rootElement == null) {
				return "{}";
			} else {
				_stringBuilder = new StringBuilder();			
				append(rootElement);
				return _stringBuilder.toString();
			}
		}
		
	}
	
}
