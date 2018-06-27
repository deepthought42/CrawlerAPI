package com.minion.actors;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.TestStatus;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestService;

/**
 * Handles retrieving tests
 *
 */
@Component
@Scope("prototype")
public class TestingActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(TestingActor.class);

	@Autowired
	private Crawler crawler;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private BrowserService browser_service;
	
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

				final long pathCrawlStartTime = System.currentTimeMillis();

			  	Browser browser = new Browser((String)acct_msg.getOptions().get("browser"));

				PageState resulting_page = null;
				if(test.getPathKeys() != null){
					int cnt = 0;
					while(browser == null && cnt < 5){
						try{
							resulting_page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, acct_msg.getOptions().get("host").toString());
							break;
						}catch(NullPointerException e){
							log.error(e.getMessage());
						}
						cnt++;
					}
				}
				final long pathCrawlEndTime = System.currentTimeMillis();
				long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime ;
				test.setRunTime(pathCrawlRunTime);
				//get current page of browser
				PageState expected_page = test.getResult();
				
				int tries=0;
				do{
					try{
						resulting_page.setLandable(browser_service.checkIfLandable(acct_msg.getOptions().get("browser").toString(), resulting_page));
						break;
					}catch(Exception e){
						log.error(e.getMessage());
						resulting_page.setLandable(false);
					}
				}while(tries < 5);

				if(!resulting_page.equals(expected_page)){
					TestRecord record = new TestRecord(new Date(), TestStatus.FAILING, browser.getBrowserName(), resulting_page, pathCrawlRunTime);
					record.setRunTime(pathCrawlRunTime);
					test.addRecord(record);
				}
				else{
					TestRecord record = null;

					if(test.getCorrect().equals(TestStatus.FAILING)){
						record = new TestRecord(new Date(), TestStatus.FAILING, browser.getBrowserName(), resulting_page, pathCrawlRunTime);
					}
					else{
						record = new TestRecord(new Date(), TestStatus.PASSING, browser.getBrowserName(), resulting_page, pathCrawlRunTime);
					}

					test.addRecord(record);
				}
				
				//tell memory worker of test record
				//Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
				//final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
				//memory_actor.tell(test_msg, getSelf() );
			  	test_service.save(test, acct_msg.getOptions().get("host").toString());
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

	
}
