package com.qanairy.services;

import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.StripeClient;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.SubscriptionPlan;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;

/**
 * 
 * 
 */
@Service
public class SubscriptionService {
	@Autowired
	private StripeClient stripe_client;
	
	@Autowired
	private AccountRepository account_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
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
	public boolean hasExceededSubscriptionTestRunsLimit(Account acct) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		SubscriptionPlan plan = getSubscriptionPlanName(acct);
    	
    	//check if user has exceeded freemium plan
    	Date date = new Date();
    	
    	int test_run_cnt = 0;
    	for(Domain domain : account_repo.getDomains(acct.getUsername())){
        	Set<TestRecord> test_records = domain_repo.getTestsByMonth(domain.getUrl(), date.getMonth());
        	test_run_cnt += test_records.size();
    	}
    	
    	if(plan.equals(SubscriptionPlan.FREE) && test_run_cnt > 50){
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
	
	public boolean hasExceededSubscriptionDiscoveredLimit(Account acct) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		SubscriptionPlan plan = getSubscriptionPlanName(acct);
    	
    	//check if user has exceeded freemium plan
    	Date date = new Date();
    	Set<DiscoveryRecord> discovery_records = account_repo.getDiscoveryRecordsByMonth(acct.getUsername(), date.getMonth());
    	int discovered_test_cnt = 0;
    	for(DiscoveryRecord record : discovery_records){
    		discovered_test_cnt += record.getTestCount();
    	}
    	
    	if(plan.equals(SubscriptionPlan.FREE) && discovered_test_cnt > 20){
    		return true;
    	}
    	else if(plan.equals(SubscriptionPlan.PRO) && discovered_test_cnt > 100){
    		return true;
    	}
    	else if(plan.equals(SubscriptionPlan.ENTERPRISE)){
    		return true;
    	}
    	
    	
    	return false;
	}
	
	
	private SubscriptionPlan getSubscriptionPlanName(Account acct) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		Subscription subscription = null;
    	
		SubscriptionPlan account_subscription = null;
    	if(acct.getSubscriptionToken() == null){
    		//free plan
    		account_subscription = SubscriptionPlan.FREE;
    	}
    	//pro/enterprise plan
    	else {
    		subscription = stripe_client.getSubscription(acct.getSubscriptionToken());
        	String subscription_item = null;
        	
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
