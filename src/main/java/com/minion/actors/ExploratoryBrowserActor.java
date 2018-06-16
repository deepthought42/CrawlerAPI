package com.minion.actors;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.config.SpringExtension;
import com.qanairy.models.Action;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Group;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.services.BrowserService;

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
	private BrowserService browser_service;
	
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
					DiscoveryRecord discovery_record = null;
					boolean error_while_saving = false;
					do{
						try{
							discovery_record = discovery_repo.findByKey(acct_msg.getOptions().get("discovery_key").toString());
							discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
					  		discovery_record.setLastPathRanAt(new Date());
					  		
							MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

					  		error_while_saving = false;
					  		break;
							//discovery_record_repo.save(discovery_record);
						}catch(Exception e){
							error_while_saving = true;
						}
					}while(error_while_saving);
					
					for(Action action : exploratory_path.getPossibleActions()){
						ExploratoryPath path = ExploratoryPath.clone(exploratory_path);
						path.addPathObject(action);
						path.addToPathKeys(action.getKey());
						
						final long pathCrawlStartTime = System.currentTimeMillis();
						int tries = 0;
						do{
							try{
								result_page = crawler.crawlPath(path.getPathKeys(), path.getPathObjects(), browser, acct_msg.getOptions().get("host").toString());
							}catch(NullPointerException e){
								browser = new Browser(browser.getBrowserName());
								log.error("Error happened while exploratory actor attempted to crawl test");
								e.printStackTrace();
							}
							tries++;
							
							try {
								Thread.sleep(120000L);
							} catch (InterruptedException e) {}
						}while(result_page == null && tries < 10);
						
						do{
							System.err.println("attempting is landable check. Attemp #"+tries);
							
							try{								
								result_page.setLandable(browser_service.checkIfLandable(acct_msg.getOptions().get("browser").toString(), result_page));
								break;
							}catch(NullPointerException e){
								browser = new Browser(browser.getBrowserName());
								log.error("Error happened while exploratory actor attempted to check landability of result page");
								e.printStackTrace();
							}
							
							tries++;
						}while(tries < 5);
						
						Domain domain = domain_repo.findByHost(acct_msg.getOptions().get("host").toString());
						domain.setTestCount(domain.getTestCount()+1);
						domain.addPageState(result_page);
						domain_repo.save(domain);
						
						final long pathCrawlEndTime = System.currentTimeMillis();

						long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
					
						if(!ExploratoryPath.hasCycle(path.getPathObjects(), result_page)){
					  		/*test.setIsUseful(false);
					  		continue;
					  	}
					  	else{
					  		test.setIsUseful(true);
					  		*/
					  		boolean results_match = false;
					  		//crawl test and get result
					  		//if this result is the same as the result achieved by the original test then replace the original test with this new test
					  		System.err.println("Starting building parent path");
					  		do{
					  			ExploratoryPath parent_path = buildParentPath(path, browser);
					  			if(parent_path == null){
					  				System.err.println("Parent path is null  !!!!!!!!!!!!!!!");
					  				break;
					  			}
					  			System.err.println("parent test length @@@@@@@@   "+parent_path);
					  			Browser new_browser = new Browser(browser.getBrowserName());
					  			System.err.println("Retrieved new browser");
					  			results_match = doesPathProduceExpectedResult(parent_path, result_page, new_browser, domain.getUrl());
					  			new_browser.close();
					  			
					  			if(results_match){
					  				path = parent_path;
					  			}
					  		}while(results_match);
							
							MessageBroadcaster.broadcastPageState(result_page, domain.getUrl());
							
							for(PageElement element : result_page.getElements()){
								try {
									MessageBroadcaster.broadcastPageElement(element, domain.getUrl() );
								} catch (JsonProcessingException e) {
								}
							}
					  		
							MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

							createTest(path.getPathKeys(), path.getPathObjects(), result_page, pathCrawlRunTime, domain, acct_msg, discovery_record);
							
							break;
						}
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
	 */
	private void createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, Domain domain, Message<?> acct_msg, DiscoveryRecord discovery ) {
		Test test = new Test(path_keys, path_objects, result_page, "Test #" + domain.getTestCount());							

		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), null, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);
		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());

		//tell memory worker of test
		final ActorRef memory_actor = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
				  .props("MemoryRegistration"), "memory_registration");
		memory_actor.tell(test_msg, getSelf());
		
		System.err.println("!!!!!!!!!!!!!!!!!!     EXPLORATORY ACTOR SENDING TEST TO PAT EXPANSION");
		final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
				  .props("PathExpansionActor"), "path_expansion");
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
	 */
	private boolean doesPathProduceExpectedResult(ExploratoryPath path, PageState result_page, Browser browser, String host_channel) throws NoSuchElementException, IOException{
		System.err.println("attempting to crawl test with length #########   "+path.getPathKeys().size());
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
		int element_idx = -1;
		System.err.println("Path object length :::::   "+path.getPathObjects().size());
		System.err.println("Path keys length :::::   "+path.getPathKeys().size());
		
		for(int idx = path.getPathObjects().size()-1; idx >= 0; idx--){
			if(path.getPathObjects().get(idx).getType().equals("PageElement")){
				elem = (PageElement)path.getPathObjects().get(idx);
				element_idx = idx;
				break;
			}
		}
		System.err.println("element :: "+elem);
		System.err.println("element index :: "+element_idx);
		if(elem != null && element_idx > -1){
			//get parent of element
			WebElement web_elem = browser.getDriver().findElement(By.xpath(elem.getXpath()));
			System.err.println("Getting parent element...");
			WebElement parent = browser_service.getParentElement(web_elem);
			System.err.println("Cloning exploratory path... ");
			//clone test and swap page element with parent
			ExploratoryPath parent_path = ExploratoryPath.clone(path);
			System.err.println("parent path clone :: "+parent_path.getPathKeys().size());
			String this_xpath = browser_service.generateXpath(parent, "", new HashMap<String, Integer>(), browser.getDriver()); 
			System.err.println("Generated xpath :: "+this_xpath);
			PageElement parent_tag = new PageElement(parent.getText(), this_xpath, parent.getTagName(), browser_service.extractAttributes(parent, browser.getDriver()), Browser.loadCssProperties(parent) );
			System.err.println("setting path element object and key at index ::"+element_idx);
			parent_path.getPathObjects().set(element_idx, parent_tag);
			parent_path.getPathKeys().set(element_idx, parent_tag.getKey());
			System.err.println("RETURN PARENT PATH !!!!!!! !!!!!  !!! !!! !!!!!!@@@@@@!!!!!");
			return parent_path;
		}
		System.err.println("returning a null parent path...................");
		return null;
	}
}
