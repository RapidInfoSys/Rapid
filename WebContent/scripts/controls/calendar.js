// Tigra Calendar v5.2 (11/20/2011)
// http://www.softcomplex.com/products/tigra_calendar/
// License: Public Domain... You're welcome.	

var cal_A_TCALTOKENS = [
	 // A full numeric representation of a year, 4 digits
	{'t': 'Y', 'r': '19\\d{2}|20\\d{2}', 'p': function (d_date, n_value) { d_date.setFullYear(Number(n_value)); return d_date; }, 'g': function (d_date) { var n_year = d_date.getFullYear(); return n_year; }},
	 // Numeric representation of a month, with leading zeros
	{'t': 'm', 'r': '0?[1-9]|1[0-2]', 'p': function (d_date, n_value) { d_date.setMonth(Number(n_value) - 1); return d_date; }, 'g': function (d_date) { var n_month = d_date.getMonth() + 1; return (n_month < 10 ? '0' : '') + n_month; }},	
	 // Day of the month, 2 digits with leading zeros
	{'t': 'd', 'r': '0?[1-9]|[12][0-9]|3[01]', 'p': function (d_date, n_value) { d_date.setDate(Number(n_value)); if (d_date.getDate() != n_value) d_date.setDate(0); return d_date; }, 'g': function (d_date) { var n_date = d_date.getDate(); return (n_date < 10 ? '0' : '') + n_date; }},
	// Day of the month without leading zeros
	{'t': 'j', 'r': '0?[1-9]|[12][0-9]|3[01]', 'p': function (d_date, n_value) { d_date.setDate(Number(n_value)); if (d_date.getDate() != n_value) d_date.setDate(0); return d_date; }, 'g': function (d_date) { var n_date = d_date.getDate(); return n_date; }},	
	// English ordinal suffix for the day of the month, 2 characters
	{'t': 'S', 'r': 'st|nd|rd|th', 'p': function (d_date, s_value) { return d_date; }, 'g': function (d_date) { n_date = d_date.getDate(); if (n_date % 10 == 1 && n_date != 11) return 'st'; if (n_date % 10 == 2 && n_date != 12) return 'nd'; if (n_date % 10 == 3 && n_date != 13) return 'rd'; return 'th'; }}	
];

function cal_f_tcalResetTime (d_date) {
	d_date.setMilliseconds(0);
	d_date.setSeconds(0);
	d_date.setMinutes(0);
	d_date.setHours(12);
	return d_date;
}

function cal_f_tcalParseDate (s_date, s_format) {

	if (!s_date) return;
	
	if (typeof(window.cal_A_TCALTOKENS_IDX) === "undefined") {
		window.cal_A_TCALTOKENS_IDX = {};
		for (var n = 0; n < cal_A_TCALTOKENS.length; n++) cal_A_TCALTOKENS_IDX[cal_A_TCALTOKENS[n]['t']] = cal_A_TCALTOKENS[n];
	}

	var s_char, s_regexp = '^', a_tokens = {}, a_options, n_token = 0;
	for (var n = 0; n < s_format.length; n++) {
		s_char = s_format.charAt(n);
		if (cal_A_TCALTOKENS_IDX[s_char]) {
			a_tokens[s_char] = ++n_token;
			s_regexp += '(' + cal_A_TCALTOKENS_IDX[s_char]['r'] + ')';
		}
		else if (s_char == ' ')
			s_regexp += '\\s';
		else
			s_regexp += (s_char.match(/[\w\d]/) ? '' : '\\') + s_char;
	}
	var r_date = new RegExp(s_regexp + '$');
	if (!s_date.match(r_date)) return;
	
	var s_val, d_date = cal_f_tcalResetTime(new Date());
	d_date.setDate(1);

	for (n = 0; n < cal_A_TCALTOKENS.length; n++) {
		s_char = cal_A_TCALTOKENS[n]['t'];
		if (!a_tokens[s_char])
			continue;
		s_val = RegExp['$' + a_tokens[s_char]];
		d_date = cal_A_TCALTOKENS[n]['p'](d_date, s_val);
	}
	
	return d_date;
}

function cal_f_tcalGenerateDate (d_date, s_format) {
	
	if (typeof(window.cal_A_TCALTOKENS_IDX) === "undefined") {
		window.cal_A_TCALTOKENS_IDX = {};
		for (var n = 0; n < cal_A_TCALTOKENS.length; n++) cal_A_TCALTOKENS_IDX[cal_A_TCALTOKENS[n]['t']] = cal_A_TCALTOKENS[n];
	}
	
	var s_char, s_date = '';
	for (var n = 0; n < s_format.length; n++) {
		s_char = s_format.charAt(n);
		s_date += cal_A_TCALTOKENS_IDX[s_char] ? cal_A_TCALTOKENS_IDX[s_char]['g'](d_date) : s_char;
	}
	return s_date;
}

/*
 
Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.
 
 */

var _calendarWeeks = ["Su","Mo","Tu","We","Th","Fr","Sa"];
var _calendarMonths = ["January","February","March","April","May","June","July","August","September","October","November","December"];
var _calendarMonthDays = [31,28,31,30,31,30,31,31,30,31,30,31];

function calendarGetMonthDays(year, month) {
	var monthDays = _calendarMonthDays[month];
	// if feb in normal leap year but not divisble by 100 exluding divisible by 400
	if (month == 1 && year % 4 == 0 && !(year % 100 == 0 && year % 400 != 0)) monthDays++;
	return monthDays;
}

function calendarSetDate(id, date) {
	var calendar = $("#" + id);
	if (date) {
		var month = date.getMonth() + 1;
		if (month < 10) month = "0" + month;
		var day = date.getDate();
		if (day < 10) day = "0" + day;
		calendar.attr("data-date",date.getFullYear() + "-" + month + "-" + day);
	} else {
		calendar.removeAttr("data-date");
	}
}

function calendarUpdate(id) {
	
	var calendar = $("#" + id);
							
	var year = calendar.attr("data-year");
	var month = calendar.attr("data-month");
	var date = new Date(calendar.attr("data-date"));
	var today = new Date();
	
	var monthStartDate = new Date(year, month, 1);
	var monthDays = calendarGetMonthDays(year, month);
			
	var header = calendar.find("div.calendarHeader");				
	var label = header.find(".calendarLabel");
	label.html(_calendarMonths[month] + " " + year);
	
	var table = calendar.find("table.calendarTable");
	var daysBody = table.find("tbody");
	var days = daysBody.find("td");
	days.removeClass("calendarToday").removeClass("calendarSelectedDay");
	
	var day = 1 - monthStartDate.getDay();
	
	days.each(function() {
		var dayCell = $(this);			
		if (day > 0 && day <= monthDays) {
			dayCell.html(day).removeClass("calendarNonDay").addClass("calendarDay").attr("data-day", day);
			if (day == today.getDate() && month == today.getMonth() && year == today.getFullYear()) dayCell.addClass("calendarToday");
			if (day == date.getDate() && month == date.getMonth() && year == date.getFullYear()) dayCell.addClass("calendarSelectedDay");
		} else {
			dayCell.html("").removeClass("calendarDay").addClass("calendarNonDay");
		}
		day++;
	});
	
	var rows = daysBody.find("tr").filter( function(idx) { return idx > 0; });
	rows.each(function() {
		var row = $(this);
		if (row.children().first().html()) {
			row.show();
		} else {
			row.hide();
		}
	});
				
}

function calendarMove(from, type, amount) {
	var calendar = $(from).closest("div.calendar");
	var id = calendar.attr("id");
	var year = calendar.attr("data-year")*1;
	var month = calendar.attr("data-month")*1;
	switch (type) {
	case ("y") :
		calendar.attr("data-year", year + amount);
		break;
	case ("m") :
		month += amount;
		if (month < 0) {
			month = 11;
			year -= 1;
		}
		if (month > 11) {
			month = 0;
			year += 1;
		}
		calendar.attr("data-year", year);
		calendar.attr("data-month", month);
		break;
	}
	calendarUpdate(id);
}

function calendarSelectDay(from) {
	var dayCell = $(from);		
	var calendar = dayCell.closest("div.calendar");
	var id = calendar.attr("id");
	var year = calendar.attr("data-year")*1;
	var month = calendar.attr("data-month")*1;
	var day = dayCell.html();
	var date = new Date(year, month, day);
	calendarSetDate(id, date);		
	calendarUpdate(id);
	if (dayCell.hasClass("calendarDay") && window["Event_selectDay_" + id]) window["Event_selectDay_" + id]($.Event('selectDay'));
}

function calendarInit(id, details) {
	
	var date = new Date();
	if (details.date) date = new Date(details.date); 
	
	calendarSetDate(id, date);
	
	var calendar = $("#" + id);		
	calendar.attr("data-year", date.getFullYear());
	calendar.attr("data-month", date.getMonth());
			
	var table = calendar.find("table.calendarTable");
	
	var weeks = table.find("thead");
	var weeksHtml = "<tr>";
	for (var i = 0; i < 7; i++) {
		weeksHtml += "<td>" + _calendarWeeks[i] + "</td>";
	}
	weeksHtml += "</tr>";
	weeks.html(weeksHtml);
				
	var days = table.find("tbody");
	var daysHtml = "";
	for (var i = 0; i < 5; i++) {
		daysHtml += "<tr>";
		for (var j = 0; j < 7; j++) {
			daysHtml += "<td onclick='calendarSelectDay(this);'></td>";				
		}
		daysHtml += "</tr>";
	}
	days.html(daysHtml);
	
	calendarUpdate(id);
}