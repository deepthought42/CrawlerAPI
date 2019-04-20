package com.minion.browsing;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.openqa.grid.common.exception.GridException;
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

import com.qanairy.api.exceptions.PagesAreNotMatchingException;
import com.qanairy.models.Action;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.PageAlert;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
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
		
		updated_path_objects.addAll(ordered_path_objects);
		
		ElementState last_element = null;
		
		//boolean screenshot_matches = false;
		//check if page is the same as expected. 
		PageState expected_page = ((PageState)ordered_path_objects.get(0));

		browser.navigateTo(expected_page.getUrl());
		browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());

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
				List<String> state_values = BrowserUtils.getPageTransition(browser);
				if(state_values.size() > 1){
					//create and transition for page state
				}
				
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
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
		}
		
		return browser_service.buildPage(browser);
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
	public void crawlPathExplorer(List<String> path_keys, List<? extends PathObject> path_objects, Browser browser, String host_channel, ExploratoryPath path) throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException{
		assert browser != null;
		assert path_keys != null;
		
		ElementState last_element = null;
		
		//boolean screenshot_matches = false;
		//check if page is the same as expected. 
		PageState expected_page = ((PageState)path_objects.get(0));

		browser.navigateTo(expected_page.getUrl());
		browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());

		for(PathObject current_obj: path_objects){
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
				List<String> state_values = BrowserUtils.getPageTransition(browser);
				if(state_values.size() > 1){
					//create and transition for page state
				}
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
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
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
	public PageState performPathCrawl(String browser_name, ExploratoryPath path, String host) {
		PageState result_page = null;
		int tries = 0;
		Browser browser = null;

		do{
			try{
				browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
				result_page = crawlPath(path.getPathKeys(), path.getPathObjects(), browser, host, path);
			}catch(NullPointerException e){
				log.info("Error happened while exploratory actor attempted to crawl test "+e.getMessage());
			} catch (GridException e) {
				log.info("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
				e.getStackTrace();
			}
			catch (NoSuchElementException e){
				log.info("Unable to locage element while performing path crawl   ::    "+ e.getMessage());
				e.printStackTrace();
			}
			catch (WebDriverException e) {
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				//log.warn("WebDriver exception encountered while trying to perform crawl of exploratory path"+e.getMessage());
				if(e.getMessage().contains("viewport")){
					throw e;
				}
			} catch (NoSuchAlgorithmException e) {
				log.info("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
			} catch(Exception e){
				log.info("Exception occurred in explortatory actor. \n"+e.getMessage());
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
				result_page = crawlPath(path_keys, path_objects, browser, host, null);
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
				log.info("WebDriver exception encountered while performing path crawl"+e.getMessage());
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				log.info("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
			} catch(Exception e){
				log.info("Exception occurred in explortatory actor. \n"+e.getMessage());
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
}