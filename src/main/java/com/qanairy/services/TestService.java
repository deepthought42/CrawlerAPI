package com.qanairy.services;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

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
import com.qanairy.models.Domain;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.TestRepository;

@Component
public class TestService {
	private static Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private TestRepository test_repo;
	
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
		 System.err.println("Test :: "+test);
		 System.err.println("TEST KEY S:: " + test.getPathKeys().size());
		 System.err.println("browser :: " + browser);
		 
		 try {
			page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, null);
			
			System.err.println("IS TEST CURRENTLY PASSING ??    "+test.getStatus()); 
			passing = Test.isTestPassing(test.getResult(), page, test.getStatus());
			
		    test.setBrowserStatus(browser.getBrowserName(), passing.toString());
		 } catch (IOException e) {		
			 log.error(e.getMessage());		
		 }	
		
		 final long pathCrawlEndTime = System.currentTimeMillis();

		 long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime ;
		 test_record = new TestRecord(new Date(), passing, browser.getBrowserName(), page, pathCrawlRunTime);

		 return test_record;		
	 }
	 
	 public Test save(Test test, String host_url){
		Test record = test_repo.findByKey(test.getKey());
			
		if(record == null){
			System.err.println("Test REPO :: "+test_repo);
			System.err.println("Test ::  "+test);
			test.setName("Test #" + (domain_repo.getTestCount(host_url)+1));
	  		
	  		test = test_repo.save(test);
			Domain domain = domain_repo.findByHost(host_url);
			domain.addTest(test);
			domain = domain_repo.save(domain);
			if(test.getBrowserStatuses() == null || test.getBrowserStatuses().isEmpty()){
				System.err.println("Broadcasting discovered test");
				
				try {
					MessageBroadcaster.broadcastDiscoveredTest(test, host_url);
				} catch (JsonProcessingException e) {
					log.error(e.getLocalizedMessage());
				}
			}
			else {
				System.err.println("Broadcasting Test...");
				try {
					MessageBroadcaster.broadcastTest(test, host_url);
				} catch (JsonProcessingException e) {
					log.error(e.getLocalizedMessage());
				}
			}
			
			for(PathObject path_obj : test.getPathObjects()){
				try {
					MessageBroadcaster.broadcastPathObject(path_obj, host_url);
				} catch (JsonProcessingException e) {
					log.error(e.getLocalizedMessage());
				}
			}
		}
		else{
			System.err.println("Test already exists  !!!!!!!");
		}
		
		return test;
	}
}
