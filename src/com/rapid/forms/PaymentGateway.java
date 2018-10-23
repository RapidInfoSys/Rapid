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

package com.rapid.forms;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.server.RapidRequest;

/**
 * @author mehmet
 *
 * A generic abstract class containing the skeleton structure -
 * that must be extended for payment gateways.
 *
 */

public abstract class PaymentGateway {

	// public static finals

	public static final int PAYMENT_SUCCESS = 1;
	public static final int PAYMENT_INVALID = 2;
	public static final int PAYMENT_REJECTED = 3;
	public static final int PAYMENT_CANCELLED = 4;
	public static final int PAYMENT_NOT_ATTEMPTED = 5;
	public static final int PAYMENT_ERROR = 6;

	// instance variables
	protected Logger _logger;

	// instance methods

	public String getPaymentStatusMessage(RapidRequest rapidRequest, int status) {

		String message = "Uknown status";

		switch (status) {
			case PaymentGateway.PAYMENT_SUCCESS:
				message = "Payment was successful";
			break;
			case PaymentGateway.PAYMENT_INVALID:
				message = "Payment was invalid";
			break;
			case PaymentGateway.PAYMENT_REJECTED:
				message = "Payment was rejected";
			break;
			case PaymentGateway.PAYMENT_CANCELLED:
				message = "Payment was cancelled";
			break;
			case PaymentGateway.PAYMENT_NOT_ATTEMPTED:
				message = "Payment was not attempted";
			break;
			case PaymentGateway.PAYMENT_ERROR:
				message = "There was an error with the payment";
			break;
		}

		return message;

	}

	// constructor

	public PaymentGateway(ServletContext servletContext, Application application) {
		// create the logger
		_logger = LogManager.getLogger();
	}

	// abstract methods

	public abstract String getPaymentUrl(RapidRequest rapidRequest) throws Exception;
	public abstract int getPaymentStatus(RapidRequest rapidRequest) throws Exception;

}
