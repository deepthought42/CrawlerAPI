package com.minion.browsing;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

import com.qanairy.api.exceptions.PagesAreNotMatchingException;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.Page;
import com.qanairy.models.PageAlert;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.enums.AlertChoice;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.message.PageFoundMessage;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.PathUtils;
import com.qanairy.utils.TimingUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

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
	private PageService page_service;
	
	@Autowired
	private DomainService domain_service;
	
	 @Autowired
    private ActorSystem actor_system;
	
	/**
	 * Crawls the path using the provided {@link Browser browser}
	 *
	 * @param browser
	 * @return {@link Page result_page} state that resulted from crawling path
	 * @throws Exception 
	 *
	 * @throws URISyntaxException 
	 *
	 * @pre path != null
	 * @pre path != null
	 */
	public PageState crawlPath(String user_id, Domain domain, List<String> path_keys, List<LookseeObject> path_objects, Browser browser, String host_channel, 
								Map<Integer, ElementState> visible_element_map, List<ElementState> known_visible_elements) 
										throws Exception{
		assert browser != null;
		assert path_keys != null;

		LookseeObject last_obj = null;
		List<LookseeObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);
		
		log.warn("path keys :: " + path_keys.size());
		ElementState last_element = null;

		//boolean screenshot_matches = false;
		//check if page is the same as expected.
		PageState expected_page = PathUtils.getFirstPage(ordered_path_objects);
		log.warn("expected page returned :: "+expected_page);
		log.warn("expected page url :: " + expected_page.getUrl());
		browser.navigateTo(expected_page.getUrl());

		for(LookseeObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
/*
				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl(), user_id);
				}
				*/
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
				
				//scroll element to middle of screen
				browser.scrollToElement(last_element);
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
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel, user_id);
			}
			else if(current_obj instanceof PageLoadAnimation){
				BrowserUtils.getLoadingAnimation(browser, host_channel, user_id);
			}
			else if(current_obj instanceof PageAlert){
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver(), AlertChoice.DISMISS);
			}

			last_obj = current_obj;
		}

		return browser_service.buildPageState(user_id, domain, browser);
	}

	/**
	 * Crawls the path using the provided {@link Browser browser}
	 * @param browser
	 * @param user_id TODO
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
	public void crawlPathWithoutBuildingResult(List<String> path_keys, List<LookseeObject> path_objects, Browser browser, String host_channel, String user_id) 
			throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, URISyntaxException{
		assert browser != null;
		assert path_keys != null;

		ElementState last_element = null;
		LookseeObject last_obj = null;
		PageState expected_page = null;
		
		List<LookseeObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);
		
		for(LookseeObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				BrowserUtils.detectShortAnimation(browser, expected_page.getUrl(), user_id);
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
				browser.scrollToElement(last_element);
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
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel, user_id);
			}
			else if(current_obj instanceof PageLoadAnimation){
				BrowserUtils.getLoadingAnimation(browser, host_channel, user_id);
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver(), AlertChoice.DISMISS);
			}

			last_obj = current_obj;
		}
	}

	/**
	 * Crawls the path using the provided {@link Browser browser}
	 * @param browser
	 * @param user_id TODO
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
	public void crawlParentPathWithoutBuildingResult(List<String> path_keys, List<LookseeObject> path_objects, Browser browser, String host_channel, ElementState child_element, String user_id)
							throws IOException, GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, URISyntaxException, NullPointerException{
		assert browser != null;
		assert path_keys != null;

		ElementState last_element = null;
		LookseeObject last_obj = null;
		PageState expected_page = null;
		
		List<LookseeObject> ordered_path_objects = PathUtils.orderPathObjects(path_keys, path_objects);
		
		for(LookseeObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				
				/*
				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl(), user_id);
				}
				*/
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
				browser.scrollToElement(last_element);
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				
				//perform action outside bounds of child elements
				WebElement elem = browser.getDriver().findElement(By.xpath(last_element.getXpath()));
				//compile child element coordinates and sizes
				
				Point click_location = generateRandomLocationWithinElementButNotWithinChildElements(elem, child_element);
				
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
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel, user_id);
			}
			else if(current_obj instanceof PageLoadAnimation){
				BrowserUtils.getLoadingAnimation(browser, host_channel, user_id);
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver(), AlertChoice.DISMISS);
			}

			last_obj = current_obj;
		}
	}

	
	/**
	 * Crawls the path using the provided {@link Browser browser}
	 * @param browser
	 * @param path list of vertex keys
	 * @param user_id TODO
	 *
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
	@Deprecated
	public void crawlPathExplorer(List<String> keys, List<LookseeObject> path_object_list, Browser browser, String host_channel, ExploratoryPath path, String user_id) throws IOException, GridException, NoSuchElementException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, URISyntaxException{
		assert browser != null;
		assert keys != null;

		ElementState last_element = null;
		LookseeObject last_obj = null;
		PageState expected_page = null;
		List<String> path_keys = new ArrayList<String>(keys);
		List<LookseeObject> ordered_path_objects = PathUtils.orderPathObjects(keys, path_object_list);
		List<LookseeObject> path_objects_explored = new ArrayList<>(ordered_path_objects);

		String last_url = null;
		int current_idx = 0;
		for(LookseeObject current_obj: ordered_path_objects){
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				last_url = expected_page.getUrl();
				
				/*
				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){				
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl(), user_id);
				}
				*/
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;
				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					browser.navigateTo(redirect.getStartUrl());
				}

				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel, user_id);
				last_url = redirect.getUrls().get(redirect.getUrls().size()-1);
			}
			else if(current_obj instanceof PageLoadAnimation){
				BrowserUtils.getLoadingAnimation(browser, host_channel, user_id);
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
				browser.scrollToElement(last_element);
				//BrowserUtils.detectShortAnimation(browser, expected_page.getUrl());
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
					PageAlert page_alert = new PageAlert(alert.getText());
					path_keys.add(page_alert.getKey());
					path_objects_explored.add(page_alert);
					current_idx++;
				}
				else{
					if(current_idx == ordered_path_objects.size()-1){
						Redirect redirect = BrowserUtils.getPageTransition(last_url, browser, host_channel, user_id);
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
				alert.performChoice(browser.getDriver(), AlertChoice.DISMISS);
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
	 * @param browser
	 * @param path list of vertex keys
	 * @param user_id TODO
	 *
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
	public PathMessage crawlPathExplorer(List<String> keys, List<LookseeObject> path_object_list, Browser browser, String host_channel, PathMessage path, String user_id) 
			throws IOException, GridException, NoSuchElementException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, URISyntaxException{
		assert browser != null;
		assert keys != null;

		ElementState last_element = null;
		LookseeObject last_obj = null;
		PageState expected_page = null;

		List<String> path_keys = new ArrayList<String>(keys);
		List<LookseeObject> ordered_path_objects = PathUtils.orderPathObjects(keys, path_object_list);

		List<LookseeObject> path_objects_explored = new ArrayList<>();

		String last_url = null;
		int current_idx = 0;
		for(LookseeObject current_obj: ordered_path_objects){
			path_objects_explored.add(current_obj);
			if(current_obj instanceof PageState){
				expected_page = (PageState)current_obj;
				last_url = expected_page.getUrl();
				
				/**
				if(browser.getXScrollOffset() != expected_page.getScrollXOffset()
						|| browser.getYScrollOffset() != expected_page.getScrollYOffset()){
					browser.scrollTo(expected_page.getScrollXOffset(), expected_page.getScrollYOffset());
					BrowserUtils.detectShortAnimation(browser, expected_page.getUrl(), user_id);
				}
				*/
			}
			else if(current_obj instanceof Redirect){
				Redirect redirect = (Redirect)current_obj;
				//if redirect is preceded by a page state or nothing then initiate navigation
				if(last_obj == null || last_obj instanceof PageState){
					browser.navigateTo(redirect.getStartUrl());
				}

				//if redirect follows an action then watch page transition
				BrowserUtils.getPageTransition(redirect.getStartUrl(), browser, host_channel, user_id);
				last_url = redirect.getUrls().get(redirect.getUrls().size()-1);
			}
			else if(current_obj instanceof PageLoadAnimation){
				BrowserUtils.getLoadingAnimation(browser, host_channel, user_id);
			}
			else if(current_obj instanceof ElementState){
				last_element = (ElementState) current_obj;
				browser.scrollToElement(last_element);
				//BrowserUtils.detectShortAnimation(browser, expected_page.getUrl());
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
					PageAlert page_alert = new PageAlert(alert.getText());
					path_keys.add(page_alert.getKey());
					path_objects_explored.add(page_alert);
					current_idx++;
				}
				else{
					if((current_idx < ordered_path_objects.size()-1
							&& !ordered_path_objects.get(current_idx+1).getKey().contains("redirect")
							&& !ordered_path_objects.get(current_idx+1).getKey().contains("elementstate"))
							|| (current_idx == ordered_path_objects.size()-1 && !last_url.equals(BrowserUtils.sanitizeUrl(browser.getDriver().getCurrentUrl())))){
						Redirect redirect = BrowserUtils.getPageTransition(last_url, browser, host_channel, user_id);
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
				alert.performChoice(browser.getDriver(), AlertChoice.ACCEPT);
			}
			last_obj = current_obj;
			current_idx++;
		}
		
		if(path.getKeys().size() != path_keys.size()){
			return new PathMessage(path_keys, path_objects_explored, path.getDiscoveryActor(), path.getStatus(), path.getBrowser(), path.getDomainActor(), path.getDomain(), path.getAccountId());
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
		TimingUtils.pauseThread(1500L);
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
		actionFactory.execAction(element, action.getValue(), action.getName());
		TimingUtils.pauseThread(1500L);
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
	public PageState performPathExploratoryCrawl(String user_id, Domain domain, String browser_name, ExploratoryPath path, String host) {
		PageState result_page = null;
		int tries = 0;
		Browser browser = null;
		do{
			try{
				browser = BrowserConnectionHelper.getConnection(BrowserType.create(browser_name), BrowserEnvironment.DISCOVERY);
				String url = PathUtils.getFirstUrl(path.getPathObjects());
				log.warn("expected path url : "+url);
				browser.navigateTo(url);
				browser.moveMouseToNonInteractive(new Point(300,300));

				crawlPathExplorer(path.getPathKeys(), path.getPathObjects(), browser, host, path, user_id);

				String browser_url = browser.getDriver().getCurrentUrl();
				browser_url = BrowserUtils.sanitizeUrl(browser_url);
				//get last page state
				PageState last_page_state = PathUtils.getLastPageState(path.getPathObjects());
				PageLoadAnimation loading_animation = BrowserUtils.getLoadingAnimation(browser, host, user_id);
				if(!browser_url.equals(last_page_state.getUrl()) && loading_animation != null){
					path.getPathKeys().add(loading_animation.getKey());
					path.getPathObjects().add(loading_animation);
				}
						
				//verify that screenshot does not match previous page
				result_page = browser_service.buildPageState(user_id, domain, browser);
				
				PageState last_page = PathUtils.getLastPageState(path.getPathObjects());
				result_page.setLoginRequired(last_page.isLoginRequired());
			}
			catch(NullPointerException e){
				log.warn("Null Pointer Exception happened during exploratory crawl ::  "+e.getMessage());
			}
			catch (GridException e) {
				log.warn("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
			}
			catch (NoSuchElementException e){
				log.warn("Unable to locate element while performing path crawl   ::    "+ e.getMessage());
			}
			catch (WebDriverException e) {
				log.warn("(Exploratory Crawl) web driver exception occurred : " + e.getMessage());
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				//log.warn("WebDriver exception encountered while trying to perform crawl of exploratory path"+e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				log.error("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getMessage());
				//e.printStackTrace();
			} catch(Exception e) {
				log.warn("Exception occurred in performPathExploratoryCrawl actor. \n"+e.getMessage());
				//e.printStackTrace();
			}
			finally{
				if(browser != null){
					browser.close();
				}
			}
			tries++;
		}while(result_page == null && tries < 1000);
		
		log.warn("done crawling exploratory path");
		return result_page;
	}

	/**
	 * Handles setting up browser for path crawl and in the event of an error, the method retries until successful
	 * @param browser
	 * @param path
	 * @param host
	 * @return
	 * @throws Exception 
	 */
	@Deprecated
	public PageState performPathExploratoryCrawl(String user_id, Domain domain, String browser_name, PathMessage path) throws Exception {
		PageState result_page = null;
		int tries = 0;
		Browser browser = null;
		PathMessage new_path = path.clone();
		boolean no_such_element_exception = false;
		
		do{
			/*
			Timeout timeout = Timeout.create(Duration.ofSeconds(30));
			Future<Object> future = Patterns.ask(path.getDomainActor(), new DiscoveryActionRequest(path.getDomain(), path.getAccountId()), timeout);
			DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());
			
			if(discovery_action == DiscoveryAction.STOP) {
				throw new DiscoveryStoppedException();
			}
			*/
			
			try{
				if(!no_such_element_exception){
					no_such_element_exception = false;
					browser = BrowserConnectionHelper.getConnection(BrowserType.create(browser_name), BrowserEnvironment.DISCOVERY);
					PageState expected_page = PathUtils.getFirstPage(path.getPathObjects());
					browser.navigateTo(expected_page.getUrl());
					browser.moveMouseToNonInteractive(new Point(300,300));
					
					new_path = crawlPathExplorer(new_path.getKeys(), new_path.getPathObjects(), browser, domain.getHost(), path, user_id);
				}
				String browser_url = browser.getDriver().getCurrentUrl();
				browser_url = BrowserUtils.sanitizeUrl(browser_url);
				//get last page state
				PageState last_page_state = PathUtils.getLastPageState(new_path.getPathObjects());
				PageLoadAnimation loading_animation = BrowserUtils.getLoadingAnimation(browser, domain.getHost(), user_id);
				if(!browser_url.equals(last_page_state.getUrl()) && loading_animation != null){
					new_path.getKeys().add(loading_animation.getKey());
					new_path.getPathObjects().add(loading_animation);
				}
								
				//verify that screenshot does not match previous page
				result_page = browser_service.buildPageState(user_id, domain, browser);
				result_page.setLoginRequired(last_page_state.isLoginRequired());
				return result_page;
			}
			catch(NullPointerException e){
				e.printStackTrace();
				log.error("NPE occurred while exploratory crawl  ::   "+e.getMessage());
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
		}while(result_page == null && tries < 1000000);
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
	public static Point generateRandomLocationWithinElementButNotWithinChildElements(WebElement web_element, ElementState child_element) {
		assert web_element != null;
		assert child_element != null;
		
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
	
	/**
	 * Crawl domain by using links to map site and reach new pages until site is completely covered. Along the way this method also executes
	 * all desired audits on each page.

	 * @param domain
	 * @param user_id
	 * @param audit_categories
	 * 
	 * @return
	 * 
	 * @throws Exception
	 * 
	 * @pre domain != null
	 * @pre user_id != null
	 * @pre audit_categories != null
	 */
	public Map<String, Page> crawlAndExtractData(Domain domain) throws Exception {
		assert domain != null;
		
		Map<String, Boolean> frontier = new HashMap<>();
		Map<String, Page> visited = new HashMap<>();
		//add link to frontier
		frontier.put(domain.getUrl(), Boolean.TRUE);
		
		
		while(!frontier.isEmpty()) {
			Map<String, Boolean> external_links = new HashMap<>();
			Page page = null;
			//remove link from beginning of frontier
			String page_url = frontier.keySet().iterator().next();
			frontier.remove(page_url);
			if(page_url.isEmpty() || page_url.contains("mailto") || page_url.contains(".jpg") || page_url.contains(".png")) {
				continue;
			}

			//construct page and add page to list of page states
			page = new Page(page_url);
			page = page_service.save( page );

			log.debug("page created with url..."+page_url);			
			domain.addPage(page);
			domain = domain_service.save(domain);
			
			visited.put(page_url, page);
			//send message to page data extractor
			ActorRef page_data_extractor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					.props("pageDataExtractor"), "pageDataExtractor"+UUID.randomUUID());
			PageFoundMessage page_found_message = new PageFoundMessage(page);
			page_data_extractor.tell(page_found_message, null);

			//retrieve html source for page
			try {
				Document doc = Jsoup.connect(page_url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get();
				log.debug("Document title :: " + doc.title());
				Elements links = doc.select("a");
				for (Element link : links) {
					String href = BrowserUtils.sanitizeUrl(link.absUrl("href"));
					
					//check if external link
					if( !href.isEmpty() && BrowserUtils.isExternalLink(domain.getHost().replace("www.", ""), href)) {
						log.debug("adding to external links :: "+href);
	   					external_links.put(href, Boolean.TRUE);
					}
					else if( !href.isEmpty() && !visited.containsKey(href) && !frontier.containsKey(href) && !BrowserUtils.isImageUrl(href)){
						log.warn("adding link to frontier :: "+href);
						//add link to frontier
						frontier.put(href, Boolean.TRUE);
					}
				}
			}catch(SocketTimeoutException e) {
				log.warn("Error occurred while navigating to :: "+page_url);
			}
			catch(HttpStatusException e) {
				log.warn("HTTP Status Exception occurred while navigating to :: "+page_url);
				e.printStackTrace();
			}
			catch(IllegalArgumentException e) {
				log.warn("illegal argument exception occurred when connecting to ::  " + page_url);
				e.printStackTrace();
			}
			catch(UnsupportedMimeTypeException e) {
				log.warn(e.getMessage() + " : " +e.getUrl());
			}
		}
		System.out.println("total links visited :::  "+visited.keySet().size());
		
		return visited;
	}
}
