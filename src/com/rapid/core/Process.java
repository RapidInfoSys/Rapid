package com.rapid.core;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.utils.JSON;

public abstract class Process extends Thread {

	// protected instance variables
	protected ServletContext _servletContext;
	protected String _name;
	protected int _interval;
	protected JSONObject _config;
	protected boolean _stopped;

	// protected static variables
	protected Logger _logger;

	// constructor
	public Process(ServletContext servletContext, JSONObject config) throws JSONException {
		// store context
		_servletContext = servletContext;
		// store config - this is the JSON from the .process.xml file
		_config = config;
		// get the name from the config
		_name = config.getString("name");
		// get the interval from  the config
		_interval = config.getInt("interval");

		// get a logger for this class
		_logger = LogManager.getLogger(this.getClass());
	}

	// abstract methods
	public abstract void doProcess();

	// protected methods

	protected ServletContext getServletContext() {
		return _servletContext;
	}

	protected String getProcessName() {
		return _name;
	}

	protected int getInterval() {
		return _interval;
	}
	
	// get a named parameter value from the parameters collection in the .process.xml file
	protected String getParameterValue(String name) {
		String value = null;
		try {
			JSONArray jsonParameters = JSON.getJSONArray(_config.optJSONObject("parameters"), "parameter");
			if (jsonParameters != null) {
				for (int i=0; i < jsonParameters.length(); i++) {
					JSONObject jsonParameter = jsonParameters.getJSONObject(i);
					if (name.equals(jsonParameter.getString("name"))) return jsonParameter.getString("value");
				}
			}
		} catch (JSONException e) {}
		return value;
	}

	protected Applications getApplications() {
		return (Applications) _servletContext.getAttribute("applications");
	}

	// override methods
	@Override
	public void start() {
		if (_interval > 0) {
			super.start();
			// log that we've started
			_logger.info("Process " + _name + " has started, checking every " + _interval + " seconds");
		} else {
			// set stopped
			_stopped = true;
			// log that we won't be started
			_logger.info("Process " + _name + " will not be started, interval must be greater than 0");
		}
	}

	@Override
	public void run() {

		// loop until stopped
		while (!_stopped) {
			Date now = new Date();
			DateFormat dateFormat = null;
			boolean runToday = false;

			try {
				// If days specified in the schema
				if (_config.has("days")) {

					_logger.trace("There are days in the xml");

					JSONObject days = _config.getJSONObject("days");
					dateFormat = new SimpleDateFormat("EEEE");
					String today = (dateFormat.format(now)).toLowerCase();

					// Check if I am on the right day.
					if (days.has(today) && days.getBoolean(today)) {

						_logger.trace("I am currently on the right day!");
						// we should run today
						runToday = true;
					} // otherwise today is not the right day

				//Otherwise, no days specified in the schema
				} else {
					// no days specified in the schema so run everyday
					runToday = true;
				}

				// If I am allowed to runToday (either I am in the right day, or no days were specified)
				if (runToday) {

					// Check if the duration are specified in the xml
					if (_config.has("duration")) {

						_logger.trace("Durations are specified!");

						// get the duration
						JSONObject duration = _config.getJSONObject("duration");

						// get the duration start and stop
						String start = duration.getString("start");
						String stop = duration.getString("stop");

						// Check whether the start/stop time has the 'seconds' portion. Add if not
						if (start.length() < 6) start += ":00";
						if (stop.length() < 6) stop += ":00";

						// get today's start and stop date objects using their time
						Date startTime = getTodayTimeDate(now, start);
						Date stopTime = getTodayTimeDate(now, stop);

						// calculate the difference between the currentTime and the 'start' time
						long millisToStart = startTime.getTime() - now.getTime();

						// calculate the difference between the currentTime and
						// the 'stop' time plus a second so that the stop time
						// in seconds is inclusive i.e up to .999 millis
						long millisToStop = stopTime.getTime() - now.getTime() + 1000;

						// if stop is less or same as than start, and we haven't
						// run yet set to 0 so we run at least once
						if (millisToStop <= millisToStart) millisToStop = millisToStart + _interval * 1000 - 1;

						// If the currentTime is before the specified start time
						if (millisToStart > 0) {

							_logger.trace("Before process start time.. Should sleep for " + millisToStart / 1000 + " seconds");

							// Sleep until the start time
							Thread.sleep(millisToStart);

							// If the currentTime is between the start and stop time
						} else if (millisToStart <= 0 && millisToStop > 0) {

							_logger.trace("Between process start and stop time..Should run the process. and then sleep for interval " + _interval);

							// run the abstract method
							doProcess();
							// Then sleep at interval rate
							Thread.sleep(_interval * 1000);

							// Otherwise, the currentTime should be after the start/stop time
						} else {

							// In this case, sleep until tomorrow's start time
							Long millisToTomorrowStart = millisToStart + (24 * 60 * 60 * 1000);

							_logger.trace("After process start time and stop time.. Should sleep until tomorrow's start: for " + millisToTomorrowStart / 1000 + " seconds");

							// Sleep until tomorrow's start time
							Thread.sleep(millisToTomorrowStart);
						}

						// Otherwise, no duration is provided in the schema
					} else {

						// run and sleep at interval rate continuously
						_logger.trace("No duration specified, run and sleep at interval rate");

						// run the abstract method
						doProcess();
						// Then sleep at interval rate
						Thread.sleep(_interval * 1000);
					}

				// Otherwise, I am not supposed to run today. Today is not the right day - days had to be provided
				} else {

					// Check if the duration is provided
					if (_config.has("duration")) {

						// Obtain the start time string
						String start = _config.getJSONObject("duration").getString("start");
						// get the start time
						Date startTime = getTodayTimeDate(now, start);

						// calculate the difference between the currentTime and the 'start' time
						long millisToStart = startTime.getTime() - now.getTime();
						// tomorrow start is today start + 24 hours milliseconds
						Long millisToTomorrowStart = millisToStart + (24 * 60 * 60 * 1000);

						_logger.trace("Not running today - sleep " + millisToTomorrowStart/1000 + " secs until tomorrow's start");

						// sleep until tomorrow's specified start
						Thread.sleep(millisToTomorrowStart);

						// Otherwise, no duration is provided in the schema. To be here, days had to be provided, but not today
					} else {

						// get the last second of today
						Date endOfToday = getTodayTimeDate(now, "23:59:59");
						// get millis to end of today plus one second to get to midnight tomorrow
						Long millisToEndOfToday = endOfToday.getTime() - now.getTime() + 1000;

						_logger.trace("Days specified, but not today, and no duration specified, sleep " + millisToEndOfToday/1000 + " secs until the end of today");

						// sleep to end of today
						Thread.sleep(millisToEndOfToday);

					} // duration check

				} // day check

			} catch (JSONException | ParseException ex) {

				_logger.error("Error in process " + _name, ex);
				_stopped = true;

			} catch (InterruptedException ex) {

				_logger.error("Process " + _name + " was interrupted!", ex);
				_stopped = true;

			}
		} // end of while

		 // log stopped
		_logger.error("Process " + _name + " has stopped");
	}

	// Returns a date object of today's date with given time
	private Date getTodayTimeDate(Date now, String time) throws ParseException {
		// Create a start and stop datetime string, to be parsed into date object
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		// append the time
		String timeString = dateFormat.format(now) + " " + time;
		// assume date format pattern without seconds
		String pattern = "dd/MM/yyyy HH:mm";
		// if time contains seconds add to pattern
		if (time.length() > 5) pattern += ":ss";
		// update the dateFormat to include time
		dateFormat = new SimpleDateFormat(pattern);
		// get the date
		Date todayTime = dateFormat.parse(timeString);
		// log
		_logger.trace("getTodayTimeDate " + now + " " + time + " = " + todayTime);
		// return the parsed date
		return todayTime;
	}

	@Override
	public void interrupt() {
		_stopped = true;
		super.interrupt();
	}

}
