package com.rapid.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

public class Safety extends RapidHttpServlet {

	private static final long serialVersionUID = 4L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// get a logger
		Logger logger = getLogger();

		// log
		logger.debug("Safety GET request : " + request.getQueryString());

	}

}
