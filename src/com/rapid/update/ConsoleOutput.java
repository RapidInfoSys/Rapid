package com.rapid.update;

public class ConsoleOutput implements UpdateOutput {

	@Override
	public void log(String message) {		
		// just use System.out
		System.out.println(message);		
	}

}
