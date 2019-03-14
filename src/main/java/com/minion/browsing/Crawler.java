package com.minion.browsing;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.repository.ActionRepository;
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
	public PageState crawlPath(List<String> path_keys, List<? extends PathObject> path_objects, Browser browser, String host_channel, ExploratoryPath path) throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException{
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
		
		log.info("#########################################################################");
		log.info("#########################################################################");
		log.info("PATH  Keys size ::   " + path.getPathKeys().size());
		log.info("PATH  Objects size ::   " + path.getPathObjects().size());

		log.info("#########################################################################");
		log.info("#########################################################################");
		
		updated_path_objects.addAll(ordered_path_objects);
		
		PageElement last_element = null;
		
		//boolean screenshot_matches = false;
		//check if page is the same as expected. 
		PageState current_page_state = null;
				
		log.info("building page for host channel :: " + host_channel);
		do{
			try{
				browser.navigateTo(((PageState)ordered_path_objects.get(0)).getUrl().toString());
				WebElement next_elem = browser.getDriver().findElement(By.xpath(((PageElement)ordered_path_objects.get(1)).getXpath()));
				if(!browser_service.isElementVisibleInPane(browser.getDriver(), next_elem)){
					Browser.scrollToElement(browser.getDriver(), next_elem);
				}
				current_page_state = browser_service.buildPage(browser);
			}catch(Exception e){
				browser.close();
				browser = browser_service.getConnection(browser.getBrowserName(), BrowserEnvironment.DISCOVERY);
				e.printStackTrace();
			}
		}while(current_page_state == null);
		
		
		int idx=0;
		for(PathObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				PageState expected_page = (PageState)current_obj;

				/*
				PageState page_record = page_state_service.findByKey(expected_page.getKey());
				if(page_record != null){
					expected_page = page_record;
				}
				
				screenshot_matches = current_page_state.equals(expected_page);
				if(!screenshot_matches){
					return current_page_state;
				}
				*/
				
				if(idx==0 && !current_page_state.equals(expected_page)){
					updated_path_objects.set(idx, current_page_state);
					path_keys.set(idx, current_page_state.getKey());
					
					path.setPathObjects(updated_path_objects);
					path.setPathKeys(path_keys);
				}
			}
			else if(current_obj instanceof PageElement){
				last_element = (PageElement) current_obj;
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
				if(!browser.getDriver().getCurrentUrl().equals(current_page_state.getUrl())){
					Browser.waitForPageToLoad(browser.getDriver());
				}
				else{
					Timing.pauseThread(1000);
				}
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
			idx++;
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

		WebElement element = driver.findElement(By.xpath(elem.getXpath()));
		actionFactory.execAction(element, action.getValue(), action.getName());
		
		return wasPerformedSuccessfully;
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
	public PageState performPathCrawl(String browser_name, ExploratoryPath path, String host) {
		PageState result_page = null;
		int tries = 0;
		Browser browser = null;

		do{
			try{
				browser = BrowserFactory.buildBrowser(browser_name, BrowserEnvironment.DISCOVERY);
				result_page = crawlPath(path.getPathKeys(), path.getPathObjects(), browser, host, path);
			}catch(NullPointerException e){
				log.warn("Error happened while exploratory actor attempted to crawl test "+e.getMessage());
				e.printStackTrace();
			} catch (GridException e) {
				log.warn("Grid exception encountered while trying to crawl exporatory path"+e.getLocalizedMessage());
			} catch (WebDriverException e) {
				log.warn("WebDriver exception encountered while trying to crawl exporatory path"+e.getLocalizedMessage());
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				log.warn("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getLocalizedMessage());
			} catch(Exception e){
				log.warn("Exception occurred in explortatory actor. \n"+e.getMessage());
			}
			finally{
				browser.close();
			}
			tries++;
		}while(result_page == null && tries < Integer.MAX_VALUE);
		return result_page;
	} 
}