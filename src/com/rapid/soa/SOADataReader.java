/*

Copyright (C) 2017 - Gareth Edwards / Rapid Information Systems

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.rapid.soa.SOAElementRestriction.MinOccursRestriction;
import com.rapid.soa.SOASchema.SOASchemaElement;
import com.rapid.soa.SOASchema.SOASchemaException;

public interface SOADataReader {

	// interface methods

	public SOAData read(String string) throws SOAReaderException;

	public SOAData read(InputStream stream) throws SOAReaderException;

	public String getAuthentication();


	// exception class

	public class SOAReaderException extends Exception {

		private static final long serialVersionUID = 1010L;

		Exception _ex;

		public SOAReaderException(Exception ex) {
			_ex = ex;
		}

		@Override
		public String getLocalizedMessage() {
			return _ex.getLocalizedMessage();
		}

		@Override
		public String getMessage() {
			return _ex.getMessage();
		}

		@Override
		public Throwable getCause() {
			return _ex;
		}

	}

	// implementing classes

	/*
	 *  SOAXMLReader
	 *
	 *  Overrides SOAXMLContentHandler to build the SOA as the XML is parsed
	 *
	 */

	public class SOAXMLReader implements SOADataReader {

		// Not sure this class is threadsafe useful guide: http://stackoverflow.com/questions/3439485/java-and-xml-jaxp-what-about-caching-and-thread-safety

		private SAXParserFactory _spf;
		private SAXParser _saxParser;
		private XMLReader _xmlReader;
		private SOAXMLContentHandler _soaXMLContentHandler;

		private static class SOAXMLContentHandler implements ContentHandler {

			private SOASchema _soaSchema;
			private int _currentColumn,  _currentRow, _previousColumn;
			private SOAElement _currentElement;
			private String _root, _currentElementId, _authentication;
			private boolean _rootFound, _ignoreHeader;

			private List<String> _columnElementIds = new ArrayList<String>();
			private List<Integer> _columnRows = new ArrayList<Integer>();
			private List<SOAElement> _columnParents = new ArrayList<SOAElement>();

			public SOAXMLContentHandler(SOASchema soaSchema, String root) {
				// retain the schema
				_soaSchema = soaSchema;
				// retain the root, we'll check it on start
				_root = root;
			}

			public SOAElement getRootElement() {
				return _currentElement;
			}

			public SOASchema getSOASchema() {
				return _soaSchema;
			}

			public String getAuthentication() {
				return _authentication;
			}

			@Override
			public void characters(char[] chars, int start, int length) throws SAXException {
				// only if we have a current element
				if (_ignoreHeader) {
					// instantiate a string using the char array we've been given
					String value = new String(chars, start, length).trim();
					if (value.length() > 0) _authentication = value;
				} else if (_currentElement != null) {
					// instantiate a string using the char array we've been given
					String value = new String(chars, start, length);
					// get any current value
					String currentValue = _currentElement.getValue();
					// check if we have a value already
					if (currentValue == null) {
						// if not set value (note the trim)
						_currentElement.setValue(value.trim());
					} else {
						// if so append this to current as SAX may call this multiple times for large values
						_currentElement.setValue(currentValue + value.trim());
					}
				}
			}

			@Override
			public void startDocument() throws SAXException {
				// reset all our counters when we first start the document
				_currentColumn = 0;
				_currentRow = 0;
				_currentElement = null;
				_currentElementId = "";
				_previousColumn = -1;
				_columnElementIds.clear();
				_columnRows.clear();
				_columnParents.clear();
				_ignoreHeader = false;
				_authentication = null;
				// check whether we got a root for us to start at
				if (_root == null) {
					// no explicit root, start at the very beginning
					_rootFound = true;
				} else if (_root.length() == 0) {
					// no explicit root either, start at the very beginning
					_rootFound = true;
				}
			}

			@Override
			public void endDocument() throws SAXException {
				// retain the root in the currentElement (if we got one)
				if (_columnParents.size() > 0) {
					_currentElement = _columnParents.get(0);
				} else {
					_currentElement = null;
				}
				// save some memory now we're done
				_columnElementIds.clear();
				_columnRows.clear();
				_columnParents.clear();

				// if we have a schema
				if (_soaSchema != null) {
					// try and validate it
					try {
						_soaSchema.getRootElement().validate(_currentElement);
					} catch (SOASchemaException ex) {
						throw new SAXException(ex);
					}
				}

			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

				// ignore all elements pertaining to the envelope
				if ("http://schemas.xmlsoap.org/soap/envelope/".equals(uri)) {

					// if this is the header element start the header ignore
					if ("Header".equals(localName)) _ignoreHeader = true;

				} else if (!_ignoreHeader) {

					// if we haven't found the root we want yet
					if (!_rootFound) {
						// check both local and qualified names for if this is the root we want
						if (localName.equals(_root) || qName.equals(_root)) {
							_rootFound = true;
							// reset the current column to 0 in case the root we want is not the first child
							_currentColumn = 0;
						}
					}

					// if we've found our root!
					if (_rootFound) {

						// make a new branch for this element
						_currentElement = new SOAElement(localName);

						// reset or resume the row counter if the column is different
						if (_previousColumn == _currentColumn) {
							_currentRow ++;
						} else {

							// add a branch id for this column if required
							if (_currentColumn > _columnElementIds.size() - 1) {
								// check root or further down
								if (_currentColumn == 0) {
									// root is simple
									_columnElementIds.add("0");
								} else {
									// other columns are the most recent parent with an extra 0
									_columnElementIds.add(_columnElementIds.get(_currentColumn - 1) + ".0");
								}
							}

							// if we've not seen this column before
							if (_currentColumn > _columnParents.size() - 1) {
								// add a parent node
								_columnParents.add(_currentElement);
							}

							// add row counter for this column if required
							if (_currentColumn > _columnRows.size() - 1) {
								// add a counter for the number of rows for this column
								_columnRows.add(0);
								// reset row counter
								_currentRow = 0;
							} else {
								// fetch in the current row for this column and inc
								_currentRow = _columnRows.get(_currentColumn) + 1;
							}

							// remember this column
							_previousColumn = _currentColumn;
						}

						// if not root add this element as a child to the parent is the prior column
						if (_currentColumn == 0) {

							_currentElementId = "0";

						} else {

							// get the parent of this column
							SOAElement parentElement = _columnParents.get(_currentColumn - 1);
							// set parent of this branch
							_currentElement.setParentElement(parentElement);
							// add this child node to its parent for cross reference
							parentElement.addChildElement(_currentElement);

							// set the current branch id from the parent
							_currentElementId = _columnElementIds.get(_currentColumn - 1);
							// inc the current branch id if not an array
							if (!parentElement.getIsArray()) _currentElementId += "." + _currentRow;

							// retain the current branch id
							_columnElementIds.set(_currentColumn, _currentElementId);
							// retain the current row
							_columnRows.set(_currentColumn, _currentRow);
							// retain the parent of this column
							_columnParents.set(_currentColumn, _currentElement);

						}

						// get the parent element
						SOAElement parentElement = _currentElement.getParentElement();

						// if we have a parent for this column already (a proxy for whether it's the second or more peer)
						if (parentElement != null) {
							// if parent is not an array already
							if (!parentElement.getIsArray()) {
								// if we are on the second child and beyond the root
								if (parentElement.getChildElements().size() > 1 && _currentColumn > 0 && _currentRow > 0) {
									// if the element at this column has the same name as what we had previously for the column
									if (localName.equals(_columnParents.get(_currentColumn).getName())) {
										// if the name is the same as the first element
										if (localName.equals(parentElement.getChildElements().get(0).getName())) {
											// set array flag to true
											parentElement.setIsArray(true);
										}
									}
								}
							} else {
								// anything that ends in "Array" is an array
								if (localName.endsWith("Array") || qName.endsWith("Array")) _currentElement.setIsArray(true);
							}
						}

						// inc the column
						_currentColumn ++;

					} // root found

				} // not soap envelope

			}

			private SOASchemaElement getSchemaElementById(String id, String elementName) {

				// retrieve the schema for this element
				SOASchemaElement schemaElement = _soaSchema.getElementById(id);

				// check we got one
				if (schemaElement != null) {

					// if the name's are different
					if (!elementName.equals(schemaElement.getName())) {

						// assume this element does not have minOccurs="0"
						boolean minOccursZero = false;
						// check restrictions
						if (schemaElement.getRestrictions() != null) {
							// loop restrictions
							for (SOAElementRestriction restriction : schemaElement.getRestrictions()) {
								// if minOccurs
								if (restriction.getClass() == MinOccursRestriction.class) {
									// cast to minOccursRestriction
									MinOccursRestriction minOccursRestriction = (MinOccursRestriction) restriction;
									// check value
									if (minOccursRestriction.getValue() == 0) minOccursZero = true;
									// we're done
									break;
								}

							}

						}
						// if the element at this position had minOccurs=0
						if (minOccursZero) {

							// fetch the parent element
							SOAElement parentElement =  _columnParents.get(_currentColumn - 1);

							// get the current element index from the id
							int index = getIdIndex(id);

							// add an empty element to represent the optional one
							parentElement.addChildElement(index, new SOAElement(schemaElement.getName()));

							// increment the index and add back to make the new id
							String nextId = incId(id);

							// update the list with this position
							_columnElementIds.set(_currentColumn, nextId);

							// fetch the next element
							schemaElement = getSchemaElementById(nextId, elementName);

						}

					}

				}

				return schemaElement;

			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {

				// ignore all elements pertaining to the envelope

				if ("http://schemas.xmlsoap.org/soap/envelope/".equals(uri)) {

					// if this is the end of the header element we can now stop ignoring it
					if ("Header".equals(localName)) _ignoreHeader = false;

				} else if (!_ignoreHeader) {

					// go back one column
					_currentColumn --;

					// validate this element if this column is the root or above and there is a schema
					if (_currentColumn >= 0 && _soaSchema != null) {

						// retrieve the last branch id for this column
						String currentElementId = _columnElementIds.get(_currentColumn);

						SOASchemaElement schemaElement = getSchemaElementById(currentElementId, localName);

						// check we found one
						if (schemaElement != null) {

							// retrieve the element
							SOAElement element = _columnParents.get(_currentColumn);

							// validate against last branch in this column
							try {
								// try and validate it
								schemaElement.validate(element);
							} catch (SOASchemaException e) {
								throw new SAXException(e);
							}

							// close the array if required
							if (schemaElement.getIsArray()) element.closeArray();

						} else {
							throw new SAXException("Element \"" + localName + "\" not recognised at column " + (_currentColumn + 1) + ", row " + (_currentRow + 1));
						}

					}

				}

			}

			@Override
			public void startPrefixMapping(String prefix, String uri) throws SAXException {}

			@Override
			public void endPrefixMapping(String prefix) throws SAXException {}

			@Override
			public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}

			@Override
			public void processingInstruction(String target, String data) throws SAXException {}

			@Override
			public void setDocumentLocator(Locator locator) {}

			@Override
			public void skippedEntity(String name) throws SAXException {}

		}

		protected static String incId(String id) {

			int seperatorPos = id.lastIndexOf(".");
			if (seperatorPos > 0) {
				int newIndex = Integer.parseInt(id.substring(seperatorPos + 1)) + 1;
				return id.substring(0, seperatorPos + 1) + newIndex;
			} else {
				int newId = Integer.parseInt(id) + 1;
				return Integer.toString(newId);
			}

		}

		protected static int getIdIndex(String id) {

			int seperatorPos = id.lastIndexOf(".");
			if (seperatorPos > 0) {
				return Integer.parseInt(id.substring(seperatorPos + 1));
			} else {
				return Integer.parseInt(id);
			}

		}

		private void init(SOASchema soaSchema, String root) throws ParserConfigurationException, SAXException {
			_spf = SAXParserFactory.newInstance();
			_spf.setNamespaceAware(true);
			_saxParser = _spf.newSAXParser();
			_soaXMLContentHandler = new SOAXMLContentHandler(soaSchema, root);
			_xmlReader = _saxParser.getXMLReader();
			_xmlReader.setContentHandler(_soaXMLContentHandler);
		}

		// constructors

		public SOAXMLReader() throws ParserConfigurationException, SAXException {
			init(null, null);
		}

		public SOAXMLReader(SOASchema soaSchema) throws ParserConfigurationException, SAXException {
			init(soaSchema, null);
		}

		public SOAXMLReader(String root) throws ParserConfigurationException, SAXException {
			init(null, root);
		}

		public SOAXMLReader(SOASchema soaSchema, String root) throws ParserConfigurationException, SAXException {
			init(soaSchema, root);
		}

		// public methods

		@Override
		public SOAData read(String xmlString) throws SOAReaderException {

			try {

				// parse xml document through reader, so it's navigation goes through our overridden content handler
				_xmlReader.parse(new InputSource(new ByteArrayInputStream(xmlString.getBytes())));

			} catch (Exception ex) {

				throw new SOAReaderException(ex);

			}

			// return a datatree based on the current branch which will now be the root
			return new SOAData(_soaXMLContentHandler.getRootElement(), _soaXMLContentHandler.getSOASchema());

		}

		@Override
		public SOAData read(InputStream xmlStream) throws SOAReaderException {

			try {

				// parse xml document through reader, so it's navigation goes through our overridden content handler
				_xmlReader.parse(new InputSource(xmlStream));

			} catch (Exception ex) {

				throw new SOAReaderException(ex);

			}

			// return a datatree based on the current branch which will now be the root
			return new SOAData(_soaXMLContentHandler.getRootElement(), _soaXMLContentHandler.getSOASchema());

		}

		@Override
		public String getAuthentication() {
			return _soaXMLContentHandler.getAuthentication();
		}

	}

	/*
	 *  SOAJSONReader
	 *
	 *  Overrides JSONTokener and JSONObject to build the SOA as the JSON is parsed
	 *
	 */

	public class SOAJSONReader implements SOADataReader {

		private static SOASchema _soaSchema;

		private int _currentColumn;
		private int _currentRow;
		private SOAElement _currentElement;
		private String _currentElementId;
		private int _previousColumn;
		private String _currentKey;
		private String _authentication;

		private ArrayList<String> _columnElementIds = new ArrayList<String>();
		private ArrayList<Integer> _columnRows = new ArrayList<Integer>();
		private ArrayList<SOAElement> _columnParents = new ArrayList<SOAElement>();

		protected static String incId(String id) {

			int seperatorPos = id.lastIndexOf(".");
			if (seperatorPos > 0) {
				int newIndex = Integer.parseInt(id.substring(seperatorPos + 1)) + 1;
				return id.substring(0, seperatorPos + 1) + newIndex;
			} else {
				int newId = Integer.parseInt(id) + 1;
				return Integer.toString(newId);
			}

		}

		protected static int getIdIndex(String id) {

			int seperatorPos = id.lastIndexOf(".");
			if (seperatorPos > 0) {
				return Integer.parseInt(id.substring(seperatorPos + 1));
			} else {
				return Integer.parseInt(id);
			}

		}

		private void newElement(boolean isParentArray) {

            // make a new branch for this element
			_currentElement = new SOAElement(_currentKey);

			// reset or resume the row counter if the column is different
			if (_previousColumn == _currentColumn) {
				_currentRow ++;
			} else {
				// add a branch id for this column if required
				if (_currentColumn > _columnElementIds.size() - 1) {
					// check root or further down
					if (_currentColumn == 0) {
						// root is simple
						_columnElementIds.add("0");
					} else {
						// other columns are the most recent parent with an extra 0
						_columnElementIds.add(_columnElementIds.get(_currentColumn - 1) + ".0");
					}
				}

				// add row counter for this column if required
				if (_currentColumn > _columnRows.size() - 1) {
					// add a counter for the number of rows for this column
					_columnRows.add(0);
					// reset row counter
					_currentRow = 0;
				} else {
					// fetch in the current row for this column and inc
					_currentRow = _columnRows.get(_currentColumn) + 1;
				}

				// add a parent node for this column if required
				if (_currentColumn > _columnParents.size() - 1) _columnParents.add(_currentElement);

				// remember this column
				_previousColumn = _currentColumn;
			}

			// if not root add this element as a child to the parent is the prior column
			if (_currentColumn == 0) {
				_currentElementId = "0";
			} else {

				// get the parent of this column
				SOAElement parentElement = _columnParents.get(_currentColumn - 1);
				// set parent of this branch
				_currentElement.setParentElement(parentElement);
				// add this child node to its parent for cross reference
				parentElement.addChildElement(_currentElement);

				// set the current branch id from the parent
				_currentElementId = _columnElementIds.get(_currentColumn - 1) + "." + _currentRow;

				// retain the current branch id
				_columnElementIds.set(_currentColumn, _currentElementId);
				// retain the current row
				_columnRows.set(_currentColumn, _currentRow);
				// retain the parent of this column
				_columnParents.set(_currentColumn, _currentElement);

				// update parent branch if this is an array member (slightly different from in the xml)
				if (isParentArray) {
					parentElement.setIsArray(true);
					parentElement.closeArray();
				}

			}

		}

		private SOASchemaElement getSchemaElementById(String id, String elementName) {

			// retrieve the schema for this element
			SOASchemaElement schemaElement = _soaSchema.getElementById(id);

			// check we got one
			if (schemaElement != null) {

				// if the name's are different
				if (!elementName.equals(schemaElement.getName())) {

					// assume this element does not have minOccurs="0"
					boolean minOccursZero = false;
					// check restrictions
					if (schemaElement.getRestrictions() != null) {
						// loop restrictions
						for (SOAElementRestriction restriction : schemaElement.getRestrictions()) {
							// if minOccurs
							if (restriction.getClass() == MinOccursRestriction.class) {
								// cast to minOccursRestriction
								MinOccursRestriction minOccursRestriction = (MinOccursRestriction) restriction;
								// check value
								if (minOccursRestriction.getValue() == 0) minOccursZero = true;
								// we're done
								break;
							}

						}

					}
					// if the element at this position had minOccurs=0
					if (minOccursZero) {

						// fetch the parent element
						SOAElement parentElement =  _columnParents.get(_currentColumn - 1);

						// get the current element index from the id
						int index = getIdIndex(id);

						// add an empty element to represent the optional one
						parentElement.addChildElement(index, new SOAElement(schemaElement.getName()));

						// increment the index and add back to make the new id
						String nextId = incId(id);

						// update the list with this position
						_columnElementIds.set(_currentColumn, nextId);

						// fetch the next element
						schemaElement = getSchemaElementById(nextId, elementName);

					}

				}

			}

			return schemaElement;

		}

		private void validateElement(String localName) throws JSONException {

			if (_soaSchema != null) {
				// retrieve the last branch id for this column
				String currentElementId = _columnElementIds.get(_currentColumn);
				// retrieve the schema for this branch
				SOASchemaElement schemaElement = getSchemaElementById(currentElementId, localName);
				// check we found one
				if (schemaElement != null) {
					// validate against last branch in this column
					try {
						schemaElement.validate(_columnParents.get(_currentColumn));
					} catch (SOASchemaException e) {
						throw new JSONException(e);
					}
				} else {
					throw new JSONException("Key \"" + localName + "\" not recognised at column " + (_currentColumn + 1) + ", row " + (_currentRow + 1));
				}
			}

		}

		// JSON library overridden classes for fast-fail parsing

		public class SOAJSONTokener extends JSONTokener {

			/*
			* we extend nextValue with overridden JSONObect and JSONArray
			* we also need access to their putOnce methods when called recursively
			*/

			@Override
			public Object nextValue() throws JSONException {
		        char c = this.nextClean();
		        String string;

		        switch (c) {
		            case '"':
		            case '\'':
		            	string = this.nextString(c);
		                return string;
		            case '{':
		                this.back();
		                SOAJSONObject soaJsonObject = new SOAJSONObject(this);
		                return soaJsonObject;
		            case '[':
		                this.back();
		                SOAJSONArray soaJsonArray = new SOAJSONArray(this);
		                return soaJsonArray;
		        }

		        /*
		         * Handle unquoted text. This could be the values true, false, or
		         * null, or it can be a number. An implementation (such as this one)
		         * is allowed to also accept non-standard forms.
		         *
		         * Accumulate characters until we reach the end of the text or a
		         * formatting character.
		         */

		        StringBuffer sb = new StringBuffer();
		        while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
		            sb.append(c);
		            c = this.next();
		        }
		        this.back();

		        string = sb.toString().trim();
		        if ("".equals(string)) throw this.syntaxError("Missing value");

		        return JSONObject.stringToValue(string);
		    }

			public SOAJSONTokener(String string) {
				super(string);
			}

			public SOAJSONTokener(InputStream stream) throws JSONException {
				super(stream);
			}

		}

		public class SOAJSONObject extends JSONObject {

			private SOAElement _rootElement;

			public SOAElement getRootElement() {
				return _rootElement;
			}

			// override the putOnce as it signals a JSONObject has finished being created so we can set its value and validate it

			@Override
			public JSONObject putOnce(String key, Object value) throws JSONException {

				//System.out.println(key + " : " + value.getClass().getSimpleName());

				if (value.getClass().equals(String.class)) _currentElement.setValue((String) value);

		        if (key != null && value != null) {
		            if (this.opt(key) != null) {
		                throw new JSONException("Duplicate key \"" + key + "\"");
		            }
		            this.put(key, value);
		        }
		        return this;
		    }

			// we extend the constructor so we can make a TreeElement as soon as the key is identified

			public SOAJSONObject(JSONTokener x) throws JSONException {

				if (x.nextClean() != '{') throw x.syntaxError("A JSONObject text must begin with '{'");

				char c;
		        String key;

		        while (true) {

		            c = x.nextClean();
		            switch (c) {
		            case 0:
		                throw x.syntaxError("A JSONObject text must end with '}'");
		            case '}':
		                return;
		            default:
		                x.back();
		                key = x.nextValue().toString();
		            }

		            // The key is followed by ':'.
		            c = x.nextClean();
		            if (c != ':') throw x.syntaxError("Expected a ':' after a key");

		            //--------------------------------------- This code is particular to creating the SOA -----------------------------------

		            // retain the current key here
		            _currentKey = key;

		            // create a new branch for this key (reused in TreeElementJSONArray) - but not if authentication key at 0,0
		            if (!"authentication".equals(key) || _currentColumn > 0 || _currentRow > 0) newElement(false);

		            // if this is column 0
		            if (_currentColumn < 1) {
			            // check if root element is null
			            if (_rootElement == null) {
			            	// set root to current - easiest if only 1
			            	_rootElement = _currentElement;
			            } else {
			            	// root element has been set by previous activity so check if we know it is an array
			            	if (_rootElement.getIsArray()) {
			            		// all subsequent elements can be added as children to the array
			            		_rootElement.addChildElement(_currentElement);
			            	} else {
			            		// make a special root element to hold the children on the root level
			            		SOAElement rootElement = new SOAElement("root", true);
			            		// add the single root from before
			            		rootElement.addChildElement(_rootElement);
			            		// add this element
			            		rootElement.addChildElement(_currentElement);
			            		// update that the root is the array element we just created
			            		_rootElement = rootElement;
			            	}
			            }
		            }

		            // inc the column
					_currentColumn ++;

		            // get the next object (this can create new columns)
		            Object o = x.nextValue();

		            // special types need casting
		            if (o instanceof Integer) {
		            	int i = (Integer) o;
		            	o = Integer.toString(i);
		            } else  if (o instanceof Double) {
		            	Double d = (Double) o;
		            	o = Double.toString(d);
		            } else if (o instanceof Boolean) {
		            	Boolean b = (Boolean) o;
		            	o = Boolean.toString(b);
		            }

		            // dec the column
					_currentColumn --;

					// if authentication key at position 0/0
					if ("authentication".equals(key) && _currentColumn == 0 && _currentRow == 0) {

						// retain the authentication value
						_authentication = (String) o;

					} else {

			            // validate the current branch
			            validateElement(key);

			            // add the object
			            this.putOnce(key, o);

					}

					//--------------------------------------- End of code particular to creating the SOA -----------------------------------

		            // Pairs are separated by ','.
		            switch (x.nextClean()) {
		            case ';':
		            case ',':
		                if (x.nextClean() == '}') return;
		                x.back();
		                break;
		            case '}':
		                return;
		            default:
		                throw x.syntaxError("Expected a ',' or '}'");
		            }
		        }
			}

		}

		public class SOAJSONArray extends JSONArray {

			private SOAElement _rootElement;

			public SOAElement getRootElement() {

				return _rootElement;

			}

			// we extend the constructor so we can make our own special array branches

			public SOAJSONArray(JSONTokener x) throws JSONException {
				super();

		        if (x.nextClean() != '[') {
		            throw x.syntaxError("A JSONArray text must start with '['");
		        }
		        if (x.nextClean() != ']') {
		            x.back();
		            for (;;) {
		                if (x.nextClean() == ',') {
		                    x.back();
		                    put(JSONObject.NULL);
		                } else {
		                    x.back();

		                    //--------------------------------------- This code is particular to creating the SOA -----------------------------------

		                    // create a new branch for this key (reused in TreeElementJSONObject)
				            newElement(true);

				            // get the next object
				            Object o = x.nextValue();

							// validate the current branch
				            validateElement(_currentKey);

				            // create root if we need one
							if (_rootElement == null) _rootElement = new SOAElement("root", true);

							// put any string values back in the relevant column
							if (o.getClass().equals(String.class)) {
								String s = (String) o;
								_columnParents.get(_currentColumn).setValue(s);
								_rootElement.addChildElement(new SOAElement(_currentKey, s));
							}

							// if this is an object add as children and then close array to make new row
							if (o.getClass().equals(SOAJSONObject.class)) {
								SOAJSONObject soaJsonObject = (SOAJSONObject) o;
								Iterator<String> keys = soaJsonObject.keys();
								while(keys.hasNext()) {
									String key = keys.next();
									_rootElement.addChildElement(new SOAElement(key, soaJsonObject.getString(key)));
								}
								_rootElement.closeArray();

							}

							//--------------------------------------- End of code particular to creating the SOA -----------------------------------

		                }
		                switch (x.nextClean()) {
		                case ',':
		                    if (x.nextClean() == ']') return;
		                    x.back();
		                    break;
		                case ']':
		                    return;
		                default:
		                    throw x.syntaxError("Expected a ',' or ']'");
		                }
		            }
		        }
		    }

		}

		// Constructors

		public SOAJSONReader() {}

		public SOAJSONReader(SOASchema dataTreeSchema) {
			_soaSchema = dataTreeSchema;
		}


		private void reset() {

			// reset all our counters when we first start the read
			_currentColumn = 0;
			_currentRow = 0;
			_currentElement = null;
			_currentElementId = "";
			_previousColumn = -1;
			_columnElementIds.clear();
			_columnRows.clear();
			_columnParents.clear();

		}

		private SOAData read(JSONTokener jsonTokener) throws JSONException {

			reset();

			SOAElement rootElement = null;

			char c = jsonTokener.nextClean();
			jsonTokener.back();

			if (c == '{') {

				SOAJSONObject soaJsonObject = new SOAJSONObject(jsonTokener);

				rootElement = soaJsonObject.getRootElement();

			}

			if (c == '[') {

				SOAJSONArray soaJsonArray = new SOAJSONArray(jsonTokener);

				rootElement = soaJsonArray.getRootElement();

			}

			return new SOAData(rootElement, _soaSchema);

		}

		@Override
		public SOAData read(String string) throws SOAReaderException {

			try {

				return read(new SOAJSONTokener(string));

			} catch (Exception e) {

				throw new SOAReaderException(e);

			}

		}

		@Override
		public SOAData read(InputStream stream) throws SOAReaderException {

			try {

				return read(new SOAJSONTokener(stream));

			} catch (Exception e) {

				throw new SOAReaderException(e);

			}

		}

		@Override
		public String getAuthentication() {
			return _authentication;
		}

	}

}