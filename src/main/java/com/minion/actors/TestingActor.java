package com.minion.actors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.services.TestService;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
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
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private Crawler crawler;
	
	@Autowired
	private TestService test_service;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
	  cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), 
	      MemberEvent.class, UnreachableMember.class);
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
	  cluster.unsubscribe(getSelf());
    }
		
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
					  	Browser browser = null;
						PageState resulting_page = null;
						Map<Integer, ElementState> visible_element_map = new HashMap<>();
						List<ElementState> visible_elements = new ArrayList<>();
						
						if(test.getPathKeys() != null){
							int cnt = 0;
							int last_idx = 0;
							while(browser == null && cnt < Integer.MAX_VALUE){
								try{
									browser = BrowserConnectionFactory.getConnection((String)message.getOptions().get("browser"), BrowserEnvironment.TEST);
									resulting_page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, message.getOptions().get("host").toString(), visible_element_map, visible_elements);
								}catch(NullPointerException e){
									log.error(e.getMessage());
								}
								finally{
									if(browser != null){
										browser.close();
									}
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
					}
					else{
						log.warn("ERROR : Message contains unknown format");
					}
					postStop();

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
