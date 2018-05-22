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
import java.util.List;

public class SOAElement {

	private static int _indentCount = 0;

	private String _name, _value;
	private boolean _isArray, _arrayOpen;
	private int _currentArray;
	private SOAElement _parentElement;
	private List<SOAElement> _childElements;

	public String getName() { return _name; }

	public String getValue() { return _value; }
	public void setValue(String value) { _value = value; }

	public boolean getIsArray() {
		return _isArray;
	}

	public void setIsArray(boolean isArray) {
		_isArray = isArray;
		// update the name (required by JSON parser)
		if (_name == null) _name = "";
		int lastIndexOfArray = _name.lastIndexOf("Array");
		if (lastIndexOfArray < 0 || lastIndexOfArray == 0) _name += "Array";
	}

	public SOAElement getParentElement() { return _parentElement; }
	public void setParentElement(SOAElement parentElement) { _parentElement = parentElement; }

	public List<SOAElement> getChildElements() { return _childElements; }
	public void setChildElements(List<SOAElement> childElements) { _childElements = childElements; }

	public SOAElement(String name) {
		_name = name;
	}

	public SOAElement(String name, SOAElement parentElement) {
		_name = name;
		_parentElement = parentElement;
	}

	public SOAElement(String name, String value) {
		_name = name;
		_value = value;
	}

	public SOAElement(String name, boolean isArray) {
		_name = name;
		_isArray = isArray;
	}

	public SOAElement(String name, String value, boolean isArray) {
		_name = name;
		_value = value;
		_isArray = isArray;
	}

	public void addChildElement(SOAElement childNode) {
		// set the parent of the childNode
		childNode.setParentElement(this);
		// instantiate the child elements array if required
		if (_childElements == null) _childElements = new ArrayList<SOAElement>();
		// if this element is an array we need to create and maintain an additional child level for each array member
		if (_isArray) {
			// if the array element is already in place we handle things slightly differently
			boolean gotArrayElement = false;
			// only if we have a parent element
			if (childNode.getParentElement() != null) {
				// if we would have added an array element in this position
				if (childNode.getParentElement().getName().equals(_name) && _name.endsWith("Array")) {
					// just add the child
					_childElements.add(childNode);
					// retain that we don't need an array element
					gotArrayElement = true;
				}
			}
			// if we didn't see an array element already in position
			if (!gotArrayElement) {
				// if the array is closed (which is the signal to create a new array member element)
				if (!_arrayOpen) {
					// add a new array member with this as its parent
					_childElements.add(new SOAElement(_name, this));
					// remember the array is now open so all other children are added to the current array node
					_arrayOpen = true;
				}
				// add the child into current array element
				_childElements.get(_currentArray).addChildElement(childNode);
			}

		} else {
			// much simpler, child can just be added
			_childElements.add(childNode);
		}
	}

	public void addChildElement(int index, SOAElement childNode) {
		// instantiate the child elements array if required
		if (_childElements == null) _childElements = new ArrayList<SOAElement>();
		// add child at specified location
		_childElements.add(index, childNode);
	}

	public void closeArray() {
		// increment the array member position counter
		_currentArray ++;
		// remember the array is now closed
		_arrayOpen = false;
	}

	private String getIndent() {
		String indent = "";
		for (int i = 0; i < _indentCount; i++) indent += " ";
		return indent;
	}

	@Override
	public String toString() {
		String element = _name + (_value == null ? "" : ":" + _value) + (_isArray ? "[]" : "");
		if (_childElements != null) {
			for (SOAElement childElement : _childElements) {
				_indentCount ++;
				element += "\n" + getIndent() + childElement.toString();
				_indentCount --;
			}
		}
		return element;
	}

}
