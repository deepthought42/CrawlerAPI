package browsing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import persistence.IAttribute;
import persistence.IPageElement;
import persistence.IPersistable;
import persistence.OrientConnectionFactory;
import tester.Test;
import util.ArrayUtility;


/**
 * Contains all the pertinent information for an element on a page. A PageElement
 *  may be a Parent and/or child of another PageElement. This heirarchy is not
 *  maintained by PageElement though. 
 *  
 * @author Brandon Kindred
 *
 */
public class PageElement implements PathObject, IPersistable<IPageElement> {
    private static final Logger log = Logger.getLogger(PageElement.class);

	private String[] actions = ActionFactory.getActions();
	public String tagName;
	public String text;
	private String xpath;
	private boolean changed=false;
	public List<Attribute> attributes = new ArrayList<Attribute>();
	public List<PageElement> child_elements = new ArrayList<PageElement>();
	Map<String, String> cssValues = new HashMap<String,String>();

	private String[] invalid_attributes = {"ng-view", "ng-include", "ng-repeat","ontouchstart", "ng-click", "ng-class", /*Wordpress generated field*/"data-blogger-escaped-onclick"};

	private String key;
		
	//transfer list to enum class
	
	/**
	 * Constructs a PageElement.
	 * 
	 * @param driver
	 * @param elem
	 */
	public PageElement( WebDriver driver, 
						WebElement elem, 
						String parentXpath, 
						String[] actions, 
						Map<String, Integer> xpathHash,
						List<String> attrib_list){
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		this.actions = actions;
		loadAttributes(attrib_list);		
		loadCssProperties(elem);
		this.xpath = this.generateXpath(driver, parentXpath, xpathHash);
		this.key = this.generateKey();
	}
	
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
		loadCssProperties(elem);
		this.xpath = this.generateXpath(elem, parentXpath, xpathHash);
		this.key = this.generateKey();

	}

	/**
	 * Constructs a PageElement.
	 * 
	 * @param driver
	 * @param elem
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
	 * Constructs a PageElement.
	 * 
	 * @param driver
	 * @param elem
	 */
	public PageElement( WebDriver driver,
						WebElement elem, 
						Page page, 
						String[] actions, 
						Map<String, Integer> xpathHash,
						List<String> attrib_list){
			 
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		this.actions = actions;
		loadAttributes(attrib_list);
		loadCssProperties(elem);
		this.xpath = this.generateXpath(driver, "", xpathHash);
		this.key = this.generateKey();
	}
	
	/**
	 * creates a unique xpath based on a given hash of xpaths
	 * 
	 * @param driver
	 * @param xpathHash
	 * @return
	 */
	public String uniqifyXpath(WebDriver driver, Map<String, Integer> xpathHash, String xpath){
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
	 * creates a unique xpath based on a given hash of xpaths
	 * 
	 * @param driver
	 * @param xpathHash
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
		for(Attribute attr : attributes){
			if(!Arrays.asList(invalid_attributes).contains(attr.getName())){
				attributeChecks.add("contains(@" + attr.getName() + ",'" + ArrayUtility.joinArray(attr.getVals()) + "')");
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

		return xpath;
	}
	
	/**
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(WebDriver driver, String xpath, Map<String, Integer> xpathHash){
		ArrayList<String> attributeChecks = new ArrayList<String>();
		xpath += "//"+this.tagName;
		for(Attribute attr : attributes){
			if(!Arrays.asList(invalid_attributes).contains(attr.getName())){
				attributeChecks.add("contains(@" + attr.getName() + ",'" + ArrayUtility.joinArray(attr.getVals()) + "')");
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
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(Element elem){
		xpath = "//" + this.tagName + "["+elem.siblingIndex()+"]";
		log.info("constructed xpath = "+xpath);
		
		return xpath;
	}
	
	/**
	 * 
	 * @param changed
	 * @return
	 */
	public boolean isChanged(){
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
	public void loadAttributes( List<String> attributeList){
		
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
	 * @param element the element to for which css styles should be loaded.
	 */
	public void loadCssProperties(WebElement element){
		//HashMap<String, String> cssValues = new HashMap<String,String>();
		String[] cssList = {"backface-visibility", "visible", "display", "position", "color", "font-family", "width", "height", "left", "right", "top", "bottom", "transform"};

		for(String propertyName : cssList){
			if(element.getCssValue(propertyName) != null){
				this.cssValues.put(propertyName, element.getCssValue(propertyName));	
			}			
		}
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
	public ArrayList<PageElement> getChildElements(WebDriver driver, WebElement elem, HashMap<String, Integer> xpathHash){
		List<WebElement> childElements = elem.findElements(By.xpath(".//"));
		ArrayList<PageElement> childPageElements = new ArrayList<PageElement>();
		for(WebElement childElement : childElements){
			childPageElements.add(new PageElement(driver, childElement, this.xpath, ActionFactory.getActions(), xpathHash, extractedAttributes(elem, (JavascriptExecutor)driver)));
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
	public PageElement data() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPageElement convertToRecord(OrientConnectionFactory framedGraph) {
		IPageElement pageElement = framedGraph.getTransaction().addVertex(UUID.randomUUID(), IPageElement.class);
		
		List<IAttribute> attribute_persist_list = new ArrayList<IAttribute>();
		for(Attribute attribute : this.attributes){
			IAttribute attribute_persist = attribute.convertToRecord(framedGraph);
			attribute_persist_list.add(attribute_persist);
		}
		pageElement.setAttributes(attribute_persist_list);
		pageElement.setChanged(this.isChanged());
		
		List<IPageElement> child_elements_persist = new ArrayList<IPageElement>();
		for(PageElement elem : this.child_elements){
			IPageElement child_element = elem.convertToRecord(framedGraph);
			child_elements_persist.add(child_element);
		}
		pageElement.setChildElements(child_elements_persist);
		
		pageElement.setCssValues(this.cssValues);
		pageElement.setTagName(this.tagName);
		pageElement.setText(this.text);
		pageElement.setXpath(this.xpath);
		pageElement.setKey(this.key);
		return pageElement;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getKey() {
		return this.key;
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
	public IPersistable<IPageElement> update(IPageElement existing_obj) {
		Iterator<IPageElement> page_element_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(page_element_iter.hasNext()){
			page_element_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.getKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IPageElement page_element = null;
		if(cnt == 0){
			page_element = connection.getTransaction().addVertex(UUID.randomUUID(), IPageElement.class);	
		}
		
		page_element = this.convertToRecord(connection);
		connection.save();
		
		return page_element;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<IPageElement> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, IPageElement.class);
	}
}
