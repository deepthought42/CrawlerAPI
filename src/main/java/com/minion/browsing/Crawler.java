package com.minion.browsing;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

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
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.services.BrowserService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.PathUtils;

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
	 * @throws ExecutionException
	 * @throws InterruptedException
	 *
	 * @pre path != null
	 * @pre path != null
	 */
	public PageState crawlPath(List<String> path_keys, List<PathObject> path_objects, Browser browser, String host_channel, Map<Integer, ElementState> visible_element_map, List<ElementState> known_visible_elements) throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, InterruptedException, ExecutionException{
		assert browser != null;
		assert path_keys != null;

		PathObject last_obj = null;
		List<PathObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);
		ordered_path_objects = PathUtils.reducePathObjects(path_keys, ordered_path_objects);

		ElementState last_element = null;

		//boolean screenshot_matches = false;
		//check if page is the same as expected.
		PageState expected_page  = null;
		if(ordered_path_objects.get(0) instanceof Redirect){
			expected_page = ((PageState)ordered_path_objects.get(1));
		}
		else if(ordered_path_objects.get(0) instanceof PageState){
			expected_page = PathUtils.getFirstPage(ordered_path_objects);
		}

		browser.navigateTo(expected_page.getUrl());

		for(PathObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;

				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
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
				browser.waitForPageToLoad();
				//Timing.pauseThread(1000);

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
			else if(current_obj instanceof PageLoadAnimation){
				log.warn("crawling loading animation");
				BrowserUtils.getLoadingAnimation(browser, host_channel);
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}

			last_obj = current_obj;
		}

		List<String> xpath_list = BrowserService.getVisibleElementsUsingJSoup(browser.getDriver().getPageSource());
		log.warn("ELEMENTS visible during crawlPath :: " + xpath_list.size());
		List<ElementState> visible_elements = browser_service.getVisibleElements(browser, browser.getViewportScreenshot(), visible_element_map, xpath_list);

		return browser_service.buildPage(browser, visible_elements);
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
	public void crawlPathWithoutBuildingResult(List<String> path_keys, List<PathObject> path_objects, Browser browser, String host_channel) throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException{
		assert browser != null;
		assert path_keys != null;

		ElementState last_element = null;
		PathObject last_obj = null;
		PageState expected_page = null;
		
		List<PathObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);
		ordered_path_objects = PathUtils.reducePathObjects(path_keys, ordered_path_objects);
		
		for(PathObject current_obj: ordered_path_objects){
			//log.warn("crawl current OBJ  ----   "+current_obj.getType());
			if(current_obj instanceof PageState){
				log.warn("current object type :: " + current_obj.getClass().getName() + "   ;;   "+current_obj.getType());
				expected_page = (PageState)current_obj;

				log.warn("Scrolling to expected coord  :: " +expected_page.getScrollXOffset()+", "+expected_page.getScrollYOffset()+";     "+browser.getXScrollOffset()+","+browser.getYScrollOffset());
				browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
				Timing.pauseThread(1000);
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
				//browser.waitForPageToLoad();
			}
			else if(current_obj instanceof PageLoadAnimation){
				BrowserUtils.getLoadingAnimation(browser, host_channel);
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
	public void crawlPathExplorer(List<String> keys, List<PathObject> path_object_list, Browser browser, String host_channel, ExploratoryPath path) throws IOException, GridException, NoSuchElementException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException{
		assert browser != null;
		assert keys != null;

		ElementState last_element = null;
		PathObject last_obj = null;
		//boolean screenshot_matches = false;
		//check if page is the same as expected.

		List<String> path_keys = new ArrayList<String>(keys);
		List<PathObject> path_objects = new ArrayList<PathObject>(path_object_list);

		List<PathObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);
		ordered_path_objects = PathUtils.reducePathObjects(path_keys, ordered_path_objects);
		List<PathObject> path_objects_explored = new ArrayList<>(ordered_path_objects);
		PageState expected_page  = null;
		if(ordered_path_objects.get(0) instanceof Redirect){
			expected_page = ((PageState)ordered_path_objects.get(1));
		}
		else if(ordered_path_objects.get(0) instanceof PageState){
			expected_page = PathUtils.getFirstPage(ordered_path_objects);
		}

		String last_url = null;
		int current_idx = 0;
		for(PathObject current_obj: ordered_path_objects){
			//log.warn("current object type ::   " + current_obj.getType() + " ::  "+current_obj);
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				last_url = expected_page.getUrl();
				log.warn("Scrolling to expected coord  :: " +expected_page.getScrollXOffset()+", "+expected_page.getScrollYOffset()+";     "+browser.getXScrollOffset()+","+browser.getYScrollOffset());
				browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;
				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					log.warn("navigating to redirect start url  ::   "+redirect.getStartUrl());
					browser.navigateTo(redirect.getStartUrl());
				}

				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel);
				last_url = redirect.getUrls().get(redirect.getUrls().size()-1);

				log.warn("setting last url to redirect url :: " + last_url);
			}
			else if(current_obj instanceof PageLoadAnimation){
				BrowserUtils.getLoadingAnimation(browser, host_channel);
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
					ordered_path_objects.add(page_alert);
					current_idx++;
				}
				else{
					if((current_idx < ordered_path_objects.size()-1
							&& !ordered_path_objects.get(current_idx+1).getKey().contains("redirect")
							&& !ordered_path_objects.get(current_idx+1).getKey().contains("elementstate"))
							|| (current_idx == ordered_path_objects.size()-1 && !last_url.equals(BrowserUtils.sanitizeUrl(browser.getDriver().getCurrentUrl())))){
						log.warn("starting to check for redirect after performing action ::  "+last_url);
						Redirect redirect = BrowserUtils.getPageTransition(last_url, browser, host_channel);
						if(redirect.getUrls().size() > 2){
							log.warn("transition with states found :: " + redirect.getUrls().size());
							//browser.waitForPageToLoad();
							log.warn("#########################################################################");
							log.warn("adding redirect object to path");
							log.warn("#########################################################################");
							//create and transition for page state
							if(current_idx == ordered_path_objects.size()-1){
								path_keys.add(redirect.getKey());
								path_objects_explored.add(redirect);
							}
							else if(current_idx < ordered_path_objects.size()-1){
								path_keys.add(current_idx+1, redirect.getKey());
								path_objects_explored.add(current_idx+1, redirect);
							}
							current_idx++;
						}
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
			path.setPathObjects(path_objects_explored);
		}
	}

	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element
	 *
	 * @return whether action was able to be performed on element or not
	 */
	public static void performAction(Action action, ElementState elem, WebDriver driver) throws NoSuchElementException{
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
		Map<Integer, ElementState> visible_element_map = new HashMap<>();
		boolean no_such_element_exception = false;
		do{
			try{
				log.warn("setting up browser :: " + browser_name);
				if(!no_such_element_exception){
					no_such_element_exception = false;
					browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
					crawlPathExplorer(path.getPathKeys(), path.getPathObjects(), browser, host, path);
				}
				String browser_url = browser.getDriver().getCurrentUrl();
				browser_url = BrowserUtils.sanitizeUrl(browser_url);
				//get last page state
				PageState last_page_state = PathUtils.getLastPageState(path.getPathObjects());
				log.warn("############################################################################");
				log.warn("browser url :: " + browser_url);
				log.warn("Last page state url :: " + last_page_state.getUrl());
				log.warn("do urls match  :: " + browser_url.equals(last_page_state.getUrl()));
				log.warn("############################################################################");
				if(!browser_url.equals(last_page_state.getUrl())){
					PageLoadAnimation loading_animation = BrowserUtils.getLoadingAnimation(browser, host);
					if(loading_animation != null){
						path.getPathKeys().add(loading_animation.getKey());
						path.getPathObjects().add(loading_animation);
					}
				}
				
				Timing.pauseThread(2000);
				
				//verify that screenshot does not match previous page
				List<String> xpath_list = BrowserService.getVisibleElementsUsingJSoup(browser.getDriver().getPageSource());
    			log.warn("element xpaths found while performing exploratory crawl   ::  " +xpath_list.size());
    			
    			List<ElementState> visible_elements = browser_service.getVisibleElementsWithinViewport(browser, browser.getViewportScreenshot(), visible_element_map, xpath_list);
			
				log.warn("element xpaths after filtering all elements NOT in viewport :: " + visible_elements.size());
				result_page = browser_service.buildPage(browser, visible_elements, browser_url);
			}catch(NullPointerException e){
				log.info("Error happened while exploratory actor attempted to crawl test ");
				//e.printStackTrace();
			} catch (GridException e) {
				log.warn("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
			}
			catch (NoSuchElementException e){
				log.warn("Unable to locate element while performing path crawl   ::    "+ e.getMessage());
				//e.printStackTrace();
				//no_such_element_exception = true;
			}
			catch (WebDriverException e) {
				log.warn("(Exploratory Crawl) web driver exception occurred : " + e.getMessage());
				//e.printStackTrace();
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				//log.warn("WebDriver exception encountered while trying to perform crawl of exploratory path"+e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				log.warn("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
				//e.printStackTrace();
			} catch(Exception e) {
				log.warn("Exception occurred in performPathExploratoryCrawl actor. \n"+e.getMessage());
				e.printStackTrace();
			}
			finally{
				if(browser != null && !no_such_element_exception){
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
		Map<Integer, ElementState> visible_element_map = new HashMap<>();
		List<ElementState> visible_elements = new ArrayList<>();
		
		do{
			try{
				browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
				result_page = crawlPath(path_keys, path_objects, browser, host, visible_element_map, visible_elements);
			}catch(NullPointerException e){
				log.info("Error happened while exploratory actor attempted to crawl test "+e.getMessage());
			} catch (GridException e) {
				log.info("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
			}
			catch (NoSuchElementException e){
				log.error("Unable to locate element while performing path crawl   ::    "+ e.getMessage());
				//e.printStackTrace();
			}
			catch (WebDriverException e) {
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				log.warn("WebDriver exception encountered while performing path crawl"+e.getMessage());
				//e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				log.info("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
			} catch(Exception e){
				log.info("Exception occurred in performPathCrawl actor. \n"+e.getMessage());
				//e.printStackTrace();
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
