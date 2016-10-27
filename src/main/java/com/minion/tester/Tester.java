package com.minion.tester;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.actors.Crawler;
import com.minion.browsing.Browser;
import com.minion.browsing.Page;

public class Tester {
	private static final Logger log = LoggerFactory.getLogger(Tester.class);

	/**
	 * 
	 * @param test test to be ran
	 * 
	 * @pre test != null
	 * @return
	 */
	public TestRecord runTest(Test test){		
		assert test != null;
		
		log.info("Running test...");
		Browser browser;
		try {
			browser = new Browser(((Page)test.getPath().getPath().get(0)).getUrl().toString());
			log.info("crawling path");
			Crawler.crawlPath(test.getPath());
			browser.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TestRecord test_record = new TestRecord(test, new Date(), checkIfTestPasses(test));
		return null;
	}
	
	public boolean checkIfTestPasses(Test test){
		
		return null;
	}
}
