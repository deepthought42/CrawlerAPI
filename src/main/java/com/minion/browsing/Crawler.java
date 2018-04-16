package com.minion.browsing;

import java.io.IOException;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
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
 * Provides methods for crawling web pages using Selenium
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
				if(browser ==  null){
					log.error("BROWSER IS NULL WHEN CRAWLING PATH");
				}
				try{
					new WebDriverWait(browser.getDriver(), 360).until(
							webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
				}catch(GridException e){
					log.error(e.getMessage());
				}
				catch(Exception e){
					log.error(e.getMessage());
				}
			}
			else if(current_obj instanceof PageElement){
				last_element = (PageElement) current_obj;
			}
			//String is action in this context
			else if(current_obj instanceof Action){
				//boolean actionPerformedSuccessfully;
				Action action = (Action)current_obj;
				int attempts = 0;
				//do{
					boolean actionPerformedSuccessfully = last_element.performAction(action, browser.getDriver());
					//attempts++;
				//}while(!actionPerformedSuccessfully && attempts < 5);
			}
			else if(current_obj instanceof PageAlert){
				log.debug("Current path node is a PageAlert");
				PageAlert alert = (PageAlert)current_obj;
				alert.performChoice(browser.getDriver());
			}
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {}
		
		return browser.getPage();
	}
}
