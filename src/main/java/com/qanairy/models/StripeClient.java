package com.qanairy.models;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.stripe.Stripe;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;

@Component
public class StripeClient {

    @Autowired
    StripeClient() {
        //Stripe.apiKey = "sk_live_Gx56wLPtGpq8JXcg9UWaRcv9";
    	Stripe.apiKey = "sk_test_PWXpP3kfBOicqxW29nSilcK1";
    }
    
    public void update_subscription(Plan plan, Subscription subscription) 
    		throws AuthenticationException, InvalidRequestException, CardException{
    	subscription.setPlan(plan);
    }
    
    public Subscription subscribe(Plan plan, Customer customer) 
    		throws StripeException{
    	Map<String, Object> item = new HashMap<String, Object>();
    	item.put("plan", plan.getId());

    	Map<String, Object> items = new HashMap<String, Object>();
    	items.put("0", item);
    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("customer", customer.getId());
    	params.put("items", items);
    	
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, 1);
		Date date = c.getTime();
		date.getTime();
		params.put("trial_end", date.getTime()/1000);

		return Subscription.create(params);
    }

    public Subscription subscribe(Plan discovery, Plan tests, Customer customer) 
    		throws StripeException{
    	Map<String, Object> discovery_plan = new HashMap<String, Object>();
    	discovery_plan.put("plan", discovery.getId());

    	Map<String, Object> test_plan = new HashMap<String, Object>();
    	test_plan.put("plan", tests.getId());
    	
    	Map<String, Object> items = new HashMap<String, Object>();
    	items.put("0", discovery_plan);
    	items.put("1", test_plan);
    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("customer", customer.getId());
    	params.put("items", items);
    	
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, 3);
		Date date = c.getTime();
		date.getTime();
		params.put("trial_end", date.getTime()/1000);

		return Subscription.create(params);
    }
    
    public Customer createCustomer(String token, String email) throws Exception {
        Map<String, Object> customerParams = new HashMap<String, Object>();
        customerParams.put("email", email);
        if(token != null){
        	customerParams.put("source", token);
        }
    	return Customer.create(customerParams);
    }

    public Customer getCustomer(String customer_id) throws Exception {
        return Customer.retrieve(customer_id);
    }

	public Subscription getSubscription(String subscriptionToken) 
			throws StripeException {
		return Subscription.retrieve(subscriptionToken);
	}
	
	public Subscription cancelSubscription(String subscription_token) throws StripeException {
		Subscription subscription = Subscription.retrieve(subscription_token);
		return subscription.cancel(null);
	}

	public Customer deleteCustomer(String customer_token) throws StripeException {
		Customer customer = Customer.retrieve(customer_token);
		return customer.delete();
	}

}
