package com.minion.api;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private StripeClient stripeClient;

    @Autowired
    PaymentController(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    @PostMapping("/charge")
    public Charge chargeCard(@RequestParam(value="token", required=true) String token,
    						 @RequestParam(value="amount", required=true) int amount) throws Exception {
        return this.stripeClient.chargeCreditCard(token, amount);
    }
    
    @PostMapping("/subscribe")
    public Subscription subscribe(HttpServletRequest request,
    								@RequestParam(value="token", required=true) String token,
    						 		@RequestParam(value="plan", required=true) String plan) throws Exception {
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	
    	Map<String, Object> customerParams = new HashMap<String, Object>();
    	customerParams.put("description", "Customer for "+username);
    	customerParams.put("source", token);
    	// ^ obtained with Stripe.js
    	//Customer customer = Customer.create(customerParams);
    	Customer customer = this.stripeClient.createCustomer(auth_access_token, username);
    	Subscription subscription = this.stripeClient.subscribe(plan, customer.getId());
    	System.err.println("Subscription :: "+subscription.toJson());
    	
    	return subscription;
    }
}