package com.minion.api;

import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *	API endpoints for interacting with {@link User} data
 */
@Controller
@RequestMapping("/user")
public class UserController {

	
	/*private User createUserAccount(UserDto accountDto, BindingResult result) {
	    User registered = null;
	    try {
	        registered = service.registerNewUserAccount(accountDto);
	    } catch (EmailExistsException e) {
	        return null;
	    }    
	    return registered;
	}
	*/
}
