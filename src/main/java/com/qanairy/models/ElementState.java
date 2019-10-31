package com.qanairy.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
 * Contains all the pertinent information for an element on a page. A ElementState
 *  may be a Parent and/or child of another ElementState. This heirarchy is not
 *  maintained by ElementState though. 
 */
@NodeEntity
public class ElementState implements Persistable, PathObject, Comparable<ElementState> {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementState.class);

	@GeneratedValue
    @Id
    private Long id;
    private String key;
    private String screenshot;
    private String screenshot_checksum;
	private String name;
	private String text;
	private String xpath;
	private String type;
	private int x_location;
	private int y_location;
	private int width;
	private int height;
	private String inner_html;
	private String css_selector;
	private String outer_html;
	
	@Properties
	private Map<String, String> cssValues = new HashMap<>();
	
	@Relationship(type = "HAS_ATTRIBUTE")
	private Set<Attribute> attributes = new HashSet<>();
	
	@Relationship(type = "HAS")
	private Set<Rule> rules = new HashSet<>();

	private String template;
			
	public ElementState(){}
	
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
	 * @pre screenshot_url != null
	 * @pre !screenshot_url.isEmpty()
	 */
	public ElementState(String text, String xpath, String name, Set<Attribute> attributes, 
			Map<String, String> css_map, String screenshot_url, int x_location, int y_location, int width, int height,
			String inner_html, String screenshot_checksum){
		assert attributes != null;
		assert css_map != null;
		assert xpath != null;
		assert name != null;
		assert screenshot_url != null;
		assert !screenshot_url.isEmpty();
		
		setType("ElementState");
		setName(name);
		setXpath(xpath);
		setAttributes(attributes);
		setScreenshot(screenshot_url);
		setScreenshotChecksum(screenshot_checksum);
		setText(text);
		setCssValues(css_map);
		setXLocation(x_location);
		setYLocation(y_location);
		setWidth(width);
		setHeight(height);
		setInnerHtml(inner_html);
		setCssSelector("");
		setTemplate("");
		setRules(new HashSet<>());
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
	 * @pre xpath != null
	 * @pre name != null
	 * @pre screenshot_url != null
	 * @pre !screenshot_url.isEmpty()
	 *  
	 */
	public ElementState(String text, String xpath, String name, Set<Attribute> attributes, Map<String, String> css_map, 
						String screenshot_url, String checksum, int x_location, int y_location, int width, int height,
						String inner_html){
		assert name != null;
		assert xpath != null;
		assert checksum != null;
		assert !checksum.isEmpty();
		
		setType("ElementState");
		setName(name);
		setXpath(xpath);
		setAttributes(attributes);
		setScreenshot(screenshot_url);
		setText(text);
		setCssValues(css_map);
		setScreenshotChecksum(checksum);
		setXLocation(x_location);
		setYLocation(y_location);
		setWidth(width);
		setHeight(height);
		setInnerHtml(inner_html);
		setCssSelector("");
		setTemplate("");
		setRules(new HashSet<>());
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
	public boolean cssMatches(ElementState elem){
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
	public boolean isChildElement(ElementState elem){
		return elem.getXpath().equals(this.getXpath()) && elem.getXpath().contains(this.getXpath());
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
		boolean exists = false;
		for(Rule existing_rule : this.rules){
			if(existing_rule.getKey().equals(rule.getKey())){
				exists = true;
			}
		}
		if(!exists){
			this.rules.add(rule);
		}
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
	
	public String getScreenshotChecksum() {
		return screenshot_checksum;
	}

	public void setScreenshotChecksum(String screenshot_checksum) {
		this.screenshot_checksum = screenshot_checksum;
	}

	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey() {
		String key = "";
		
		List<String> css_keys = getCssValues().keySet().stream().collect(Collectors.toList());
		Collections.sort(css_keys, (o1, o2) -> o1.compareTo(o2));
		for(String css_key : css_keys){
			key += css_key+cssValues.get(css_key);
		}

/*
		List<Attribute> attributes = getAttributes().stream().collect(Collectors.toList());
		Collections.sort(attributes, (o1, o2) -> o1.getName().compareTo(o2.getName()));
		
		for(Attribute attribute : attributes){
			key += attribute.getKey();
		}
	*/	
		key += this.getName();
		key += this.getText();
		//key += this.getWidth();
		//key += this.getHeight();
		//key += this.getInnerHtml();
		
		return "elementstate::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
	}
	

	/**
	 * Prints this elements xpath
	 */
	public String toString(){
		return this.xpath;
	}

	/**
	 * Checks if {@link ElementState elements} are equal
	 * 
	 * @param elem
	 * @return whether or not elements are equal
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof ElementState)) return false;
        
        ElementState that = (ElementState)o;
		return this.getKey().equals(that.getKey()) || getScreenshotChecksum().equals(that.getScreenshotChecksum());
	}


	public ElementState clone() {
		ElementState page_elem = new ElementState();
		page_elem.setAttributes(this.getAttributes());
		page_elem.setCssValues(this.getCssValues());
		page_elem.setKey(this.getKey());
		page_elem.setName(this.getName());
		page_elem.setScreenshot(this.getScreenshot());
		page_elem.setScreenshotChecksum(this.getScreenshotChecksum());
		page_elem.setText(this.getText());
		page_elem.setType(this.getType());
		page_elem.setXpath(this.getXpath());
		page_elem.setYLocation(this.getYLocation());
		page_elem.setXLocation(this.getXLocation());
		page_elem.setWidth(this.getWidth());
		page_elem.setHeight(this.getHeight());
		
		return page_elem;
	}

	public int getXLocation() {
		return x_location;
	}

	public void setXLocation(int x_location) {
		this.x_location = x_location;
	}

	public int getYLocation() {
		return y_location;
	}

	public void setYLocation(int y_location) {
		this.y_location = y_location;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int compareTo(ElementState o) {
		 if(this.getYLocation() == o.getYLocation())
             return 0;
         return this.getYLocation() < o.getYLocation() ? -1 : 1;
	}

	public String getInnerHtml() {
		return inner_html;
	}

	public void setInnerHtml(String inner_html) {
		this.inner_html = inner_html;
	}

	public String getCssSelector() {
		return css_selector;
	}

	public void setCssSelector(String css_selector) {
		this.css_selector = css_selector;
	}

	public void setOuterHtml(String outer_html) {
		this.outer_html = outer_html;
	}

	public String getOuterHtml() {
		return outer_html;
	}

	public String getTemplate(){
		return this.template;
	}
	
	public void setTemplate(String template) {
		this.template = template;
	}
}
