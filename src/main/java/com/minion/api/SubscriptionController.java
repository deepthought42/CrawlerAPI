package com.minion.api;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.models.Account;
import com.qanairy.models.enums.SubscriptionPlan;
import com.qanairy.services.AccountService;
import com.qanairy.services.SubscriptionService;

@RestController
@RequestMapping("/subscribe")
public class SubscriptionController {
    
    @Autowired
    private AccountService account_service;
    
    @Autowired
    private SubscriptionService subscription_service;
    
    /**
     * 
     * @param request
     * @param plan
     * 
     * @throws Exception
     */
    @PreAuthorize("hasAuthority('create:domains')")
    @RequestMapping(method = RequestMethod.PUT)
    public void subscribe(HttpServletRequest request,
					 		@RequestParam(value="plan", required=true) String plan,
					 		@RequestParam(value="source_token", required=true) String source_token) throws Exception {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	subscription_service.changeSubscription(acct, SubscriptionPlan.valueOf(plan.toUpperCase()), source_token);
    }
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class UnknownSubscriptionPlanException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560715915L;

	public UnknownSubscriptionPlanException() {
		super("Could not find the requested plan.");
	}
}