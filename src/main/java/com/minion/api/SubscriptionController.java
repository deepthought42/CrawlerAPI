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
import com.qanairy.models.repository.AccountRepository;
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
    SubscriptionController(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }
    
    @PutMapping
    public void subscribe(HttpServletRequest request,
					 		@RequestParam(value="plan", required=true) String plan) throws Exception {
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	Account acct = account_repo.findByUsername(username);
    	
    	if(plan == "pro"){
    		Plan pro_tier = Plan.retrieve("plan_Dr1tjSakC3uGXq");
    		
    		Customer customer = null;
    		if(acct.getCustomerToken() == null || acct.getCustomerToken().isEmpty()){
    			customer = this.stripeClient.createCustomer(null, username);
            	acct.setCustomerToken(customer.getId());
    		}
    		else{
    			customer = this.stripeClient.getCustomer(acct.getCustomerToken());
    		}
    		
        	Subscription subscription = null;
        	
        	if(acct.getSubscriptionToken() == null || acct.getSubscriptionToken().isEmpty()){
        		subscription = this.stripeClient.subscribe(pro_tier, customer);
        	}
        	else{
            	Map<String, Object> item = new HashMap<>();
            	subscription = Subscription.retrieve(acct.getSubscriptionToken());
            	item.put("id", subscription.getId());
            	item.put("plan", pro_tier.getId());

            	Map<String, Object> items = new HashMap<>();
            	items.put("0", item);

            	Map<String, Object> params = new HashMap<>();
            	params.put("items", items);
            	
            	subscription.update(params);
        	}
        	
        	acct.setSubscriptionToken(subscription.getId());
    	}
    	else {
    		throw new UnknownSubscriptionPlanException();
    	}
    }
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class UnknownSubscriptionPlanException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560715915L;

	public UnknownSubscriptionPlanException() {
		super("Could not find the requested plan.");
	}
}