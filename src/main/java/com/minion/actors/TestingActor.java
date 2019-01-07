package com.minion.actors;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.minion.util.Timing;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.services.TestService;

import akka.actor.AbstractActor;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * Handles retrieving tests
 *
 */
@Component
@Scope("prototype")
public class TestingActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(TestingActor.class);

	@Autowired
	private Crawler crawler;
	
	@Autowired
	private TestService test_service;
	
	
    /**
     * Inputs
     * 
     * URL url: 	Get all tests for this url
     * Test:		Execute test and determine if still correct or not
     * List<Test>   Execute list of tests and get the outcomes for all of them
     */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
					if(message.getData() instanceof Test){
						Test test = (Test)message.getData();
		
						final long pathCrawlStartTime = System.currentTimeMillis();
		
					  	Browser browser = new Browser((String)message.getOptions().get("browser"));
		
						PageState resulting_page = null;
						if(test.getPathKeys() != null){
							int cnt = 0;
							while(browser == null && cnt < Integer.MAX_VALUE){
								try{
									browser = new Browser((String)message.getOptions().get("browser"));
									resulting_page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, message.getOptions().get("host").toString());
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
								
						if(!resulting_page.equals(expected_page)){
							TestRecord record = new TestRecord(new Date(), TestStatus.FAILING, browser.getBrowserName(), resulting_page, pathCrawlRunTime);
							record.setRunTime(pathCrawlRunTime);
							test.addRecord(record);
						}
						else{
							TestRecord record = null;
		
							if(test.getStatus().equals(TestStatus.FAILING)){
								record = new TestRecord(new Date(), TestStatus.FAILING, browser.getBrowserName(), resulting_page, pathCrawlRunTime);
							}
							else{
								record = new TestRecord(new Date(), TestStatus.PASSING, browser.getBrowserName(), resulting_page, pathCrawlRunTime);
							}
		
							test.addRecord(record);
						}
		
					  	test_service.save(test, message.getOptions().get("host").toString());
						browser.close();
					}
					else{
						log.warn("ERROR : Message contains unknown format");
					}
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
}
