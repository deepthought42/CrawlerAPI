package com.minion.browsing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
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
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.message.PathMessage;
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
	 * @throws URISyntaxException 
	 *
	 * @pre path != null
	 * @pre path != null
	 */
	public PageState crawlPath(List<String> path_keys, List<PathObject> path_objects, Browser browser, String host_channel, 
								Map<Integer, ElementState> visible_element_map, List<ElementState> known_visible_elements) 
										throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, InterruptedException, ExecutionException{
		assert browser != null;
		assert path_keys != null;

		PathObject last_obj = null;
		List<PathObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);

		ElementState last_element = null;

		//boolean screenshot_matches = false;
		//check if page is the same as expected.
		PageState expected_page = PathUtils.getFirstPage(ordered_path_objects);

		browser.navigateTo(expected_page.getUrl());

		for(PathObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;

				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl());
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
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}

			last_obj = current_obj;
		}

		Timing.pauseThread(1000);
		//List<String> xpath_list = BrowserService.getXpathsUsingJSoup(browser.getDriver().getPageSource());
		List<ElementState> element_list = BrowserService.getElementsUsingJSoup(browser.getDriver().getPageSource());
		List<ElementState> visible_elements = browser_service.getVisibleElementsWithinViewport(browser, browser.getViewportScreenshot(), visible_element_map, element_list, true);
		String browser_url = browser.getDriver().getCurrentUrl();
		String url_without_params = BrowserUtils.sanitizeUrl(browser_url);
		
		
		return browser_service.buildPage(browser, visible_elements, url_without_params);
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
	 * @throws URISyntaxException 
	 *
	 * @pre path != null
	 * @pre path != null
	 */
	public void crawlPathWithoutBuildingResult(List<String> path_keys, List<PathObject> path_objects, Browser browser, String host_channel) 
			throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, URISyntaxException{
		assert browser != null;
		assert path_keys != null;

		ElementState last_element = null;
		PathObject last_obj = null;
		PageState expected_page = null;
		
		List<PathObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);
		
		for(PathObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl());
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
					action_repo.save(action);
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
	 * @param browser
	 *
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws WebDriverException
	 * @throws GridException
	 * @throws URISyntaxException 
	 *
	 * @pre path != null
	 * @pre path != null
	 */
	public void crawlParentPathWithoutBuildingResult(List<String> path_keys, List<PathObject> path_objects, Browser browser, String host_channel, ElementState child_element)
							throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, URISyntaxException, NullPointerException{
		assert browser != null;
		assert path_keys != null;

		ElementState last_element = null;
		PathObject last_obj = null;
		PageState expected_page = null;
		
		List<PathObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);
		
		for(PathObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl());
				}
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				
				//perform action outside bounds of child elements
				WebElement elem = browser.getDriver().findElement(By.xpath(last_element.getXpath()));
				//compile child element coordinates and sizes
				
				Point click_location = generateRandomLocationWithinElementButNotWithinChildElements(elem, child_element, new Point(browser.getXScrollOffset(), browser.getYScrollOffset()));
				
				Action action = (Action)current_obj;
				
				performAction(action, last_element, browser.getDriver(), click_location);
				
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
	 * @throws URISyntaxException 
	 *
	 * @pre path != null
	 * @pre path != null
	 */
	public void crawlPathExplorer(List<String> keys, List<PathObject> path_object_list, Browser browser, String host_channel, ExploratoryPath path) throws IOException, GridException, NoSuchElementException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, URISyntaxException{
		assert browser != null;
		assert keys != null;

		ElementState last_element = null;
		PathObject last_obj = null;
		PageState expected_page = null;
		List<String> path_keys = new ArrayList<String>(keys);
		List<PathObject> ordered_path_objects = PathUtils.orderPathObjects(keys, path_object_list);
		List<PathObject> path_objects_explored = new ArrayList<>(ordered_path_objects);

		String last_url = null;
		int current_idx = 0;
		for(PathObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				last_url = expected_page.getUrl();
				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){				
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl());
				}
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;
				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					browser.navigateTo(redirect.getStartUrl());
				}

				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel);
				last_url = redirect.getUrls().get(redirect.getUrls().size()-1);
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
					action_repo.save(action);
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
					if(current_idx == ordered_path_objects.size()-1){
						Redirect redirect = BrowserUtils.getPageTransition(last_url, browser, host_channel);
						if(redirect.getUrls().size() > 2){
							path_keys.add(redirect.getKey());
							path_objects_explored.add(redirect);

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
	 * @throws URISyntaxException 
	 *
	 * @pre path != null
	 * @pre path != null
	 */
	public PathMessage crawlPathExplorer(List<String> keys, List<PathObject> path_object_list, Browser browser, String host_channel, PathMessage path) 
			throws IOException, GridException, NoSuchElementException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, URISyntaxException{
		assert browser != null;
		assert keys != null;

		ElementState last_element = null;
		PathObject last_obj = null;
		PageState expected_page = null;

		List<String> path_keys = new ArrayList<String>(keys);
		List<PathObject> ordered_path_objects = PathUtils.orderPathObjects(keys, path_object_list);

		List<PathObject> path_objects_explored = new ArrayList<>();

		String last_url = null;
		int current_idx = 0;
		for(PathObject current_obj: ordered_path_objects){
			path_objects_explored.add(current_obj);
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				last_url = expected_page.getUrl();
				
				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl());
				}
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;
				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					browser.navigateTo(redirect.getStartUrl());
				}

				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel);
				last_url = redirect.getUrls().get(redirect.getUrls().size()-1);
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
					action_repo.save(action);
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
						Redirect redirect = BrowserUtils.getPageTransition(last_url, browser, host_channel);
						if(redirect.getUrls().size() > 2){
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

		if(path.getKeys().size() != path_keys.size()){
			return new PathMessage(path_keys, path_objects_explored, path.getDiscoveryActor(), path.getStatus(), path.getBrowser(), path.getDomainActor(), path.getDomain());
		}
		
		return path;
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

	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element
	 *
	 * @return whether action was able to be performed on element or not
	 */
	public static void performAction(Action action, ElementState elem, WebDriver driver, Point location) throws NoSuchElementException{
		ActionFactory actionFactory = new ActionFactory(driver);
		WebElement element = driver.findElement(By.xpath(elem.getXpath()));
		actionFactory.execAction(element, action.getValue(), action.getName(), location);
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
				browser = BrowserConnectionFactory.getConnection(BrowserType.create(browser_name), BrowserEnvironment.DISCOVERY);
				PageState expected_page = PathUtils.getFirstPage(path.getPathObjects());
				log.warn("expected path url : "+expected_page.getUrl());
				browser.navigateTo(expected_page.getUrl());

				crawlPathExplorer(path.getPathKeys(), path.getPathObjects(), browser, host, path);

				String browser_url = browser.getDriver().getCurrentUrl();
				browser_url = BrowserUtils.sanitizeUrl(browser_url);
				//get last page state
				PageState last_page_state = PathUtils.getLastPageState(path.getPathObjects());
				PageLoadAnimation loading_animation = BrowserUtils.getLoadingAnimation(browser, host);
				if(!browser_url.equals(last_page_state.getUrl())){
					if(loading_animation != null){
						path.getPathKeys().add(loading_animation.getKey());
						path.getPathObjects().add(loading_animation);
					}
				}
								
				//verify that screenshot does not match previous page
				List<ElementState> element_list = BrowserService.getElementsUsingJSoup(browser.getDriver().getPageSource());
				List<ElementState> visible_elements = browser_service.getVisibleElements(browser, element_list);
			
				result_page = browser_service.buildPage(browser, visible_elements, browser_url);
				
				PageState last_page = PathUtils.getLastPageState(path.getPathObjects());
				result_page.setLoginRequired(last_page.isLoginRequired());
			}
			catch(MalformedURLException e){
				log.warn(e.getMessage());
			}
			catch(NullPointerException e){
				e.printStackTrace();
				log.warn("Error happened while exploratory actor attempted to crawl test ");
			}
			catch (GridException e) {
				log.debug("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
			}
			catch (NoSuchElementException e){
				log.warn("Unable to locate element while performing path crawl   ::    "+ e.getMessage());
			}
			catch (WebDriverException e) {
				log.debug("(Exploratory Crawl) web driver exception occurred : " + e.getMessage());
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				//log.warn("WebDriver exception encountered while trying to perform crawl of exploratory path"+e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				log.error("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
				//e.printStackTrace();
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
		}while(result_page == null && tries < 100000);
		
		log.warn("done crawling exploratory path");
		return result_page;
	}

	/**
	 * Handles setting up browser for path crawl and in the event of an error, the method retries until successful
	 * @param browser
	 * @param path
	 * @param host
	 * @return
	 */
	public PageState performPathExploratoryCrawl(String browser_name, PathMessage path, String host) {
		PageState result_page = null;
		int tries = 0;
		Browser browser = null;
		PathMessage new_path = path.clone();
		boolean no_such_element_exception = false;
		
		do{
			try{
				if(!no_such_element_exception){
					no_such_element_exception = false;
					browser = BrowserConnectionFactory.getConnection(BrowserType.create(browser_name), BrowserEnvironment.DISCOVERY);
					PageState expected_page = PathUtils.getFirstPage(path.getPathObjects());
					browser.navigateTo(expected_page.getUrl());
					browser.moveMouseToNonInteractive(new Point(300,300));
					
					new_path = crawlPathExplorer(new_path.getKeys(), new_path.getPathObjects(), browser, host, path);
				}
				Timing.pauseThread(2000);
				String browser_url = browser.getDriver().getCurrentUrl();
				browser_url = BrowserUtils.sanitizeUrl(browser_url);
				//get last page state
				PageState last_page_state = PathUtils.getLastPageState(new_path.getPathObjects());
				PageLoadAnimation loading_animation = BrowserUtils.getLoadingAnimation(browser, host);
				if(!browser_url.equals(last_page_state.getUrl())){
					if(loading_animation != null){
						new_path.getKeys().add(loading_animation.getKey());
						new_path.getPathObjects().add(loading_animation);
					}
				}
								
				//verify that screenshot does not match previous page
				//List<String> xpath_list = BrowserService.getXpathsUsingJSoup(browser.getDriver().getPageSource());
				List<ElementState> element_list = BrowserService.getElementsUsingJSoup(browser.getDriver().getPageSource());

    			List<ElementState> visible_elements = browser_service.getVisibleElements(browser, element_list);
			
				result_page = browser_service.buildPage(browser, visible_elements, browser_url);
				PageState last_page = PathUtils.getLastPageState(path.getPathObjects());
				result_page.setLoginRequired(last_page.isLoginRequired());
			}
			catch(MalformedURLException e){
				log.warn(e.getMessage());
				if(e.getMessage().contains("unknown protocol: tel")){
					break;
				}
			}
			catch(NullPointerException e){
				e.printStackTrace();
				log.error("Error happened while exploratory actor attempted to crawl test ");
			} 
			catch (GridException e) {
				log.debug("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
			}
			catch (NoSuchElementException e){
				log.warn("Unable to locate element while performing path crawl   ::    "+ e.getMessage());
			}
			catch (WebDriverException e) {
				log.debug("(Exploratory Crawl) web driver exception occurred : " + e.getMessage());
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				//log.warn("WebDriver exception encountered while trying to perform crawl of exploratory path"+e.getMessage());
			} 
			catch (NoSuchAlgorithmException e) {
				log.error("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
				//e.printStackTrace();
			} 
			catch(Exception e) {
				log.warn("Exception occurred in performPathExploratoryCrawl using PathMessage actor. \n"+e.getMessage());
				e.printStackTrace();
			}
			finally{
				if(browser != null && !no_such_element_exception){
					browser.close();
				}
			}
			tries++;
		}while(result_page == null && tries < 100000);
		return result_page;
	}
	
	/**
	 * 
	 * @param web_element
	 * @return
	 * 
	 * @pre web_element != null
	 * @pre child_element != null
	 * @pre offset != null
	 */
	public static Point generateRandomLocationWithinElementButNotWithinChildElements(WebElement web_element, ElementState child_element, Point offset) {
		assert web_element != null;
		assert child_element != null;
		assert offset != null;
		
		Point elem_location = web_element.getLocation();

		int left_lower_x = 0;
		int left_upper_x = child_element.getXLocation()- elem_location.getX();
		int right_lower_x = (child_element.getXLocation() - elem_location.getX()) + child_element.getWidth();
		int right_upper_x = web_element.getSize().getWidth();
		
		int top_lower_y = 0;
		int top_upper_y = child_element.getYLocation() - elem_location.getY();
		int bottom_lower_y = child_element.getYLocation() - elem_location.getY() + child_element.getHeight();
		int bottom_upper_y = web_element.getSize().getHeight();
		
		int x_coord = 0;
		int y_coord = 0;
		
		if(left_lower_x != left_upper_x && left_upper_x > 0){
			x_coord = new Random().nextInt(left_upper_x);
		}
		else {
			int difference = right_upper_x - right_lower_x;
			int x_offset = 0;
			if(difference == 0){
				x_offset = new Random().nextInt(right_upper_x);
			}
			else{
				x_offset = new Random().nextInt(difference);
			}
			x_coord = right_lower_x + x_offset;
		}
		
		if(top_lower_y != top_upper_y && top_upper_y > 0){
			y_coord = new Random().nextInt(top_upper_y);
		}
		else {
			int difference = bottom_upper_y - bottom_lower_y;
			int y_offset = 0;
			if(difference == 0){
				y_offset = new Random().nextInt(bottom_upper_y);
			}
			else{
				y_offset = new Random().nextInt(bottom_upper_y - bottom_lower_y);
			}
			y_coord = bottom_lower_y + y_offset;
		}

		return new Point(x_coord, y_coord);
	}
}
