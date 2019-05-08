package com.minion.browsing;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
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
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.PageAlert;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.services.BrowserService;
import com.qanairy.utils.BrowserUtils;

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
	
	/**
	 * Crawls the path using the provided {@link Browser browser}
	 * 
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
		
		PathObject last_obj = null;
		List<PathObject> updated_path_objects = new ArrayList<PathObject>();
		List<PathObject> ordered_path_objects = new ArrayList<PathObject>();
		//Ensure Order path objects
		for(String path_obj_key : path_keys){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(path_obj_key)){
					ordered_path_objects.add(obj);
				}
			}
		}
		
		PathObject last_path_obj = null;
		List<PathObject> reduced_path_obj = new ArrayList<PathObject>();
		//scrub path objects for duplicates
		for(PathObject obj : ordered_path_objects){
			if(last_path_obj == null || !obj.getKey().equals(last_path_obj.getKey())){
				last_path_obj = obj;
				reduced_path_obj.add(obj);
			}
		}
		ordered_path_objects = reduced_path_obj;		
		updated_path_objects.addAll(ordered_path_objects);

		ElementState last_element = null;
		
		//boolean screenshot_matches = false;
		//check if page is the same as expected. 
		PageState expected_page  = null;
		if(ordered_path_objects.get(0) instanceof Redirect){
			expected_page = ((PageState)ordered_path_objects.get(1));
		}
		else if(ordered_path_objects.get(0) instanceof PageState){
			expected_page = ((PageState)ordered_path_objects.get(0));
		}
		
		browser.navigateTo(expected_page.getUrl());
		//browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());

		for(PathObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				
				while(browser.getXScrollOffset() != expected_page.getScrollXOffset() 
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					log.warn("Scrolling to expected coord  :: " +expected_page.getScrollXOffset()+", "+expected_page.getScrollYOffset()+";     "+browser.getXScrollOffset()+","+browser.getYScrollOffset());
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
				}
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				Action action = (Action)current_obj;
				Action action_record = action_repo.findByKey(action.getKey());
				if(action_record==null){
					action = action_repo.save(action);
				}
				else{
					action = action_record;
				}
				
				performAction(action, last_element, browser.getDriver());
				Timing.pauseThread(1000);
				
				/*
				if(!browser.getDriver().getCurrentUrl().equals(expected_page.getUrl())){
					browser.waitForPageToLoad();
				}
				else{
					Timing.pauseThread(1000);
				}
				*/
				
				Point p = browser.getViewportScrollOffset();
				browser.setXScrollOffset(p.getX());
				browser.setYScrollOffset(p.getY());
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;

				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					browser.navigateTo(redirect.getStartUrl());
				}
				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel);
				browser.waitForPageToLoad();
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
			
			last_obj = current_obj;
		}
		
		return browser_service.buildPage(browser);
	}
	
	/**
	 * Crawls the path using the provided {@link Browser browser}
	 * 
	 * @param browser
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws WebDriverException 
	 * @throws GridException 
	 * 
	 * @pre path != null
	 * @pre path != null
	 */
	public void crawlPathWithoutBuildingResult(List<String> path_keys, List<? extends PathObject> path_objects, Browser browser, String host_channel) throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException{
		assert browser != null;
		assert path_keys != null;
		
		PathObject last_obj = null;
		List<PathObject> updated_path_objects = new ArrayList<PathObject>();
		List<PathObject> ordered_path_objects = new ArrayList<PathObject>();
		//Ensure Order path objects
		for(String path_obj_key : path_keys){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(path_obj_key)){
					ordered_path_objects.add(obj);
				}
			}
		}
		PathObject last_path_obj = null;
		List<PathObject> reduced_path_obj = new ArrayList<PathObject>();
		//scrub path objects for duplicates
		for(PathObject obj : ordered_path_objects){
			if(last_path_obj == null || !obj.getKey().equals(last_path_obj.getKey())){
				last_path_obj = obj;
				reduced_path_obj.add(obj);
			}
		}
		ordered_path_objects = reduced_path_obj;
		updated_path_objects.addAll(ordered_path_objects);
		
		ElementState last_element = null;
		
		//log.warn("getting expected page value");
		//boolean screenshot_matches = false;
		//check if page is the same as expected. 
		PageState expected_page  = null;
		String init_url = null;
		if(ordered_path_objects.get(0) instanceof Redirect){
			expected_page = ((PageState)ordered_path_objects.get(1));
			Redirect redirect = (Redirect)ordered_path_objects.get(0);
			init_url = redirect.getStartUrl();
		}
		else if(ordered_path_objects.get(0) instanceof PageState){
			expected_page = ((PageState)ordered_path_objects.get(0));
			init_url = expected_page.getUrl();
		}
		
		//log.warn("navigating to url :: " + init_url);
		browser.navigateTo(init_url);
		//browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());

		
		for(PathObject current_obj: ordered_path_objects){
			//log.warn("crawl current OBJ  ----   "+current_obj.getType());
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				
				while(browser.getXScrollOffset() != expected_page.getScrollXOffset() 
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					log.warn("Scrolling to expected coord  :: " +expected_page.getScrollXOffset()+", "+expected_page.getScrollYOffset()+";     "+browser.getXScrollOffset()+","+browser.getYScrollOffset());
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
				}
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				Action action = (Action)current_obj;
				Action action_record = action_repo.findByKey(action.getKey());
				if(action_record==null){
					action = action_repo.save(action);
				}
				else{
					action = action_record;
				}
				
				performAction(action, last_element, browser.getDriver());
				Timing.pauseThread(1000);
				//BrowserUtils.getElementAnimation(browser, last_element, host_channel);
				
				Point p = browser.getViewportScrollOffset();
				browser.setXScrollOffset(p.getX());
				browser.setYScrollOffset(p.getY());
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;

				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					browser.navigateTo(redirect.getStartUrl());
				}
				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel);
				browser.waitForPageToLoad();
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
			
			last_obj = current_obj;
		}
	}
	
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
	public void crawlPathExplorer(List<String> keys, List<PathObject> path_object_list, Browser browser, String host_channel, ExploratoryPath path) throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException{
		assert browser != null;
		assert keys != null;
		
		ElementState last_element = null;
		PathObject last_obj = null;
		//boolean screenshot_matches = false;
		//check if page is the same as expected. 

		List<String> path_keys = new ArrayList<String>();
		List<PathObject> path_objects = new ArrayList<PathObject>();
		
		path_keys.addAll(keys);
		path_objects.addAll(path_object_list);	
		
		List<PathObject> ordered_path_objects = new ArrayList<PathObject>();
		//Ensure Order path objects
		for(String path_obj_key : path_keys){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(path_obj_key)){
					ordered_path_objects.add(obj);
				}
			}
		}
		
		PathObject last_path_obj = null;
		List<PathObject> reduced_path_obj = new ArrayList<PathObject>();
		//scrub path objects for duplicates
		for(PathObject obj : ordered_path_objects){
			if(last_path_obj == null || !obj.getKey().equals(last_path_obj.getKey())){
				last_path_obj = obj;
				reduced_path_obj.add(obj);
			}
		}
		ordered_path_objects = reduced_path_obj;
		path_objects = new ArrayList<PathObject>(ordered_path_objects);
		
		PageState expected_page = null;
		
		for(PathObject obj : path_objects){
			if(obj instanceof PageState){
				expected_page = ((PageState)obj);
				break;
			}
		}

		if(!(path_objects.get(0) instanceof Redirect)){
			browser.navigateTo(expected_page.getUrl());

			log.warn("checking for page redirect");
			Redirect initial_redirect = BrowserUtils.getPageTransition(expected_page.getUrl(), browser, host_channel);	
			if(initial_redirect.getUrls().size() > 0){
				log.warn("redirect found");
				path_keys.add(0,initial_redirect.getKey());
				path_objects.add(0,initial_redirect);
			}
		}
		
		//TODO: check for continuously animated elements
		
		
		String last_url = null;
		int current_idx = 0;
		for(PathObject current_obj: ordered_path_objects){
			//log.warn("current object type ::   " + current_obj.getType() + " ::  "+current_obj);
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				last_url = expected_page.getUrl();
				while(browser.getXScrollOffset() != expected_page.getScrollXOffset() 
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					log.warn("Scrolling to expected coord  :: " +expected_page.getScrollXOffset()+", "+expected_page.getScrollYOffset()+";     "+browser.getXScrollOffset()+","+browser.getYScrollOffset());
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
				}
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;
				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					log.warn("navigating to redirect start url");
					browser.navigateTo(redirect.getStartUrl());
				}
				
				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel);
				
				last_url = redirect.getUrls().get(redirect.getUrls().size()-1);
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				Action action = (Action)current_obj;
				Action action_record = action_repo.findByKey(action.getKey());
				if(action_record==null){
					action = action_repo.save(action);
				}
				else{
					action = action_record;
				}
				
				performAction(action, last_element, browser.getDriver());
				//check for page alert presence
				Alert alert = browser.isAlertPresent();
				if(alert != null){
					log.warn("Alert was encountered!!!");
					PageAlert page_alert = new PageAlert(expected_page, "dismiss", alert.getText());
					path_keys.add(page_alert.getKey());
					path_objects.add(page_alert);
					current_idx++;
				}
				else{
					if((current_idx < path_objects.size()-1 
							&& !path_objects.get(current_idx+1).getKey().contains("redirect") 
							&& !path_objects.get(current_idx+1).getKey().contains("elementstate")) 
							|| current_idx == path_objects.size()-1){
						log.warn("starting to check for redirect after performing action ::  "+last_url);
						Redirect redirect = BrowserUtils.getPageTransition(last_url, browser, host_channel);
						if(redirect.getUrls().size() > 1){
							log.warn("transition with states found :: " + redirect.getUrls().size());
							//browser.waitForPageToLoad();
							log.warn("#########################################################################");
							log.warn("adding redirect object to path");
							log.warn("#########################################################################");
							//create and transition for page state
							if(current_idx == path_objects.size()-1){
								path_keys.add(redirect.getKey());
								path_objects.add(redirect);
							}
							else if(current_idx < path_objects.size()-1){
								path_keys.add(current_idx+1, redirect.getKey());
								path_objects.add(current_idx+1, redirect);
							}
							current_idx++;
						}
					}
					else if(current_idx < path_objects.size()-1 && !path_objects.get(current_idx+1).getKey().contains("redirect") ){
						log.warn("PAUSING AFTER ACTION PERFORMED   !!!!!!!!!");
						//TODO: Replace the following with animation detection
						//BrowserUtils.getElementAnimation(browser, last_element, host_channel);
						Timing.pauseThread(1000);
					}
				
					Point p = browser.getViewportScrollOffset();
					browser.setXScrollOffset(p.getX());
					browser.setYScrollOffset(p.getY());
				}
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
			last_obj = current_obj;
			current_idx++;
		}
		
		if(path.getPathKeys().size() != path_keys.size()){
			path.setPathKeys(path_keys);
			path.setPathObjects(path_objects);
		}
	}
	
	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element 
	 * 
	 * @return whether action was able to be performed on element or not
	 */
	public static void performAction(Action action, ElementState elem, WebDriver driver){
		ActionFactory actionFactory = new ActionFactory(driver);
		WebElement element = driver.findElement(By.xpath(elem.getXpath()));
		actionFactory.execAction(element, action.getValue(), action.getName());
	}
	
	public static void scrollDown(WebDriver driver, int distance) 
    { 
        ((JavascriptExecutor)driver).executeScript("scroll(0,"+ distance +");"); 
    }

	/**
	 * Handles setting up browser for path crawl and in the event of an error, the method retries until successful
	 * @param browser
	 * @param path
	 * @param host
	 * @return
	 */
	public PageState performPathExploratoryCrawl(String browser_name, ExploratoryPath path, String host) {
		PageState result_page = null;
		int tries = 0;
		Browser browser = null;

		do{
			try{
				log.warn("setting up browser :: " + browser_name);
				browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
				log.warn("exploratory crawl with keys   ::   "+path.getPathKeys());
				crawlPathExplorer(path.getPathKeys(), path.getPathObjects(), browser, host, path);
				result_page = browser_service.buildPage(browser);
			}catch(NullPointerException e){
				log.info("Error happened while exploratory actor attempted to crawl test "+e.getMessage());
				e.printStackTrace();
			} catch (GridException e) {
				log.warn("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
			}
			catch (NoSuchElementException e){
				log.warn("Unable to locage element while performing path crawl   ::    "+ e.getMessage());
				e.printStackTrace();
			}
			catch (WebDriverException e) {
				//log.warn("web driver exception occurred : " + e.getMessage());
				//e.printStackTrace();
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				//log.warn("WebDriver exception encountered while trying to perform crawl of exploratory path"+e.getMessage());
				if(e.getMessage().contains("viewport")){
					throw e;
				}
			} catch (NoSuchAlgorithmException e) {
				log.warn("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
				e.printStackTrace();
			} catch(Exception e) {
				log.warn("Exception occurred in performPathExploratoryCrawl actor. \n"+e.getMessage());
				e.printStackTrace();
			}
			finally{
				if(browser != null){
					browser.close();
				}
			}
			tries++;
		}while(result_page == null && tries < Integer.MAX_VALUE);
		return result_page;
	}
	
	/**
	 * Handles setting up browser for path crawl and in the event of an error, the method retries until successful
	 * @param browser
	 * @param path
	 * @param host
	 * @return
	 */
	public PageState performPathCrawl(String browser_name, List<String> path_keys, List<PathObject> path_objects, String host) {
		PageState result_page = null;
		int tries = 0;
		Browser browser = null;

		do{
			try{
				browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
				result_page = crawlPath(path_keys, path_objects, browser, host);
			}catch(NullPointerException e){
				log.info("Error happened while exploratory actor attempted to crawl test "+e.getMessage());
			} catch (GridException e) {
				log.info("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
			}
			catch (NoSuchElementException e){
				log.error("Unable to locage element while performing path crawl   ::    "+ e.getMessage());
				e.printStackTrace();
			}
			catch (WebDriverException e) {
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				log.warn("WebDriver exception encountered while performing path crawl"+e.getMessage());
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				log.info("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
			} catch(Exception e){
				log.info("Exception occurred in performPathCrawl actor. \n"+e.getMessage());
				e.printStackTrace();
			}
			finally{
				if(browser != null){
					browser.close();
				}
			}
			tries++;
		}while(result_page == null && tries < Integer.MAX_VALUE);
		return result_page;
	}

	public void crawlPartialPath(List<String> path_keys, List<PathObject> path_objects, Browser browser, String host, ElementState last_element) throws GridException, IOException {
		assert browser != null;
		assert path_keys != null;
		
		List<PathObject> updated_path_objects = new ArrayList<PathObject>();
		List<PathObject> ordered_path_objects = new ArrayList<PathObject>();
		//Ensure Order path objects
		for(String path_obj_key : path_keys){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(path_obj_key)){
					ordered_path_objects.add(obj);
				}
			}
		}
		PathObject last_path_obj = null;
		List<PathObject> reduced_path_obj = new ArrayList<PathObject>();
		//scrub path objects for duplicates
		for(PathObject obj : ordered_path_objects){
			if(last_path_obj == null || !obj.getKey().equals(last_path_obj.getKey())){
				last_path_obj = obj;
				reduced_path_obj.add(obj);
			}
		}
		ordered_path_objects = reduced_path_obj;
		updated_path_objects.addAll(ordered_path_objects);
				
		log.warn("getting expected page value");
		//boolean screenshot_matches = false;
		//check if page is the same as expected. 
		PageState expected_page  = null;
		
		PathObject last_obj = null;
		for(PathObject current_obj: ordered_path_objects){
			log.warn("crawl current OBJ  ----   "+current_obj.getType());
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				
				while(browser.getXScrollOffset() != expected_page.getScrollXOffset() 
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					log.warn("Scrolling to expected coord  :: " +expected_page.getScrollXOffset()+", "+expected_page.getScrollYOffset()+";     "+browser.getXScrollOffset()+","+browser.getYScrollOffset());
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
				}
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				Action action = (Action)current_obj;
				Action action_record = action_repo.findByKey(action.getKey());
				if(action_record==null){
					action = action_repo.save(action);
				}
				else{
					action = action_record;
				}
				
				performAction(action, last_element, browser.getDriver());
				//BrowserUtils.getElementAnimation(browser, last_element, host);
				Timing.pauseThread(1000);
				Point p = browser.getViewportScrollOffset();
				browser.setXScrollOffset(p.getX());
				browser.setYScrollOffset(p.getY());
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;

				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					browser.navigateTo(redirect.getStartUrl());
				}
				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host);
				browser.waitForPageToLoad();
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
			
			last_obj = current_obj;
		}
	}
}