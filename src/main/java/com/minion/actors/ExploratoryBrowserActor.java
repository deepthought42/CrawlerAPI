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
import com.qanairy.models.Action;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Group;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.DiscoveryRecordRepository;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.OrientConnectionFactory;

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
	 * 		 is also a Path and thus if the order is reversed, then the ExploratoryPath code never runs when it should
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
				DomainRepository domain_repo = new DomainRepository();	

				browser = new Browser((String)acct_msg.getOptions().get("browser"));
				
				Page last_page = exploratory_path.findLastPage();
				
				if(exploratory_path.getPath() != null){
					Page result_page = null;

					// increment total paths being explored for domain
					String domain_url = last_page.getUrl().getHost();
					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					DiscoveryRecordRepository discovery_repo = new DiscoveryRecordRepository();
					DiscoveryRecord discovery_record = discovery_repo.find(conn, acct_msg.getOptions().get("discovery_key").toString());
					discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
			  		discovery_record.setLastPathRanAt(new Date());
					discovery_repo.save(conn, discovery_record);
					
					for(Action action : exploratory_path.getPossibleActions()){
						Path path = Path.clone(exploratory_path);
						path.add(action);
						final long pathCrawlStartTime = System.currentTimeMillis();
						int tries = 0;
						do{
							try{
								result_page = Crawler.crawlPath(path, browser);
							}catch(NullPointerException e){
								browser = new Browser(browser.getBrowserName());
								log.error("Error happened while exploratory actor attempted to crawl path");
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
								log.error("Error happened while exploratory actor attempted to crawl path");
							}
							
							tries++;
						}while(tries < 5);
						
						final long pathCrawlEndTime = System.currentTimeMillis();

						long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
					
						if(ExploratoryPath.hasCycle(path, result_page)){
					  		path.setIsUseful(false);
					  		continue;
					  	}
					  	else{
					  		path.setIsUseful(true);
					  		boolean results_match = false;
					  		//crawl path and get result
					  		//if this result is the same as the result achieved by the original path then replace the original path with this new path
					  		
					  		do{
					  			Path parent_path = buildParentPath(path, browser.getDriver());
					  			if(parent_path == null){
					  				break;
					  			}
					  			System.err.println("parent path length @@@@@@@@   "+parent_path);
					  			Browser new_browser = new Browser(browser.getBrowserName());
					  			System.err.println("Retrieved new browser");
					  			results_match = doesPathProduceExpectedResult(parent_path, result_page, new_browser);
					  			new_browser.close();
					  			
					  			if(results_match){
					  				path = parent_path;
					  			}
					  		}while(results_match);
					  		
					  		Domain domain = domain_repo.find(conn, domain_url);
							domain.setTestCount(domain.getTestCount()+1);
							domain_repo.save(conn, domain);
							
					  		createTest(path, result_page, pathCrawlRunTime, domain, acct_msg, discovery_record);
							MessageBroadcaster.broadcastDiscoveryStatus(domain.getUrl(), discovery_record);

					  		Path new_path = Path.clone(path);
							new_path.add(result_page);
							Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), new_path, acct_msg.getOptions());

							final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
							path_expansion_actor.tell(path_msg, getSelf());
					  	}
						
						if(path.isUseful()){
							break;
						}
					}
				  	browser.close();
				  	conn.close();
				}

				
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);

		}else unhandled(message);
	}
	
	/**
	 * Generates {@link Test Tests} for path
	 * @param path
	 * @param result_page
	 */
	private void createTest(Path path, Page result_page, long crawl_time, Domain domain, Message<?> acct_msg, DiscoveryRecord discovery ) {
		path.setIsUseful(true);
		Test test = new Test(path, result_page, domain, "Test #" + domain.getTestCount());							
		TestRepository test_repo = new TestRepository();
		test.setKey(test_repo.generateKey(test));
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), null, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
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
		for(PathObject path_obj: test.getPath().getPath()){
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
	 * Checks if the result of crawling a given {@link Path path} results in an identical {@link Page page state} to the 
	 *   result_page that is passed in.
	 * 
	 * @param path
	 * @param result_page
	 * @param browser
	 * @return
	 * 
	 * @throws NoSuchElementException
	 * @throws IOException
	 */
	private boolean doesPathProduceExpectedResult(Path path, Page result_page, Browser browser) throws NoSuchElementException, IOException{
		System.err.println("attempting to crawl path with length #########   "+path.size());
		Page parent_result = Crawler.crawlPath(path, browser);
		return parent_result.equals(result_page);
	}
	
	/**
	 * Takes in a {@link Path path} and {@link WebDriver driver} and builds a new path such that
	 *  the last {@link PageElement element} is replaced with it's parent element from the html document controlled by the 
	 *  given {@link WebDriver driver}
	 *  
	 * @param path
	 * @param driver
	 * @return
	 */
	private Path buildParentPath(Path path, WebDriver driver){
		PageElement elem = null;
		int element_idx = -1;
		for(int idx = path.size()-1; idx >= 0; idx--){
			if(path.getPath().get(idx).getType().equals("PageElement")){
				elem = (PageElement)path.getPath().get(idx);
				element_idx = idx;
				break;
			}
		}
		
		if(elem != null && element_idx > -1){
			//get parent of element
			WebElement web_elem = driver.findElement(By.xpath(elem.getXpath()));
			WebElement parent = Browser.getParentElement(web_elem);
			
			//clone path and swap page element with parent
			Path parent_path = Path.clone(path);
			String this_xpath = Browser.generateXpath(parent, "", new HashMap<String, Integer>(), driver); 
			
			PageElement parent_tag = new PageElement(parent.getText(), this_xpath, parent.getTagName(), Browser.extractedAttributes(parent, (JavascriptExecutor)driver), PageElement.loadCssProperties(parent) );
			parent_path.getPath().set(element_idx, parent_tag);
			
			return parent_path;
		}
		return null;
	}
}
