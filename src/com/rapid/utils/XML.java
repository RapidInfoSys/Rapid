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

package com.rapid.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XML {

	private static DocumentBuilder getDocBuilder() throws ParserConfigurationException {

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        return docBuilderFactory.newDocumentBuilder();

	}

	public static Document openDocument(File file) throws ParserConfigurationException, SAXException, IOException {

		InputStream inputStream = new FileInputStream(file);
        Reader reader = new InputStreamReader(inputStream,"UTF-8");
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        Document document = getDocBuilder().parse (is);

        return document;

	}

	public static Document openDocument(String string) throws ParserConfigurationException, SAXException, IOException {

        Document document = getDocBuilder().parse( new InputSource( new StringReader(string)));

        return document;

	}

	public static Document openDocument(Reader reader) throws ParserConfigurationException, SAXException, IOException {

        Document document = getDocBuilder().parse( new InputSource( reader));

        return document;

	}

	public static void saveDocument(Document document, File file) throws TransformerFactoryConfigurationError, TransformerException {

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Source input = new DOMSource(document);
		Result output = new StreamResult(file);

		transformer.transform(input, output);

	}

	public static Node getChildElement(Node node) {

		NodeList nodeList = node.getChildNodes();

		if (nodeList.getLength() > 0) {

			return nodeList.item(0);

		}

		return null;

	}

	public static Node getChildElement(Node node, String elementName) {

		NodeList nodeList = node.getChildNodes();

		for (int i = 0; i < nodeList.getLength(); i++) {

			Node childNode = nodeList.item(i);

			if (elementName.equals(childNode.getNodeName())) return childNode;

		}

		return null;

	}

	public static String getChildElementValue(Node node, String elementName) {

		Node childNode = getChildElement(node, elementName);

		if (childNode != null) return childNode.getTextContent();

		return null;

	}

	public static String getElementValue(String xml, String elementName) {

		String value = null;

		int startPos = xml.indexOf("<" + elementName);

	    if (startPos >= 0) {

	    	char c = xml.charAt(startPos + elementName.length() + 1);

	    	if (c == '>') {

	    		startPos += elementName.length() + 2;

	    	} else if (c == ' ') {

	    		startPos = xml.indexOf(">", startPos) + 1;

	    	} else {

	    		startPos =-1;

	    	}

	    	if (startPos >= 0) {

	    		int endPos = xml.indexOf("</" + elementName + ">");

	    		value = xml.substring(startPos, endPos);

	    	}

	    }

		return value;

	}

	public static String getElementAttributeValue(String xml, String elementName, String attributeName) {

		String value = null;

		int startPos = xml.indexOf("<" + elementName);

	    if (startPos > 0) {

	    	int endPos = xml.indexOf(">", startPos);

	    	if (endPos > startPos) {

	    		startPos = xml.indexOf(attributeName, startPos);

	    		if (startPos > 0) {

	    			startPos = xml.indexOf("=", startPos);

	    			if (startPos > 0) {

	    				String delimiter = xml.substring(startPos + 1, startPos + 2);

	    				endPos = xml.indexOf(delimiter, startPos + 2);

	    				if (endPos > startPos) {

	    					value = xml.substring(startPos + 2, endPos);

	    				}

	    			}

	    		}

	    	}

	    }

		return value;

	}

	public static String escape(String value) {

		if (value == null) {

			return null;

		} else {

			return value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;").replace("'", "&apos;");

		}

	}

	public static String unescape(String value) {

		if (value == null) {

			return null;

		} else {

			return value.replace("&lt;","<").replace("&gt;",">").replace("&amp;","&").replace("&apos;","'");

		}

	}

	public static InputStream transform(InputStream xml, String xslt) throws TransformerException {

		Source xmlSource = new StreamSource( xml );

		TransformerFactory factory = TransformerFactory.newInstance();

        Source xsltSource = new StreamSource( new StringReader(xslt) );

        Transformer transformer = factory.newTransformer(xsltSource);

        ByteArrayOutputStream xmlStream = new ByteArrayOutputStream();

        transformer.transform(xmlSource, new StreamResult(xmlStream));

        ByteArrayInputStream xmlOut = new ByteArrayInputStream(xmlStream.toByteArray());

        return xmlOut;
	}

}
