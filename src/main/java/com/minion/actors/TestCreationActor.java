package com.minion.actors;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.analytics.SegmentAnalyticsHelper;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.ActionService;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DomainService;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.TimingUtils;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * Creates new tests based on {@link JSONOBject} containing test steps as defined by the plugin
 */
@Component
@Scope("prototype")
public class TestCreationActor extends AbstractActor  {
	private static Logger log = LoggerFactory.getLogger(TestCreationActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private DomainService domain_service;

	@Autowired
	private ElementStateService page_element_service;

	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private TestRepository test_repo;

	@Autowired
	private ActionService action_service;


	//subscribe to cluster changes
	@Override
	public void preStart() {
	  cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
	      MemberEvent.class, UnreachableMember.class);
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
	  cluster.unsubscribe(getSelf());
    }

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, acct_message -> {
					if(acct_message.getData() instanceof JSONObject){
						JSONObject test_json = (JSONObject) acct_message.getData();
				    	JSONArray path_json = (JSONArray) test_json.get("path");
				    	String name = test_json.get("name").toString();
				    	String browser_name = acct_message.getOptions().get("browser").toString();
				    	int attempts = 0;
				    	Test test = null;
				    	Domain domain = acct_message.getDomain();
		    			do{
				    		List<String> path_keys = new ArrayList<String>();
				        	List<LookseeObject> path_objects = new ArrayList<LookseeObject>();
					    	Browser browser = null;

				    		try{
				    			browser = BrowserConnectionHelper.getConnection(BrowserType.create(browser_name), BrowserEnvironment.DISCOVERY);
				    			
				    			long start_time = System.currentTimeMillis();
				    			domain = buildTestPathFromPathJson(path_json, path_keys, path_objects, browser, acct_message.getAccountKey(), domain);
				    			long end_time = System.currentTimeMillis();
				    			TimingUtils.pauseThread(1500);
						
				    			PageState result_page = browser_service.buildPageState(acct_message.getAccountKey(), domain, browser);
								
				    			boolean leaves_domain = BrowserUtils.doesSpanMutlipleDomains(domain.getUrl(), result_page.getUrl(), path_objects);
								
								test = new Test(path_keys, path_objects, result_page, leaves_domain);
								test.setSpansMultipleDomains(leaves_domain);
								
						    	Test test_record = test_repo.findByKey(test.getKey(), domain.getUrl(), acct_message.getAccountKey());
						    	if(test_record == null){
						    		TestRecord test_record_record = new TestRecord();
						    		test_record_record.setBrowser(browser_name);
						    		test_record_record.setRanAt(new Date());
						    		test_record_record.setResult(result_page);
						    		test_record_record.setRunTime(end_time-start_time);
						    		test_record_record.setStatus(TestStatus.PASSING);

						    		test.addRecord(test_record_record);
						    		test.setStatus(TestStatus.PASSING);
							    	test.getBrowserStatuses().put(browser_name, TestStatus.PASSING.toString());

						    		test = test_repo.save(test);
						    		
						    		SegmentAnalyticsHelper.sendTestCreatedInRecorder(acct_message.getAccountKey(), test.getKey());

						    		log.warn("test creation domain url :: " + domain.getUrl());
						    		domain_service.addTest(domain.getUrl(), test, acct_message.getAccountKey());

						    		//here we check if the test passed in had a key indicating that it is an existing test. If it does have a key then we look up the test with the key
						    		// and set its status to archived
							    	if(test_json.get("key") != null && !test_json.get("key").toString().equals("null") && test_json.get("key").toString().length() > 0 ){
								    	Test old_test = test_repo.findByKey(test_json.get("key").toString(), domain.getUrl(), acct_message.getAccountKey());
							    		old_test.setArchived(true);
							    		test_repo.save(old_test);
								    }
						    	}
						    	else{
						    		test = test_record;
						    		test.setName(name);
						    		test = test_repo.save(test);
						    	}
				    		}
				    		catch(Exception e){
				    			log.warn("Error occurred while creating new test from IDE ::  "+e.getLocalizedMessage());
				    		}
				    		finally {
				    			if(browser != null){
				    				browser.close();
				    			}
				    		}
				    		attempts++;
				    	}while(test == null && attempts < 100000);

				    	MessageBroadcaster.broadcastTestCreatedConfirmation(test, acct_message.getAccountKey());
				    	MessageBroadcaster.broadcastTest(test, acct_message.getAccountKey());
					}
					postStop();
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.info("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.info("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.info("received unknown message");
				})
				.build();
	}

	private Domain buildTestPathFromPathJson(
				JSONArray path, 
				List<String> path_keys, 
				List<LookseeObject> path_objects, 
				Browser browser, 
				String user_id, 
				Domain domain
			) throws JSONException, Exception {
		boolean first_page = true;
		PageState page_state = null;

		for(int idx=0; idx < path.length(); idx++){
        	JSONObject path_obj_json = new JSONObject(path.get(idx).toString());

    		if(path_obj_json.has("url")){
    			String path_url = path_obj_json.getString("url");
    			if(first_page){
    				browser.navigateTo(path_url);
    			}
    			page_state = browser_service.buildPageState(user_id, domain, browser);
    			long start_time = System.currentTimeMillis();
			  	List<ElementState> elements = browser_service.extractElementStatesWithUserAndDomain(page_state.getSrc(), user_id, domain);
			  	long end_time = System.currentTimeMillis();
				log.warn("element state time to get all elements ::  "+(end_time-start_time));
				page_state.addElements(elements);
				page_state = page_state_service.saveUserAndDomain(user_id, domain.getUrl(), page_state);
				log.warn("DOM elements found :: "+elements.size());
				path_keys.add(page_state.getKey());
    			path_objects.add(page_state);
    			first_page = false;

    			path_keys.add(page_state.getKey());
    			path_objects.add(page_state);
    		}
    		else {
    			JSONObject element_json = path_obj_json.getJSONObject("element");

    			ElementState element = createElementState(user_id, element_json.getString("xpath"), browser);

    			ElementState page_elem_record = page_element_service.findByKeyAndUserId(user_id, element.getKey());
    			if(page_elem_record == null){
					path_objects.add(element);
				}
				else{
					element = page_elem_record;
				}

    			//add to path
    			path_keys.add(element.getKey());

    			JSONObject action_json = path_obj_json.getJSONObject("action");

    			//create new action
    			//add action to Test
    			String action_type = action_json.getString("name");
    			String action_value = action_json.getString("value");

    			Action action = createAction(action_type, action_value);
    			path_keys.add(action.getKey());
    			path_objects.add(action);

    			Crawler.performAction(action, element, browser.getDriver());

    			// CHECK IF NEXT OBJECT IS A URL BEFORE EXECUTING NEXT STEP.
    			// IF NEXT OBJECT DOESN'T CONTAIN A URL, THEN CREATE NEW PAGE STATE
	        	if(idx+1 < path.length()){
	    			path_obj_json = new JSONObject(path.get(idx+1).toString());

	    			if(!path_obj_json.has("url")){
		    			//capture new page state and add it to path
		    			page_state = browser_service.buildPageState(user_id, domain, browser);
		    			path_keys.add(page_state.getKey());
		    			path_objects.add(page_state);
	    			}
	        	}
    		}
    	}
		return domain;
	}

	private Action createAction(String action_type, String action_value) {
		Action action = new Action(action_type, action_value);
		Action action_record = action_service.findByKey(action.getKey());
		if(action_record != null){
			action = action_record;
		}

		return action;
	}

	private ElementState createElementState(String user_id, String temp_xpath, Browser browser) throws Exception {
		//use xpath to identify WebElement.
		WebElement element = browser.findWebElementByXpath(temp_xpath);
		//use WebElement to generate system usable xpath
		Map<String, String> attributes = browser.extractAttributes(element);
		BufferedImage img = browser.getElementScreenshot(element);
		String checksum = PageState.getFileChecksum(img);
		
		String xpath = browser_service.generateXpath(element, browser.getDriver(), attributes);
		//Map<String, String> css_map = Browser.loadCssProperties(element);
		ElementState elem = new ElementState(element.getText(), xpath, element.getTagName(), attributes, new HashMap<>(), "", element.getLocation().getX(), element.getLocation().getY(), element.getSize().getWidth(), element.getSize().getHeight(), element.getAttribute("innerHTML"), checksum, element.isDisplayed(), element.getAttribute("outerHTML"));
		String screenshot_url = UploadObjectSingleOperation.saveImageToS3ForUser(img, new URL(browser.getDriver().getCurrentUrl()).getHost(), checksum, BrowserType.create(browser.getBrowserName()), user_id);
		elem.setScreenshotUrl(screenshot_url);
		elem.setOuterHtml(element.getAttribute("outerHTML"));
		elem.setTemplate(BrowserService.extractTemplate(elem.getOuterHtml()));
		elem = page_element_service.save(elem);
		return elem;
	}
}
