/*

Copyright (C) 2016 - Gareth Edwards / Rapid Information Systems

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
	private JSONObject _resources;

	// properties
	public String getType() { return _type; }
	public String getName() { return _name; }
	public String getCSS() { return _css; }
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
		// convert the xml string into JSON
		JSONObject jsonTemplate = org.json.XML.toJSONObject(xml).getJSONObject("template");
		// retain properties
		_type = jsonTemplate.getString("type");
		_name = jsonTemplate.getString("name");
		_css = jsonTemplate.getString("css").trim();
		_resources = jsonTemplate.optJSONObject("resources");
		_headerHtml = jsonTemplate.optString("headerHtml", null);
		_footerHtml = jsonTemplate.optString("footerHtml", null);
		_headerHtmlDesigner = jsonTemplate.optString("headerHtmlDesigner", null);
		_footerHtmlDesigner = jsonTemplate.optString("footerHtmlDesigner", null);
		_formSummaryHeaderHtml = jsonTemplate.optString("formSummaryHeaderHtml", null);
		_formSummaryPageStartHtml = jsonTemplate.optString("formSummaryPageStartHtml", null);
		_formSummaryPageEndHtml = jsonTemplate.optString("formSummaryPageEndHtml", null);
		_formSummaryPagesEndHtml = jsonTemplate.optString("formSummaryPagesEndHtml", null);
		_formSummaryFooterHtml = jsonTemplate.optString("formSummaryFooterHtml", null);
	}

}
