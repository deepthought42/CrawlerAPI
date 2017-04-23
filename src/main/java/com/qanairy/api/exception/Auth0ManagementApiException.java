package com.qanairy.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "Error occured while updating user account")
public class Auth0ManagementApiException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5133451250284340743L;

	public Auth0ManagementApiException(){}
}
