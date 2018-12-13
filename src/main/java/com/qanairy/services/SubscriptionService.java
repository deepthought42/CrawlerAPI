package com.qanairy.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.StripeClient;

import com.qanairy.models.enums.SubscriptionPlan;
import com.qanairy.models.repository.AccountRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;

/**
 * Provides methods to check if an {@link Account} user has permission to access a restricted resource and verifying that
 * the {@link Account} user has not exceeded their usage.
 * 
 */
@Service
public class SubscriptionService {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private StripeClient stripe_client;
	
	@Autowired
	private AccountRepository account_repo;
	
	
	
	public SubscriptionService(AccountRepository account_repo){
		this.account_repo = account_repo;
	}

	/**
	 * Updates the {@link Subscription} for a given {@link Account}
	 * 
	 * @param acct
	 * @param plan
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	public void changeSubscription(Account acct, SubscriptionPlan plan, String source_token) throws Exception{
		Plan plan_tier = null;
		System.err.println("Change subscription to plan :: " + plan.toString());
		if("FREE".equals(plan.toString())){
			//check if account has a subscription, if so then unsubscribe and remove subscription token
			if(acct.getSubscriptionToken() != null && 
					!acct.getSubscriptionToken().isEmpty()){
	    		stripe_client.cancelSubscription(acct.getSubscriptionToken());
	    		acct.setSubscriptionToken("");
	    		acct.setSubscriptionType("FREE");
	    		account_repo.save(acct);
			}
			else{
				log.warn("User already has free plan");
			}
		}
		else if("PRO".equals(plan.toString())){
			//STAGING
    		//plan_tier = Plan.retrieve("plan_Dr1tjSakC3uGXq");
    		//PRODUCTION
			plan_tier = Plan.retrieve("plan_DuOeI8iaT85x2h");

			Customer customer = null;
			if(acct.getCustomerToken() == null || acct.getCustomerToken().isEmpty()){
				customer = stripe_client.createCustomer(source_token, acct.getUsername());
	        	acct.setCustomerToken(customer.getId());
			}
			else{
				customer = stripe_client.getCustomer(acct.getCustomerToken());
			}
			
	    	Subscription subscription = null;
	    	
	    	if(acct.getSubscriptionToken() == null || acct.getSubscriptionToken().isEmpty()){
	    		subscription = stripe_client.subscribe(plan_tier, customer);
	    	}else{
	        	Map<String, Object> item = new HashMap<>();
	        	subscription = Subscription.retrieve(acct.getSubscriptionToken());
	        	item.put("id", subscription.getId());
	        	item.put("plan", plan_tier.getId());
	
	        	Map<String, Object> items = new HashMap<>();
	        	items.put("0", item);
	
	        	Map<String, Object> params = new HashMap<>();
	        	params.put("items", items);
	        	
	        	subscription.update(params);
	    	}
	    	
	    	acct.setSubscriptionToken(subscription.getId());
    		acct.setSubscriptionType("PRO");
	    	account_repo.save(acct);
		}
	}
	
	/**
	 * checks if user has exceeded test run limit for their subscription
	 * 
	 * @param acct {@link Account}
	 * 
	 * @return true if user has exceeded limits for their {@link SubscriptionPlan}, otherwise false
	 * 
	 * @throws StripeException
	 */
	public boolean hasExceededSubscriptionTestRunsLimit(Account acct, SubscriptionPlan plan) throws StripeException{
    	//check if user has exceeded freemium plan
    	Date date = new Date();
    	int test_run_cnt = account_repo.getTestCountByMonth(acct.getUsername(), date.getMonth());
    	
    	if(plan.equals(SubscriptionPlan.FREE) && test_run_cnt > 100){
    		return true;
    	}
    	else if(plan.equals(SubscriptionPlan.PRO) && test_run_cnt > 500){
    		return true;
    	}
    	else if(plan.equals(SubscriptionPlan.ENTERPRISE)){
    		return true;
    	}
    	
    	return false;
	}
	
	/**
	 * Checks if a user has exceeded their {@link Subscription} limit on {@link Discovery}s
	 * 
	 * @param acct {@link Account}
	 * @param plan {@Subscription}
	 * 
	 * @return true if user has exceeded the limits for their {@SubscriptionPlan}
	 * 
	 * @throws StripeException
	 */
	public boolean hasExceededSubscriptionDiscoveredLimit(Account acct, SubscriptionPlan plan) throws StripeException{    	
    	//check if user has exceeded freemium plan
    	Date date = new Date();
    	Set<DiscoveryRecord> discovery_records = account_repo.getDiscoveryRecordsByMonth(acct.getUsername(), date.getMonth());
    	int discovered_test_cnt = 0;
    	
    	for(DiscoveryRecord record : discovery_records){
    		if(record.getStartTime().getMonth() == date.getMonth()){
    			discovered_test_cnt += record.getTestCount();
    		}
    	}
    	
    	if(plan.equals(SubscriptionPlan.FREE) && discovered_test_cnt > 50){
    		return true;
    	}
    	else if(plan.equals(SubscriptionPlan.PRO) && discovered_test_cnt > 250){
    		return true;
    	}
    	else if(plan.equals(SubscriptionPlan.ENTERPRISE)){
    		return true;
    	}
    	
    	
    	return false;
	}
	
	/**
	 * Uses the user {@link Account} to retrieve a subscription
	 * 
	 * @param acct
	 * @return
	 * @throws StripeException
	 */
	public SubscriptionPlan getSubscriptionPlanName(Account acct) throws StripeException {
		Subscription subscription = null;
		SubscriptionPlan account_subscription = null;
    	if(acct.getSubscriptionToken() == null || acct.getSubscriptionToken().isEmpty()){
    		//free plan
    		account_subscription = SubscriptionPlan.FREE;
    	}
    	//pro/enterprise plan
    	else {
    		subscription = stripe_client.getSubscription(acct.getSubscriptionToken());        	
        	//check for product
        	Plan plan = subscription.getPlan();
        	if(plan.getId().equals("plan_Dr1tjSakC3uGXq")){
        		account_subscription = SubscriptionPlan.PRO;
        	}
        	else{
        		account_subscription = SubscriptionPlan.ENTERPRISE;
        	}
    	}
    	
    	return account_subscription;
	}
}
