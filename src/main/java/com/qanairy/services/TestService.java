package com.qanairy.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.TestRecordRepository;
import com.qanairy.models.repository.TestRepository;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;

@Component
public class TestService {
	private static Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	private AccountRepository account_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private TestRepository test_repo;
	
	@Autowired
	private PageStateRepository page_repo;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private TestRecordRepository test_record_repo;
	
	@Autowired
	private Crawler crawler;
	
	/**		
	 * Runs an {@code Test} 		
	 * 		
	 * @param test test to be ran		
	 * 		
	 * @pre test != null		
	 * @return	{@link TestRecord} indicating passing status and {@link Page} if not passing 
	 * @throws NoSuchAlgorithmException 
	 * @throws WebDriverException 
	 * @throws GridException 
	 */		
	 public TestRecord runTest(Test test, Browser browser) throws GridException, WebDriverException, NoSuchAlgorithmException{				
		 assert test != null;		
	 			
		 TestStatus passing = null;		
		 PageState page = null;
		 TestRecord test_record = null;
		 final long pathCrawlStartTime = System.currentTimeMillis();
		 log.info("Test :: "+test);
		 log.info("TEST KEY S:: " + test.getPathKeys().size());
		 log.info("browser :: " + browser);
		 
		 try {
			page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, null);
			
			log.info("IS TEST CURRENTLY PASSING ??    "+test.getStatus()); 
			passing = Test.isTestPassing(test.getResult(), page, test.getStatus());
			
		    test.setBrowserStatus(browser.getBrowserName(), passing.toString());
		 } catch (IOException e) {		
			 log.error(e.getMessage());		
		 }	
		
		 final long pathCrawlEndTime = System.currentTimeMillis();
		 PageState page_record = page_repo.findByKey(page.getKey());
		 if(page_record == null){
			 page_record = page_repo.save(page);
		 }
		 long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime ;
		 test_record = new TestRecord(new Date(), passing, browser.getBrowserName(), page_record, pathCrawlRunTime);

		 return test_record;		
	 }
	 
	 public Test save(Test test, String host_url){
		Test record = test_repo.findByKey(test.getKey());
			
		if(record == null){
			log.info("Test REPO :: "+test_repo);
			log.info("Test ::  "+test);
			test.setName("Test #" + (domain_repo.getTestCount(host_url)+1));
	  		
	  		Test new_test = test_repo.save(test);
			Domain domain = domain_repo.findByHost(host_url);
			domain.addTest(new_test);
			domain = domain_repo.save(domain);
			if(new_test.getBrowserStatuses() == null || new_test.getBrowserStatuses().isEmpty()){
				log.info("Broadcasting discovered test");
				
				try {
					MessageBroadcaster.broadcastDiscoveredTest(new_test, host_url);
				} catch (JsonProcessingException e) {
					log.error(e.getLocalizedMessage());
				}
			}
			else {
				log.info("Broadcasting Test...");
				try {
					MessageBroadcaster.broadcastTest(new_test, host_url);
				} catch (JsonProcessingException e) {
					log.error(e.getLocalizedMessage());
				}
			}
			
			for(PathObject path_obj : new_test.getPathObjects()){
				try {
					MessageBroadcaster.broadcastPathObject(path_obj, host_url);
				} catch (JsonProcessingException e) {
					log.error(e.getLocalizedMessage());
				}
			}
			return new_test;
		}
		else{
			log.info("Test already exists  !!!!!!!");
		}
		
		return test;
	}
	 
	 public List<TestRecord> runAllTests(Account acct, Domain domain) throws MalformedURLException, NullPointerException, GridException, WebDriverException, NoSuchAlgorithmException{
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
    	List<TestRecord> test_records = new ArrayList<TestRecord>();
    	
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
    		test_records.add(record);
    		
	    	test.getBrowserStatuses().put(record.getBrowser(), record.getPassing().toString());			
    		
	    	test.addRecord(record);
			test.setStatus(is_passing);
			test.setLastRunTimestamp(new Date());
			test.setRunTime(record.getRunTime());
			test_repo.save(test);

			acct.addTestRecord(record);
			account_repo.save(acct);
   		}
    	
    	return test_records;
	 }
}
