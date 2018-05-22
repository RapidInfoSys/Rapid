package com.rapid.utils;

import java.io.PrintStream;
import java.io.PrintWriter;

public class Exceptions {
	
	// this class is useful for rethrowing Exceptions, say after cleaning up database connections
	public static class RethrownException extends Exception {
		
		private Exception _ex;
		
		public RethrownException(Exception ex) {
			_ex = ex;
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.fillInStackTrace();
			}
		}

		@Override
		public synchronized Throwable getCause() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.getCause();
			}
		}

		@Override
		public String getLocalizedMessage() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.getLocalizedMessage();
			}
		}

		@Override
		public String getMessage() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.getMessage();
			}
		}

		@Override
		public StackTraceElement[] getStackTrace() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.getStackTrace();
			}
		}

		@Override
		public synchronized Throwable initCause(Throwable arg0) {
			if (_ex == null) {
				return null;
			} else {
				return _ex.initCause(arg0);
			}
		}

		@Override
		public void printStackTrace() {
			if (_ex != null) _ex.printStackTrace();			
		}

		@Override
		public void printStackTrace(PrintStream arg0) {
			if (_ex != null) _ex.printStackTrace(arg0);
		}

		@Override
		public void printStackTrace(PrintWriter arg0) {
			if (_ex != null) _ex.printStackTrace(arg0);
		}

		@Override
		public void setStackTrace(StackTraceElement[] arg0) {
			if (_ex != null) _ex.setStackTrace(arg0);
		}

		@Override
		public String toString() {
			if (_ex == null) {
				return null;
			} else {
				return _ex.toString();
			}
		}
		
	}
	
	// print an exception stack trace
	public static String getStringStackTrace(Exception ex) {
		
		String stackTrace = "\n\n";

		stackTrace += ex.getClass().getName() + "\n\n";

		if (ex.getStackTrace() != null) for (StackTraceElement element : ex.getStackTrace()) stackTrace += element + "\n";

		return stackTrace;
		
	}

}
