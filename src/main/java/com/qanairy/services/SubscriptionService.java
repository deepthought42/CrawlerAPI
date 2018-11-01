package com.qanairy.services;

import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.StripeClient;

import com.qanairy.models.enums.SubscriptionPlan;
import com.qanairy.models.repository.AccountRepository;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;

/**
 * Provides methods to check if an {@link Account} user has permission to access a restricted resource and verifying that
 * the {@link Account} user has not exceeded their usage.
 * 
 */
@Service
public class SubscriptionService {
	@Autowired
	private StripeClient stripe_client;
	
	@Autowired
	private AccountRepository account_repo;
	
	public SubscriptionService(AccountRepository account_repo){
		this.account_repo = account_repo;
	}
	/**
	 * checks if user has exceeded test run limit for their subscription
	 * @param acct
	 * @return
	 * @throws APIException 
	 * @throws CardException 
	 * @throws APIConnectionException 
	 * @throws InvalidRequestException 
	 * @throws AuthenticationException 
	 */
	public boolean hasExceededSubscriptionTestRunsLimit(Account acct, SubscriptionPlan plan) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
    	//check if user has exceeded freemium plan
    	Date date = new Date();
    	int test_run_cnt = account_repo.getTestCountByMonth(acct.getUsername(), date.getMonth());
    	
    	if(plan.equals(SubscriptionPlan.FREE) && test_run_cnt > 100){
    		return true;
    	}
    	else if(plan.equals(SubscriptionPlan.PRO) && test_run_cnt > 5000){
    		return true;
    	}
    	else if(plan.equals(SubscriptionPlan.ENTERPRISE)){
    		return true;
    	}
    	
    	return false;
	}
	
	public boolean hasExceededSubscriptionDiscoveredLimit(Account acct, SubscriptionPlan plan) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{    	
    	//check if user has exceeded freemium plan
    	Date date = new Date();
    	Set<DiscoveryRecord> discovery_records = account_repo.getDiscoveryRecordsByMonth(acct.getUsername(), date.getMonth());
    	int discovered_test_cnt = 0;

    	for(DiscoveryRecord record : discovery_records){
    		discovered_test_cnt += record.getTestCount();
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
	
	
	public SubscriptionPlan getSubscriptionPlanName(Account acct) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		Subscription subscription = null;
		SubscriptionPlan account_subscription = null;
    	if(acct.getSubscriptionToken() == null){
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
