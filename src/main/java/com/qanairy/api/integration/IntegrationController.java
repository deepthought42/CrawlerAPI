package com.qanairy.api.integration;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.minion.api.exception.InvalidApiKeyException;
import com.minion.api.exception.PaymentDueException;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.services.SubscriptionService;
import com.qanairy.services.TestService;
import com.qanairy.utils.JUnitXmlConversionUtil;
import com.stripe.exception.StripeException;

/**
 * 
 */
@RestController
@RequestMapping("/integrations")
public class IntegrationController {

	@Autowired
	private AccountRepository account_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private SubscriptionService subscription_service;
	
	/**
	 * Runs all tests for a given domain and account using an api key to locate the account
	 * In the event that the domain is not registered with the {@link Account} then the system throws
	 * an exception
	 * 
	 * @param domain
	 * @param api_key
	 * 
	 * @return
	 * @throws InvalidApiKeyException 
	 * @throws PaymentDueException 
	 * @throws StripeException 
	 */
    @RequestMapping(method = RequestMethod.GET)
	public String runAllTests(@RequestBody String host,
						   @RequestBody String api_key) throws InvalidApiKeyException, PaymentDueException, StripeException{
    	Account acct = account_repo.getAccountByApiKey(api_key);
		if(acct == null){
    		throw new InvalidApiKeyException("Invalid API key");
    	}
    	
    	Domain domain = domain_repo.findByHost(host, acct.getUserId());

    	if(subscription_service.hasExceededSubscriptionTestRunsLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 test runs available. Upgrade now to run more tests");
        }    	
		
		Date start_date = new Date();
		long start = System.currentTimeMillis();
    	List<TestRecord> test_results = test_service.runAllTests(acct, domain);
    	long end = System.currentTimeMillis();
    	
    	long time_in_sec = (end-start)/1000;
    	
    	int failing_cnt = 0;
    	//count passing and failing tests
    	for(TestRecord record : test_results){
    		if(record.getStatus().equals(TestStatus.FAILING)){
    			failing_cnt++;
    		}
    	}
    	
    	//Generate junit xml doc 
    	return JUnitXmlConversionUtil.convertToJUnitXml(test_results, failing_cnt, time_in_sec, start_date);
	}
}
