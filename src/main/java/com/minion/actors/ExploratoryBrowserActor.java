package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import com.minion.browsing.BrowserFactory;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Group;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.services.ActionService;
import com.qanairy.services.BrowserService;
import com.qanairy.services.EmailService;
import com.qanairy.services.PageElementService;
import com.qanairy.services.PageStateService;
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
	private EmailService email_service;
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private PageElementService page_element_service;
	
	@Autowired
	private ActionService action_service;
	
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
					if (acct_msg.getData() instanceof ExploratoryPath){
						Browser browser = null;

						String browser_name = acct_msg.getOptions().get("browser").toString();
						ExploratoryPath exploratory_path = (ExploratoryPath)acct_msg.getData();
		
						if(exploratory_path.getPathObjects() != null){
							System.err.println("EXPLORATORY PATH OBJECTS  ::   " + exploratory_path.getPathObjects().size());
							PageState result_page = null;
		
							//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
							//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 						
							for(Action action : exploratory_path.getPossibleActions()){
								ExploratoryPath path = ExploratoryPath.clone(exploratory_path);
								Action action_record = action_service.findByKey(action.getKey());
								if(action_record != null){
									action = action_record;
								}
								path.addPathObject(action);
								path.addToPathKeys(action.getKey());
								
								final long pathCrawlStartTime = System.currentTimeMillis();
								result_page = crawler.performPathCrawl(browser_name, path, acct_msg.getOptions().get("host").toString());
								
								result_page = page_state_service.save(result_page);
								
								//have page checked for landability
								//Domain domain = domain_repo.findByHost(acct_msg.getOptions().get("host").toString());
								String page_url = acct_msg.getOptions().get("host").toString();
								final long pathCrawlEndTime = System.currentTimeMillis();
								long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
							
								if(!ExploratoryPath.hasCycle(path.getPathObjects(), result_page)){
							  		boolean results_match = false;
							  		ExploratoryPath last_path = null;
							  		//crawl test and get result
							  		//if this result is the same as the result achieved by the original test then replace the original test with this new test
							  		
							  		int cnt=0;
							  		do{
						  				log.info("building parent path...attempt # ::  "+cnt);

							  			try{
							  				browser = browser_service.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
							  				ExploratoryPath parent_path = buildParentPath(path, browser);
							  			
								  			if(parent_path == null){
								  				break;
								  			}
								  			
							  				results_match = doesPathProduceExpectedResult(parent_path, result_page, browser, page_url);
							  				log.info("Does path produce expected result???  "+results_match);
								  			if(results_match){
								  				last_path = path;
								  				path = parent_path;
								  			}
								  			break;
							  			}catch(Exception e){
							  				log.warn("Exception thrown while building parent path : " + e.getLocalizedMessage());
							  				results_match = false;
							  			}
							  			finally{
							  				browser.close();
							  			}
							  			cnt++;
							  		}while(results_match && cnt < Integer.MAX_VALUE);
							  		
							  		if(last_path == null){
							  			last_path = path;
							  		}
									
							  		System.err.println("Creating test for parent path");
							  		Test test = createTest(last_path.getPathKeys(), last_path.getPathObjects(), result_page, pathCrawlRunTime, acct_msg);
							  		
							  		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());

									final ActorRef path_expansion = actor_system.actorOf(SpringExtProvider.get(actor_system)
											  .props("pathExpansionActor"), "path_expansion_actor"+UUID.randomUUID());
									path_expansion.tell(test_msg, getSelf());
									
									DiscoveryRecord discovery_record = discovery_repo.findByKey(acct_msg.getOptions().get("discovery_key").toString());
									discovery_record.setTestCount(discovery_record.getTestCount()+1);
							  		discovery_repo.save(discovery_record);
								}
							}
							
							DiscoveryRecord discovery_record = null;
							do{
								discovery_record = discovery_repo.findByKey(acct_msg.getOptions().get("discovery_key").toString());
								discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
						  		discovery_record.setLastPathRanAt(new Date());
						  		discovery_record = discovery_repo.save(discovery_record);
							}while(discovery_record == null);
					  		//send email if this is the last test
					  		if(discovery_record.getExaminedPathCount() >= discovery_record.getTotalPathCount()){
						    	email_service.sendSimpleMessage(acct_msg.getAccountKey(), "Discovery on "+discovery_record.getDomainUrl()+" has finished. Visit the <a href='app.qanairy.com/discovery>Discovery panel</a> to start classifying your tests", "The test has finished running");
							}
							try{
								MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
						  	}catch(Exception e){
						  		log.error("Error sending discovery status from Exploratory Actor :: "+e.getMessage());
							}
						}
		
						
						//PLACE CALL TO LEARNING SYSTEM HERE
						//Brain.learn(test, test.getIsUseful());
					}
					postStop();

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
	 * @throws MalformedURLException 
	 */
	private Test createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, Message<?> acct_msg ) throws JsonProcessingException, MalformedURLException {
		log.info("Creating test........");
		Test test = new Test(path_keys, path_objects, result_page, null);
		
		log.info("Looking up test by key ..... ");
		Test test_db = test_service.findByKey(test.getKey());
		if(test_db != null){
			test = test_db;
		}
		
		log.info("Setting test data");
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		log.info("Creating test record");
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), TestStatus.UNVERIFIED, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);

		log.error("Test :: "+test);
		log.error("Test first page :: "+test.firstPage());
		log.error("Test first page url  :: "+test.firstPage().getUrl());
		log.error("Test Result  :: "+test.getResult());
		log.error("Test result url  :: "+test.getResult().getUrl());
		boolean leaves_domain = (!test.firstPage().getUrl().contains(new URL(test.getResult().getUrl()).getHost()));
		test.setSpansMultipleDomains(leaves_domain);
		return test_service.save(test, acct_msg.getOptions().get("host").toString());
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
		PageState parent_result = crawler.crawlPath(path.getPathKeys(), path.getPathObjects(), browser, host_channel, path);
		return parent_result.equals(result_page);
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
		for(int element_idx=path.getPathKeys().size()-1; element_idx > 0 ; element_idx--){
			if(path.getPathObjects().get(element_idx).getType().equals("PageElement")){
				elem = (PageElement)path.getPathObjects().get(element_idx);
				idx = element_idx;
				break;
			}
		}
		
		if(elem != null){
			List<String> path_keys = path.getPathKeys().subList(0, idx+1);
			List<PathObject> path_objects = path.getPathObjects().subList(0, idx+1);
			System.err.println("path objects length :: " + path.getPathObjects().size());
			crawler.crawlPath(path_keys, path_objects, browser, ((PageState) path_objects.get(0)).getUrl(), path);
			
			//perform action on the element
			//ensure page is equal to expected page
			//get parent of element
			WebElement web_elem = browser.getDriver().findElement(By.xpath(elem.getXpath()));
			WebElement parent = browser_service.getParentElement(web_elem);
			String parent_tag_name = parent.getTagName();
			if(!parent_tag_name.equals("body")){
				//clone test and swap page element with parent
				ExploratoryPath parent_path = ExploratoryPath.clone(path);
				Set<Attribute> attributes = browser_service.extractAttributes(parent, browser.getDriver());
				String this_xpath = browser_service.generateXpath(parent, "", new HashMap<String, Integer>(), browser.getDriver(), attributes); 
				String screenshot_url = browser_service.retrieveAndUploadBrowserScreenshot(browser.getDriver(), parent);
				PageElement parent_tag = new PageElement(parent.getText(), this_xpath, parent_tag_name, attributes, Browser.loadCssProperties(parent), screenshot_url );
				
				PageElement parent_tag_record = page_element_service.findByKey(parent_tag.getKey());
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
