package com.minion.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
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
	@RequestMapping(value = "/user/registration", method = RequestMethod.GET)
	public String registerUser(HttpServletRequest request, 
				 @RequestParam(value="email", required=true) String email,
				 @RequestParam(value="password", required=true) String password,
				 @RequestParam(value="confirmation_password", required=true) String confirmation_password,
				 @RequestParam(value="acct_name", required=false) String account_name) {
		User registered = new User();
	    if (!result.hasErrors()) {
	        registered = createUserAccount(accountDto, result);
	    }
	    if (registered == null) {
	        result.rejectValue("email", "message.regError");
	    }
	    return "registration";
	}
}
