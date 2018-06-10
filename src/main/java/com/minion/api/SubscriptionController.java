package com.minion.api;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.StripeClient;
import com.qanairy.models.repository.AccountRepository;

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
    	Plan new_plan = Plan.retrieve(plan);
    	Subscription subscription = Subscription.retrieve(acct.getSubscriptionToken());
    	
    	Map<String, Object> item = new HashMap<>();
    	item.put("id", subscription.getSubscriptionItems().getData().get(0).getId());
    	item.put("plan", new_plan.getId());

    	Map<String, Object> items = new HashMap<>();
    	items.put("0", item);

    	Map<String, Object> params = new HashMap<>();
    	params.put("items", items);
    	
    	subscription.update(params);
    }
}