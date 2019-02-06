/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapid.soa.SOASchema.SOASchemaElement;

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
			if (_soaData != null) appendElement(_soaData.getRootElement());
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

		protected StringBuilder _stringBuilder;
		protected SOASchema _soaSchema;

		public SOAJSONWriter(SOAData soaData) {
			super(soaData);
		}

		private String getJsonValue(SOAElement element, String id) {

			// get the value (always a string)
			String jsonValue = jsonEscape(element.getValue());

			// assume it is a string
			boolean isString = true;

			// if we have a schema
			if (_soaSchema != null) {
				// get this element by its id
				SOASchemaElement schemaElement = _soaSchema.getElementById(id);
				// if we got one
				if (schemaElement != null) {
					// check type and adjust isString accordingly
					switch (schemaElement.getDataType()) {
					case SOASchema.BOOLEAN : case SOASchema.DECIMAL : case SOASchema.INTEGER : case SOASchema.NONE : isString = false; break;
					}
				}
			}

			// if string, add quotes
			if (isString) jsonValue = "\"" + jsonValue + "\"";

			// return
			return jsonValue;

		}

		private void append(SOAElement element, String id) {

			if (element.getIsArray()) {

				_stringBuilder.append("\"" + element.getName() + "\":[");

				List<SOAElement> childElements = element.getChildElements();

				if (childElements != null) {

					for (int i = 0; i < childElements.size(); i ++) {

						SOAElement childElement = childElements.get(i);

						append(childElement, id + "." + i);

						if (i < childElements.size() - 1) _stringBuilder.append(",");

					}

				}

				_stringBuilder.append("]");

			} else {

				// only the root element has no parent and we have already established that it is not an array
				if (element.getParentElement() == null) {

					// open json object
					_stringBuilder.append("{");

					// get any children
					List<SOAElement> childElements = element.getChildElements();

					// if no child data elements
					if (childElements == null) {

						// assume no schema
						boolean gotSchema = false;

						// look for a schema
						if (_soaSchema != null) {
							// look for an element
							SOASchemaElement schemaElement = _soaSchema.getElementById(id);
							// if we got one
							if (schemaElement != null) {
								// we found a schema - set to true to avoid printing anything
								gotSchema = true;
							}
						}

						// no schema found print the element name
						if (!gotSchema) _stringBuilder.append("\"" + element.getName() + "\":" + getJsonValue(element, id));

					} else {

						for (int i = 0; i < childElements.size(); i ++) {

							SOAElement childElement = childElements.get(i);

							append(childElement, id + "." + i);

							if (i < childElements.size() - 1) _stringBuilder.append(",");

						}

					}

					// close json object
					_stringBuilder.append("}");

				} else {

					if (element.getParentElement().getIsArray()) {

						List<SOAElement> childElements = element.getChildElements();

						if (childElements != null) {

							_stringBuilder.append("{");

							for (int i = 0; i < childElements.size(); i ++) {

								SOAElement childElement = childElements.get(i);

								append(childElement, id + "." + i);

								// if we're not on the final element
								if (i < childElements.size() - 1) {
									// get next child element
									SOAElement nextChildElement = element.getChildElements().get(i + 1);
									// only add a separating comma if the next element has a value, or is an array with child elements
									if (nextChildElement.getValue() != null || (nextChildElement.getIsArray() && nextChildElement.getChildElements() != null && nextChildElement.getChildElements().size() > 0)) _stringBuilder.append(",");

								}

							}

							_stringBuilder.append("}");

						}

					} else {

						String value = element.getValue();

						if (value != null) {

							// append value to json!
							_stringBuilder.append("\"" + element.getName() + "\":" + getJsonValue(element, id));

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
				_soaSchema = _soaData.getSchema();
				append(rootElement,"0");
				return _stringBuilder.toString();
			}

		}

	}

	// writes soa data in the standard Rapid front end format: {fields:[],rows:[][]}

	public static class SOARapidWriter extends SOADataWriter {

		protected Map<String,List<String>> _fieldsMap;
		protected StringBuilder _stringBuilder;

		public SOARapidWriter(SOAData soaData) {
			super(soaData);
			_fieldsMap = new HashMap<>();
		}

		private void parse(SOAElement element) {

			List<SOAElement> childElements = element.getChildElements();

			// if this element has children
			if (childElements != null) {

				// get this elements name
				String elementName = element.getName();

				// get any previous field map for the element
				List<String> fields = _fieldsMap.get(elementName);

				// if there wasn't one
				if (fields == null) {
					// make a new one
					fields = new ArrayList<>();
					// add this elements field map to fields map
					_fieldsMap.put(elementName, fields);
				}


				// loop the children
				for (int i = 0; i < childElements.size(); i++) {

					// get the child element
					SOAElement childElement = childElements.get(i);

					// check is array
					if (element.getIsArray()) {

						// get the array element
						List<SOAElement> arrayElements = childElement.getChildElements();

						// check we got some
						if (arrayElements != null && arrayElements.size() > 0) {

							// loop it's children too!
							for (int j = 0; j < arrayElements.size(); j++) {

								// get the array element
								SOAElement arrayElement = arrayElements.get(j);

								// get its name
								String arrayElementName = arrayElement.getName();

								// if fields has been populated
								if (fields.size() == 0) {

									// add straight away
									fields.add(arrayElementName);

								} else {

									// if not in the fields already
									if (!fields.contains(arrayElementName)) {

										// assume we should add it
										boolean add = true;

										// need at least one before this one
										if (j > 0) {

											// work backwards form the one before
											for (int k = j - 1; k > 1; k--) {

												// get the name to check
												String checkName = arrayElements.get(k).getName();

												// get pos
												int pos = fields.indexOf(checkName);

												// if we got one
												if (pos >= 0) {

													// insert after this position
													fields.add(k + 1, arrayElementName);

													// remember not to add
													add = false;

													// we're done
													break;

												}

											}

										}

										// if we're still needing to add and there are other fields left to check
										if (add && j < arrayElements.size()) {

											// work forwards from the one after
											for (int k = j + 1; k < arrayElements.size(); k++) {

												// get the name to check
												String checkName = arrayElements.get(k).getName();

												// get pos
												int pos = fields.indexOf(checkName);

												// if we got one
												if (pos >= 0) {

													// insert before this position
													fields.add(k, arrayElementName);

													// remember not to add
													add = false;

													// we're done
													break;

												}

											}

										}

										// add if we need to
										if (add) fields.add(arrayElementName);

									}

								}

							}

						}

					} else {

						// get its name
						String childElementName = childElement.getName();

						// look for it in the fieldMap, add if not
						if (!fields.contains(childElementName)) fields.add(childElementName);

					}

					// if it has children, parse it too
					if (childElement.getChildElements() != null && childElement.getChildElements().size() > 0) parse(childElement);

				} // array check

			}

		}


		private void append(SOAElement element) {

			String elementName = element.getName();

			if (element.getIsArray()) {

				_stringBuilder.append("{");

				List<SOAElement> collectionElements = element.getChildElements();

				if (collectionElements != null) {

					_stringBuilder.append("'fields':[");

					List<String> fields = _fieldsMap.get(elementName);

					for (int i = 0; i < fields.size(); i++) {

						_stringBuilder.append("'" + jsonEscape(fields.get(i)) + "'");

						if (i < fields.size() - 1) _stringBuilder.append(",");

					}

					_stringBuilder.append("],rows:[");

					for (int i = 0; i < collectionElements.size(); i ++) {

						SOAElement collectionElement = collectionElements.get(i);

						List<SOAElement> childElements = collectionElement.getChildElements();

						_stringBuilder.append("[");

						int j = 0; // element pos
						int o = 0; // offset

						if (childElements != null && childElements.size() > 0) {

							for (j = 0; j < childElements.size(); j++) {

								SOAElement childElement = childElements.get(j);

								// get the name of the child element
								String childElementName = childElement.getName();

								// get the position of the child element
								int childElementPos = fields.indexOf(childElementName);

								// if there are elements before us, that we haven't printed yet
								if (childElementPos > j + o) {
									int k;
									// add missing elements
									for (k = 0; k < childElementPos - j - o; k++) _stringBuilder.append("null,");
									// increment offset
									o += k;
								}

								if (childElement.getChildElements() == null || childElement.getChildElements().size() == 0) {

									_stringBuilder.append("'" + jsonEscape(childElement.getValue()) + "'");

								} else {

									append(childElement);

								}

								if (j < childElements.size() - 1) _stringBuilder.append(",");

							}

						}

						// if there are elements after us that we haven't printed yet
						if (j + o < fields.size()) {
							// if we had no elements
							if (j == 0) {
								// add a null without a leading comma
								_stringBuilder.append("null");
								// move j on 1
								j++;
							}
							// add missing elements
							for (int k = j + o; k < fields.size(); k++) {
								// add null with leading comma
								_stringBuilder.append(",null");
							}
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

				if (childElements != null) {

					_stringBuilder.append("'fields':[");

					List<String> fields = _fieldsMap.get(elementName);

					for (int i = 0; i < fields.size(); i++) {

						_stringBuilder.append("'" + jsonEscape(fields.get(i)) + "'");

						if (i < fields.size() - 1) _stringBuilder.append(",");

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

					_stringBuilder.append("]]");

				}

				_stringBuilder.append("}");

			}

		}

		@Override
		public String write() {
			SOAElement rootElement = _soaData.getRootElement();
			if (rootElement == null) {
				return "{}";
			} else {
				// make a new string builder which we will print to
				_stringBuilder = new StringBuilder();
				// parse the elements to build the fields lists
				parse(rootElement);
				// append the elements
				append(rootElement);
				// return the string
				return _stringBuilder.toString();
			}
		}

	}

	// shared public static methods

	private static String jsonEscape(String value) {
		if (value == null) {
			return value;
		} else {
			// the order of these is very important - not sure they're 100% correct, especially as far as apostrophes relative to the backslashes are concerned
			return value.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n");
		}
	}

}