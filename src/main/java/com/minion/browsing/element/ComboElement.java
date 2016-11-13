package com.minion.browsing.element;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.ActionFactory;
import com.minion.browsing.PageElement;
import com.minion.browsing.actions.Action;

/**
 * Represents a container with an input field as well as label
 */
public class ComboElement {
    private static final Logger log = LoggerFactory.getLogger(ComboElement.class);

	private List<PageElement> elements;
	
	/**
	 * Constructs new InputContiner without a label
	 * 
	 * @param screenshot_url
	 * @param inputs
	 * 
	 * @pre inputs != null
	 * @pre inputs.size() > 0;
	 */
	public ComboElement(List<PageElement> elements){
		assert elements != null;
		assert elements.size() > 0;
		
		this.setElements(elements);
	}

	public List<PageElement> getElements() {
		return elements;
	}

	public void setElements(List<PageElement> elements) {
		this.elements = elements;
	}

	public boolean performAction(Action action, String value, WebDriver driver) throws UnreachableBrowserException {
		ActionFactory actionFactory = new ActionFactory(driver);
		boolean wasPerformedSuccessfully = true;
		
		for(PageElement elem : this.getElements()){
			try{
				WebElement element = driver.findElement(By.xpath(elem.getXpath()));
				actionFactory.execAction(element, value, action.getName());
				
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
		}
		
		
		return wasPerformedSuccessfully;
	}
}
