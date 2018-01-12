package com.minion.actors;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.Page;
import com.qanairy.models.Path;

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

			  	Browser browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), (String)acct_msg.getOptions().get("browser"));

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
					resulting_page.setLandable(resulting_page.checkIfLandable(acct_msg.getOptions().get("browser").toString()));
				}catch(Exception e){
					log.error(e.getMessage());
					resulting_page.setLandable(false);
				}
				if(!resulting_page.equals(expected_page)){
					TestRecord record = new TestRecord(new Date(), false, resulting_page);
					record.setRunTime(pathCrawlRunTime);
					test.addRecord(record);

					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
					//tell memory worker of path
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					memory_actor.tell(test_msg, getSelf() );
				}
				else{
					TestRecord record = null;
					if(!test.isCorrect()){
						record = new TestRecord(new Date(), false, resulting_page);
					}
					else{
						record = new TestRecord(new Date(), true);
					}

					test.addRecord(record);
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());

					//tell memory worker of test record
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					memory_actor.tell(test_msg, getSelf() );

					//PastPathExperienceController.broadcastTestExperience(test);
				}

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
			page = Crawler.crawlPath(test.getPath(), browser);
			passing = test.isTestPassing(page, test.isCorrect());
			test_record = new TestRecord(new Date(), passing, page);
			
			Capabilities cap = ((RemoteWebDriver) browser.getDriver()).getCapabilities();
			    String browserName = cap.getBrowserName().toLowerCase();
			    String os = cap.getPlatform().toString();
			    String v = cap.getVersion().toString();
			    
		    test.setBrowserStatus(browserName, passing);
			    
		 } catch (IOException e) {		
			 log.error(e.getMessage());		
		 }	
		
		 final long pathCrawlEndTime = System.currentTimeMillis();

		 long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime ;

		 test_record.setRunTime(pathCrawlRunTime);

		 return test_record;		
	 }
}
