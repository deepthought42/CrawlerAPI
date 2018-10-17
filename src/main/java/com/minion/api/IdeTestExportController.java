package com.minion.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minion.browsing.Browser;
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
public class IDETestExportController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private TestRepository test_repo;
	
	@Autowired
	BrowserService browser_service;
    
    /**
     * Contructs a new {@link Test} using an array of {@link JSONObject}s containing info for {@link PageState}s
     *  {@link PageElement}s and {@link Action}s
     * 
     * @param authorization_header
     * @param url 
     * @param screenshot_url
     * @param browser_name
     * 
     * @return
	 * @throws IOException 
     * @throws Exception 
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Test> create( @RequestParam(value="name", required=true) String name,
    									@RequestBody(required=true) JSONObject[] path) throws IOException {
    	
    	List<String> path_keys = new ArrayList<String>();
    	List<PathObject> path_objects = new ArrayList<PathObject>();
    	Browser browser = new Browser("chrome");
    	boolean first_page = true;
    	int current_idx = 0;
    	for(JSONObject obj : path){
    		if(obj.has("url")){
    			String url = obj.getString("url");
    			if(first_page){
    				browser.navigateTo(url);
    			}
    			
    			//construct a new page
    			PageState page_state = browser_service.buildPage(browser);
    			path_keys.add(page_state.getKey());
    			path_objects.add(page_state);
    		}
    		else if(obj.has("xpath")){
    			//use xpath to identify WebElement. 
    			WebElement element = browser.findWebElementByXpath(obj.getString("xpath"));
    			//use WebElement to generate system usable xpath
    			Set<Attribute> attributes = browser_service.extractAttributes(element, browser.getDriver());
    			String screenshot_url = browser_service.retrieveAndUploadBrowserScreenshot(browser.getDriver(), element);

    			String xpath = browser_service.generateXpath(element, obj.getString("xpath"), new HashMap<String, Integer>(), browser.getDriver(), attributes);
    			PageElement elem = new PageElement(element.getText(), xpath, element.getTagName(), attributes, Browser.loadCssProperties(element), screenshot_url);
    			
    			//add to path
    			path_keys.add(elem.getKey());
    			path_objects.add(elem);
    		}
    		else if(obj.has("action")){
    			//create new action
    			//add action to Test
    			String action_type = obj.getString("action");
    			String action_value = obj.getString("value");
    			Action action = new Action(action_type, action_value);
    			
    			path_keys.add(action.getKey());
    			path_objects.add(action);
    			Timing.pauseThread(5000L);    			
    			
    			//******************************************************
    			// THIS IS LIKELY TO BE PROBLEMATIC IF THE CLIENT ACTUALLY EXPERIENCED A TRANSITION STATE
    			// BECAUSE IT SHOULD HAVE ADDED A URL TO THE PATH. CHECK IF NEXT OBJECT IS  A URL BEFORE EXECUTING
    			//******************************************************
    			if(!path[current_idx].has("url")){
	    			//capture new page state and add it to path
	    			PageState page_state = browser_service.buildPage(browser);
	    			path_keys.add(page_state.getKey());
	    			path_objects.add(page_state);
    			}
    		}
    		
    		current_idx++;
    	}
    	PageState result_page = browser_service.buildPage(browser);
    	Test test = new Test(path_keys, path_objects, result_page, name);
    	test = test_repo.save(test);
    	return new ResponseEntity<>(test, HttpStatus.ACCEPTED );
	}
}