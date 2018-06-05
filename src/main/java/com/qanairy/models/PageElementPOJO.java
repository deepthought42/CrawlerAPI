package com.qanairy.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Rule;

/**
 * Contains all the pertinent information for an element on a page. A PageElement
 *  may be a Parent and/or child of another PageElement. This heirarchy is not
 *  maintained by PageElement though. 
 */
public class PageElementPOJO extends PageElement {
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
	
	@Override
	public String getXpath() {
		return xpath;
	}
	
	@Override
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	@Override
	public Map<String, String> getCssValues() {
		return cssValues;
	}

	@Override
	public void setCssValues(Map<String, String> cssValues) {
		this.cssValues = cssValues;
	}
	
	@Override
	public String getKey() {
		return this.key;
	}
	
	@Override
	public void setKey(String key) {
		this.key = key;
	}
	
	@Override
	public void setAttributes(List<Attribute> attribute_persist_list) {
		this.attributes = attribute_persist_list;
	}
	
	@Override
	public List<Rule> getRules(){
		return this.rules;
	}

	@Override
	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	@Override
	public void addRule(Rule rule) {
		this.rules.add(rule);
	}

	
	@Override
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
	

	@Override
	public void addAttribute(Attribute attribute) {
		this.attributes.add(attribute);
	}
	
	
	@Override
	public String getScreenshot() {
		return this.screenshot;
	}

	@Override
	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
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
		
		//areElementsEqual = this.cssMatches(that);
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
}
