package com.qanairy.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Domain is not owned by the user account")
public class DomainNotOwnedByAccountException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8418257650116390173L;

	public DomainNotOwnedByAccountException(){
		
	}
}
