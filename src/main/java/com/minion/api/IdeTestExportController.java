package com.minion.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mortbay.log.Log;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.util.Timing;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.BrowserService;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/testIDE")
public class IdeTestExportController {
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private TestRepository test_repo;
	
	@Autowired
	BrowserService browser_service;
    
    /**
     * Contructs a new {@link Test} using an array of {@link JSONObject}s containing info for {@link PageState}s
     *  {@link PageElement}s and {@link Action}s
	 * 
	 * @param json_str JSON String
	 * 
	 * @return A boolean value indicating that the system successfully created a {@link Test} using the provided JSON
	 * 
	 * @throws Exception
	 */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Boolean> create( @RequestBody(required=true) String json_str) 
    										throws Exception {
    	int attempts = 0;
    	PageState result_page = null;
    	
    	JSONObject test_json = new JSONObject(json_str);
    	List<String> path_keys = new ArrayList<String>();
    	List<PathObject> path_objects = new ArrayList<PathObject>();
    	System.err.println("Test JSON :: "+test_json.toString());
    	boolean first_page = true;
    	PageElement last_element = null;
    	String name = test_json.get("name").toString();
    	JSONArray path = (JSONArray) test_json.get("path");
    	
    	do{
	    	Browser browser = new Browser("chrome");

    		try{
		    	System.err.println("navigating over path :: "+path);
		    	for(int idx=0; idx < path.length(); idx++){
		        	JSONObject path_obj_json = new JSONObject(path.get(idx).toString());
		
		        	System.err.println("PATH OBJECT :: " + path_obj_json);
		    		if(path_obj_json.has("url")){
		    			System.err.println("PATH OBJECT IS A URL :: " + path_obj_json);
		    			String url = path_obj_json.getString("url");
		    			if(first_page){
		    				System.err.println("NAVIGATING TO URL :: " + url);
		    				browser.navigateTo(url);
		    				first_page = false;
		    			}
		    			
		    			//construct a new page
		    			PageState page_state = browser_service.buildPage(browser);
		    			path_keys.add(page_state.getKey());
		    			path_objects.add(page_state);
		    		}
		    		else {
		    			System.err.println("ELEMENT IN JSON :: " + path_obj_json.getJSONObject("element").toString());
		    			JSONObject element_json = path_obj_json.getJSONObject("element");
		    			//use xpath to identify WebElement. 
		    			WebElement element = browser.findWebElementByXpath(element_json.getString("xpath"));
		    			//use WebElement to generate system usable xpath
		    			Set<Attribute> attributes = browser_service.extractAttributes(element, browser.getDriver());
		    			String screenshot_url = browser_service.retrieveAndUploadBrowserScreenshot(browser.getDriver(), element);
		
		    			String xpath = browser_service.generateXpath(element, "", new HashMap<String, Integer>(), browser.getDriver(), attributes);
		    			PageElement elem = new PageElement(element.getText(), xpath, element.getTagName(), attributes, Browser.loadCssProperties(element), screenshot_url);
		    			last_element = elem;
		    			//add to path
		    			path_keys.add(elem.getKey());
		    			path_objects.add(elem);

		    			System.err.println("ACTION IN JSON ::  " + path_obj_json.getJSONObject("action"));
		    			JSONObject action_json = path_obj_json.getJSONObject("action");
		    			//create new action
		    			//add action to Test
		    			String action_type = action_json.getString("name");
		    			String action_value = action_json.getString("value");
		    			Action action = new Action(action_type, action_value);
		    			
		    			path_keys.add(action.getKey());
		    			path_objects.add(action);
		    			System.err.println("Performing action!");
		    			Crawler.performAction(action, last_element, browser.getDriver());
		    			System.err.println("FINISHED PERFORMING ACTION");
		    			Timing.pauseThread(5000L);    			
		    			
		    			//******************************************************
		    			// THIS IS LIKELY TO BE PROBLEMATIC IF THE CLIENT ACTUALLY EXPERIENCED A TRANSITION STATE
		    			// BECAUSE IT SHOULD HAVE ADDED A URL TO THE PATH. CHECK IF NEXT OBJECT IS  A URL BEFORE EXECUTING
		    			//******************************************************
			        	if(idx+1 < path.length()-1){
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
		    	result_page = browser_service.buildPage(browser);
		    	Test test = new Test(path_keys, path_objects, result_page, name);
		    	Test test_record = test_repo.findByKey(test.getKey());
		    	if(test_record == null){
		    		test = test_repo.save(test);
		    	}
    		}
    		catch(Exception e){
    			Log.warn("Error occurred while creating new test from IDE ::  "+e.getLocalizedMessage());
    			e.printStackTrace();
				first_page = true;
    			browser.close();
    		}
    		attempts++;
    	}while(result_page == null && attempts < 10000);
    	return new ResponseEntity<>(Boolean.TRUE, HttpStatus.ACCEPTED );
	}
}