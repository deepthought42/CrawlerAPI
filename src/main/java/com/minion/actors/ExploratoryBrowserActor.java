package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.ArrayUtils;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.actors.LandabilityChecker.BrowserPageState;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.config.SpringExtension;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Group;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.TestStatus;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestService;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;

/**
 * 
 */
@Component
@Scope("prototype")
public class ExploratoryBrowserActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(ExploratoryBrowserActor.class.getName());

	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private TestRepository test_repo;
	
	@Autowired
	private ActionRepository action_repo;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private Crawler crawler;
	
	/**
	 * {@inheritDoc}
	 * 
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Test and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 * @throws NullPointerException 
	 * @throws IOException 
	 * @throws NoSuchElementException 
	 */
	@Override
	public void onReceive(Object message) throws NullPointerException, NoSuchElementException, IOException {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			Browser browser = null;
			if (acct_msg.getData() instanceof ExploratoryPath){
				ExploratoryPath exploratory_path = (ExploratoryPath)acct_msg.getData();
				browser = new Browser((String)acct_msg.getOptions().get("browser"));

				if(exploratory_path.getPathObjects() != null){
					PageState result_page = null;

					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					int start_idx = 0;
					int idx = 0;
					for(PathObject path_obj : exploratory_path.getPathObjects()){
						if(path_obj instanceof PageState){
							PageState page = page_state_repo.findByKey(path_obj.getKey());
							if(page.isLandable()){
								start_idx=idx;
								break;
							}
							idx++;
						}
					}
					
					if(start_idx > 0){
						exploratory_path.setPathObjects(exploratory_path.getPathObjects().subList(start_idx, exploratory_path.getPathObjects().size()));
					}
					for(Action action : exploratory_path.getPossibleActions()){
						ExploratoryPath path = ExploratoryPath.clone(exploratory_path);
						Action action_record = action_repo.findByKey(action.getKey());
						if(action_record != null){
							action = action_record;
						}
						path.addPathObject(action);
						path.addToPathKeys(action.getKey());
						
						final long pathCrawlStartTime = System.currentTimeMillis();
						int tries = 0;
						do{
							try{
								System.err.println("Crawling path");
								result_page = crawler.crawlPath(path.getPathKeys(), path.getPathObjects(), browser, acct_msg.getOptions().get("host").toString());
								break;
							}catch(NullPointerException e){
								browser = new Browser(browser.getBrowserName());
								log.error("Error happened while exploratory actor attempted to crawl test "+e.getLocalizedMessage());
								e.printStackTrace();
							} catch (GridException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (WebDriverException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (NoSuchAlgorithmException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							tries++;
						}while(result_page == null && tries < 5);
					
						//have page checked for landability
						System.err.println("EXPLORATORY BROWSER ACTOR PAGE STATE SCREENSHOTS :: "+result_page.getBrowserScreenshots().size());

						BrowserPageState bps = new BrowserPageState(result_page, browser.getBrowserName());
						final ActorRef landibility_checker = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("landabilityChecker"), "landability_checker"+UUID.randomUUID());
						landibility_checker.tell(bps, ActorRef.noSender() );
						
						Domain domain = domain_repo.findByHost(acct_msg.getOptions().get("host").toString());
						
						final long pathCrawlEndTime = System.currentTimeMillis();

						long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
					
						if(!ExploratoryPath.hasCycle(path.getPathKeys(), result_page)){
					  		boolean results_match = false;
					  		ExploratoryPath last_path = null;
					  		//crawl test and get result
					  		//if this result is the same as the result achieved by the original test then replace the original test with this new test
					  		System.err.println("Starting building parent path");
					  		do{
					  			try{
					  				ExploratoryPath parent_path = buildParentPath(path, browser);
					  			
						  			if(parent_path == null){
						  				break;
						  			}
					  				results_match = doesPathProduceExpectedResult(parent_path, result_page, browser, domain.getUrl());
					  			
						  			if(results_match){
						  				last_path=path;
						  				path = parent_path;
						  			}
					  			}catch(Exception e){
					  				browser = new Browser(browser.getBrowserName());
					  				results_match = false;
					  			}
					  		}while(results_match);

					  		if(last_path == null){
					  			last_path = path;
					  		}
					  		
							createTest(path.getPathKeys(), path.getPathObjects(), result_page, pathCrawlRunTime, domain, acct_msg);
							DiscoveryRecord discovery_record = discovery_repo.findByKey(acct_msg.getOptions().get("discovery_key").toString());
							discovery_record.setTestCount(discovery_record.getTestCount()+1);
					  		discovery_repo.save(discovery_record);
							break;
						}
					}
					
					DiscoveryRecord discovery_record = discovery_repo.findByKey(acct_msg.getOptions().get("discovery_key").toString());
					discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
			  		discovery_record.setLastPathRanAt(new Date());
			  		discovery_record = discovery_repo.save(discovery_record);
					try{
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
				  	}catch(Exception e){
					
					}
					
				  	browser.close();
				}

				
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(test, test.getIsUseful());
			}
			//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);

		}else unhandled(message);
	}
	
	/**
	 * Generates {@link Test Tests} for test
	 * @param test
	 * @param result_page
	 * @throws JsonProcessingException 
	 */
	private void createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, Domain domain, Message<?> acct_msg ) throws JsonProcessingException {
		Test test = new Test(path_keys, path_objects, result_page, null);							

		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), TestStatus.UNVERIFIED, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);
		test = test_service.save(test, acct_msg.getOptions().get("host").toString());

		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());


		System.err.println("!!!!!!!!!!!!!!!!!!     EXPLORATORY ACTOR SENDING TEST TO PATH EXPANSION");
		final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
		path_expansion_actor.tell(test_msg, getSelf());
	}
	
	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 * 
	 * @param test {@linkplain Test} that you want to label
	 */
	private void addFormGroupsToPath(Test test) {
		//check if test has any form elements
		for(PathObject path_obj: test.getPathObjects()){
			if(path_obj.getClass().equals(PageElement.class)){
				PageElement elem = (PageElement)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					test_repo.save(test);
					break;
				}
			}
		}
	}
	
	/**
	 * Checks if the result of crawling a given {@link Test test} results in an identical {@link PageState page state} to the 
	 *   result_page that is passed in.
	 * 
	 * @param test
	 * @param result_page
	 * @param browser
	 * @return
	 * 
	 * @throws NoSuchElementException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws WebDriverException 
	 * @throws GridException 
	 */
	private boolean doesPathProduceExpectedResult(ExploratoryPath path, PageState result_page, Browser browser, String host_channel) throws NoSuchElementException, IOException, GridException, WebDriverException, NoSuchAlgorithmException{
		PageState parent_result = crawler.crawlPath(path.getPathKeys(), path.getPathObjects(), browser, host_channel);
		return parent_result.equals(result_page);
	}
	
	/**
	 * Takes in a {@link Test test} and {@link WebDriver driver} and builds a new test such that
	 *  the last {@link PageElement element} is replaced with it's parent element from the html document controlled by the 
	 *  given {@link WebDriver driver}
	 *  
	 * @param test
	 * @param driver
	 * @return
	 */
	private ExploratoryPath buildParentPath(ExploratoryPath path, Browser browser){
		PageElement elem = null;
		int element_idx = 0;

		for(PathObject obj : path.getPathObjects()){
			if(obj.getType().equals("PageElement")){
				elem = (PageElement)obj;
			}
			element_idx++;
		}
		
		if(elem != null){
			//get parent of element
			WebElement web_elem = browser.getDriver().findElement(By.xpath(elem.getXpath()));
			WebElement parent = browser_service.getParentElement(web_elem);

			//clone test and swap page element with parent
			ExploratoryPath parent_path = ExploratoryPath.clone(path);
			Set<Attribute> attributes = browser_service.extractAttributes(parent, browser.getDriver());
			String this_xpath = browser_service.generateXpath(parent, "", new HashMap<String, Integer>(), browser.getDriver(), attributes); 
			String screenshot_url = browser_service.retrieveAndUploadBrowserScreenshot(browser.getDriver(), parent);
			PageElement parent_tag = new PageElement(parent.getText(), this_xpath, parent.getTagName(), attributes, Browser.loadCssProperties(parent), screenshot_url );
			
			//Ensure Order path objects
			int idx = 0;
			for(PathObject obj : parent_path.getPathObjects()){
				if(obj.getKey().equals(parent_tag)){
					break;
				}
				idx++;
			}
			
			parent_path.getPathObjects().set(idx, parent_tag);
			
			parent_path.getPathKeys().set(element_idx, parent_tag.getKey());
			return parent_path;
		}
		return null;
	}
}
