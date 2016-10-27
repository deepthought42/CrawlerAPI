package com.minion.actors;

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

import com.minion.browsing.ActionFactory;
import com.minion.browsing.Browser;
import com.minion.browsing.Page;
import com.minion.browsing.PageAlert;
import com.minion.browsing.PageElement;
import com.minion.browsing.PathObject;
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
	public static void crawlPath(Path path) throws java.util.NoSuchElementException, UnhandledAlertException, IOException{
		assert path != null;
		
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
					actionPerformedSuccessfully = performAction(last_element, action.getName(), browser.getDriver() );
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

	}
	
	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element 
	 * 
	 * @param elemAction ElementAction pair
	 * @return whether action was able to be performed on element or not
	 */
	private static boolean performAction(PageElement elem, String action, WebDriver driver) throws UnreachableBrowserException {
		ActionFactory actionFactory = new ActionFactory(driver);
		boolean wasPerformedSuccessfully = true;
		
		try{
			WebElement element = driver.findElement(By.xpath(elem.getXpath()));
			actionFactory.execAction(element, action);
			
			log.info("CRAWLER Performed action "+ action
					+ " On element with xpath :: "+elem.getXpath());
		}
		catch(StaleElementReferenceException e){
			
			 log.info("STALE ELEMENT REFERENCE EXCEPTION OCCURRED WHILE ACTOR WAS PERFORMING ACTION : "
					+ action + ". ");
			wasPerformedSuccessfully = false;			
		}
		catch(ElementNotVisibleException e){
			log.info("ELEMENT IS NOT CURRENTLY VISIBLE.");
		}
		catch(NoSuchElementException e){
			log.info(" NO SUCH ELEMENT EXCEPTION WHILE PERFORMING "+action);
			wasPerformedSuccessfully = false;
		}
		catch(WebDriverException e){
			log.info("Element can not have action performed on it at point performed");
			wasPerformedSuccessfully = false;
		}
		
		return wasPerformedSuccessfully;
	}
}
