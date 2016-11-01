package com.minion.browsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.minion.persistence.IAttribute;
import com.minion.persistence.IPageElement;
import com.minion.persistence.IPersistable;
import com.minion.persistence.OrientConnectionFactory;
import com.minion.tester.Test;
import com.minion.util.ArrayUtility;


/**
 * Contains all the pertinent information for an element on a page. A PageElement
 *  may be a Parent and/or child of another PageElement. This heirarchy is not
 *  maintained by PageElement though. 
 *  
 * @author Brandon Kindred
 *
 */
public class PageElement extends PathObject<IPageElement> {
    private static final Logger log = LoggerFactory.getLogger(PageElement.class);

    private String key;
	private String[] actions = ActionFactory.getActions();
	private String tagName;
	private String text;
	private String xpath;
	private boolean changed=false;
	public List<Attribute> attributes = new ArrayList<Attribute>();
	public List<PageElement> child_elements = new ArrayList<PageElement>();
	Map<String, String> cssValues = new HashMap<String,String>();
	private String screenshot = null;

	private String[] invalid_attributes = {"ng-view", "ng-include", "ng-repeat","ontouchstart", "ng-click", "ng-class", "onload", "lang", "xml:lang", "xmlns", "xmlns:fb", "onsubmit", "webdriver",/*Wordpress generated field*/"data-blogger-escaped-onclick", "src", "alt", "scale", "title", "name","data-analytics","onmousedown", "data-rank", "data-domain", "data-url", "data-subreddit", "data-fullname", "data-type", "onclick", "data-outbound-expiration", "data-outbound-url", "rel", "onmouseover","height","width","onmouseout"};
	
	/**
	 * Constructs an empty PageElement.
	 * 
	 * @param driver
	 * @param elem
	 */
	public PageElement(){}
	
	/**
	 * Constructs a PageElement.
	 * 
	 * @param driver
	 * @param elem
	 */
	public PageElement(WebElement elem, 
					   String parentXpath, 
					   String[] actions, 
					   Map<String, Integer> xpathHash, 
					   List<String> attrib_list){
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		this.actions = actions;
		loadAttributes(attrib_list);
		//loadCssProperties(elem);
		this.xpath = this.generateXpath(elem, parentXpath, xpathHash);
		this.key = this.generateKey();

	}

	/**
 	 * Constructs a PageElement.
	 * 
	 * @param elem
	 * @param actions
	 * @param xpathHash
	 * @param attrib_list
	 */
	public PageElement( Element elem, 
			   		 	String[] actions, 
						Map<String, Integer> xpathHash,
						List<String> attrib_list){
		this.tagName = elem.tagName();
		this.text    = elem.text(); 
		loadAttributes(attrib_list);
		//loadCssProperties(web_elem);
		this.xpath = this.generateXpath(elem);
		this.key = this.generateKey();
	}
	
	/**
	 * 
	 * @param changed
	 * 
	 * @return
	 */
	public boolean isChanged(){
		return this.changed;
	}
	
	/**
	 * 
	 * @param changed
	 * 
	 * @return
	 */
	public void setChanged(boolean changed){
		this.changed = changed;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public void setKey(String key){
		this.key = key;
	}
	
	/**
	 * Loads attributes for this element into a list of {@link Attribute}s
	 * 
	 * @param attributeList
	 */
	public void loadAttributes( List<String> attributeList){
		for(int i = 0; i < attributeList.size(); i++){
			String[] attributes = attributeList.get(i).split("::");
			String[] attributeVals;
			if(attributes.length > 1){
				attributeVals = attributes[1].split(" ");
			}
			else{
				attributeVals = new String[0];
			}
			
			this.attributes.add(new Attribute(attributes[0].trim().replace("\'", "'"), attributeVals));
		}
	}

	/**
	 * Extract all attributes from a given {@link WebElement}
	 * 
	 * @param element {@link WebElement} to have attributes loaded for
	 * @param javascriptDriver - 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<String> extractedAttributes(WebElement element, JavascriptExecutor javascriptDriver) {
		return (ArrayList<String>)javascriptDriver.executeScript("var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;", element);
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
	 * Sets the css property map
	 * @param cssValueMap
	 */
	public Map<String, String> getCssProperties(){
		 return this.cssValues;
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
	 * Get immediate child elements for a given element
	 * 
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	/*public ArrayList<PageElement> getChildElements(WebDriver driver, WebElement elem, HashMap<String, Integer> xpathHash){
		List<WebElement> childElements = elem.findElements(By.xpath(".//"));
		ArrayList<PageElement> childPageElements = new ArrayList<PageElement>();
		for(WebElement childElement : childElements){
			childPageElements.add(new PageElement(driver, childElement, this.xpath, ActionFactory.getActions(), xpathHash, extractedAttributes(elem, (JavascriptExecutor)driver)));
		}
		
		return childPageElements;
	}
	*/
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
		
		if(this.getTagName().equals(that.getTagName())
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
	
	/**
	 * creates a unique xpath based on a given hash of xpaths
	 * 
	 * @param driver
	 * @param xpathHash
	 * 
	 * @return
	 */
	public String uniqifyXpath(WebElement elem, Map<String, Integer> xpathHash, String xpath){
		if(elem.findElements(By.xpath(xpath)).size() <= 1){
			return xpath;
		}
		else{
			int count = 1;
			if(xpathHash.containsKey(xpath)){
				count = xpathHash.get(xpath);
				count += 1;
			}
		
			xpathHash.put(xpath, count);
			xpath = xpath+"[" + count + "]";
		}
		return xpath;
	}

	/**
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(WebElement element, String xpath, Map<String, Integer> xpathHash){
		ArrayList<String> attributeChecks = new ArrayList<String>();
		xpath += "//"+this.tagName;
		log.info("Xpath : "+xpath);
		for(Attribute attr : attributes){
			if(!Arrays.asList(invalid_attributes).contains(attr.getName())){
				attributeChecks.add("contains(@" + attr.getName() + ",\"" + ArrayUtility.joinArray(attr.getVals()) + "\")");
			}
		}
		if(attributeChecks.size()>0){
			xpath += "[";
			for(int i = 0; i < attributeChecks.size(); i++){
				xpath += attributeChecks.get(i).toString();
				if(i < attributeChecks.size()-1){
					xpath += " and ";
				}
			}
			xpath += "]";
		}
		xpath = uniqifyXpath(element, xpathHash, xpath);
		log.info("Final Xpath : "+xpath);
		return xpath;
	}
	
	/**
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(Element elem){
		String xpath = "//" + this.tagName + "["+elem.siblingIndex()+"]";
		log.info("constructed xpath = "+xpath);
		
		return xpath;
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
	 * returns the xpath generated for this element
	 * @return xpath of this element
	 */
	public String getXpath() {
		return this.xpath;
	}

	/**
	 * Sets the xpath for this element
	 * 
	 * @param xpath the xpath that identifies the unique location 
	 * 				of this element on the page
	 */
	public void setXpath(String new_xpath) {
		this.xpath = new_xpath;
	}
	
	public void setText(String text){
		this.text = text;
	}

	public String getText(){
		return this.text;
	}

	public List<Attribute> getAttributes() {
		return this.attributes;
	}
	
	public String getTagName(){
		return this.tagName;
	}

	public boolean isIgnorable() {
		return Arrays.asList().contains(this.tagName);
	}
	
	public List<PageElement> getChild_elements() {
		return child_elements;
	}

	public void setChild_elements(ArrayList<PageElement> child_elements) {
		this.child_elements = child_elements;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 5 + tagName.hashCode();
        hash = hash * 17 + text.hashCode();
        hash = hash * 31 + xpath.hashCode();
        
        for(Attribute attr : attributes){
        	hash = hash * 13 + attr.hashCode();
        }
        return hash;
    }

	public String[] getActions() {
		return this.actions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPageElement convertToRecord(OrientConnectionFactory framedGraph) {
		Iterable<IPageElement> page_elements = findByKey(this.getKey(), framedGraph);
		
		int cnt = 0;
		Iterator<IPageElement> iter = page_elements.iterator();
		IPageElement page_element = null;
		while(iter.hasNext()){
			iter.next();
			cnt++;
		}
		
		if(cnt == 0){
			page_element = framedGraph.getTransaction().addVertex("class:"+IPageElement.class.getCanonicalName()+","+UUID.randomUUID(), IPageElement.class);

			List<IAttribute> attribute_persist_list = new ArrayList<IAttribute>();
			/*for(Attribute attribute : this.attributes){
				IAttribute attribute_persist = attribute.convertToRecord(framedGraph);
				attribute_persist_list.add(attribute_persist);
			}
			*/
			//page_element.setAttributes(attribute_persist_list);
			page_element.setChanged(this.isChanged());
			
			/*List<IPageElement> child_elements_persist = new ArrayList<IPageElement>();
			for(PageElement elem : this.child_elements){
				IPageElement child_element = elem.convertToRecord(framedGraph);
				child_elements_persist.add(child_element);
			}
			*/
			//page_element.setChildElements(child_elements_persist);
			
			page_element.setCssValues(this.cssValues);
			page_element.setTagName(this.tagName);
			page_element.setText(this.text);
			page_element.setXpath(this.xpath);
			page_element.setKey(this.key);
			page_element.setType(this.getClass().getName());
			page_element.setScreenshot(this.getScreenshot());
		}
		else{
			page_element = page_elements.iterator().next();
		}
		return page_element;
	}
	
	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	@Override
	public String generateKey() {
		return "::"+this.getXpath().hashCode()+"::";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IPageElement> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IPageElement> update() {
		Iterator<IPageElement> page_element_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(page_element_iter.hasNext()){
			page_element_iter.next();
			cnt++;
		}
		log.info("# of existing page element records with key "+this.getKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IPageElement page_element = null;
		if(cnt == 0){
			page_element = connection.getTransaction().addVertex("class:"+IPageElement.class.getCanonicalName()+","+UUID.randomUUID(), IPageElement.class);	
		}
		
		page_element = this.convertToRecord(connection);
		connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<IPageElement> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, IPageElement.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<IPageElement> findByKey(String generated_key, OrientConnectionFactory orient_connection) {
		return orient_connection.getTransaction().getVertices("key", generated_key, IPageElement.class);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public PageElement convertFromRecord(IPageElement data) {
		PageElement page_elem = new PageElement();
		page_elem.setChanged(data.getChanged());
		page_elem.setKey(data.getKey());
		page_elem.setXpath(data.getXpath());
		page_elem.setText(data.getText());
		page_elem.setScreenshot(data.getScreenshot());
		
		return page_elem;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject<?> clone() {
		PageElement page_elem = new PageElement();
		page_elem.setChanged(this.isChanged());
		page_elem.setKey(this.getKey());
		page_elem.setXpath(this.getXpath());

		return page_elem;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}
}
