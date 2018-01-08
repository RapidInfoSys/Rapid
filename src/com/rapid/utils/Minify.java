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

package com.rapid.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

public class Minify {

	// public static finals

	public static final int JAVASCRIPT = 0;
	public static final int CSS = 1;

	public static class MinifyIssue {

		// private instance variables
		private String _message, _sourceName, _lineSource;
		int _line, _column;

		// constructor
		public MinifyIssue(String message, String sourceName, int line, String lineSource, int column) {
			_message = message;
			_sourceName = sourceName;
			_line = line;
			_lineSource = lineSource;
			_column = column;
		}

		// override
		@Override
		public String toString() {
			return _message + " at line " + _line + ", column " + _column + " in " + _sourceName + (_lineSource == null ? "" : " : " + _lineSource);
		}

	}

	public static class MinifyErrorReporter implements ErrorReporter {

		// private instance variables
		private String _sourceName;
		private List<MinifyIssue> _errors;
		private List<MinifyIssue> _warnings;

		// constructor
		public MinifyErrorReporter(String sourceName) {
			_sourceName = sourceName;
			_errors = new ArrayList<MinifyIssue>();
			_warnings = new ArrayList<MinifyIssue>();
		}

		// properties
		public List<MinifyIssue> getErrors() { return _errors; }
		public List<MinifyIssue> getWarnings() { return _warnings; }

		// overrides

		@Override
		public void error(String message, String sourceName, int line, String lineSource, int column) {
			_errors.add(new MinifyIssue(message, _sourceName, line, lineSource, column));
		}

		@Override
		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int column) {
			_errors.add(new MinifyIssue(message, _sourceName, line, lineSource, column));
			return new EvaluatorException(message, _sourceName, line, lineSource, column);
		}

		@Override
		public void warning(String message, String _sourceName, int line, String lineSource, int column) {
			_warnings.add(new MinifyIssue(message, _sourceName, line, lineSource, column));
		}

		public String getDetails() {

			StringBuilder detail = new StringBuilder();

			// if we have any errors
			if (_errors.size() > 0) {
				// append to the String builder
				detail.append("/* This file could not be minified due to the following errors */\n\n/*\n");
				// loop the errors
				for (MinifyIssue error : _errors) {
					detail.append(error.toString() + "\n\n");
				}
				// close the comments
				detail.append("*/\n\n");
			}

			// if we have any warnings
			if (_warnings.size() > 0) {
				// append to the String builder
				detail.append("/* This file contained the following warnings */\n\n/*\n");
				// loop the errors
				for (MinifyIssue warning : _warnings) {
					detail.append(warning.toString() + "\n\n");
				}
				// close the comments
				detail.append("*/\n\n");
			}

			// return the detail
			return detail.toString();

		}

	}

	// the main method
	public static Writer toWriter(String string, Writer writer, int type, String sourceName) throws IOException {

		// get an error reporter
		MinifyErrorReporter errorReporter = new MinifyErrorReporter(sourceName);

		// get a string reader
		StringReader sr = new StringReader(string);

		try {
			// check the type
			switch (type) {
				case CSS :
					/*
					// get the css compressor
					CssCompressor cssCompressor = new CssCompressor(sr);
					// compress
					cssCompressor.compress(writer, 0)
					*/

					// The css compression is maxing out memory and requiring a higher, non standard, stack size so for now we'll make do with the simple techniques below

					// the string we're going to compress everything into
					String compressed = string;

					// get a logger
					Logger logger = LogManager.getLogger(Minify.class);

					// detailed logging
					logger.trace("Starting css minify");

					// remove all comments
					int start = compressed.indexOf("/*");
					int end = 0;
					while (start > -1) {
						end = compressed.indexOf("*/", start);
						if (end > -1) {
							compressed = compressed.substring(0, start) + compressed.substring(end + 2);
						}
						start = compressed.indexOf("/*");
					}

					// detailed logging
					logger.trace("CSS comments stripped");

					// replace all double spaces with a single until none are left
					start = compressed.indexOf("  ");
					while (start > -1) {
						compressed = compressed.substring(0, start) + compressed.substring(start + 1);
						start = compressed.indexOf("  ");
					}

					// detailed logging
					logger.trace("CSS double spaces removed");

					// now do the simple patterns
					compressed = compressed.replace("\t", "")
					.replace("\r", "")
					.replace("\n", "")
					.replace("\n ", "")
					.replace(" { ", "{")
					.replace(" {", "{")
					.replace("{ ", "{")
					.replace(" }", "}")
					.replace("} ", "}")
					.replace(";}", "}")
					.replace(" ; ", ";")
					.replace("; ", ";")
					.replace(" ;", ";")
					.replace(" : ", ":")
					.replace(" :", ":")
					.replace(": ", ":")
					.replace("}", "}\n");

					logger.trace("CSS minified");

					// write the result
					writer.write(compressed);

				break;
				default :
					// the error reporter is only used with js
					JavaScriptCompressor jsCompressor = new JavaScriptCompressor(sr, errorReporter);
					// compress
					jsCompressor.compress(writer, 1000, true, false, false, false);
					// check for any errors
					if (errorReporter.getErrors().size() > 0) {
						// write the original string
						writer.write(string);
						// add the details
						writer.write("\n\n" + errorReporter.getDetails());
						// return the original string and the details
						return writer;
					}
					// check for any warnings
					if (errorReporter.getWarnings().size() > 0) {
						// add the details
						writer.write("\n\n" + errorReporter.getDetails());
					}
			}
			// return the writer
			return writer;

		} catch (EvaluatorException ex) {

			// write the original string
			writer.write(string);

			// return the original string and the error details
			writer.write("\n\n" + errorReporter.getDetails());

			// return
			return writer;

		}

	}

	public static String toString(String string, int type, String sourceName) throws IOException {
		// check the input string for non-null
		if (string != null) {
			// check the input string for any content
			if (string.length() > 0) {
				// get the string writer
				StringWriter sw = (StringWriter) toWriter(string, new StringWriter(), type, sourceName);
				// turn to string and return
				return sw.toString();
			}
		}
		// return input as-is
		return string;
	}

	public static void toFile(File fromFile, File toFile, int type, String sourceName) throws IOException {
		// read the string from file
		String string = Strings.getString(fromFile);
		// create a file writer
		FileWriter fw = (FileWriter) toWriter(string, new FileWriter(toFile), type, sourceName);
		// close file writer
		fw.close();
	}

	public static void toFile(String string, File file, int type, String sourceName) throws IOException {
		// create a file writer
		FileWriter fw = new FileWriter(file);
		// compress
		fw.write(toString(string, type, sourceName));
		// close file writer
		fw.close();
	}

	public static void toFile(String string, String file, int type, String sourceName) throws IOException {
		// get a file
		File fileObject = new File(file);
		// run it
		toFile(string, fileObject, type, sourceName);
	}

}
