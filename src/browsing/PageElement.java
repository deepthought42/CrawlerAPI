package browsing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import util.ArrayUtility;

public class PageElement {

	private String tagName;
	private String text;
	private String xpath;
	private ArrayList<Attribute> attributes = new ArrayList<Attribute>();
	private String[] invalidAttributes = {"ng-view", "ng-include", "ng-repeat","ontouchstart"};
	
	//map loaded with k,v where k=propertyName, and v=propertyValue
	private HashMap<String, String> cssValues = new HashMap<String,String>();
	
	//transfer list to enum class
	private String[] cssList = {"backface-visibility", "visible", "display", "position", "color", "font-family", "width", "height", "left", "right", "top", "bottom", "transform"};

	/**
	 * Constructs a PageElement.
	 * @param driver
	 * @param elem
	 */
	public PageElement(WebDriver driver, WebElement elem){
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
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		loadAttributes(driver, elem);
		loadCssProperties(elem);
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
	 * Loads attributes for this element into a list of {@link Attribute}s
	 * @param driver
	 */
	public void loadAttributes(WebDriver driver, WebElement element){
		JavascriptExecutor javascriptDriver = (JavascriptExecutor)driver;

		String attributeString = javascriptDriver.executeScript("var items = {}; for (index = 0; index < arguments[0].attributes.length; ++index) { items[arguments[0].attributes[index].name] = arguments[0].attributes[index].value }; return items;", element).toString();
		attributeString = attributeString.replace("{","");
		attributeString = attributeString.replace("}","");
		attributeString = attributeString.trim();
		if(!attributeString.isEmpty()){
			String[] attributeArray = attributeString.split(",");
			for(int i=0; i < attributeArray.length; i++){
				String[] attributes = attributeArray[i].split("=");
				String[] attributeVals;
				if(attributes.length > 1){
					attributeVals = attributes[1].split(" ");
				}
				else{
					attributeVals = new String[0];
				}
				
				this.attributes.add(new Attribute(attributes[0].trim(), attributeVals));
			}
		}
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
	 * 
	 */
	public String toString(){
		String pageElementString = "";
		
		pageElementString += this.tagName + "\n";
		pageElementString += this.text + "\n";
		
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
}
