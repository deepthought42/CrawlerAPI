package com.qanairy.services;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.BrowserEnvironment;
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
	private ActionService action_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private PageElementService page_element_service;
	
	@Autowired
	private BrowserService browser_service;
	
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
	 public TestRecord runTest(Test test, String browser_name, TestStatus last_test_status) {				
		 assert test != null;		
	 			
		 TestStatus passing = null;		
		 PageState page = null;
		 TestRecord test_record = null;
		 final long pathCrawlStartTime = System.currentTimeMillis();
		 
		 int cnt = 0;
		 boolean pages_dont_match = false;
		 Browser browser = null;
		 do{
			 try {
				browser = browser_service.getConnection(browser_name.trim(), BrowserEnvironment.TEST);	
				page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, null, null);
			 } catch(PagesAreNotMatchingException e){
				 log.warn(e.getLocalizedMessage());
				 pages_dont_match = true;
			 }
			 catch (Exception e) {
				 log.error(e.getLocalizedMessage());
			 } 
			 finally{
				browser.close();
			 }
			 
			 cnt++;
		 }while(cnt < Integer.MAX_VALUE && page == null);
		
		 final long pathCrawlEndTime = System.currentTimeMillis();
		 long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
		 
		 if(pages_dont_match){
			return new TestRecord(new Date(), TestStatus.FAILING, browser_name.trim(), page, pathCrawlRunTime);
		 }
		 else{
			 passing = Test.isTestPassing(test.getResult(), page, last_test_status);
	 		 test_record = new TestRecord(new Date(), passing, browser_name.trim(), page, pathCrawlRunTime);
			 
			 return test_record;
		 }
	 }
	 
	 public Test save(Test test, String host_url){
		Test record = test_repo.findByKey(test.getKey());
				
		if(record == null){
			List<PathObject> path_objects = new ArrayList<PathObject>();
			for(PathObject path_obj : test.getPathObjects()){
				if(path_obj instanceof PageState){
					path_objects.add(page_state_service.save((PageState)path_obj));
				}
				else if(path_obj instanceof PageElement){
					path_objects.add(page_element_service.save((PageElement)path_obj));
				}
				else if(path_obj instanceof Action){
					path_objects.add(action_service.save((Action)path_obj));
				}
			}
			test.setPathObjects(path_objects);
			test.setResult(page_state_service.save(test.getResult()));
	
			log.info("Test REPO :: "+test_repo);
			log.info("Test ::  "+test);
			test.setName("Test #" + (domain_repo.getTestCount(host_url)+1));
	  		
	  		test = test_repo.save(test);
			Domain domain = domain_repo.findByHost(host_url);
			domain.addTest(test);
			domain = domain_repo.save(domain);
		
			try {
				MessageBroadcaster.broadcastDiscoveredTest(test, host_url);
			} catch (JsonProcessingException e) {
				log.error(e.getLocalizedMessage());
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
			
			log.info("Test already exists  !!!!!!!");
			try {
				MessageBroadcaster.broadcastTest(test, host_url);
			} catch (JsonProcessingException e) {
				log.error(e.getLocalizedMessage());
			}
			
			test = record;
			List<PathObject> path_objects = test_repo.getPathObjects(test.getKey());
			test.setPathObjects(path_objects);
		}
		
		return test;
	}
	 
	public void init(Crawler crawler, BrowserService browser_service){
		this.crawler = crawler;
		this.browser_service = browser_service;
	}
	
	public Test findByKey(String key){
		return test_repo.findByKey(key);
	}
}
