package com.minion.actors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.minion.api.PastPathExperienceController;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.Page;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;

/**
 * Handles retrieving tests
 *
 */
public class TestingActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(TestingActor.class);

    /**
     * Inputs
     * 
     * URL url: 	Get all tests for this url
     * Test:		Execute test and determine if still correct or not
     * List<Test>   Execute list of tests and get the outcomes for all of them
     */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			if(acct_msg.getData() instanceof Test){
				Test test = (Test)acct_msg.getData();
				Path path = test.getPath();

				final long pathCrawlStartTime = System.currentTimeMillis();

			  	Browser browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), "phantomjs");

				Page resulting_page = null;
				if(path.getPath() != null){
					int cnt = 0;
					while(browser == null && cnt < 5){
						try{
							resulting_page = Crawler.crawlPath(path, browser );
							break;
						}catch(NullPointerException e){
							log.error(e.getMessage());
						}
						cnt++;
					}
				}
				final long pathCrawlEndTime = System.currentTimeMillis();

				long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime ;
				//log.info("Path crawl time :: "+pathCrawlRunTime);

				test.setRunTime(pathCrawlRunTime);

				//get current page of browser
				Page expected_page = test.getResult();
				//Page last_page = path.findLastPage();
				
				try{
					resulting_page.setLandable(resulting_page.checkIfLandable());
				}catch(Exception e){
					log.error(e.getMessage());
					resulting_page.setLandable(false);
				}
				if(!resulting_page.equals(expected_page)){
					//log.info("Saving test, cuz it has changed");
					
					//Test test_new = new Test(path, expected_page, expected_page.getUrl().getHost());
					TestRecord record = new TestRecord(new Date(), false, resulting_page);
					record.setRunTime(pathCrawlRunTime);
					test.addRecord(record);
					
					//log.info("Test Actor -> Sending test record to be saved");
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
					//tell memory worker of path
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					memory_actor.tell(test_msg, getSelf() );
				}
				else{
					//log.info("Saving unchanged test");
					
					TestRecord record = null;
					if(!test.isCorrect()){
						record = new TestRecord(new Date(), false, resulting_page);
					}
					else{
						record = new TestRecord(new Date(), true);
					}

					test.addRecord(record);
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);

					//tell memory worker of test record
					//log.info("Test Actor -> Sending test record to be saved");

					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					memory_actor.tell(test_msg, getSelf() );

					//PastPathExperienceController.broadcastTestExperience(test);
				}

				//broadcast path
			  	browser.close();
			}
			else{
				log.warn("ERROR : Message contains unknown format");
			}
		}
		else{
			log.warn("ERROR : Did not receive a Message object");
		}
	}

	/**		
	 * Runs an {@code Test} 		
	 * 		
	 * @param test test to be ran		
	 * 		
	 * @pre test != null		
	 * @return	{@link TestRecord} indicating passing status and {@link Page} if not passing 
	 */		
	 public static TestRecord runTest(Test test, Browser browser){				
		 assert test != null;		
	 			
		 Boolean passing = false;		
		 Page page = null;
		 TestRecord test_record = null;
		 final long pathCrawlStartTime = System.currentTimeMillis();

		 try {		
		
			int cnt = 0;
			while(browser == null && cnt < 5){
				try{
					page = Crawler.crawlPath(test.getPath(), browser);
					break;
				}catch(NullPointerException e){
					log.error(e.getMessage());
				}
				cnt++;
			 }
			 passing = test.isTestPassing(page, test.isCorrect());
			 
			 Capabilities cap = ((RemoteWebDriver) browser.getDriver()).getCapabilities();
			    String browserName = cap.getBrowserName().toLowerCase();
			    //log.info(browserName);
			    String os = cap.getPlatform().toString();
			    //log.info(os);
			    String v = cap.getVersion().toString();
			    //log.info(v);
			    
		    test.setBrowserStatus(browserName, passing);
			    
			 test_record = new TestRecord(new Date(), passing, page);
		 } catch (IOException e) {		
			 e.printStackTrace();		
		 }	
		
		 final long pathCrawlEndTime = System.currentTimeMillis();

		 long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime ;

		 test_record.setRunTime(pathCrawlRunTime);

		 return test_record;		
	 }
}
