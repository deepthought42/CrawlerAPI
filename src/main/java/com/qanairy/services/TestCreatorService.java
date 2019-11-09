package com.qanairy.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jca.context.SpringContextResourceAdapter;
import org.springframework.stereotype.Component;

import com.qanairy.models.Group;
import com.qanairy.models.PageLoadAnimation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qanairy.analytics.SegmentAnalyticsHelper;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.Transition;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.message.AccountRequest;
import com.qanairy.models.message.DiscoveryActionRequest;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.PathUtils;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

@Component
public class TestCreatorService {
	private static Logger log = LoggerFactory.getLogger(TestCreatorService.class.getName());
	
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
	public Test createLandingPageTest(PageState page_state, String browser_name, Transition transition, PageLoadAnimation animation, Domain domain)
			throws MalformedURLException, IOException, NullPointerException, GridException, WebDriverException, NoSuchAlgorithmException{
		page_state.setLandable(true);
		page_state.setLastLandabilityCheck(LocalDateTime.now());

	  	List<String> path_keys = new ArrayList<String>();
	  	List<PathObject> path_objects = new ArrayList<PathObject>();
	  	if(transition != null 
	  			&& ((transition instanceof Redirect && ((Redirect)transition).getUrls().size() > 1))){
	  		path_keys.add(transition.getKey());
	  		path_objects.add(transition);
	  	}
	  	
	  	if(animation != null){
	  		path_keys.add(animation.getKey());
	  		path_objects.add(animation);
	  	}

	  	path_keys.add(page_state.getKey());
	  	path_objects.add(page_state);
	  	
	  	log.warn("domain url :: "+domain.getUrl());
	  	URL domain_url = new URL(domain.getProtocol()+"://"+domain.getUrl());
	  	
	  	Test test = createTest(path_keys, path_objects, page_state, 1L, browser_name, domain_url.getHost());

	  	String url = BrowserUtils.sanitizeUrl(page_state.getUrl());
		
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
	 * Generates {@link Test Tests} for test
	 * @param test
	 * @param result_page
	 * @throws JsonProcessingException
	 * @throws MalformedURLException
	 */
	public Test createTest(
			List<String> path_keys, 
			List<PathObject> path_objects, 
			PageState result_page, 
			long crawl_time, 
			String browser_name, 
			String domain_host,
			ActorRef discovery_actor
	) throws JsonProcessingException, MalformedURLException {
		assert path_keys != null;
		assert path_objects != null;
		
		log.warn("Creating test........");
		boolean leaves_domain = !(domain_host.trim().equals(new URL(result_page.getUrl()).getHost()) || result_page.getUrl().contains(new URL(PathUtils.getLastPageState(path_objects).getUrl()).getHost()));
		Test test = new Test(path_keys, path_objects, result_page, false, leaves_domain);

		Test test_db = test_service.findByKey(test.getKey());
		if(test_db == null){
			test.setRunTime(crawl_time);
			test.setLastRunTimestamp(new Date());
			addFormGroupsToPath(test);

			TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), TestStatus.UNVERIFIED, browser_name, result_page, crawl_time, test.getPathKeys());
			test.addRecord(test_record);
			
			Timeout timeout = Timeout.create(Duration.ofSeconds(120));
			Future<Object> future = Patterns.ask(discovery_actor, new AccountRequest(), timeout);
			Account account;
			try {
				account = (Account) Await.result(future, timeout.duration());
				SegmentAnalyticsHelper.testCreated(account.getUserId(), test.getKey());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return test;
		}

		return test;
	}

	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 *
	 * @param test {@linkplain Test} that you want to label
	 * @throws MalformedURLException 
	 */
	private void addFormGroupsToPath(Test test) throws MalformedURLException {
		//check if test has any form elements
		for(PathObject path_obj: test.getPathObjects()){
			if(path_obj.getClass().equals(ElementState.class)){
				ElementState elem = (ElementState)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					//test_service.save(test);
					break;
				}
			}
		}
	}
}
