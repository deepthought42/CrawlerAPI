package com.qanairy.services;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.minion.structs.Message;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

@Component
public class TestCreatorService {
	private static Logger log = LoggerFactory.getLogger(TestCreatorService.class.getName());

	@Autowired
	private DomainRepository domain_repo;

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private ActorSystem actor_system;
	
	/**
	 * Generates a landing page test based on a given URL
	 * 
	 * @param browser
	 * @param msg
	 * 
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws WebDriverException 
	 * @throws GridException 
	 * 
	 * @pre browser != null
	 * @pre msg != null
	 */
	public Test generateLandingPageTest(String discovery_key, String host, String url, Browser browser, Message<?> message) 
			throws MalformedURLException, IOException, NullPointerException, GridException, WebDriverException, NoSuchAlgorithmException{
		
		browser.navigateTo(url);
		log.info("building page for landing test");
	  	PageState page_obj = browser_service.buildPage(browser);
	  	page_obj.setLandable(true);
	  	page_obj.setLastLandabilityCheck(LocalDateTime.now());
  		page_obj = page_state_service.save(page_obj);
  	
  	//SEND RESULT PAGE TO MEMORY REGISTRY ACTOR
		Message<PageState> page_state_msg = new Message<PageState>(message.getAccountKey(), page_obj, message.getOptions());

	  	final ActorRef memory_registry_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				  .props("memoryRegistryActor"), "memory_registry_actor"+UUID.randomUUID());
	  	memory_registry_actor.tell(page_state_msg, null );
	  	
	  	List<String> path_keys = new ArrayList<String>();	  	
	  	List<PathObject> path_objects = new ArrayList<PathObject>();
	  	path_keys.add(page_obj.getKey());
	  	path_objects.add(page_obj);

	  	log.info("path keys size ::   " + path_keys.size());
	  	log.info("Path objects size   :::   " + path_objects.size());
		Domain domain = domain_repo.findByHost( host);
		Test test = createTest(path_keys, path_objects, page_obj, 1L, browser.getBrowserName());
		test = test_service.save(test, domain.getUrl());
		
		return test;
	}
	
	/**
	 * Generates {@link Test Tests} for path
	 * @param path
	 * @param result_page
	 */
	private Test createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, String browser_name ) {
		assert path_keys != null;
		assert path_objects != null;
		
		Test test = new Test(path_keys, path_objects, result_page, null);						
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), TestStatus.UNVERIFIED, browser_name, test.getResult(), crawl_time);
		test.addRecord(test_record);

		return test;
	}
	
	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 * 
	 * @param test {@linkplain Test} that you want to label
	 */
	private void addFormGroupsToPath(Test test) {
		//check if test has any form elements
		for(PathObject path_obj: test.getPathObjects()){
			if(path_obj.getClass().equals(PageElement.class)){
				PageElement elem = (PageElement)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					test_service.save(test, test.firstPage().getUrl());
					break;
				}
			}
		}
	}
}
