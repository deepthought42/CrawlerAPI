package com.minion.api;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.StripeClient;
import com.qanairy.models.enums.SubscriptionPlan;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.services.SubscriptionService;
import com.stripe.model.Customer;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;

@RestController
@RequestMapping("/subscribe")
public class SubscriptionController {

    private StripeClient stripeClient;
    
    @Autowired
    AccountRepository account_repo;
    
    @Autowired
    SubscriptionService subscription_service;
    
    @Autowired
    SubscriptionController(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }
    
    /**
     * 
     * @param request
     * @param plan
     * 
     * @throws Exception
     */
    @PutMapping
    public void subscribe(HttpServletRequest request,
					 		@RequestParam(value="plan", required=true) String plan) throws Exception {
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	Account acct = account_repo.findByUsername(username);
    	
    	subscription_service.changeSubscription(acct, SubscriptionPlan.valueOf(plan));
    }
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class UnknownSubscriptionPlanException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560715915L;

	public UnknownSubscriptionPlanException() {
		super("Could not find the requested plan.");
	}
}