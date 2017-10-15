package com.minion.actors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private static Logger log = LogManager.getLogger(TestingActor.class);

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
			  	Browser browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), "phantomjs");

				Page resulting_page = null;
				if(path.getPath() != null){
					resulting_page = Crawler.crawlPath(path, browser );
				}
				
				//get current page of browser
				Page expected_page = test.getResult();
				//Page last_page = path.findLastPage();
				
				resulting_page.setLandable(resulting_page.checkIfLandable());
				
				if(!resulting_page.equals(expected_page)){
					System.out.println("Saving test, cuz it has changed");
					
					//Test test_new = new Test(path, expected_page, expected_page.getUrl().getHost());
					TestRecord record = new TestRecord(new Date(), false, resulting_page, test);
					test.addRecord(record);
					
					System.out.println("Test Actor -> Sending test record to be saved");
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
					//tell memory worker of path
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					memory_actor.tell(test_msg, getSelf() );
				}
				else{
					System.out.println("Saving unchanged test");
					
					TestRecord record = null;
					if(!test.isCorrect()){
						record = new TestRecord(new Date(), false, resulting_page, test);
					}
					else{
						record = new TestRecord(new Date(), true);
					}

					test.addRecord(record);
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);

					//tell memory worker of test record
					System.out.println("Test Actor -> Sending test record to be saved");

					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					memory_actor.tell(test_msg, getSelf() );

					PastPathExperienceController.broadcastTestExperience(test);
				}

				//broadcast path
			  	browser.close();
			}
			else{
				System.out.println("ERROR : Message contains unknown format");
			}
		}
		else{
			System.out.println("ERROR : Did not receive a Message object");
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
	 			
		 System.out.println("Running test...");		
		 boolean passing = false;		
		 Page page = null;
		 TestRecord test_record = null;
		 try {		
			 page = Crawler.crawlPath(test.getPath(), browser);	
			 passing = test.isTestPassing(page);
			 
			 Capabilities cap = ((RemoteWebDriver) browser.getDriver()).getCapabilities();
			    String browserName = cap.getBrowserName().toLowerCase();
			    System.out.println(browserName);
			    String os = cap.getPlatform().toString();
			    System.out.println(os);
			    String v = cap.getVersion().toString();
			    System.out.println(v);
			    
		    test.setBrowserStatus(browserName, passing);
			    
		    
			 /*if(passing){
				 System.out.println("Test is passing");
				 test_record = new TestRecord(new Date(), passing, page, test);	 
			 }
			 else{*/
				 System.out.println("Test status :: "+passing);
				 test_record = new TestRecord(new Date(), passing, page, test );
			 //}
		 } catch (IOException e) {		
			 e.printStackTrace();		
		 }	
		
		 return test_record;		
	 }
}
