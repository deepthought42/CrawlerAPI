package com.minion.browsing.element;

import java.util.List;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import com.minion.browsing.ActionFactory;
import com.minion.browsing.form.FormField;
import com.qanairy.models.Action;

/**
 * Represents a container with an input field as well as label
 */
public class ComplexField {
	private static Logger log = LoggerFactory.getLogger(ComplexField.class);

	private List<FormField> elements;
	
	/**
	 * Constructs new InputContiner without a label
	 * 
	 * @param elements
	 * 
	 * @pre elements != null
	 * @pre elements.size() > 0;
	 */
	public ComplexField(List<FormField> fields){
		assert fields != null;
		assert fields.size() > 0;
		
		this.setElements(fields);
	}

	public List<FormField> getElements() {
		return elements;
	}

	public void setElements(List<FormField> elements) {
		this.elements = elements;
	}

	public boolean performAction(Action action, String value, WebDriver driver) throws UnreachableBrowserException {
		ActionFactory actionFactory = new ActionFactory(driver);
		boolean wasPerformedSuccessfully = true;
		
		for(FormField elem : this.getElements()){
			try{
				WebElement element = driver.findElement(By.xpath(elem.getInputElement().getXpath()));
				actionFactory.execAction(element, value, action.getName());				
			}
			catch(StaleElementReferenceException e){
				log.error(e.getMessage());
				wasPerformedSuccessfully = false;			
			}
			catch(ElementNotVisibleException e){
				log.error(e.getMessage());
			}
			catch(NoSuchElementException e){
				log.error(e.getMessage());
				wasPerformedSuccessfully = false;
			}
			catch(WebDriverException e){
				log.error(e.getMessage());
				wasPerformedSuccessfully = false;
			}
		}
		
		
		return wasPerformedSuccessfully;
	}
}
