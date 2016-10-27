package com.minion.tester;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.actors.Crawler;
import com.minion.browsing.Page;

public class Tester {
	private static final Logger log = LoggerFactory.getLogger(Tester.class);

	/**
	 * Runs an {@code Test} 
	 * 
	 * @param test test to be ran
	 * 
	 * @pre test != null
	 * @return
	 */
	public static TestRecord runTest(Test test){		
		assert test != null;
		
		log.info("Running test...");
		boolean passing = false;
		try {
			Page page = Crawler.crawlPath(test.getPath());
			passing = test.isTestPassing(page);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TestRecord test_record = new TestRecord(test, new Date(), passing );
		return test_record;
	}
}
