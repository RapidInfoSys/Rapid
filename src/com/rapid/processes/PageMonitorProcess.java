package com.rapid.processes;

import java.util.Date;

import javax.servlet.ServletContext;

import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.core.Applications;
import com.rapid.core.Process;

// this monitor class runs on it's own thread and removed pages from memory that have not been accessed in more than a specified time
public class PageMonitorProcess extends Process {

	// private static finals
	private static final int MAX_AGE = 1800;

	// private instance variables
	private int  _maxPageAge;

	// constructor
	public PageMonitorProcess(ServletContext servletContext, JSONObject parameters) throws JSONException {

		// required to invoke super
		super(servletContext, parameters);

		// assume default page max age
		_maxPageAge = MAX_AGE;
		// try and read the value from web.xml
		try {
			String pageMaxAgeString = servletContext.getInitParameter("pageMaxAge");
			if (pageMaxAgeString != null) _maxPageAge = Integer.parseInt(pageMaxAgeString);
		} catch (Exception ex) {
			_logger.error("pageMaxAge is not an integer");
		}

		// log our details
		_logger.info("Page monitor process will check for pages not accessed in the last " + _maxPageAge + " seconds");

	}

	@Override
	public void doProcess() {

		// log start of checking
		_logger.debug("Page monitor is checking for stale pages");

		// get the current date / time
		Date now = new Date();

		// get the applications
		Applications applications = (Applications) _servletContext.getAttribute("applications");

		// loop them
		for (Application application : applications.get()) {
			// get old pages
			application.getPages().clearOldPages(now, _maxPageAge);
		}

	}

	@Override
	public void interrupt() {
		// call the super method so it actually stops!
		super.interrupt();
		// log that we've started
		_logger.info("Page monitor has stopped");
	}

}
