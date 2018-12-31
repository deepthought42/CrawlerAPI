package com.minion.browsing;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

import com.minion.util.Timing;
import com.qanairy.api.exceptions.PagesAreNotMatchingException;
import com.qanairy.models.Action;
import com.qanairy.models.PageAlert;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.services.BrowserService;

/**
 * Provides methods for crawling web pages using Selenium
 */
@Component
public class Crawler {
	private static Logger log = LoggerFactory.getLogger(Crawler.class);

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private ActionRepository action_repo;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	/**
	 * Crawls the path using the provided {@link Browser browser}
	 * 
	 * @param path list of vertex keys
	 * @param browser
	 * @return {@link Page result_page} state that resulted from crawling path
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws WebDriverException 
	 * @throws GridException 
	 * 
	 * @pre path != null
	 * @pre path != null
	 */
	public PageState crawlPath(List<String> path_keys, List<? extends PathObject> path_objects, Browser browser, String host_channel) throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException{
		assert browser != null;
		assert path_keys != null;
		
		List<PathObject> ordered_path_objects = new ArrayList<PathObject>();
		//Ensure Order path objects
		for(String path_obj_key : path_keys){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(path_obj_key)){
					ordered_path_objects.add(obj);
				}
			}
		}
		
		PageElement last_element = null;
		browser.navigateTo(((PageState)ordered_path_objects.get(0)).getUrl().toString());
		
		//check if page is the same as expected. 
		PageState current_page_state = null;

		for(PathObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				PageState expected_page = (PageState)current_obj;
				PageState page_record = page_state_repo.findByKey(expected_page.getKey());
				if(page_record != null){
					expected_page = page_record;
				}
				boolean screenshot_matches = false;
				current_page_state = browser_service.buildPage(browser);
				screenshot_matches = current_page_state.equals(expected_page); //browser_service.doScreenshotsMatch(browser, current_page);
				if(!screenshot_matches){
					return current_page_state;
				}
			}
			else if(current_obj instanceof PageElement){
				last_element = (PageElement) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				//boolean actionPerformedSuccessfully;
				Action action = (Action)current_obj;
				Action action_record = action_repo.findByKey(action.getKey());
				if(action_record==null){
					action = action_repo.save(action);
				}
				else{
					action = action_record;
				}
				
				performAction(action, last_element, browser.getDriver());
				Timing.pauseThread(10000L);
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
		}
		return browser_service.buildPage(browser);
	}
	
	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element 
	 * 
	 * @return whether action was able to be performed on element or not
	 */
	public static boolean performAction(Action action, PageElement elem, WebDriver driver){
		ActionFactory actionFactory = new ActionFactory(driver);
		boolean wasPerformedSuccessfully = true;

		try{
			WebElement element = driver.findElement(By.xpath(elem.getXpath()));
			actionFactory.execAction(element, action.getValue(), action.getName());
		}
		catch(StaleElementReferenceException e){
			log.warn("STALE ELEMENT REFERENCE EXCEPTION OCCURRED WHILE ACTOR WAS PERFORMING ACTION : "
					+ action + ". ", e.getMessage());
			wasPerformedSuccessfully = false;			
		}
		catch(ElementNotVisibleException e){
			log.warn("ELEMENT IS NOT CURRENTLY VISIBLE.", e.getMessage());
		}
		catch(WebDriverException e){
			log.warn("Element can not have action performed on it at point performed", e.getMessage());
			wasPerformedSuccessfully = false;
		}
		
		return wasPerformedSuccessfully;
	}
}