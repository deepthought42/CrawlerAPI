package com.minion.actors;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.GroupPOJO;
import com.qanairy.models.PageElementPOJO;
import com.qanairy.models.TestPOJO;
import com.qanairy.models.TestRecordPOJO;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.impl.DiscoveryRecordDaoImpl;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.models.dao.impl.PageStateDaoImpl;
import com.qanairy.persistence.Action;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class ExploratoryBrowserActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(ExploratoryBrowserActor.class.getName());

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

		  		OrientConnectionFactory conn = new OrientConnectionFactory();
				DomainDao domain_dao = new DomainDaoImpl();	

				browser = new Browser((String)acct_msg.getOptions().get("browser"));
				
				PageState last_page = exploratory_path.findLastPage();
				
				if(exploratory_path.getPathObjects() != null){
					PageState result_page = null;

					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					DiscoveryRecordDao discovery_repo = new DiscoveryRecordDaoImpl();
					DiscoveryRecord discovery_record = null;
					boolean error_while_saving = false;
					do{
						try{
							discovery_record = discovery_repo.find(acct_msg.getOptions().get("discovery_key").toString());
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
								result_page = Crawler.crawlPath(path.getPathKeys(), path.getPathObjects(), browser, acct_msg.getOptions().get("host").toString());
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
								result_page.setLandable(Browser.checkIfLandable(acct_msg.getOptions().get("browser").toString(), result_page));
								break;
							}catch(NullPointerException e){
								browser = new Browser(browser.getBrowserName());
								log.error("Error happened while exploratory actor attempted to check landability of result page");
								e.printStackTrace();
							}
							
							tries++;
						}while(tries < 5);
						PageStateDao page_state_dao = new PageStateDaoImpl();
						Domain domain = null;
						boolean exception_occurred = false;
						do{
							try{
						  		domain = domain_dao.find(acct_msg.getOptions().get("host").toString());
								domain.setTestCount(domain.getTestCount()+1);
							}catch(Exception e){
								
							}
						}while(exception_occurred);
						
						exception_occurred = false;
						do{
							try{
								domain = domain_dao.find(acct_msg.getOptions().get("host").toString());
								
								boolean exists = false;
								for(PageState state : domain.getPageStates()){
									if(state.getKey().equals(result_page.getKey())){
										exists = true;
									}
								}

								if(!exists){
									domain.addPageState(page_state_dao.save(result_page));
								}
								exception_occurred = false;
							}catch(Exception e){
								exception_occurred = true;
							}
						}while(exception_occurred);
						//domain_dao.save(domain);
						
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
					  			ExploratoryPath parent_path = buildParentPath(path, browser.getDriver());
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
				  	conn.close();
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
		Test test = new TestPOJO(path_keys, path_objects, result_page, "Test #" + domain.getTestCount());							

		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecordPOJO(test.getLastRunTimestamp(), null, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);
		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());

		//tell memory worker of test
		final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
		memory_actor.tell(test_msg, getSelf());
		
		System.err.println("!!!!!!!!!!!!!!!!!!     EXPLORATORY ACTOR SENDING TEST TO PAT EXPANSION");
		final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
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
					test.addGroup(new GroupPOJO("form"));
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
		PageState parent_result = Crawler.crawlPath(path.getPathKeys(), path.getPathObjects(), browser, host_channel);
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
	private ExploratoryPath buildParentPath(ExploratoryPath path, WebDriver driver){
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
			WebElement web_elem = driver.findElement(By.xpath(elem.getXpath()));
			System.err.println("Getting parent element...");
			WebElement parent = Browser.getParentElement(web_elem);
			System.err.println("Cloning exploratory path... ");
			//clone test and swap page element with parent
			ExploratoryPath parent_path = ExploratoryPath.clone(path);
			System.err.println("parent path clone :: "+parent_path.getPathKeys().size());
			String this_xpath = Browser.generateXpath(parent, "", new HashMap<String, Integer>(), driver); 
			System.err.println("Generated xpath :: "+this_xpath);
			PageElement parent_tag = new PageElementPOJO(parent.getText(), this_xpath, parent.getTagName(), Browser.extractedAttributes(parent, (JavascriptExecutor)driver), Browser.loadCssProperties(parent) );
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
