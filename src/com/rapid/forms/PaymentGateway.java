/**
 * 
 */
package com.rapid.forms;

import javax.servlet.ServletContext;
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
		
	// instance methods
	
	public String getPaymentStatusMessage(RapidRequest rapidRequest, int status) {
		
		String message = "Uknown status";
		
		switch (status) {
		
		case PaymentGateway.PAYMENT_SUCCESS:
			message = "Payment successful";
			break;
			
		case PaymentGateway.PAYMENT_INVALID:
			message = "Payment invalid";
			break;
			
		case PaymentGateway.PAYMENT_REJECTED:
			message = "Payment rejected";
			break;
			
		case PaymentGateway.PAYMENT_CANCELLED:
			message = "Payment cancelled";
			break;
			
		case PaymentGateway.PAYMENT_NOT_ATTEMPTED:
			message = "Payment not attempted";
			break;
			
		case PaymentGateway.PAYMENT_ERROR:
			message = "Payment error";
			break;
		}
		
		return message;
		
	}
	
	// constructor
	
	public PaymentGateway(ServletContext servletContext, Application application) {}
	
	// abstract methods
	
	public abstract String getPaymentUrl(RapidRequest rapidRequest);
	public abstract int getPaymentStatus(RapidRequest rapidRequest);
	
}
