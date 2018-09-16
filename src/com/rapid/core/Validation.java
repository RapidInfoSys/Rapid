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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.actions.Logic.Condition;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Validation {

	// static class for logicMessages
	public static class LogicMessage {

		private List<Condition> _conditions;
		private String _conditionsType, _text;

		public List<Condition> getConditions() { return _conditions; }
		public void setConditions(List<Condition> conditions) { _conditions = conditions; }

		public String getConditionsType() { return _conditionsType; }
		public void setConditionsType(String conditionsType) { _conditionsType = conditionsType; }

		public String getText() { return _text; }
		public void setText(String text) { _text = text; }

	}

	// instance variables

	private String _type, _regEx, _message, _javaScript;
	private List<LogicMessage> _logicMessages;
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

	public List<LogicMessage> getLogicMessages() { return _logicMessages; }
	public void setLogicMessages(List<LogicMessage> logicMessages) { _logicMessages = logicMessages; }

	public String getJavaScript() { return _javaScript; }
	public void setJavaScript(String javaScript) { _javaScript = javaScript; }


	// constructors

	public Validation() {};

	public Validation(String type, boolean passHidden, boolean allowNulls, String regEx, String message, String logicMessages, String javaScript) throws JSONException {
		_type = type;
		_passHidden = passHidden;
		_allowNulls = allowNulls;
		_regEx = regEx;
		_message = message;

		// initialise conditions list
		_logicMessages = new ArrayList<LogicMessage>();
		// if we have any logicMessages
		if (logicMessages != null && logicMessages.length() > 0) {
			// grab conditions from json
			JSONArray jsonLogicMessages = new JSONArray(logicMessages);
			// if we got some
			if (jsonLogicMessages != null) {
				// loop them
				for (int i = 0; i < jsonLogicMessages.length(); i++) {
					// get this message
					JSONObject jsonLogicMessage = jsonLogicMessages.getJSONObject(i);
					// create a logic message for it
					LogicMessage logicMessage = new LogicMessage();
					// look for the conditions
					JSONArray jsonLogicMessageConditions = jsonLogicMessage.optJSONArray("conditions");
					// if there were some
					if (jsonLogicMessageConditions != null && jsonLogicMessageConditions.length() > 0) {
						// make a list of conditions
						List<Condition> conditions = new ArrayList<Condition>();
						// loop the conditions
						for (int j = 0; j < jsonLogicMessageConditions.length(); j++) {
							// create a condition
							Condition condition = new Condition(jsonLogicMessageConditions.getJSONObject(j));
							// add to conditions
							conditions.add(condition);
						}
						// add conditions to logic message
						logicMessage.setConditions(conditions);
					}
					// set the type
					logicMessage.setConditionsType(jsonLogicMessage.optString("conditionsType"));
					// add the message
					logicMessage.setText(jsonLogicMessage.optString("text"));
					// add to our list
					_logicMessages.add(logicMessage);
				}
			}
		}
		_javaScript = javaScript;
	}

}
