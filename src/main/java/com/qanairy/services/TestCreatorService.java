package com.qanairy.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Group;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;

@Component
public class TestCreatorService {
	private static Logger log = LoggerFactory.getLogger(TestCreatorService.class.getName());

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private GroupService group_service;
	
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
	public Test createLandingPageTest(PageState page_state, String browser_name) 
			throws MalformedURLException, IOException, NullPointerException, GridException, WebDriverException, NoSuchAlgorithmException{
		page_state.setLandable(true);
		page_state.setLastLandabilityCheck(LocalDateTime.now());
		page_state = page_state_service.save(page_state);
  	  	
	  	List<String> path_keys = new ArrayList<String>();	  	
	  	List<PathObject> path_objects = new ArrayList<PathObject>();
	  	path_keys.add(page_state.getKey());
	  	path_objects.add(page_state);

	  	Test test = createTest(path_keys, path_objects, page_state, 1L, browser_name);
		
		String url = page_state.getUrl();
		if(!url.contains("http")){
			url = "http://"+url;
		}
		String url_path = new URL(url).getPath();
		url_path = url_path.replace("/", " ").trim();
		if(url_path.isEmpty()){
			url_path = "home";
		}
		test.setName(url_path + " page loaded");
		
		//add group "smoke" to test
		Group group = new Group("smoke");
		group = group_service.save(group);
		test.addGroup(group);
		
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
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), TestStatus.UNVERIFIED, browser_name, result_page, crawl_time);
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
			if(path_obj.getClass().equals(ElementState.class)){
				ElementState elem = (ElementState)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					test_service.save(test, test.firstPage().getUrl());
					break;
				}
			}
		}
	}
}
