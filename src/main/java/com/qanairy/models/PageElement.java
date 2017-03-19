package com.qanairy.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IPageElement;
import com.qanairy.persistence.IAttribute;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Contains all the pertinent information for an element on a page. A PageElement
 *  may be a Parent and/or child of another PageElement. This heirarchy is not
 *  maintained by PageElement though. 
 */
public class PageElement extends PathObject<IPageElement>{
	private static final Logger log = LoggerFactory.getLogger(PageElement.class);

    private String key;
    private String screenshot;
	private String name;
	private String text;
	private String xpath;
	private Map<String, String> cssValues = new HashMap<String,String>();
	private List<Attribute> attributes = new ArrayList<Attribute>();

	public PageElement(){
		
	}
	
	public PageElement(String text, String xpath, String name, List<Attribute> attributes){
		this.name = name;
		this.xpath = xpath;
		this.attributes = attributes;
		this.text = text;
		this.cssValues = this.getCssValues();
		this.key = generateKey();
	}
	
	/**
	 * Print Attributes for this element in a prettyish format
	 */
	public void printAttributes(){
		System.out.println("+++++++++++++++++++++++++++++++++++++++");
		for(int j=0; j < this.attributes.size(); j++){
			System.out.print(this.attributes.get(j).getName() + " : ");
			for(int i=0; i < attributes.get(j).getVals().length; i++){
				System.out.print( this.attributes.get(j).getVals()[i] + " ");
			}
		}
		System.out.println("\n+++++++++++++++++++++++++++++++++++++++");
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 */
	public void loadCssProperties(WebElement element){
		//HashMap<String, String> cssValues = new HashMap<String,String>();
		String[] cssList = {"backface-visibility", "visible", "display", "position", "color", "font-family", "width", "height", "left", "right", "top", "bottom", "transform"};
		
		Date start = new Date();
		log.info("Loading " + cssList.length+ "  css properties for for page element...");
		
		for(String propertyName : cssList){
			String element_value = element.getCssValue(propertyName);
			if(element_value != null){
				this.cssValues.put(propertyName, element_value);
			}
		}
		
		Date end = new Date();
		
		log.info("All Css properties extracted in " + ((end.getTime() - start.getTime())/1000.0) + " seconds");
	}

	/**
	 * checks if css properties match between {@link WebElement elements}
	 * 
	 * @param elem
	 * @return whether attributes match or not
	 */
	public boolean cssMatches(PageElement elem){
		for(String propertyName : cssValues.keySet()){
			if(!cssValues.get(propertyName).equals(elem.cssValues.get(propertyName))){
				//System.err.println("CSS PROPERTIES DO NOT MATCH");
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * checks if the current element is a child of the element passed
	 * 
	 * @param elem
	 * @return
	 */
	public boolean isChildElement(PageElement elem){
		if(elem.getXpath().equals(this.getXpath()) && elem.getXpath().contains(this.getXpath())){
			return true;
		}
		return false;
	}
		
	public String getName() {
		return name;
	}
	
	public void setName(String tagName) {
		this.name = tagName;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getXpath() {
		return xpath;
	}
	
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public Map<String, String> getCssValues() {
		return cssValues;
	}

	public void setCssValues(Map<String, String> cssValues) {
		this.cssValues = cssValues;
	}
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public void setAttributes(List<Attribute> attribute_persist_list) {
		this.attributes = attribute_persist_list;
	}
	
	
	/**
	 * Converts to string with following format:
	 * {tagName}:{innertext of tag}
	 * 
	 */
	public String toString(){
		return this.xpath;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 5 + name.hashCode();
        hash = hash * 17 + text.hashCode();
        hash = hash * 31 + xpath.hashCode();
        
        for(Attribute attr : attributes){
        	hash = hash * 13 + attr.hashCode();
        }
        return hash;
    }
	
	public List<Attribute> getAttributes() {
		return attributes;
	}
	
	public String[] getAttributeValues(String attr_name){
		String id = "";
		//get id for element
		for(Attribute tag_attr : this.attributes){
			if(tag_attr.getName().equals("id")){
				log.info("ID FOUND ON CHECKBOX :: " + id);
				return tag_attr.getVals();
			}
		}
		
		return null;
	}
	
	public Attribute getAttribute(String attr_name){
		String id = "";
		//get id for element
		for(Attribute tag_attr : this.attributes){
			if(tag_attr.getName().equals(attr_name)){
				log.info("ID FOUND ON CHECKBOX :: " + id);
				return tag_attr;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if {@link PageElement elements} are equal
	 * 
	 * @param elem
	 * @return whether or not elements are equal
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof PageElement)) return false;
        
        PageElement that = (PageElement)o;
		
		List<Attribute> newPageElementAttributes = that.getAttributes();
		
		boolean areElementsEqual =  false;
		
		if(this.getName().equals(that.getName())
				|| this.getText().equals(that.getText())){
			areElementsEqual = true;
		}
		
		if(areElementsEqual && this.getAttributes().size() == newPageElementAttributes.size())
		{
			for(int attrIdx = 0; attrIdx < this.getAttributes().size(); attrIdx++)
			{
				areElementsEqual = this.getAttributes().get(attrIdx).equals(newPageElementAttributes.get(attrIdx));
				if(!areElementsEqual){
					return false;
				}
			}
		}
		
		areElementsEqual = this.cssMatches(that);
		return areElementsEqual;
	}


	public PathObject<?> clone() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getScreenshot() {
		return this.screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element 
	 * 
	 * @param elemAction ElementAction pair
	 * @return whether action was able to be performed on element or not
	 */
	public boolean performAction(Action action, String value, WebDriver driver) throws UnreachableBrowserException {
		ActionFactory actionFactory = new ActionFactory(driver);
		boolean wasPerformedSuccessfully = true;
		
		try{
			WebElement element = driver.findElement(By.xpath(this.getXpath()));
			actionFactory.execAction(element, value, action.getName());
			
			log.info("CRAWLER Performed action "+ action
					+ " On element with xpath :: "+this.getXpath());
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

	public boolean performAction(Action action, WebDriver driver) throws UnreachableBrowserException {
		return false;
	}
}
