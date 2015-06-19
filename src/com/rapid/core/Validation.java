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

package com.rapid.core;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Validation {

	// instance variables
	
	private String _type, _regEx, _message, _javaScript;
	private boolean _passHidden, _allowNulls;
	
	// properties
	
	public String getType() { return _type; }
	public void setType(String type) { _type = type; }
	
	public boolean getPassHidden() { return _passHidden; }
	public void setPassHidden(boolean passHidden) { _passHidden = passHidden; }
	
	public boolean getAllowNulls() { return _allowNulls; }
	public void setAllowNulls(boolean allowNulls) { _allowNulls = allowNulls; }
	
	public String getRegEx() { return _regEx; }
	public void setRegEx(String regEx) { _regEx = regEx; }
	
	public String getMessage() { return _message; }
	public void setMessage(String message) { _message = message; }
	
	public String getJavaScript() { return _javaScript; }
	public void setJavaScript(String javaScript) { _javaScript = javaScript; }
	
	
	// constructors
	
	public Validation() {};
	
	public Validation(String type, boolean passHidden, boolean allowNulls, String regEx, String message, String javaScript) {
		_type = type;
		_passHidden = passHidden;
		_allowNulls = allowNulls;
		_regEx = regEx;
		_message = message;
		_javaScript = javaScript;
	}
	
}
