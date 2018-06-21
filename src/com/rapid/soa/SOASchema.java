/*

Copyright (C) 2018 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.soa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.rapid.soa.SOAElementRestriction.ExistsRestriction;
import com.rapid.soa.SOAElementRestriction.MaxOccursRestriction;
import com.rapid.soa.SOAElementRestriction.MinOccursRestriction;
import com.rapid.soa.SOAElementRestriction.NameRestriction;
import com.rapid.soa.SOAElementRestriction.TypeRestriction;

public class SOASchema {

	// public finals

	public static final int NONE = 0;
	public static final int STRING = 1;
	public static final int INTEGER = 2;
	public static final int DECIMAL = 3;
	public static final int DATE = 4;
	public static final int DATETIME = 5;
	public static final int BOOLEAN = 6;

	// exception class

	public static class SOASchemaException extends Exception {

		private static final long serialVersionUID = 1010L;

		String _message;
		Exception _ex;

		public SOASchemaException(String message) {
			_message = message;
		}

		public SOASchemaException(Exception ex) {
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
			return _ex;
		}

	}

	public static class SOASchemaElement {

		// instance variables

		private String _id, _name, _field;
		private int _dataType;
		private boolean _isArray, _isChoice, _isOptional;
		private List<SOAElementRestriction> _restrictions;
		private List<SOASchemaElement> _childSchemaElements;

		// properties

		public String getId() { return _id; }
		public void setId(String id) { _id = id; }

		public String getName() { return _name; }
		public void setName(String name) { _name = name; }

		public String getField() { return _field; }
		public void setField(String field) { _field = field; }

		public int getDataType() { return _dataType; }
		public void setDataType(int dataType) { _dataType = dataType; }

		public boolean getIsArray() { return _isArray; }
		public void setIsArray(boolean isArray) { _isArray = isArray; }

		public boolean getIsChoice() { return _isChoice; }
		public void setIsChoice(boolean isChoice) { _isChoice = isChoice; }

		public List<SOASchemaElement> getChildElements() { return _childSchemaElements; }
		public void setChildElements(List<SOASchemaElement> childSchemaElements ) { _childSchemaElements = childSchemaElements; }

		public List<SOAElementRestriction> getRestrictions() { return _restrictions; }
		public void setRestrictions(List<SOAElementRestriction> restrictions) { _restrictions = restrictions; }

		public boolean getIsOptional() { return _isOptional; }

		// constructors

		public SOASchemaElement() {};

		public SOASchemaElement(String id, String name) {
			_id = id;
			_name = name;
		}

		public SOASchemaElement(String id, String name, int dataType) {
			_id = id;
			_name = name;
			_dataType = dataType;
		}

		public SOASchemaElement(String id, String name, boolean isArray) {
			_id = id;
			_name = name;
			_isArray = isArray;
		}

		// instance methods

		public String getNameArrayCheck() {
			if (_isArray) return _name + "Array";
			return _name;
		}

		public SOASchemaElement addRestriction(SOAElementRestriction restriction) {
			if (_restrictions == null) _restrictions = new ArrayList<SOAElementRestriction>();
			_restrictions.add(restriction);
			if (restriction instanceof SOAElementRestriction.MinOccursRestriction) {
				SOAElementRestriction.MinOccursRestriction minOccurs = (MinOccursRestriction) restriction;
				if (minOccurs.getValue() < 1) _isOptional = true;
			}
			return this;
		}

		public SOASchemaElement addChildElement(String ElementName) {
			if (_childSchemaElements == null) _childSchemaElements = new ArrayList<SOASchemaElement>();
			SOASchemaElement childSchemaElement = new SOASchemaElement(_id + "." + _childSchemaElements.size(), ElementName);
			_childSchemaElements.add(childSchemaElement);
			return childSchemaElement;
		}

		public SOASchemaElement addChildElement(String ElementName, int dataType) {
			if (_childSchemaElements == null) _childSchemaElements = new ArrayList<SOASchemaElement>();
			SOASchemaElement childSchemaElement = new SOASchemaElement(_id + "." + _childSchemaElements.size(), ElementName, dataType);
			_childSchemaElements.add(childSchemaElement);
			return childSchemaElement;
		}

		public SOASchemaElement addChildElement(String ElementName, boolean isArray) {
			if (_childSchemaElements == null) _childSchemaElements = new ArrayList<SOASchemaElement>();
			SOASchemaElement childSchemaElement = new SOASchemaElement(_id + "." + _childSchemaElements.size(), ElementName, isArray);
			_childSchemaElements.add(childSchemaElement);
			return childSchemaElement;
		}

		public void validate(SOAElement element) throws SOASchemaException {

			// validate that the element is not null (this should never be provided explicitly)
			_existsRestriction.validate(this, element);

			// validate the name first (this should never be provided explicitly)
			_defaultNameRestriction.validate(this, element);

			// validate the type (this should never be provided explicitly)
			_defaultTypeRestriction.validate(this, element);

			// check restrictions on this level
			if (_restrictions != null) {
				for (SOAElementRestriction restriction : _restrictions) {
					if (!restriction.checkAtParent()) restriction.validate(this, element);
				}
			}

			// check for child restrictions
			if (_childSchemaElements != null) {

				// choice is a bit different
				if (_isChoice) {

					// check that we have 1 child and 1 child only
					if (element.getChildElements().size() != 1) {
						throw new SOASchemaException("Choice restriction - \" Only 1 element can be provided under \"" + element.getName() + "\".");
					}

				} else {

					for (SOASchemaElement childSchemaElement : _childSchemaElements) {
						// retain whether these restrictions (which are checked at parent level have bee provided)
						boolean minOccursValidated = false;
						boolean maxOccursValidated = false;
						// get the list of explicitly set restrictions
						List<SOAElementRestriction> childRestrictions = childSchemaElement.getRestrictions();
						// check we had some restrictions
						if (childRestrictions != null) {
							// loop the restrictions
							for (SOAElementRestriction restriction : childRestrictions) {
								// we're now asking the parent level to check restrictions on it's children
								if (restriction.checkAtParent()) {
									// validate
									restriction.validate(childSchemaElement, element);
									// retain if minOccurs
									if (restriction.getClass() == MinOccursRestriction.class) minOccursValidated = true;
									// retain if maxOccurs (also no need to check manually if an array as unbounded is added for us)
									if (restriction.getClass() == MaxOccursRestriction.class || childSchemaElement.getIsArray()) maxOccursValidated = true;

								}
							}
						}

						// if we've not validated minOccurs, default is 1 so do so now
						if (!minOccursValidated) _defaultMinOccursRestriction.validate(childSchemaElement, element);
						// if we've not validated maxOccurs, default is 1 so do so now
						if (!maxOccursValidated) _defaultMaxOccursRestriction.validate(childSchemaElement, element);

					}
				}
			}
		}

		@Override
		public String toString() {

			String ElementString = "";

			for (int i = 0; i < _id.split("\\.").length - 1; i++) ElementString += " ";

			ElementString += _name;
			if (_isArray) ElementString += "Array[]";
			ElementString += " " + _id;

			if (_restrictions != null) {
				ElementString += " Restrictions : ";
				for (int i = 0; i < _restrictions.size(); i++) {
					ElementString += _restrictions.get(i).toString();
					if (i < _restrictions.size() - 1) ElementString += ", ";
				}
			}

			ElementString += "\n";

			if (_childSchemaElements != null) {
				for (SOASchemaElement ElementSchema : _childSchemaElements) {
					ElementString += ElementSchema.toString();
				}
			}
			return  ElementString;
		}

	}

	private static SOAElementRestriction _existsRestriction;
	private static SOAElementRestriction _defaultNameRestriction;
	private static SOAElementRestriction _defaultTypeRestriction;
	private static SOAElementRestriction _defaultMinOccursRestriction;
	private static SOAElementRestriction _defaultMaxOccursRestriction;

	private SOASchemaElement _rootSchemaElement;
	private HashMap<String,SOASchemaElement> _elements;

	public SOASchema() {
		// instantiate reusable default restrictions
		_existsRestriction = new ExistsRestriction();
		_defaultNameRestriction = new NameRestriction();
		_defaultTypeRestriction = new TypeRestriction();
		_defaultMinOccursRestriction = new MinOccursRestriction(1);
		_defaultMaxOccursRestriction = new MaxOccursRestriction(1);
	};

	public SOASchema(String rootElementName) {
		// call parameterless constructor
		this();
		// create and set the provided root element
		_rootSchemaElement = new SOASchemaElement("0", rootElementName);
	}

	public SOASchema(String rootElementName, boolean isArray) {
		// call parameterless constructor
		this();
		// create and set the provided root element
		_rootSchemaElement = new SOASchemaElement("0", rootElementName, isArray);
	}

	public SOASchemaElement getRootElement() { return _rootSchemaElement; }
	public void setRootElement(SOASchemaElement rootSchemaElement) {
		// retain the new root element
		_rootSchemaElement = rootSchemaElement;
		// changing the root element means all other elements need re-hashing
		_elements = null;
	}

	// called recursively to build a map of element id's to elements for faster retrieval
	private void hashElement(SOASchemaElement element) {
		_elements.put(element.getId(), element);
		List<SOASchemaElement> childElements = element.getChildElements();
		if (childElements != null) {
			for (SOASchemaElement childElement : childElements) {
				hashElement(childElement);
			}
		}
	}

	// rebuilds the map of element id's to elements
	private void hashElements() {
		_elements = new HashMap<String,SOASchemaElement>();
		hashElement(_rootSchemaElement);
	}

	public SOASchemaElement getElementById(String currentElementId) {
		SOASchemaElement element = null;
		// if the hashmap is not initialised do so now
		if (_elements == null) hashElements();
		// try and get the element
		element = _elements.get(currentElementId);
		// if we didn't find the element on the first pass
		if (element == null) {
			// rebuild the hashmap
			hashElements();
			// try again
			element = _elements.get(currentElementId);
		}
		// return
		return _elements.get(currentElementId);
	}

	public SOASchemaElement addChildElement(String elementName) {
		_elements = null;
		return _rootSchemaElement.addChildElement(elementName);
	}

	public SOASchemaElement addChildElement(String elementName, int dataType) {
		_elements = null;
		return _rootSchemaElement.addChildElement(elementName, dataType);
	}

	public SOASchemaElement addChildElement(String elementName, boolean isArray) {
		_elements = null;
		return _rootSchemaElement.addChildElement(elementName, isArray);
	}

	// static methods

	public static String getDataTypeName(int dataType) {
		// this method is used in the restrictions and the schema generation
		switch (dataType) {
		case STRING : return "string";
		case INTEGER : return "integer";
		case DECIMAL : return "decimal";
		case DATE : return "date";
		case DATETIME : return "dateTime";
		case BOOLEAN : return "boolean";
		default : return "unknown";
		}
	}

}
