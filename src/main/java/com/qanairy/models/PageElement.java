package com.qanairy.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.rules.Rule;
/**
 * Contains all the pertinent information for an element on a page. A PageElement
 *  may be a Parent and/or child of another PageElement. This heirarchy is not
 *  maintained by PageElement though. 
 */
@NodeEntity
public class PageElement implements Persistable, PathObject {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageElement.class);

	@GeneratedValue
    @Id
    private Long id;
	
    private String key;
    private String screenshot;
	private String name;
	private String text;
	private String xpath;
	private String type;
	
	@Properties
	private Map<String, String> cssValues = new HashMap<>();
	
	@Relationship(type = "HAS_ATTRIBUTE")
	private Set<Attribute> attributes = new HashSet<>();
	
	@Relationship(type = "HAS_RULE")
	private Set<Rule> rules = new HashSet<>();
			
	public PageElement(){}
	
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
	public PageElement(String text, String xpath, String name, Set<Attribute> attributes, Map<String, String> css_map){
		assert attributes != null;
		assert css_map != null;
		assert xpath != null;
		assert name != null;
		
		setType("PageElement");
		setName(name);
		setXpath(xpath);
		setAttributes(attributes);
		setScreenshot("");
		setText(text);
		setCssValues(css_map);
		setKey(generateKey());
	}
	
	/**
	 * Print Attributes for this element in a prettyish format
	 */
	public void printAttributes(){
		System.out.print("+++++++++++++++++++++++++++++++++++++++");
		for(Attribute attribute : this.attributes){
			System.out.print(attribute.getName() + " : ");
			for(int i=0; i < attribute.getVals().size(); i++){
				System.out.print( attribute.getVals().get(i) + " ");
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
	
	public void setAttributes(Set<Attribute> attribute_persist_list) {
		this.attributes = attribute_persist_list;
	}
	
	public Set<Rule> getRules(){
		return this.rules;
	}

	public void setRules(Set<Rule> rules) {
		this.rules = rules;
	}

	public void addRule(Rule rule) {
		this.rules.add(rule);
	}

	public Set<Attribute> getAttributes() {
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
	

	public void addAttribute(Attribute attribute) {
		this.attributes.add(attribute);
	}
	
	public String getScreenshot() {
		return this.screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public String getType() {
		return this.type;
	}

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
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(getXpath()+":"+getText()+":"+getType());   
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
		Set<Attribute> newPageElementAttributes = that.getAttributes();
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
		PageElement page_elem = new PageElement();
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
