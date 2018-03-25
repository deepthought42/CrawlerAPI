package com.minion.api;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
import com.stripe.model.Plan;
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
    
    public void update_subscription(Plan plan, Subscription subscription) 
    		throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
    	subscription.setPlan(plan);
    }
    
    public Subscription subscribe(Plan plan, Customer customer) 
    		throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
    	Map<String, Object> item = new HashMap<String, Object>();
    	item.put("plan", plan.getId());

    	Map<String, Object> items = new HashMap<String, Object>();
    	items.put("0", item);
    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("customer", customer.getId());
    	params.put("items", items);
    	
    	if(plan.getTrialPeriodDays()>0){
    		Calendar c = Calendar.getInstance();
    		c.add(Calendar.MONTH, 1);
    		Date date = c.getTime();
    		date.getTime();
    		params.put("trial_end", date.getTime()/1000);
    	}
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

	public Subscription getSubscription(String subscriptionToken) 
			throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {
		return Subscription.retrieve(subscriptionToken);
	}
}
