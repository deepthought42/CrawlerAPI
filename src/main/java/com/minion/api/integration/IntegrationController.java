package com.minion.api.integration;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.minion.api.exception.InvalidApiKeyException;
import com.minion.api.exception.PaymentDueException;
import com.minion.browsing.Browser;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.TestRecordRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.SubscriptionService;
import com.qanairy.services.TestService;
import com.qanairy.utils.JUnitXmlConversionUtil;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;


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
	private TestRecordRepository test_record_repo;
	
	@Autowired
	private TestRepository test_repo;
	
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
	 */
    @RequestMapping(method = RequestMethod.GET)
	public String runAllTests(@RequestBody String host,
						   @RequestBody String api_key) throws InvalidApiKeyException{
    	Account acct = account_repo.getAccountByApiKey(api_key);
    	Domain domain = account_repo.getAccountDomainByApiKeyAndHost(api_key, host);
		if(acct == null){
    		throw new InvalidApiKeyException("Invalid API key");
    	}
    	
		/* UNCOMMENT WHEN READY TO HANDLE SUBSCRIPTIONS
		 
    	if(subscription_service.hasExceededSubscriptionTestRunsLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 test runs available. Upgrade now to run more tests");
        }
    	*/
		
    	List<TestRecord> test_results = test_service.runAllTests(acct, domain);
    	
    	//Generate junit xml doc 
    	return JUnitXmlConversionUtil.convertToJUnitXml(test_results);
	}
}
