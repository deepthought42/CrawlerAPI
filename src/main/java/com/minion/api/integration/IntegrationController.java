package com.minion.api.integration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;


/**
 * 
 */
@RestController
@RequestMapping("/accounts")
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
	 */
    @RequestMapping(method = RequestMethod.GET)
	public XML runAllTests(@RequestBody String host,
						   @RequestBody String api_key){
    	Account acct = account_repo.getAccountByApiKey(api_key);
		Domain domain = account_repo.getAccountDomainByApiKeyAndHost(api_key, host);

		if(acct == null){
    		throw new InvalidApiKeyException();
    	}
    	
    	if(subscription_service.hasExceededSubscriptionTestRunsLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 test runs available. Upgrade now to run more tests");
        }
    	
    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("account", acct.getUsername());
        traits.put("api_key", acct.getApiToken());        
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(acct.getUsername())
    		    .traits(traits)
    		);
    	
    	//Fire discovery started event	
    	Set<Test> tests = domain_repo.getVerifiedTests(domain.getUrl());
	   	Map<String, String> run_test_batch_props= new HashMap<String, String>();
	   	run_test_batch_props.put("total tests", Integer.toString(tests.size()));
	   	analytics.enqueue(TrackMessage.builder("Running tests")
	   		    .userId(acct.getUsername())
	   		    .properties(run_test_batch_props)
	   		);
	   	
    	Map<String, TestRecord> test_results = new HashMap<String, TestRecord>();
    	
    	for(Test test : tests){
    		
			Browser browser_dto = new Browser(domain.getDiscoveryBrowserName());
			TestRecord record = test_service.runTest(test, browser_dto);
			browser_dto.close();
			    		
			test_results.put(test.getKey(), record);
			TestStatus is_passing = TestStatus.PASSING;
			//update overall passing status based on all browser passing statuses
			for(String status : test.getBrowserStatuses().values()){
				if(status.equals(TestStatus.UNVERIFIED) || status.equals(TestStatus.FAILING)){
					is_passing = TestStatus.FAILING;
					break;
				}
			}
    		
    		record = test_record_repo.save(record);
    		
	    	test.getBrowserStatuses().put(record.getBrowser(), record.getPassing().toString());			
    		
	    	test.addRecord(record);
			test.setStatus(is_passing);
			test.setLastRunTimestamp(new Date());
			test.setRunTime(record.getRunTime());
			test_repo.save(test);
			

			acct.addTestRecord(record);
			account_repo.save(acct);
   		}
		
		return test_results;
	}
}
