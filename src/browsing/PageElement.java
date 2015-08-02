package browsing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

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
 *  //TODO Maintain heirarchy between pageElements
 * 
 * @author Brandon Kindred
 *
 */
public class PageElement {
	private UUID uuid = null;
	private String tagName;
	private String text;
	private String xpath;
	private boolean changed=false;
	private ArrayList<Attribute> attributes = new ArrayList<Attribute>();
	private String[] invalidAttributes = {"ng-view", "ng-include", "ng-repeat","ontouchstart", "ng-click", "ng-class"};
	private ValueDomain positiveDomain = new ValueDomain();
	
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
	public PageElement(WebDriver driver, WebElement elem){
		this.uuid = UUID.randomUUID();
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		loadAttributes(driver, elem);
		loadCssProperties(elem);
		this.xpath = this.generateXpath(driver);
	}
	
	/**
	 * Constructs a PageElement.
	 * @param driver
	 * @param elem
	 */
	public PageElement(WebDriver driver, WebElement elem, String parentXpath, HashMap<String, Integer> xpathHash){
		this.uuid = UUID.randomUUID();
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		loadAttributes(driver, elem);
		
		//Assuming no previous known domain values, load up positiveDomain with a bunch of random values across the board
		positiveDomain.generateAllValueTypes();
		
		//loadCssProperties(elem);
		this.xpath = parentXpath + this.generateXpath(driver);
		this.xpath = uniqifyXpath(driver, xpathHash);
	}
	
	
	public String uniqifyXpath(WebDriver driver, HashMap<String, Integer> xpathHash){
		
		Stack<String> elementStack = new Stack<String>();
		String newXpath = this.xpath;
		
		if(driver.findElements(By.xpath(newXpath)).size() <= 1){
			return newXpath;
		}
		
		//System.out.println("PRE PROCESSING newXpath :: "+newXpath);

		
		while(driver.findElements(By.xpath(newXpath)).size() > 1){
			elementStack.push(newXpath.substring(newXpath.lastIndexOf("//")));
			newXpath = newXpath.substring(0, newXpath.lastIndexOf("//"));
			//System.out.println("SHORETENED newXpath :: "+newXpath);
		}
		
		while(!elementStack.isEmpty()){			
			if(driver.findElements(By.xpath(newXpath+elementStack.peek())).size() <= 1){
				newXpath = newXpath+elementStack.pop();
				//System.out.println("NEW1 UNIQUE XPATH :: "+newXpath +" HAS "+driver.findElements(By.xpath(newXpath)).size() + " ELEMENTS ASSOCIATED WITH IT");
				continue;
			}
			
			if(xpathHash.containsKey(newXpath+elementStack.peek())){
				String modified_xpath = newXpath+elementStack.pop();
				Integer count = xpathHash.get(modified_xpath);
				count += 1;
				xpathHash.put(modified_xpath, count);
				newXpath = modified_xpath+"[" + count + "]";
			}
			else{
				xpathHash.put(newXpath+elementStack.peek(), 0);
				//System.out.println("ADDING XPATH STEM TO HASH ++++ " + newXpath+elementStack.peek());
			}
		}
		//System.out.println("NEW UNIQUE XPATH :: "+newXpath +" HAS "+driver.findElements(By.xpath(newXpath)).size() + " ELEMENTS ASSOCIATED WITH IT");
		//System.out.println("THERE ARE NOW "+elementStack.size() +" XPATH FRAGMENTS ON THE STACK");
		return newXpath;
	}
	
	/**
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(WebDriver driver){
		String xpath = "";
		ArrayList<String> attributeChecks = new ArrayList<String>();
		xpath += "//"+this.tagName;
		for(Attribute attr : attributes){
			if(!Arrays.asList(invalidAttributes).contains(attr.getName())){
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
		System.out.println(Thread.currentThread().getName()+" :: style Properties loaded");
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
				System.err.println("CSS PROPERTIES DO NOT MATCH");

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
		List<WebElement> childElements = elem.findElements(By.xpath("*"));
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
	public boolean equals(PageElement elem){
		ArrayList<Attribute> oldPageElementAttributes = this.getAttributes();
		ArrayList<Attribute> newPageElementAttributes = elem.getAttributes();
		boolean areElementsEqual =  false;
		
		if(this.getTagName().equals(elem.getTagName())
				|| this.getText().equals(elem.getText()))
		{
			areElementsEqual = true;
		}
		
		if(oldPageElementAttributes.size() == newPageElementAttributes.size())
		{
			for(int attrIdx = 0; attrIdx < oldPageElementAttributes.size(); attrIdx++)
			{
				areElementsEqual = oldPageElementAttributes.get(attrIdx).equals(newPageElementAttributes.get(attrIdx));
				if(!areElementsEqual){
					return false;
				}
			}
		}
		
		areElementsEqual = this.cssMatches(elem);
		return areElementsEqual;
	}

	
	/**
	 * checks if the current element is a child of the element passed
	 * 
	 * @param elem
	 * @return
	 */
	public boolean isChildElement(PageElement elem){
		if(elem.getXpath().contains(this.getXpath())){
			return true;
		}
		return false;
	}

	
	/**
	 * Converts to string with following format:
	 * Tag Name: {tagName}
	 * text:	{innertext of tag}
	 * 
	 */
	public String toString(){
		String pageElementString = "";
		
		pageElementString += "Tag Name: " + this.tagName + "\n";
		pageElementString += "text: " + this.text + "\n";
		
		return pageElementString;
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
	
	public UUID getUuid(){
		return this.uuid;
	}
}
