package browsing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import util.ArrayUtility;


/**
 * Contains all the pertinent information for an element on a page. A PageElement
 *  may be a Parent and/or child of another PageElement. This heirarchy is not
 *  maintained by PageElement though. 
 *  
 * @author Brandon Kindred
 *
 */
public class PageElement implements IBrowserObject {
	private String[] actions = ActionFactory.getActions();
	public String tagName;
	public String text;
	public String xpath;
	public boolean changed=false;
	public ArrayList<Attribute> attributes = new ArrayList<Attribute>();
	public ArrayList<PageElement> child_elements = new ArrayList<PageElement>();

	private String[] invalid_attributes = {"ng-view", "ng-include", "ng-repeat","ontouchstart", "ng-click", "ng-class", /*Wordpress generated field*/"data-blogger-escaped-onclick"};
	
	//map loaded with k,v where k=propertyName, and v=propertyValue
	private HashMap<String, String> cssValues = new HashMap<String,String>();
	
	//transfer list to enum class
	private String[] cssList = {"backface-visibility", "visible", "display", "position", "color", "font-family", "width", "height", "left", "right", "top", "bottom", "transform"};
	
	/**
	 * Constructs a PageElement.
	 * 
	 * @param driver
	 * @param elem
	 */
	public PageElement(WebDriver driver, WebElement elem, String parentXpath, String[] actions, HashMap<String, Integer> xpathHash){
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		this.actions = actions;
		loadAttributes(driver, elem);
		
		//loadCssProperties(elem);
		this.xpath = this.generateXpath(driver, parentXpath, xpathHash);
	}
	
	/**
	 * Constructs a PageElement.
	 * 
	 * @param driver
	 * @param elem
	 */
	public PageElement(WebDriver driver, WebElement elem, String parentXpath, HashMap<String, Integer> xpathHash){
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		loadAttributes(driver, elem);
		loadCssProperties(elem);
		this.xpath = this.generateXpath(driver, parentXpath, xpathHash);
	}
	
	/**
	 * Constructs a PageElement.
	 * 
	 * @param driver
	 * @param elem
	 */
	public PageElement(WebDriver driver, WebElement elem, Page page, String[] actions, HashMap<String, Integer> xpathHash){
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		this.actions = actions;
		loadAttributes(driver, elem);
		loadCssProperties(elem);
		this.xpath = this.generateXpath(driver, "", xpathHash);
	}
	
	/**
	 * creates a unique xpath based on a given hash of xpaths
	 * 
	 * @param driver
	 * @param xpathHash
	 * @return
	 */
	public String uniqifyXpath(WebDriver driver, HashMap<String, Integer> xpathHash, String xpath){
		if(driver.findElements(By.xpath(xpath)).size() <= 1){
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
	public String generateXpath(WebDriver driver, String xpath, HashMap<String, Integer> xpathHash){
		ArrayList<String> attributeChecks = new ArrayList<String>();
		xpath += "//"+this.tagName;
		for(Attribute attr : attributes){
			if(!Arrays.asList(invalid_attributes).contains(attr.getName())){
				attributeChecks.add("contains(@" + attr.getName() + ",'" + ArrayUtility.joinArray(attr.getVal()) + "')");
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
		xpath = uniqifyXpath(driver, xpathHash, xpath);

		return xpath;
	}
	
	/**
	 * 
	 * @param changed
	 * @return
	 */
	public boolean isChanged(boolean changed){
		return this.changed;
	}
	
	/**
	 * 
	 * @param changed
	 * @return
	 */
	public void setChanged(boolean changed){
		this.changed = changed;
	}
	
	/**
	 * Loads attributes for this element into a list of {@link Attribute}s
	 * @param driver
	 */
	public void loadAttributes(WebDriver driver, WebElement element){
		ArrayList<String> attributeList = extractedAttributes(element, driver);
		for(int i = 0; i < attributeList.size(); i++){
			//System.out.println("ATTRIBUTE ITEM :: "+attributeList.get(i));
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
	private ArrayList<String> extractedAttributes(WebElement element, WebDriver driver) {
		JavascriptExecutor javascriptDriver = (JavascriptExecutor)driver;
		return (ArrayList<String>)javascriptDriver.executeScript("var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;", element);
	}
	
	/**
	 * Print Attributes for this element in a prettyish format
	 */
	public void printAttributes(){
		System.out.println("+++++++++++++++++++++++++++++++++++++++");
		for(int j=0; j < this.attributes.size(); j++){
			System.out.print(this.attributes.get(j).getName() + " : ");
			for(int i=0; i < attributes.get(j).getVal().length; i++){
				System.out.print( this.attributes.get(j).getVal()[i] + " ");
			}
		}
		System.out.println("\n+++++++++++++++++++++++++++++++++++++++");
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * @param element the element to for which css styles should be loaded.
	 */
	public void loadCssProperties(WebElement element){
		for(String propertyName : cssList){
			if(element.getCssValue(propertyName) != null){
				this.cssValues.put(propertyName, element.getCssValue(propertyName));	
			}			
		}
		//System.out.println(Thread.currentThread().getName()+" :: style Properties loaded");
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
	public ArrayList<PageElement> getChildElements(WebDriver driver, WebElement elem, HashMap<String, Integer> xpathHash){
		List<WebElement> childElements = elem.findElements(By.xpath(".//"));
		ArrayList<PageElement> childPageElements = new ArrayList<PageElement>();
		for(WebElement childElement : childElements){
			childPageElements.add(new PageElement(driver, childElement, this.xpath, xpathHash));
		}
		
		return childPageElements;
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
		
		ArrayList<Attribute> newPageElementAttributes = that.getAttributes();
		
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
	

	public String getText(){
		return this.text;
	}

	public ArrayList<Attribute> getAttributes() {
		return this.attributes;
	}
	
	public String getTagName(){
		return this.tagName;
	}

	public boolean isIgnorable() {
		return Arrays.asList().contains(this.tagName);
	}
	
	public ArrayList<PageElement> getChild_elements() {
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
	public double getCost() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getReward() {
		// TODO Auto-generated method stub
		return 0;
	}
}
