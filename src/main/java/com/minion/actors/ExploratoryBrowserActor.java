package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
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
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.minion.util.Timing;
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
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.PageElementRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestService;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * 
 */
@Component
@Scope("prototype")
public class ExploratoryBrowserActor extends AbstractActor {
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
	private PageElementRepository page_element_repo;
	
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
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message-> {
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
								exploratory_path.setPathKeys(exploratory_path.getPathKeys().subList(start_idx, exploratory_path.getPathKeys().size()));
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
										result_page = crawler.crawlPath(path.getPathKeys(), path.getPathObjects(), browser, acct_msg.getOptions().get("host").toString());
										break;
									}catch(NullPointerException e){
										browser = new Browser(browser.getBrowserName());
										log.error("Error happened while exploratory actor attempted to crawl test "+e.getLocalizedMessage());
									} catch (GridException e) {
										browser = new Browser(browser.getBrowserName());
										log.error("Grid exception encountered while trying to crawl exporatory path"+e.getLocalizedMessage());
									} catch (WebDriverException e) {
										browser = new Browser(browser.getBrowserName());
										log.error("WebDriver exception encountered while trying to crawl exporatory path"+e.getLocalizedMessage());
									} catch (NoSuchAlgorithmException e) {
										log.error("No Such Algorithm exception encountered while trying to crawl exporatory path"+e.getLocalizedMessage());
									}
									catch(Exception e){
										log.error("Exception occurred in explortatory actor. \n"+e.getMessage());
									}

									Timing.pauseThread(60000L);
									tries++;
								}while(result_page == null && tries < 30);
							
								//have page checked for landability
								Domain domain = domain_repo.findByHost(acct_msg.getOptions().get("host").toString());
								
								final long pathCrawlEndTime = System.currentTimeMillis();
								long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
							
								if(!ExploratoryPath.hasCycle(path.getPathKeys(), result_page)){
							  		boolean results_match = false;
							  		ExploratoryPath last_path = null;
							  		//crawl test and get result
							  		//if this result is the same as the result achieved by the original test then replace the original test with this new test
							  		int cnt=0;
							  		do{
							  			try{
							  				ExploratoryPath parent_path = buildParentPath(path, browser);
							  			
								  			if(parent_path == null){
								  				break;
								  			}
								  			
							  				results_match = doesPathProduceExpectedResult(parent_path, result_page, browser, domain.getUrl());
							  			
								  			if(results_match){
								  				last_path = path;
								  				path = parent_path;
								  			}
								  			break;
							  			}catch(Exception e){
							  				log.warn("Exception thrown while building parent path : " + e.getLocalizedMessage());
							  				browser = new Browser(browser.getBrowserName());
							  				results_match = false;
							  			}
							  			cnt++;
							  		}while(results_match && cnt < 20);
							  		
							  		if(last_path == null){
							  			last_path = path;
							  		}
									
							  		PageState result_page_record = page_state_repo.findByKey(result_page.getKey());
							  		if(result_page_record != null){
							  			result_page = result_page_record;
							  		}
							  		createTest(last_path.getPathKeys(), last_path.getPathObjects(), result_page, pathCrawlRunTime, domain, acct_msg);
									DiscoveryRecord discovery_record = discovery_repo.findByKey(acct_msg.getOptions().get("discovery_key").toString());
									discovery_record.setTestCount(discovery_record.getTestCount()+1);
							  		discovery_repo.save(discovery_record);
									//break;
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
		
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.info("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.info("Member is Removed: {}", mRemoved.member());
				})	
				.matchAny(o -> {
					log.info("received unknown message");
				})
				.build();
	}
	
	/**
	 * Generates {@link Test Tests} for test
	 * @param test
	 * @param result_page
	 * @throws JsonProcessingException 
	 */
	private void createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, Domain domain, Message<?> acct_msg ) throws JsonProcessingException {
		
		Test test = new Test(path_keys, path_objects, result_page, null);							
		Test test_db = test_repo.findByKey(test.getKey());
		if(test_db != null){
			test = test_db;
		}
		
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), TestStatus.UNVERIFIED, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);
		test = test_service.save(test, acct_msg.getOptions().get("host").toString());

		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());

		final ActorRef test_simplifier = actor_system.actorOf(SpringExtProvider.get(actor_system)
				  .props("pathExpansionActor"), "path_expansion_actor"+UUID.randomUUID());
		test_simplifier.tell(test_msg, getSelf());
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
					test_service.save(test, test.firstPage().getUrl()); 
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
		return parent_result.getKey().equals(result_page.getKey());
	}
	
	/**
	 * Takes in a {@link Test test} and {@link WebDriver driver} and builds a new test such that
	 *  the last {@link PageElement element} is replaced with it's parent element from the html document controlled by the 
	 *  given {@link WebDriver driver}
	 *  
	 * @param test
	 * @param driver
	 * 
	 * @return
	 * @throws Exception 
	 * 
	 * @pre path != null
	 * @pre browser != null
	 */
	private ExploratoryPath buildParentPath(ExploratoryPath path, Browser browser) throws Exception{
		assert path != null;
		assert browser != null;
		
		PageElement elem = null;
		int idx = 0;
		for(int element_idx=0; element_idx < path.getPathKeys().size(); element_idx++){
			if(path.getPathObjects().get(element_idx).getType().equals("PageElement")){
				elem = (PageElement)path.getPathObjects().get(element_idx);
				idx = element_idx;
			}
		}
		
		if(elem != null){
			List<String> path_keys = path.getPathKeys().subList(0, idx+1);
			List<PathObject> path_objects = path.getPathObjects().subList(0, idx+1);
			crawler.crawlPath(path_keys, path_objects, browser, ((PageState) path_objects.get(0)).getUrl());
			
			//perform action on the element
			//ensure page is equal to expected page
			//get parent of element
			WebElement web_elem = browser.getDriver().findElement(By.xpath(elem.getXpath()));
			WebElement parent = browser_service.getParentElement(web_elem);

			if(!parent.getTagName().equals("body")){
				//clone test and swap page element with parent
				ExploratoryPath parent_path = ExploratoryPath.clone(path);
				Set<Attribute> attributes = browser_service.extractAttributes(parent, browser.getDriver());
				String this_xpath = browser_service.generateXpath(parent, "", new HashMap<String, Integer>(), browser.getDriver(), attributes); 
				String screenshot_url = browser_service.retrieveAndUploadBrowserScreenshot(browser.getDriver(), parent);
				PageElement parent_tag = new PageElement(parent.getText(), this_xpath, parent.getTagName(), attributes, Browser.loadCssProperties(parent), screenshot_url );
				
				PageElement parent_tag_record = page_element_repo.findByKey(parent_tag.getKey());
				if(parent_tag_record != null){
					parent_tag = parent_tag_record;
				}
				
				parent_path.getPathObjects().set(idx, parent_tag);
				
				parent_path.getPathKeys().set(idx, parent_tag.getKey());
				return parent_path;
			}
		}
		return null;
	}
}
