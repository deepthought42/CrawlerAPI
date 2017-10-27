package com.minion.browsing;

import java.io.IOException;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

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
	private static Logger log = LoggerFactory.getLogger(Crawler.class);

	/**
	 * Crawls the path using the provided {@link Browser browser}
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
		for(PathObject current_obj: path.getPath()){

			if(current_obj instanceof Page){
				try{
					new WebDriverWait(browser.getDriver(), 360).until(
							webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
				}catch(GridException e){
					log.warn(e.getMessage());
				}
			}
			else if(current_obj instanceof PageElement){
				last_element = (PageElement) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				boolean actionPerformedSuccessfully;
				Action action = (Action)current_obj;
				int attempts = 0;
				do{
					actionPerformedSuccessfully = last_element.performAction(action, "String should be entered here", browser.getDriver());
					attempts++;
				}while(!actionPerformedSuccessfully && attempts < 50);
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
		}

		return browser.getPage();
	}

	/**
	 * Crawls the {@link Path path} using the given {@link Browser browser}
	 *
	 * @return
	 * @throws java.util.NoSuchElementException
	 * @throws UnhandledAlertException
	 * @throws IOException
	 */
	public static Page crawlPath(Path path, Browser browser, Action final_action) throws java.util.NoSuchElementException, IOException{
		assert path != null;

		PageElement last_element = null;

		//skip first node since we should have already loaded it during initialization
		for(PathObject current_obj: path.getPath()){
			if(current_obj instanceof Page){
				log.debug("Current path node is a Page");
			}
			else if(current_obj instanceof PageElement){
				log.debug("Current path node is a WebElement");
				last_element = (PageElement) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				log.debug("Current path node is an Action");
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
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
		}

		last_element.performAction(final_action, browser.getDriver());
	  	Page page = browser.getPage();
	  	return page;
	}
}
