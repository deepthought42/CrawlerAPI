package com.minion.browsing;

import java.io.IOException;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.actions.Action;
import com.minion.structs.Path;

/**
 * Provides methods for crawling webpages using selenium
 * 
 * @author brandon kindred
 */
public class Crawler {
    private static final Logger log = LoggerFactory.getLogger(Crawler.class);

	/**
	 * Crawls the path for the current BrowserActor.
	 * 
	 * @return
	 * @throws java.util.NoSuchElementException
	 * @throws UnhandledAlertException
	 * @throws IOException 
	 */
	public static Page crawlPath(Path path) throws java.util.NoSuchElementException, UnhandledAlertException, IOException{
		assert path != null;
		log.info("PATH :: "+path);
		
		Browser browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString());
		
		PageElement last_element = null;
		
		//skip first node since we should have already loaded it during initialization
	  	log.info("crawling path...");
		for(PathObject<?> current_obj: path.getPath()){
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
		
	  	log.info("Path crawl completed");
	  	Page page = browser.getPage();
	  	browser.close();
	  	return page;
	}

}
