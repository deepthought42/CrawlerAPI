package com.qanairy.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Invalid user")
public class InvalidUserException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8418257650116390173L;

	public InvalidUserException(){
		
	}
}
