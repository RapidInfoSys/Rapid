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

package com.rapid.soa;

public class SOAData {

	SOASchema _soaSchema;
	SOAElement _rootElement;
	static int _column;

	public SOAData(SOAElement rootElement) {
		_rootElement = rootElement;
	}

	public SOAData(SOASchema soaSchema) {
		_soaSchema = soaSchema;
	}

	public SOAData(SOAElement rootElement, SOASchema soaSchema) {
		_rootElement = rootElement;
		_soaSchema = soaSchema;
	}

	public SOAElement getRootElement() { return _rootElement; }
	public void setRootElement(SOAElement rootElement) { _rootElement = rootElement; }

	public SOASchema getSchema() { return _soaSchema; }
	public void setSchema(SOASchema soaSchema) { _soaSchema = soaSchema; }

	@Override
	public String toString() {
		_column = 0;
		if (_rootElement == null) {
			return null;
		} else {
			return _rootElement.toString();
		}
	}

}
