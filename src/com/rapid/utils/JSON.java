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

package com.rapid.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSON {

	// turns a JSONArray, either from a single member, or muliple if they exist - solves the issue when creating JSON from XML if there is only one member
	public static JSONArray getJSONArray(JSONObject jsonObject, String memberName) throws JSONException {

		// if we got one
		if (jsonObject != null) {
			// look for an array
			JSONArray jsonArray = jsonObject.optJSONArray(memberName);
			// null check
			if (jsonArray == null) {
				// look for a single object
				Object jsonMemberObject = jsonObject.opt(memberName);
				// if we got one
				if (jsonMemberObject != null) {
					// create an empty array
					jsonArray = new JSONArray();
					// add member
					jsonArray.put(jsonMemberObject);
				}
			}
			// return the array
			return jsonArray;
		}
		// no object
		return null;
	}

	// gets a JSONObject if an attribute if of a specified type
	public static String getStringWithAttributeValue(Object object, String attribute, String value) throws JSONException {

		// if we got one
		if (object != null) {
			// if the object is a json object
			if (object instanceof JSONObject) {
				// cast
				JSONObject jsonObject = (JSONObject) object;
				// look for the attribute
				String objectAttribute = jsonObject.optString(attribute, null);
				// check we got one
				if (objectAttribute != null) {
					// check it matches our value
					if (objectAttribute.equals(value)) {
						// extract and send string
						return jsonObject.optString("content", null);
					} else {
						// does not match specified attribute value, return null
						return null;
					}
				}
			}
			return object.toString();
		}
		// no object, or no attribute
		return null;
	}

	// turns a jsonArray into a String array
	public static String[] getStringArray(JSONArray jsonArray) throws JSONException {

		String[] strings = null;

		if (jsonArray != null) {

			strings = new String[jsonArray.length()];

			for (int i = 0; i < jsonArray.length(); i++) {

				strings[i] = jsonArray.getString(i);

			}

		}

		return strings;

	}

}
