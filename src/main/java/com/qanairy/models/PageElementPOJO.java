package com.qanairy.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Direction;
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
import com.qanairy.models.dao.AttributeDao;
import com.qanairy.models.dao.impl.AttributeDaoImpl;
import com.qanairy.persistence.Action;
import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Rule;
import com.qanairy.persistence.edges.PathEdge;
import com.syncleus.ferma.annotations.Incidence;

/**
 * Contains all the pertinent information for an element on a page. A PageElement
 *  may be a Parent and/or child of another PageElement. This heirarchy is not
 *  maintained by PageElement though. 
 */
public class PageElementPOJO extends PageElement implements PathObject{
	private static Logger log = LoggerFactory.getLogger(PageElementPOJO.class);

    private String key;
    private String screenshot;
	private String name;
	private String text;
	private String xpath;
	private Map<String, String> cssValues;
	private List<Attribute> attributes;
	private List<Rule> rules;
	private String type;
	private List<PathEdge> edges;
			
	public PageElementPOJO(){
		setType("PageElement");
		setCssValues(new HashMap<String,String>());
		setAttributes(new ArrayList<Attribute>());
		setRules(new ArrayList<Rule>());
		setKey(generateKey());
	}
	
	/**
	 * 
	 * @param text
	 * @param xpath
	 * @param name
	 * @param attributes
	 * @param css_map
	 * 
	 * @pre attributes != null
	 * @pre css_map != null
	 * @pre xpath != null
	 * @pre name != null
	 */
	public PageElementPOJO(String text, String xpath, String name, List<Attribute> attributes, Map<String, String> css_map){
		assert attributes != null;
		assert css_map != null;
		assert xpath != null;
		assert name != null;
		
		setType("PageElement");
		setName(name);
		setXpath(xpath);
		setAttributes(attributes);
		setText(text);
		setCssValues(css_map);
		setRules(new ArrayList<Rule>());
		setKey(generateKey());
	}
	
	/**
	 * 
	 * @param key
	 * @param text
	 * @param xpath
	 * @param name
	 * @param attributes
	 * @param css_map
	 * @param rules
	 * 
	 * @pre attributes != null
	 * @pre css_map != null
	 * @pre xpath != null
	 * @pre name != null
	 */
	public PageElementPOJO(String text, String xpath, String name, List<Attribute> attributes, Map<String, String> css_map, List<Rule> rules){
		assert attributes != null;
		assert css_map != null;
		assert xpath != null;
		assert name != null;
		
		setType("PageElement");
		setName(name);
		setXpath(xpath);
		setAttributes(attributes);
		setText(text);
		setCssValues(css_map);
		setRules(rules);
		setKey(generateKey());
	}
	
	/**
	 * Print Attributes for this element in a prettyish format
	 */
	public void printAttributes(){
		System.out.print("+++++++++++++++++++++++++++++++++++++++");
		for(int j=0; j < this.attributes.size(); j++){
			System.out.print(this.attributes.get(j).getName() + " : ");
			for(int i=0; i < attributes.get(j).getVals().size(); i++){
				System.out.print( this.attributes.get(j).getVals().get(i) + " ");
			}
		}
		System.out.print("\n+++++++++++++++++++++++++++++++++++++++");
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 */
	public static Map<String, String> loadCssProperties(WebElement element){
		String[] cssList = {"backface-visibility", "visible", "display", "position", "color", "font-family", "width", "height", "left", "right", "top", "bottom", "transform"};
		Map<String, String> css_map = new HashMap<String, String>();
		
		for(String propertyName : cssList){
			try{
				String element_value = element.getCssValue(propertyName);
				if(element_value != null){
					css_map.put(propertyName, element_value);
				}
			}catch(Exception e){
				
			}
		}
		
		return css_map;
	}

	/**
	 * checks if css properties match between {@link WebElement elements}
	 * 
	 * @param elem
	 * @return whether attributes match or not
	 */
	public boolean cssMatches(PageElement elem){
		for(String propertyName : cssValues.keySet()){
			if(propertyName.contains("-moz-") || propertyName.contains("-webkit-") || propertyName.contains("-o-") || propertyName.contains("-ms-")){
				continue;
			}
			if(!cssValues.get(propertyName).equals(elem.getCssValues().get(propertyName))){
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
		return this.key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public void setAttributes(List<Attribute> attribute_persist_list) {
		this.attributes = attribute_persist_list;
	}
	
	public List<Rule> getRules(){
		return this.rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}
	
	public void addRules(List<Rule> rules) {
		this.rules.addAll(rules);
	}
	
	public void addRule(Rule rule) {
		this.rules.add(rule);
	}
	
	/**
	 * Prints this elements xpath
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
	
	public List<String> getAttributeValues(String attr_name){
		//get id for element
		for(Attribute tag_attr : this.attributes){
			if(tag_attr.getName().equals("id")){
				return tag_attr.getVals();
			}
		}
		
		return null;
	}
	
	public Attribute getAttribute(String attr_name){
		//get id for element
		for(Attribute tag_attr : this.attributes){
			if(tag_attr.getName().equals(attr_name)){
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
		boolean areElementsEqual =  true;
		
		if(!this.getText().equals(that.getText())){
			return false;
		}
		
		if(this.getAttributes().size() == newPageElementAttributes.size())
		{
			Map<String, Attribute> attribute_map = new HashMap<String, Attribute>();
			for(Attribute attr : this.getAttributes()){
				attribute_map.put(attr.getName(), attr);		
			}
			
			for(Attribute attr : newPageElementAttributes){
				if(attr.equals(attribute_map.get(attr.getName()))){
					attribute_map.remove(attr.getName());
				}
			}

			if(!attribute_map.isEmpty()){
				return false;
			}
		}
		else{
			return false;
		}
		
		areElementsEqual = this.cssMatches(that);
		return areElementsEqual;
	}


	public PathObject clone() {
		PageElement page_elem = new PageElementPOJO();
		page_elem.setAttributes(this.getAttributes());
		page_elem.setCssValues(this.getCssValues());
		page_elem.setKey(this.getKey());
		page_elem.setName(this.getName());
		page_elem.setScreenshot(this.getScreenshot());
		page_elem.setText(this.getText());
		page_elem.setType(this.getType());
		page_elem.setXpath(this.getXpath());
		
		return page_elem;
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
	public boolean performAction(Action action, WebDriver driver) throws UnreachableBrowserException, NoSuchElementException {
		ActionFactory actionFactory = new ActionFactory(driver);
		boolean wasPerformedSuccessfully = true;
		
		try{
			WebElement element = driver.findElement(By.xpath(this.getXpath()));
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

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public void addAttribute(Attribute attribute) {
		this.attributes.add(attribute);
	}
	
	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(getXpath());   
	}
}
