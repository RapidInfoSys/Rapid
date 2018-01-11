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

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Event {

	// a list of all jQuery event types which if we recognise we'll add a listener for
	public static final String _jQueryEventTypes[] = {"bind","blur","change","click","dblclick","delegate","die","error","focus","focusin","focusout","hover","keydown","keypress","keyup","live","load","mousedown","mouseenter","mouseleave","mousemove","mouseout","mouseover","mouseup","off","on","one","ready","resize","scroll","select","submit","toggle","trigger","triggerHandler","unbind","undelegate","unload"};

	// instance variables

	private String _type, _extra, _filter;
	private List<Action> _actions;

	// properties

	// the type is defined in the control.xml file
	public String getType() { return _type; }
	public void setType(String type) { _type = type; }

	// the type is defined in the control.xml file
	public String getExtra() { return _extra; }
	public void setExtra(String extra) { _extra = extra; }

	// the filter is a javascript function defined in the control.xml file that executes before the actions and can modify the event or even stop it from firing
	public String getFilter() { return _filter; }
	public void setFilter(String filter) { _filter = filter; }

	// these are the actions to perform when this event occurs to the control in which its sitting
	public List<Action> getActions() { return _actions; }
	public void setActions(List<Action> actions) { _actions = actions; }

	// constructors

	public Event() {};

	public Event(String type, String extra, String filter) {
		_type = type;
		_extra = extra;
		_filter = filter;
		_actions = new ArrayList<Action>();
	}

	// methods

	public boolean isCustomType() {
		// if we have a list of known types
		if (_jQueryEventTypes != null) {
			// loop them
			for (String jQueryEventType : _jQueryEventTypes) {
				// check
				if (jQueryEventType.equals(_type)) {
					// we've recognised the type so its not custom, return false
					return false;
				}
			}
		}
		// failed to find it, must be custom
		return true;
	}

	public String getPageLoadJavaScript(Control control) {

		// add the line if a recognised Jquery event type
		if (isCustomType()) {
			return "";
		} else {
			// assume extra is empty
			String extra = "";
			// if there was an extra
			if (_extra != null) {
				// retrieve it
				extra = _extra;
				// add leading . if there is something and no . already
				if (extra.length() > 0 && !extra.startsWith(".")) extra = "." + extra;
			}
			return "$('#" + control.getId() + "')" + extra + "." + _type + "(Event_" + _type + "_" + control.getId() + ");\n";
		}

	}

}
