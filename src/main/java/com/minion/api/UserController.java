package com.minion.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.qanairy.models.dto.UserDto;

/**
 *	API endpoints for interacting with {@link User} data
 */
@Controller
@RequestMapping("/user")
public class UserController {

	/**
	 * 
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/registration", method = RequestMethod.POST)
	public int registerUser(HttpServletRequest request, 
				 @RequestParam(value="email", required=true) String email,
				 @RequestParam(value="password", required=true) String password,
				 @RequestParam(value="confirmation_password", required=true) String confirmation_password,
				 @RequestParam(value="acct_name", required=false) String account_name) {
		UserDto registered = new UserDto();
		
		if(!password.equals(confirmation_password)){
			return HttpStatus.SC_BAD_REQUEST;
		}
		
		registered.setEmail(email);
		registered.setPassword(password);
		/*
		 * if (!result.hasErrors()) {
	        registered = createUserAccount(accountDto, result);
	    }
	    if (registered == null) {
	        result.rejectValue("email", "message.regError");
	    }
	    */
		return HttpStatus.SC_ACCEPTED;
	    // rest of the implementation
	}
	
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
