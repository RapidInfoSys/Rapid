/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

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

public class Html {

	private static final int ERROR = 0;
    private static final int OPEN = 1;
    private static final int CLOSE = 2;
    private static final int SELFCLOSE = 3;

    private static int getTagType(String html) {
    	// check we got something
    	if (html != null) {
    		if (html.length() >= 3) {
    			if (html.charAt(0) == '<' && html.charAt(html.length() - 1) == '>') {
    				// close tags have a / as their second character
    	    		if (html.charAt(1) == '/') return CLOSE;
    	    		// if the penultimate character is / this is a self close
    	    		if (html.charAt(html.length() - 2) == '/') return SELFCLOSE;
    	    		// opener and closer, not mixed, not close, not self close must be open
    	    		return OPEN;
    	    	}
    		}
    	}
    	// return error if null, shorter than 3 chars, doesn't have < at begining and > at end
    	return ERROR;
    }

    private static String getNextTag(String html, int pos) {
    	int openPos = html.indexOf("<", pos);
    	if (openPos > - 1) {
    		int closePos = html.indexOf(">",openPos);
    		if (closePos > -1) {
    			return html.substring(openPos, closePos + 1);
    		}
    	}
    	return null;
    }

    private static String getIndents(int count) {
    	String indents = "";
    	for (int i = 0; i < count; i++) indents += "\t";
    	return indents;
    }

	public static String getPrettyHtml(String html) {

    	// the pretty string we're about to build
    	StringBuilder prettyHtml = new StringBuilder();
    	// variables to keep track off the start and end of the current text grab
    	int startPos = 0;
    	int endPos = 0;
    	// tabs increase and decrease according to opens and closes
    	int tabCount = 0;

    	// get the first tag now
    	String tag = getNextTag(html, startPos);
    	// get the first tag type
    	int tagType = getTagType(tag);
    	// the next tag can be blank as it's the first thing we get
    	String nextTag = "";
    	// getting the type is the first thing we do
    	int nextTagType = SELFCLOSE;

    	// keep moving through the html either until our end position is at the the html end, or there's a problem with the tag
		while (endPos < html.length() - 1 && tagType != ERROR) {
    		// bail as soon as there is a problem with the current tag
        	if (tagType == ERROR) break;
        	// get the posistion of the end of this tag
        	endPos = html.indexOf(tag, startPos) + tag.length() - 1;
        	// get the next tag
        	nextTag = getNextTag(html, endPos);
        	// get the next tag type
        	nextTagType = getTagType(nextTag);

        	switch (tagType) {
        	case OPEN :
        		// increase the tab count
        		tabCount++;
        		// always print the indents and then the tag
        		prettyHtml.append(getIndents(tabCount) + html.substring(startPos, endPos + 1));
        		// if the next tag is an open do a line break now
        		if (nextTagType == OPEN) prettyHtml.append("\n");
        		break;
        	case CLOSE :
        		// decrease the tab count
        		tabCount--;
        		// always print the tag
        		prettyHtml.append(html.substring(startPos, endPos + 1) + "\n");
        		// if the next tag is a close give it some indents
        		if (nextTagType == CLOSE) prettyHtml.append(getIndents(tabCount));
        		break;
        	case SELFCLOSE :
        		// just print the tag
        		prettyHtml.append(html.substring(startPos, endPos + 1));
        		break;
        	}
        	// start the next round at the end of this round
        	startPos = endPos + 1;
        	// tag in the next round is this current next tag
        	tag = nextTag;
        	// tag type in the next round is this current tag type
        	tagType = nextTagType;
    	}
    	// we're done!
    	return prettyHtml.toString();

    }

	// escape common characters to avoid cross-site-scripting
	public static String escape(String string) {
		if (string == null) {
			return "";
		} else {
			return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot").replace("'", "&apos;").replace("/", "&#x2F;");
		}
	}

	// un-escape common characters
	public static String unescape(String string) {
		if (string == null) {
			return "";
		} else {
			return string.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot", "\"").replace("&apos;", "'").replace("&#x2F;", "/");
		}
	}

}
