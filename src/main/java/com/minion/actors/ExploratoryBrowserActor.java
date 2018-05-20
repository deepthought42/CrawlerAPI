package com.minion.actors;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.GroupPOJO;
import com.qanairy.models.TestPOJO;
import com.qanairy.models.TestRecordPOJO;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.TestDao;
import com.qanairy.models.dao.impl.DiscoveryRecordDaoImpl;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.models.dao.impl.TestDaoImpl;
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
	@SuppressWarnings("unused")
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

					// increment total paths being explored for domain
					String domain_url = last_page.getUrl().getHost();
					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					DiscoveryRecordDao discovery_repo = new DiscoveryRecordDaoImpl();
					DiscoveryRecord discovery_record = discovery_repo.find(acct_msg.getOptions().get("discovery_key").toString());
					discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
			  		discovery_record.setLastPathRanAt(new Date());
					discovery_repo.save(discovery_record);
					
					for(Action action : exploratory_path.getPossibleActions()){
						Test test = Test.clone(exploratory_path);
						test.addPathObject(action);
						final long pathCrawlStartTime = System.currentTimeMillis();
						int tries = 0;
						do{
							try{
								result_page = Crawler.crawlPath(test, browser);
							}catch(NullPointerException e){
								browser = new Browser(browser.getBrowserName());
								log.error("Error happened while exploratory actor attempted to crawl test");
							}
							tries++;
						}while(result_page == null && tries < 5);
						
						do{
							System.err.println("attempting is landable check. Attemp #"+tries);
							
							try{
								result_page.setLandable(last_page.isLandable(acct_msg.getOptions().get("browser").toString()));
								break;
							}catch(NullPointerException e){
								browser = new Browser(browser.getBrowserName());
								log.error("Error happened while exploratory actor attempted to crawl test");
							}
							
							tries++;
						}while(tries < 5);
						
						final long pathCrawlEndTime = System.currentTimeMillis();

						long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
					
						if(ExploratoryPath.hasCycle(test, result_page)){
					  		test.setIsUseful(false);
					  		continue;
					  	}
					  	else{
					  		test.setIsUseful(true);
					  		boolean results_match = false;
					  		//crawl test and get result
					  		//if this result is the same as the result achieved by the original test then replace the original test with this new test
					  		
					  		do{
					  			Test parent_path = buildParentPath(test, browser.getDriver());
					  			if(parent_path == null){
					  				break;
					  			}
					  			System.err.println("parent test length @@@@@@@@   "+parent_path);
					  			Browser new_browser = new Browser(browser.getBrowserName());
					  			System.err.println("Retrieved new browser");
					  			results_match = doesPathProduceExpectedResult(parent_path, result_page, new_browser);
					  			new_browser.close();
					  			
					  			if(results_match){
					  				test = parent_path;
					  			}
					  		}while(results_match);
					  		
					  		Domain domain = domain_dao.find(domain_url);
							domain.setTestCount(domain.getTestCount()+1);
							domain_dao.save(domain);
							
					  		createTest(test, result_page, pathCrawlRunTime, domain, acct_msg, discovery_record);
							MessageBroadcaster.broadcastDiscoveryStatus(domain.getUrl(), discovery_record);

					  		Test new_path = Test.clone(test);
							new_path.add(result_page);
							Message<Test> path_msg = new Message<Test>(acct_msg.getAccountKey(), new_path, acct_msg.getOptions());

							final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
							path_expansion_actor.tell(path_msg, getSelf());
					  	}
						
						if(test.isUseful()){
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
	private void createTest(Test test, PageState result_page, long crawl_time, Domain domain, Message<?> acct_msg, DiscoveryRecord discovery ) {
		test.setIsUseful(true);
		Test test = new TestPOJO(test, result_page, "Test #" + domain.getTestCount());							
		TestDao test_repo = new TestDaoImpl();
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecordPOJO(test.getLastRunTimestamp(), null, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);
		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());

		//tell memory worker of test
		final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
		memory_actor.tell(test_msg, getSelf());
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
	private boolean doesPathProduceExpectedResult(Test test, PageState result_page, Browser browser) throws NoSuchElementException, IOException{
		System.err.println("attempting to crawl test with length #########   "+test.size());
		PageState parent_result = Crawler.crawlPath(test, browser);
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
	private Test buildParentPath(Test test, WebDriver driver){
		PageElement elem = null;
		int element_idx = -1;
		for(int idx = test.size()-1; idx >= 0; idx--){
			if(test.getPath().get(idx).getType().equals("PageElement")){
				elem = (PageElement)test.getPath().get(idx);
				element_idx = idx;
				break;
			}
		}
		
		if(elem != null && element_idx > -1){
			//get parent of element
			WebElement web_elem = driver.findElement(By.xpath(elem.getXpath()));
			WebElement parent = Browser.getParentElement(web_elem);
			
			//clone test and swap page element with parent
			Test parent_path = Test.clone(test);
			String this_xpath = Browser.generateXpath(parent, "", new HashMap<String, Integer>(), driver); 
			
			PageElement parent_tag = new PageElement(parent.getText(), this_xpath, parent.getTagName(), Browser.extractedAttributes(parent, (JavascriptExecutor)driver), PageElement.loadCssProperties(parent) );
			parent_path.getPath().set(element_idx, parent_tag);
			
			return parent_path;
		}
		return null;
	}
}
