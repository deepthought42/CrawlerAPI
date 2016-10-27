package com.minion.tester;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.actors.Crawler;
import com.minion.actors.MemoryRegistryActor;
import com.minion.actors.PathExpansionActor;
import com.minion.browsing.Browser;
import com.minion.browsing.Page;
import com.minion.structs.Message;
import com.minion.structs.Path;

import akka.actor.ActorRef;
import akka.actor.Props;

public class Tester {
	private static final Logger log = LoggerFactory.getLogger(Tester.class);

	public TestRecord runTest(Test test){
		log.info("PATH PASSED TO BROWSER ACTOR");
		Path path = test.getPath();
		
		log.info("Creating new Browser");
		Browser browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString());
		if(path.getPath() != null){
			log.info("crawling path");
			Crawler.crawlPath(path, browser);
		}
		 
		//get current page of browser
		Page current_page = null;
		
		log.info("Getting last page");
		Page last_page = path.findLastPage();
		last_page.setLandable(last_page.checkIfLandable());
		
		log.info("Checking equality of page sources " + last_page.equals(current_page));
		if(last_page.isLandable()){
	  		log.info("Page sources match(Path Message)");
	  		current_page = last_page;
	  		path.setIsUseful(false);
	  	}
	  	else{
	  		log.info("Page sources don't match(Path Message)");
	  		current_page = browser.getPage();
	  		
	  		log.info("PAGES ARE DIFFERENT, PATH IS VALUABLE (Path Message)");
			path.setIsUseful(true);
			if(path.size() > 1){
				path.add(current_page);
			}

			final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
			path_expansion_actor.tell(path_msg, getSelf() );
	  	}
		
    	this.browser.close();
		
		// IF PAGES ARE DIFFERENT THEN DEFINE NEW TEST THAT HAS PATH WITH PAGE
		// 	ELSE DEFINE NEW TEST THAT HAS PATH WITH NULL PAGE
		log.info("Sending test to Memory Actor");
		Test test = new Test(path, current_page, current_page.getUrl());
		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);

		//add test to sequences for session
		//SessionTestTracker seqTracker = SessionTestTracker.getInstance();
		//TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
		//testMap.addTest(test);
		
		//tell memory worker of path
		final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
		
		//tell memory worker of path
		memory_actor.tell(test_msg, getSelf() );
	}
}
