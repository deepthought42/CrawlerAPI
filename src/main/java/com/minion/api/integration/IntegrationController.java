package com.minion.api.integration;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minion.api.exception.InvalidApiKeyException;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.services.TestService;
import com.qanairy.utils.JUnitXmlConversionUtil;

/**
 * 
 */
@RestController
@RequestMapping("/integrations")
public class IntegrationController {

	@Autowired
	private AccountRepository account_repo;
	
	@Autowired
	private TestService test_service;
	
	/**
	 * 
	 * @param api_key
	 * @return
	 * @throws InvalidApiKeyException
	 */
    @RequestMapping(path = "apiKeys", method = RequestMethod.GET)
	public List<Domain> getDomains(@RequestBody String api_key) throws InvalidApiKeyException {
		Account acct = account_repo.getAccountByApiKey(api_key);
		if(acct == null){
    		throw new InvalidApiKeyException("Invalid API key");
    	}
    	return account_repo.getDomainsByApiKey(api_key);
	}
	
    /**
	 * 
	 * @param api_key
	 * @return
	 * @throws InvalidApiKeyException
	 */
    @RequestMapping(path = "groups", method = RequestMethod.GET)
	public List<Group> getGroups(@RequestParam String api_key, @RequestParam String domain_id) throws InvalidApiKeyException {
		Account acct = account_repo.getAccountByApiKey(api_key);
		if(acct == null){
    		throw new InvalidApiKeyException("Invalid API key");
    	}
    	return account_repo.getGroupsByDomain(api_key);
	}
	
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
	 */
    @RequestMapping(method = RequestMethod.GET)
	public String runAllTests(@RequestBody String host,
						   @RequestBody String api_key) throws InvalidApiKeyException{
    	Account acct = account_repo.getAccountByApiKey(api_key);
		if(acct == null){
    		throw new InvalidApiKeyException("Invalid API key");
    	}
    	Domain domain = account_repo.getAccountDomainByApiKeyAndHost(api_key, host);

		/* UNCOMMENT WHEN READY TO HANDLE SUBSCRIPTIONS
		 
    	if(subscription_service.hasExceededSubscriptionTestRunsLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 test runs available. Upgrade now to run more tests");
        }
    	*/
		
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
