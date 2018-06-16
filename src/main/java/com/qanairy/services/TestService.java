package com.qanairy.services;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;

@Component
@Scope("prototype")
public class TestService {
	private static Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	private Crawler crawler;
	
	/**		
	 * Runs an {@code Test} 		
	 * 		
	 * @param test test to be ran		
	 * 		
	 * @pre test != null		
	 * @return	{@link TestRecord} indicating passing status and {@link Page} if not passing 
	 */		
	 public TestRecord runTest(Test test, Browser browser){				
		 assert test != null;		
	 			
		 Boolean passing = null;		
		 PageState page = null;
		 TestRecord test_record = null;
		 final long pathCrawlStartTime = System.currentTimeMillis();

		 try {
			page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, null);
			
			System.err.println("IS TEST CURRENTLY PASSING ??    "+test.getCorrect());
			passing = Test.isTestPassing(test.getResult(), page, test.getCorrect());
			
		    test.setBrowserStatus(browser.getBrowserName(), passing);
		 } catch (IOException e) {		
			 log.error(e.getMessage());		
		 }	
		
		 final long pathCrawlEndTime = System.currentTimeMillis();

		 long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime ;
		 test_record = new TestRecord(new Date(), passing, browser.getBrowserName(), page, pathCrawlRunTime);

		 return test_record;		
	 }
}
