package com.minion.actors;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserFactory;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.minion.util.Timing;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.Domain;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.PageElementRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.BrowserService;

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
	private static Logger log = LoggerFactory.getLogger(WorkAllocationActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private DomainRepository domain_repo;

	@Autowired
	private PageElementRepository page_element_repo;

	@Autowired
	private TestRepository test_repo;

	@Autowired
	private ActionRepository action_repo;


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
				    	JSONArray path = (JSONArray) test_json.get("path");
				    	String name = test_json.get("name").toString();

				    	int attempts = 0;
				    	Test test = null;
				    	Domain domain = null;

				    	do{
				    		List<String> path_keys = new ArrayList<String>();
				        	List<PathObject> path_objects = new ArrayList<PathObject>();
					    	Browser browser = null;
					    	
				    		try{
				    			browser = BrowserFactory.buildBrowser(domain.getDiscoveryBrowserName(), BrowserEnvironment.TEST);
				    			domain = buildTestPathFromPathJson(path, path_keys, path_objects, browser);

				    			PageState result_page = browser_service.buildPage(browser);
						    	test = new Test(path_keys, path_objects, result_page, name);
						    	test.setStatus(TestStatus.PASSING);
						    	test.getBrowserStatuses().put("chrome", TestStatus.PASSING.toString());

						    	Test test_record = test_repo.findByKey(test.getKey());
						    	if(test_record == null){
						    		test = test_repo.save(test);
						    		domain.addTest(test);
							    	domain_repo.save(domain);

							    	if(test_json.get("key") != null && !test_json.get("key").toString().equals("null") && test_json.get("key").toString().length() > 0 ){
								    	Test old_test = test_repo.findByKey(test_json.get("key").toString());
							    		old_test.setArchived(true);
							    		test_repo.save(old_test);
								    }
						    	}
						    	else{
						    		test = test_record;
						    		test.setName(name);
						    		test = test_repo.save(test);
						    	}
						    	browser.close();
						    	break;
				    		}
				    		catch(Exception e){
				    			log.warn("Error occurred while creating new test from IDE ::  "+e.getLocalizedMessage());
				    			e.printStackTrace();
				    			browser.close();
				    		}
				    		browser.close();
				    		attempts++;
				    	}while(test == null && attempts < 10000);

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

	private Domain buildTestPathFromPathJson(JSONArray path, List<String> path_keys, List<PathObject> path_objects, Browser browser) throws JSONException, Exception {
		boolean first_page = true;
		Domain domain = null;
		for(int idx=0; idx < path.length(); idx++){
        	JSONObject path_obj_json = new JSONObject(path.get(idx).toString());

    		if(path_obj_json.has("url")){
    			System.err.println("PATH OBJECT IS A URL :: " + path_obj_json);
    			String url = path_obj_json.getString("url");
    			String host = new URL(url).getHost();

    			if(!first_page){
    				PageState page_state = browser_service.buildPage(browser);
    				path_keys.add(page_state.getKey());
	    			path_objects.add(page_state);
    			}
    			else{
    				int dot_idx = host.indexOf('.');
    		    	int last_dot_idx = host.lastIndexOf('.');
    		    	String formatted_url = host;
    		    	if(dot_idx == last_dot_idx){
    		    		formatted_url = "www."+host;
    		    	}
    				domain = domain_repo.findByHost(formatted_url);
    			}

    			PageState page_state = navigateToAndCreatePageState(url, browser);

    			first_page = false;
    			path_keys.add(page_state.getKey());
    			path_objects.add(page_state);
    		}
    		else {
    			System.err.println("ELEMENT IN JSON :: " + path_obj_json.getJSONObject("element").toString());
    			JSONObject element_json = path_obj_json.getJSONObject("element");

    			PageElement element = createPageElement(element_json.getString("xpath"), browser);
    			//add to path
    			path_keys.add(element.getKey());
    			path_objects.add(element);

    			System.err.println("ACTION IN JSON ::  " + path_obj_json.getJSONObject("action"));
    			JSONObject action_json = path_obj_json.getJSONObject("action");

    			//create new action
    			//add action to Test
    			String action_type = action_json.getString("name");
    			String action_value = action_json.getString("value");

    			Action action = createAction(action_type, action_value);
    			path_keys.add(action.getKey());
    			path_objects.add(action);

    			Crawler.performAction(action, element, browser.getDriver());
    			Timing.pauseThread(5000L);

    			//******************************************************
    			// CHECK IF NEXT OBJECT IS  A URL BEFORE EXECUTING NEXT STEP.
    			// IF NEXT OBJECT DOESN'T CONTAIN A URL, THEN CREATE NEW PAGE STATE
    			//******************************************************
	        	if(idx+1 < path.length()){
	    			path_obj_json = new JSONObject(path.get(idx+1).toString());

	    			if(!path_obj_json.has("url")){
		    			//capture new page state and add it to path
		    			PageState page_state = browser_service.buildPage(browser);
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
		Action action_record = action_repo.findByKey(action.getKey());
		if(action_record != null){
			action = action_record;
		}

		return action;
	}

	private PageElement createPageElement(String temp_xpath, Browser browser) throws Exception {
		//use xpath to identify WebElement.
		WebElement element = browser.findWebElementByXpath(temp_xpath);
		//use WebElement to generate system usable xpath
		Set<Attribute> attributes = browser_service.extractAttributes(element, browser.getDriver());
		String screenshot_url = browser_service.retrieveAndUploadBrowserScreenshot(browser.getDriver(), element);

		String xpath = browser_service.generateXpath(element, "", new HashMap<String, Integer>(), browser.getDriver(), attributes);
		PageElement elem = new PageElement(element.getText(), xpath, element.getTagName(), attributes, Browser.loadCssProperties(element), screenshot_url);
		PageElement elem_record = page_element_repo.findByKey(elem.getKey());

		if(elem_record != null){
			elem = elem_record;
		}
		return elem;
	}

	/**
	 * Navigates to url in the given browser
	 *
	 * @param url {@link String} value of {@link URL} object
	 * @param isFirstPage
	 * @param browser
	 * @return
	 * @throws GridException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private PageState navigateToAndCreatePageState(String url, Browser browser)
									throws GridException, NoSuchAlgorithmException, IOException {
		browser.navigateTo(url);

		//construct a new page
		return browser_service.buildPage(browser);
	}
}
