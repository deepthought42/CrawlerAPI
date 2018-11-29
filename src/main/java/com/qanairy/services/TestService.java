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
import com.qanairy.api.exceptions.PagesAreNotMatchingException;
import com.qanairy.models.Domain;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.TestRepository;

@Component
public class TestService {
	private static Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private TestRepository test_repo;
	
	@Autowired
	private PageStateRepository page_repo;
	
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
	 public TestRecord runTest(Test test, Browser browser, TestStatus last_test_status) throws GridException, WebDriverException, NoSuchAlgorithmException{				
		 assert test != null;		
	 			
		 TestStatus passing = null;		
		 PageState page = null;
		 TestRecord test_record = null;
		 final long pathCrawlStartTime = System.currentTimeMillis();
		 
		 try {
			page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, null);
			passing = Test.isTestPassing(test.getResult(), page, last_test_status);
			
		 } catch (IOException e) {		
			 System.err.println(e.getMessage());		
		 } catch(PagesAreNotMatchingException e){
			 passing = TestStatus.FAILING;
			 test.setBrowserStatus(browser.getBrowserName(), TestStatus.FAILING.toString());
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
}
