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

package com.rapid.core;

import org.json.JSONException;
import org.json.JSONObject;

public class Theme  {

	// private instance variables
	private String _type, _name, _css, _headerHtml, _footerHtml, _headerHtmlDesigner, _footerHtmlDesigner, _formSummaryHeaderHtml, _formSummaryPageStartHtml, _formSummaryPageEndHtml, _formSummaryPagesEndHtml, _formSummaryFooterHtml;
	private boolean _visible;
	private JSONObject _parameters, _resources;

	// properties
	public String getType() { return _type; }
	public String getName() { return _name; }
	public boolean getVisible() { return _visible; }
	public String getCSS() { return _css; }
	public JSONObject getParameters()  { return _parameters; }
	public JSONObject getResources()  { return _resources; }
	public String getHeaderHtml() { return _headerHtml; }
	public String getFooterHtml() { return _footerHtml; }
	public String getHeaderHtmlDesigner() { return _headerHtmlDesigner; }
	public String getFooterHtmlDesigner() { return _footerHtmlDesigner; }
	public String getFormSummaryHeaderHtml() { return _formSummaryHeaderHtml; }
	public String getFormSummaryPageStartHtml() { return _formSummaryPageStartHtml; }
	public String getFormSummaryPageEndHtml() { return _formSummaryPageEndHtml; }
	public String getFormSummaryPagesEndHtml() { return _formSummaryPagesEndHtml; }
	public String getFormSummaryFooterHtml() { return _formSummaryFooterHtml; }

	// constructor
	public Theme(String xml) throws JSONException {
		// convert the xml string into JSON - themes, were once called templates
		JSONObject jsonTheme = org.json.XML.toJSONObject(xml).getJSONObject("template");
		// retain properties
		_type = jsonTheme.getString("type");
		_name = jsonTheme.getString("name");
		_visible = jsonTheme.optBoolean("visible", true);
		_css = jsonTheme.getString("css").trim();
		_parameters = jsonTheme.optJSONObject("parameters");
		_resources = jsonTheme.optJSONObject("resources");
		_headerHtml = jsonTheme.optString("headerHtml", null);
		_footerHtml = jsonTheme.optString("footerHtml", null);
		_headerHtmlDesigner = jsonTheme.optString("headerHtmlDesigner", null);
		_footerHtmlDesigner = jsonTheme.optString("footerHtmlDesigner", null);
		_formSummaryHeaderHtml = jsonTheme.optString("formSummaryHeaderHtml", null);
		_formSummaryPageStartHtml = jsonTheme.optString("formSummaryPageStartHtml", null);
		_formSummaryPageEndHtml = jsonTheme.optString("formSummaryPageEndHtml", null);
		_formSummaryPagesEndHtml = jsonTheme.optString("formSummaryPagesEndHtml", null);
		_formSummaryFooterHtml = jsonTheme.optString("formSummaryFooterHtml", null);
	}

}
