package com.minion.browsing;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.qanairy.models.Action;
import com.qanairy.models.Page;
import com.qanairy.models.PageAlert;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;

/**
 * Provides methods for crawling webpages using selenium
 */
public class Crawler {
	private static Logger log = Logger.getLogger(Crawler.class);

	/**
	 * Crawls the path for the current BrowserActor.
	 * 
	 * @return
	 * @throws java.util.NoSuchElementException
	 * @throws UnhandledAlertException
	 * @throws IOException 
	 */
	public static Page crawlPath(Path path, Browser browser) throws java.util.NoSuchElementException, IOException{
		assert path != null;
		
		PageElement last_element = null;
		
		//skip first node since we should have already loaded it during initialization
	  	log.info("crawling path...");
		for(PathObject current_obj: path.getPath()){
			if(current_obj instanceof Page){
				log.info("Current path node is a Page");
			}
			else if(current_obj instanceof PageElement){
				log.info("Current path node is a WebElement");
				last_element = (PageElement) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				boolean actionPerformedSuccessfully;
				Action action = (Action)current_obj;
				
				log.info("Current path node is an Action : "+action.getName());
				log.info(" :: With driver : "+browser.getDriver());
				int attempts = 0;
				do{
					//actionPerformedSuccessfully = performAction(last_element, action.getName(), browser.getDriver() );
					actionPerformedSuccessfully = last_element.performAction(action, "String should be entered here", browser.getDriver());
					attempts++;
				}while(!actionPerformedSuccessfully && attempts < 50);
			}
			else if(current_obj instanceof PageAlert){
				log.info("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
		}
		
	  	log.info("Path crawl completed");
	  	Page page = browser.getPage();
	  	return page;
	}

	/**
	 * Crawls the path for the current BrowserActor.
	 * 
	 * @return
	 * @throws java.util.NoSuchElementException
	 * @throws UnhandledAlertException
	 * @throws IOException 
	 */
	public static Page crawlPath(Path path, Browser browser, Action final_action) throws java.util.NoSuchElementException, IOException{
		assert path != null;
		log.info("PATH :: "+path);

		PageElement last_element = null;
		
		//skip first node since we should have already loaded it during initialization
	  	log.info("crawling path...");
		for(PathObject current_obj: path.getPath()){
			if(current_obj instanceof Page){
				log.info("Current path node is a Page");
			}
			else if(current_obj instanceof PageElement){
				log.info("Current path node is a WebElement");
				last_element = (PageElement) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				log.info("Current path node is an Action");
				boolean actionPerformedSuccessfully;
				Action action = (Action)current_obj;
				//browser.updatePage( DateFormat.getDateInstance());
				int attempts = 0;
				do{
					//actionPerformedSuccessfully = performAction(last_element, action.getName(), browser.getDriver() );
					actionPerformedSuccessfully = last_element.performAction(action, "String should be entered here", browser.getDriver());
					attempts++;
				}while(!actionPerformedSuccessfully && attempts < 50);
			}
			else if(current_obj instanceof PageAlert){
				log.info("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
		}

		last_element.performAction(final_action, browser.getDriver());
	  	log.info("Path crawl completed");
	  	Page page = browser.getPage();
	  	return page;
	}
}
