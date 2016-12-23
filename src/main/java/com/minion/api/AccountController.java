package com.minion.api;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpStatus;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.qanairy.models.dto.AccountDto;
import com.qanairy.models.dto.UserDto;

/**
 *	API endpoints for interacting with {@link User} data
 */
@Controller
@RequestMapping("/account")
public class AccountController {
	
	/**
	 * 
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/account/new", method = RequestMethod.POST)
	public int register(HttpServletRequest request, 
				 @RequestParam(value="org_name", required=true) String org_name,
				 @RequestParam(value="service_package", required=true) String service_package,
				 @RequestParam(value="payment_account", required=true) String payment_account) {
		AccountDto account = new AccountDto();
		
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
	
}
