package actors;

import java.io.IOException;
import java.text.DateFormat;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;

import browsing.ActionFactory;
import browsing.Browser;
import browsing.Page;
import browsing.PageAlert;
import browsing.PageElement;
import browsing.PathObject;
import browsing.actions.Action;
import structs.Path;

/**
 * Provides methods for crawling webpages using selenium
 * 
 * @author brandon kindred
 */
public class Crawler {
    private static final Logger log = Logger.getLogger(Crawler.class);

	/**
	 * Crawls the path for the current BrowserActor.
	 * 
	 * @return
	 * @throws java.util.NoSuchElementException
	 * @throws UnhandledAlertException
	 * @throws IOException 
	 */
	public static void crawlPath(Path path, Browser browser) throws java.util.NoSuchElementException, UnhandledAlertException, IOException{
		PageElement last_element = null;
		
		//skip first node since we should have already loaded it during initialization
	
		for(PathObject browser_obj: path.getPath()){
			if(browser_obj.getData() instanceof Page){
				//pageNode = (Page)browser_obj.getData();
			}
			else if(browser_obj.getData() instanceof PageElement){
				last_element = (PageElement) browser_obj.getData();
			}
			//String is action in this context
			else if(browser_obj.getData() instanceof Action){
				boolean actionPerformedSuccessfully;
				Action action = (Action)browser_obj.getData();
				browser.updatePage( DateFormat.getDateInstance());
				int attempts = 0;
				do{
					actionPerformedSuccessfully = performAction(last_element, action.getName(), browser );
					attempts++;
				}while(!actionPerformedSuccessfully && attempts < 20);
			}
			else if(browser_obj.getData() instanceof PageAlert){
				PageAlert alert = (PageAlert)browser_obj.getData();
				alert.performChoice(browser.getDriver());
			}
		}
	}
	
	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element 
	 * 
	 * @param elemAction ElementAction pair
	 * @return whether action was able to be performed on element or not
	 */
	private static boolean performAction(PageElement elem, String action, Browser browser) throws UnreachableBrowserException {
		ActionFactory actionFactory = new ActionFactory(browser.getDriver());
		boolean wasPerformedSuccessfully = true;
		
		try{
			WebElement element = browser.getDriver().findElement(By.xpath(elem.getXpath()));
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
