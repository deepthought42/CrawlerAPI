package com.minion.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;

@Component
public class StripeClient {

    @Autowired
    StripeClient() {
        Stripe.apiKey = "sk_test_PWXpP3kfBOicqxW29nSilcK1";
    }

    public Charge chargeCreditCard(String token, int amount) throws Exception {
        Map<String, Object> chargeParams = new HashMap<String, Object>();
        chargeParams.put("amount", amount);
        chargeParams.put("currency", "USD");
        chargeParams.put("source", token);
        Charge charge = Charge.create(chargeParams);
        return charge;
    }
    
    public Subscription subscribe(String plan, String customer_key) 
    		throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
    	Map<String, Object> item = new HashMap<String, Object>();
    	item.put("plan", plan);

    	Map<String, Object> items = new HashMap<String, Object>();
    	items.put("discoveries", 4);
    	items.put("test", 10000);

    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("customer", customer_key);
    	
    	return Subscription.create(params);
    }
    
    public Customer createCustomer(String token, String email) throws Exception {
        Map<String, Object> customerParams = new HashMap<String, Object>();
        customerParams.put("email", email);
        customerParams.put("source", token);
        return Customer.create(customerParams);
    }
}
