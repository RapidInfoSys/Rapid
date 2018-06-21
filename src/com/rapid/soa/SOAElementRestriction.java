/*

Copyright (C) 2018 - Gareth Edwards / Rapid Information Systems

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

import java.util.List;

import com.rapid.soa.SOASchema.SOASchemaElement;
import com.rapid.soa.SOASchema.SOASchemaException;

public abstract class SOAElementRestriction {

	// public property

	public String getType() { return this.getClass().getSimpleName(); }

	// abstract methods

	public abstract boolean checkAtParent();
	public abstract String getSchemaRule();
	public abstract void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException;

	// implementation classes (note: these all need manually adding to JAXB in the RapidServletContextListener)

	public static class ExistsRestriction extends SOAElementRestriction {

		// constructors

		public ExistsRestriction() {}

		// instance methods

		public void fail(SOASchemaElement schemaElement) throws SOASchemaException {
			throw new SOASchemaException(schemaElement.getName() + " is expected");
		}

		// overrides

		@Override
		public boolean checkAtParent() {
			return false;
		}

		@Override
		public String getSchemaRule() {
			return "";
		}

		@Override
		public void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException  {
			if (element == null) {
				 fail(schemaElement);
			}
		}

		@Override
		public String toString() {
			return "ExistsRestriction - must be provided";
		}

	}

	public static class NameRestriction extends SOAElementRestriction {

		// constructors

		public NameRestriction() {}

		// instance methods

		public void fail(SOASchemaElement schemaElement, String elementName) throws SOASchemaException {
			throw new SOASchemaException("NameRestriction - \"" + elementName + "\" is not a valid name for an element at this position, \"" + schemaElement.getName() + "\" expected");
		}

		// overrides

		@Override
		public boolean checkAtParent() {
			return false;
		}

		@Override
		public String getSchemaRule() {
			return "";
		}

		@Override
		public void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException  {
			if (element == null) {
				 fail(schemaElement, schemaElement.getName() + " is expected");
			} else if (element.getIsArray()) {
				if (!schemaElement.getName().equals(element.getName().substring(0, element.getName().length() - 5))) fail(schemaElement, element.getName());
			} else if (!schemaElement.getName().equals(element.getName())) fail(schemaElement, element.getName());
		}

		@Override
		public String toString() {
			return "NameRestriction - name must match";
		}

	}

	public static class TypeRestriction extends SOAElementRestriction {

		private static final int[] _monthDays = {31,28,31,30,31,30,31,31,30,31,30,31};

		// constructors

		public TypeRestriction() {}

		// instance methods

		public void failNull(SOASchemaElement schemaElement) throws SOASchemaException {
			throw new SOASchemaException("TypeRestriction - empty elements are not a valid type of " + SOASchema.getDataTypeName(schemaElement.getDataType()) + " for \"" + schemaElement.getName() + "\"");
		}

		public void failType(SOASchemaElement schemaElement, String elementValue) throws SOASchemaException {
			throw new SOASchemaException("TypeRestriction - \"" + elementValue + "\" is not a valid type of " + SOASchema.getDataTypeName(schemaElement.getDataType()) + " for \"" + schemaElement.getName() + "\"");
		}

		public void checkMonth(SOASchemaElement schemaElement, String value) throws SOASchemaException {
			String[] dateParts = value.substring(0,10).split("-");
			int year = Integer.parseInt(dateParts[0]);
			int month = Integer.parseInt(dateParts[1]);
			int day = Integer.parseInt(dateParts[2]);
			boolean leapYear = (year % 4 == 0) && year != 2000;
			if (leapYear && month == 2) {
				if (day > 29) failType(schemaElement, value);
			} else {
				if (day > _monthDays[month-1]) failType(schemaElement, value);
			}
		}

		// overrides

		@Override
		public boolean checkAtParent() {
			return false;
		}

		@Override
		public String getSchemaRule() {
			return "";
		}

		@Override
		public void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException  {

			String value = element.getValue();
			int elementDataType = schemaElement.getDataType();

			if (elementDataType > SOASchema.STRING && value == null) failNull(schemaElement);

			switch (elementDataType) {
			case SOASchema.INTEGER :
				if (!value.matches("^[-]?\\d+$")) failType(schemaElement, value);
				break;
			case SOASchema.DECIMAL :
				if (!value.matches("^[-]?\\d*[.]?\\d*$")) failType(schemaElement, value);
				break;
			case SOASchema.DATE :
				if (!value.matches("^(19|20)\\d\\d[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])$")) failType(schemaElement, value);
				if (value.matches("^(19|20)\\d\\d[-](0[1-9]|1[012])[-](29|30|31)$")) checkMonth(schemaElement, value);
				break;
			case SOASchema.DATETIME :
				if (!value.matches("^(19|20)\\d\\d[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])[T]([01][0-9]|20|21|22|23)[:]([0-5][0-9])[:]([0-5][0-9])[Z]?$")) failType(schemaElement, value);
				if (value.matches("(19|20)\\d\\d[-](0[1-9]|1[012])[-](29|30|31).*?")) checkMonth(schemaElement, value);
				break;
			case SOASchema.BOOLEAN :
				if (!value.matches("^true|false$")) failType(schemaElement, value);
				break;
			}
		}

		@Override
		public String toString() {
			return "Type restriction - type must be as per schema element";
		}

	}

	public static class MinOccursRestriction extends SOAElementRestriction {

		// private instance variables

		private int _value;

		// properties

		public int getValue() { return _value; }
		public void setValue(int value) { _value = value; }

		// constructors

		public MinOccursRestriction() {}

		public MinOccursRestriction(int value) {
			_value = value;
		}

		// overrides

		@Override
		public boolean checkAtParent() {
			return true;
		}

		@Override
		public String getSchemaRule() {
			return "minOccurs=\"" + _value + "\"";
		}

		@Override
		public void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException  {
			int occurs = 0;
			if (schemaElement.getIsArray()) {

				// TODO

			} else {
				// get the child elements
				List<SOAElement> childElements = element.getChildElements();
				// check we had some
				if (childElements != null) {
					// loop them
					for (SOAElement peer : element.getChildElements()) {
						if (peer.getName().equals(schemaElement.getName())) occurs ++;
					}
				}
				if (occurs < _value) throw new SOASchemaException("MinOccursRestriction - Too few of \"" + schemaElement.getName() + "\" under \"" + element.getName() + "\". At least " + _value  + " required");
			}
		}

		@Override
		public String toString() {
			return "MinOccursRestriction - must have at least " + _value;
		}

	}

	public static class MaxOccursRestriction extends SOAElementRestriction {

		// private instance variables

		private int _value;

		// properties

		public int getValue() { return _value; }
		public void setValue(int value) { _value = value; }

		// constructors

		public MaxOccursRestriction() {}

		public MaxOccursRestriction(int value) {
			_value = value;
		}

		// overrides

		@Override
		public boolean checkAtParent() {
			return true;
		}

		@Override
		public String getSchemaRule() {
			return "maxOccurs=\"" + _value + "\"";
		}

		@Override
		public void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException  {
			int occurs = 0;
			if (schemaElement.getIsArray()) {
				// arrays must have their children checked
				if (element.getChildElements() != null) {

					// not sure how to check this any more - may need parent

				} else throw new SOASchemaException("MaxOccursRestriction - \"" + schemaElement.getName() + "Array\" must be provided under \"" + element.getName() + "\".");
			} else {
				// get the list of child elements
				List<SOAElement> childElements = element.getChildElements();
				// check we have some
				if (childElements != null) {
					// loop them
					for (SOAElement peer : element.getChildElements()) {
						if (peer.getName().equals(schemaElement.getName())) occurs ++;
						if (occurs > _value) throw new SOASchemaException("MaxOccursRestriction - Too many of \"" + schemaElement.getName() + "\" under \"" + element.getName() + "\". No more than " + _value  + " allowed");
					}
				}
			}
		}

		@Override
		public String toString() {
			return "MaxOccursRestriction - no more than " + _value;
		}

	}

	public static class MinLengthRestriction extends SOAElementRestriction {

		// private instance variables

		private int _value;

		// properties

		public int getValue() { return _value; }
		public void setValue(int value) { _value = value; }

		// constructors

		public MinLengthRestriction() {}

		public MinLengthRestriction(int value) {
			_value = value;
		}

		// overrides

		@Override
		public boolean checkAtParent() {
			return false;
		}

		@Override
		public String getSchemaRule() {
			return "<xs:minLength value=\"" + _value + "\"/>";
		}

		@Override
		public void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException  {
			String text = element.getValue();
			if (text == null) {
				throw new SOASchemaException("MaxLengthRestriction - \"" + schemaElement.getName() + "\" with value \"" + text + "\" is shorter than " + _value + " required characters");
			} else {
				if (text.length() < _value) throw new SOASchemaException("MaxLengthRestriction - \"" + schemaElement.getName() + "\" with value \"" + text + "\" is shorter than " + _value + " required characters");
			}
		}

		@Override
		public String toString() {
			return "MinLengthRestriction - must be " + _value + " characters or more";
		}

	}

	public static class MaxLengthRestriction extends SOAElementRestriction {

		// private instance variables

		private int _value;

		// properties

		public int getValue() { return _value; }
		public void setValue(int value) { _value = value; }

		// constructors

		public MaxLengthRestriction() {}

		public MaxLengthRestriction(int value) {
			_value = value;
		}

		// overrides

		@Override
		public boolean checkAtParent() {
			return false;
		}

		@Override
		public String getSchemaRule() {
			return "<xs:maxLength value=\"" + _value + "\"/>";
		}

		@Override
		public void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException  {
			String text = element.getValue();
			if (text != null) {
				if (text.length() > _value) throw new SOASchemaException("MaxLengthRestriction - \"" + schemaElement.getName() + "\" with value \"" + text + "\" is longer than " + _value + " allowed characters");
			}
		}

		@Override
		public String toString() {
			return "MaxLengthRestriction - must be " + _value + " characters or less";
		}

	}

	public static class EnumerationRestriction extends SOAElementRestriction {

		// private instance variables

		private String _value;

		// properties

		public String getValue() { return _value; }
		public void setValue(String value) { _value = value; }

		// constructors

		public EnumerationRestriction() {}

		public EnumerationRestriction(String value) {
			_value = value;
		}

		// overrides

		@Override
		public boolean checkAtParent() {
			return false;
		}

		@Override
		public String getSchemaRule() {
			String enumerations = "";
			for (String enumeration : _value.split(",")) {
				enumerations += "<xs:enumeration value=\"" + enumeration + "\"/>";
			}
			return enumerations;
		}

		@Override
		public void validate(SOASchemaElement schemaElement, SOAElement element) throws SOASchemaException  {
			String text = element.getValue();
			if (text == null) {
				throw new SOASchemaException("EnumerationRestriction - \"" + schemaElement.getName() + "\" with value \"" + text + "\" is is not one of required values " + _value);
			} else {
				boolean matchedValue = false;
				for (String enumeration : _value.split(",")) {
					if (enumeration.trim().equals(text)) {
						matchedValue = true;
						break;
					}
				}
				if (!matchedValue) throw new SOASchemaException("EnumerationRestriction - \"" + schemaElement.getName() + "\" with value \"" + text + "\" is is not one of required values " + _value);
			}
		}

		@Override
		public String toString() {
			return "EnumerationRestriction - name must be one of " + _value;
		}

	}

}
