package browsing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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
	
	//list of performed actions is not required with node coloring being implemented.
	//TODO :: Remove once node coloring is in place
	//private ArrayList<String> actionsPerformed = new ArrayList<String>();
	
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
		//loadCssProperties();
		this.xpath = this.generateXpath();
	}
	
	/**
	 * Constructs a PageElement.
	 * @param driver
	 * @param elem
	 */
	public PageElement(WebDriver driver, WebElement elem, String parentXpath){
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		loadAttributes(driver, elem);
		//loadCssProperties();
		this.xpath = parentXpath + this.generateXpath();
	}
	
	/**
	 * generates an xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(){
		String xpath = "";
		ArrayList<String> attributeChecks = new ArrayList<String>();
		xpath += "//"+this.tagName;
		for(Attribute attr : attributes){
			if(attr.getName().equals("id")){
				attributeChecks.add("@id='" + ArrayUtility.joinArray(attr.getVal()) + "'"); 
			}
			if(attr.getName().equals("class")){
				attributeChecks.add("contains(@class, '" + ArrayUtility.joinArray(attr.getVal()) +"')");
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
	
	public void loadCssProperties(WebElement element){
		for(String propertyName : cssList){
			if(element.getCssValue(propertyName) != null){
				//System.out.println(propertyName + " : " + this.element.getCssValue(propertyName));
				this.cssValues.put(propertyName, element.getCssValue(propertyName));	
			}			
		}	
	}
	
	/**
	 * 
	 * @param elem
	 * @return
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
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public ArrayList<PageElement> getChildElements(WebDriver driver, WebElement elem){
		List<WebElement> childElements = elem.findElements(By.xpath("*"));
		ArrayList<PageElement> childPageElements = new ArrayList<PageElement>();
		for(WebElement childElement : childElements){
			childPageElements.add(new PageElement(driver, childElement, this.xpath));
		}
		
		return childPageElements;
	}
	
	public boolean equals(PageElement elem){
		ArrayList<Attribute> oldPageElementAttributes = this.getAttributes();
		ArrayList<Attribute> newPageElementAttributes = elem.getAttributes();
		boolean areElementsEqual =  true;
		
		if(!this.getTagName().equals(elem.getTagName())
				|| !this.getText().equals(elem.getText()))
		{
			areElementsEqual = false;
		}
		
		if(areElementsEqual 
				&& oldPageElementAttributes.size() == newPageElementAttributes.size())
		{
			for(int attrIdx = 0; attrIdx < oldPageElementAttributes.size(); attrIdx++)
			{
				//System.err.println("OLD PAGE ATTRIBUTE :: " + oldPageElementAttributes.get(attrIdx).getName() + " -- " + oldPageElementAttributes.get(attrIdx).getVal());
				//System.err.println("NEW PAGE ATTRIBUTE :: " + newPageElementAttributes.get(attrIdx).getName() + " -- " + newPageElementAttributes.get(attrIdx).getVal());
				 areElementsEqual = oldPageElementAttributes.get(attrIdx).equals(newPageElementAttributes.get(attrIdx));
				 if(!areElementsEqual){
					 break;
				 }
			}
		}
		
		if(!this.cssMatches(elem)){
			areElementsEqual = false;
		}
		return areElementsEqual;
	}
	
	public String toString(){
		String pageElementString = "";
		
		pageElementString += this.tagName + "\n";
		pageElementString += this.text + "\n";
		
		return pageElementString;
	}

	/**
	 * Adds given action to list of actions that have been performed on this element
	 * @param action
	 */
	/*
	 * TODO : remove once node coloring is implemented
	public void addAction(String action) {
		this.actionsPerformed.add(action);
		
	}
	*/

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
