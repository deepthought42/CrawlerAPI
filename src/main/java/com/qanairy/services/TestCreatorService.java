package com.qanairy.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.TestRepository;

@Component
public class TestCreatorService {
	
	@Autowired
	DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	DomainRepository domain_repo;
	
	@Autowired
	PageStateRepository page_state_repo;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private BrowserService browser_service;
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
	public Test generate_landing_page_test(String browser_name, String discovery_key, String host, String url) 
			throws MalformedURLException, IOException, NullPointerException, GridException, WebDriverException, NoSuchAlgorithmException{
		
		Browser browser = new Browser(browser_name);

		browser.navigateTo(url);
		
		System.err.println("building page");
	  	PageState page_obj = browser_service.buildPage(browser);

	  	PageState page_record = page_state_repo.findByKey(page_obj.getKey());
	  	if(page_record == null){
		  	page_obj.setLandable(true);
	  		page_obj = page_state_repo.save(page_obj);
	  	}
	  	else{
	  		page_obj = page_record;
	  	}
	  	System.err.println("Page built");
	  	
	  	try{
	  		browser.close();
	  	}catch(Exception e){}
	  	
	  	List<String> path_keys = new ArrayList<String>();
	  	path_keys.add(page_obj.getKey());
	  	
	  	List<PathObject> path_objects = new ArrayList<PathObject>();
	  	path_objects.add(page_obj);
	  	
	  	DiscoveryRecord discovery_record = discovery_repo.findByKey( discovery_key);
		discovery_record.setLastPathRanAt(new Date());
		discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
		discovery_record.setTestCount(discovery_record.getTestCount()+1);
		discovery_record = discovery_repo.save(discovery_record);

		Domain domain = domain_repo.findByHost( host);
		
		MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

		System.err.println("result page elements count :: "+page_obj.getElements().size());
		return createTest(path_keys, path_objects, page_obj, 1L, domain ,discovery_record, browser_name);
	}
	
	/**
	 * Generates {@link Test Tests} for path
	 * @param path
	 * @param result_page
	 */
	private Test createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, Domain domain, DiscoveryRecord discovery, String browser_name ) {
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
