package com.minion.browsing;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
import com.qanairy.models.ElementState;
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
		
		updated_path_objects.addAll(ordered_path_objects);
		
		ElementState last_element = null;
		
		//boolean screenshot_matches = false;
		//check if page is the same as expected. 
		PageState expected_page = ((PageState)ordered_path_objects.get(0));
				

		browser.navigateTo(expected_page.getUrl());
		browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());

		for(PathObject current_obj: ordered_path_objects){
			if(current_obj.getClass().getSimpleName().equals("PageState")){
				expected_page = (PageState)current_obj;
				
				do{
					log.warn("Scrolling to expected coord  :: " +expected_page.getScrollXOffset()+", "+expected_page.getScrollYOffset());
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
				}while(browser.getXScrollOffset() != expected_page.getScrollXOffset() 
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset());
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
				if(!BrowserService.isElementVisibleInPane(browser, last_element)){
					log.warn("last element is not visible in current viewport" + last_element.getKey());
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
				}
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
				if(!browser.getDriver().getCurrentUrl().equals(expected_page.getUrl())){
					Browser.waitForPageToLoad(browser.getDriver());
				}
				else{
					Timing.pauseThread(5000);
				}
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
	public static boolean performAction(Action action, ElementState elem, WebDriver driver){
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
				browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
				result_page = crawlPath(path.getPathKeys(), path.getPathObjects(), browser, host, path);
			}catch(NullPointerException e){
				log.warn("Error happened while exploratory actor attempted to crawl test "+e.getMessage());
				e.printStackTrace();
			} catch (GridException e) {
				log.warn("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
				e.getStackTrace();
			}
			catch (NoSuchElementException e){
				log.error("Unable to locage element while performing path crawl   ::    "+ e.getMessage());
				e.printStackTrace();
			}
			catch (WebDriverException e) {
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				//log.warn("WebDriver exception encountered while trying to perform crawl of exploratory path"+e.getMessage());
				if(e.getMessage().contains("viewport")){
					throw e;
				}
			} catch (NoSuchAlgorithmException e) {
				log.warn("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
			} catch(Exception e){
				log.warn("Exception occurred in explortatory actor. \n"+e.getMessage());
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
				log.warn("Error happened while exploratory actor attempted to crawl test "+e.getMessage());
			} catch (GridException e) {
				log.warn("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
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
				log.warn("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
			} catch(Exception e){
				log.warn("Exception occurred in explortatory actor. \n"+e.getMessage());
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

	public static List<PageState> createPageStates(Browser browser, String url) {
		browser.navigateTo(url);
		
		//get all page elements
		//while elements is not null
			//get all elements that are currently visible within viewport
			//build all elements visible in viewport
			//build page state and add it to page state list
			
		
		return null;
	} 

}