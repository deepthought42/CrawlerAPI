package com.minion.browsing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.qanairy.models.PageAlert;
import com.qanairy.persistence.Action;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;

/**
 * Provides methods for crawling web pages using Selenium
 */
public class Crawler {
	private static Logger log = LoggerFactory.getLogger(Crawler.class);

	/**
	 * Crawls the path using the provided {@link Browser browser}
	 * 
	 * @param path list of vertex keys
	 * @param browser
	 * @return {@link Page result_page} state that resulted from crawling path
	 * 
	 * @throws java.util.NoSuchElementException
	 * @throws IOException
	 * 
	 * @pre path != null
	 * @pre path != null
	 */
	public static PageState crawlPath(List<String> path, List<? extends PathObject> path_objects, Browser browser) throws NoSuchElementException, IOException{
		assert browser != null;
		assert path != null;

		List<PathObject> ordered_path_objects = new ArrayList<PathObject>();
		//Ensure Order path objects
		for(String path_obj_key : path){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(path_obj_key)){
					ordered_path_objects.add(obj);
				}
			}
		}
		
		PageElement last_element = null;

		
		browser.getDriver().get(((PageState)ordered_path_objects.get(0)).getUrl().toString());
		try{
			new WebDriverWait(browser.getDriver(), 360).until(
					webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
		}catch(GridException e){
			log.error(e.getMessage());
		}
		catch(Exception e){
			log.error(e.getMessage());
		}

		//skip first node since we should have already loaded it during initialization
		for(PathObject current_obj: ordered_path_objects){

			if(current_obj instanceof PageState){
				//Do Nothing for now
			}
			else if(current_obj instanceof PageElement){
				last_element = (PageElement) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				//boolean actionPerformedSuccessfully;
				Action action = (Action)current_obj;
				boolean actionPerformedSuccessfully = performAction(action, last_element, browser.getDriver());
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
		}
		
		return browser.buildPage();
	}
	
	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element 
	 * 
	 * @param elemAction ElementAction pair
	 * @return whether action was able to be performed on element or not
	 */
	public static boolean performAction(Action action, PageElement elem, WebDriver driver){
		ActionFactory actionFactory = new ActionFactory(driver);
		boolean wasPerformedSuccessfully = true;
		
		try{
			WebElement element = driver.findElement(By.xpath(elem.getXpath()));
			actionFactory.execAction(element, action.getValue(), action.getName());
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {}
		}
		catch(StaleElementReferenceException e){
			
			log.warn("STALE ELEMENT REFERENCE EXCEPTION OCCURRED WHILE ACTOR WAS PERFORMING ACTION : "
					+ action + ". ", e.getMessage());
			wasPerformedSuccessfully = false;			
		}
		catch(ElementNotVisibleException e){
			log.warn("ELEMENT IS NOT CURRENTLY VISIBLE.", e.getMessage());
		}
		catch(WebDriverException e){
			log.warn("Element can not have action performed on it at point performed", e.getMessage());
			wasPerformedSuccessfully = false;
		}
		
		return wasPerformedSuccessfully;
	}
}
