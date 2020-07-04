package com.qanairy.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.enums.ElementClassification;
import com.qanairy.models.rules.Rule;
import com.qanairy.services.BrowserService;

/**
 * Contains all the pertinent information for an element on a page. A ElementState
 *  may be a Parent and/or child of another ElementState. This heirarchy is not
 *  maintained by ElementState though. 
 */
public class ElementState extends LookseeObject implements PathObject, Comparable<ElementState> {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementState.class);

	private String name;
	private String text;
	private String xpath;
	private String type;
	private String inner_html;
	private String css_selector;
	private String outer_html;
	private String classification;
	private String template;
	
	private String screenshot_url;
	private String screenshot_checksum;
	private int x_location;
	private int y_location;
	private int width;
	private int height;
	
	@Deprecated
	private boolean part_of_form;
	
	@Properties
	private Map<String, String> css_values = new HashMap<>();
	
	@Properties
	private Map<String, String> attributes = new HashMap<>();
	
	@Relationship(type = "HAS")
	private Set<Rule> rules = new HashSet<>();

	@Relationship(type = "HAS_CHILD")
	private List<ElementState> child_elements = new ArrayList<>();
	

	public ElementState(){
		super();
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
	 * @pre screenshot_url != null
	 * @pre !screenshot_url.isEmpty()
	 */
	public ElementState(String text, String xpath, String name, Map<String, String> attributes, 
			Map<String, String> css_map, String screenshot_url, int x_location, int y_location, int width, int height,
			String inner_html, String screenshot_checksum, boolean displayed, String outer_html){
		super();
		assert attributes != null;
		assert css_map != null;
		assert xpath != null;
		assert name != null;
		assert screenshot_url != null;
		
		setType("ElementState");
		setName(name);
		setXpath(xpath);
		setAttributes(attributes);
		setScreenshotUrl(screenshot_url);
		setScreenshotChecksum(screenshot_checksum);
		setText(text);
		setCssValues(css_map);
		setXLocation(x_location);
		setYLocation(y_location);
		setWidth(width);
		setHeight(height);
		setInnerHtml(inner_html);
		setOuterHtml(outer_html);
		setCssSelector("");
		setTemplate(BrowserService.extractTemplate(getOuterHtml()));
		setRules(new HashSet<>());
		setKey(generateKey());
		setClassification(ElementClassification.CHILD);
	}
	
	/**
	 * 
	 * @param text
	 * @param xpath
	 * @param name
	 * @param attributes
	 * @param css_map
	 * @param outer_html TODO
	 * @pre xpath != null
	 * @pre name != null
	 * @pre screenshot_url != null
	 * @pre !screenshot_url.isEmpty()
	 * @pre outer_html != null;
	 * @pre assert !outer_html.isEmpty()
	 */
	public ElementState(String text, String xpath, String name, Map<String, String> attributes, Map<String, String> css_map, 
						String screenshot_url, String checksum, int x_location, int y_location, int width, int height,
						String inner_html, ElementClassification classification, boolean displayed, String outer_html){
		assert name != null;
		assert xpath != null;
		assert outer_html != null;
		assert !outer_html.isEmpty();
		
		setType("ElementState");
		setName(name);
		setXpath(xpath);
		setAttributes(attributes);
		setScreenshotUrl(screenshot_url);
		setText(text);
		setCssValues(css_map);
		setScreenshotChecksum(checksum);
		setXLocation(x_location);
		setYLocation(y_location);
		setWidth(width);
		setHeight(height);
		setInnerHtml(inner_html);
		setOuterHtml(outer_html);
		setCssSelector("");
		setTemplate(BrowserService.extractTemplate(getOuterHtml()));
		setRules(new HashSet<>());
		setClassification(classification);
		setKey(generateKey());
	}
	
	/**
	 * Print Attributes for this element in a prettyish format
	 */
	public void printAttributes(){
		System.out.print("+++++++++++++++++++++++++++++++++++++++");
		for(String attribute : this.attributes.keySet()){
			System.out.print(attribute + " : ");
			System.out.print( attributes.get(attribute) + " ");
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
		for(String propertyName : css_values.keySet()){
			if(propertyName.contains("-moz-") || propertyName.contains("-webkit-") || propertyName.contains("-o-") || propertyName.contains("-ms-")){
				continue;
			}
			if(!css_values.get(propertyName).equals(elem.getCssValues().get(propertyName))){
				return false;
			}
		}
		return true;
	}
	
	/** GETTERS AND SETTERS  **/
		
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
		return css_values;
	}

	public void setCssValues(Map<String, String> css_values) {
		this.css_values = css_values;
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

	public void setAttributes(Map<String, String> attribute_persist_list) {
		this.attributes = attribute_persist_list;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public String getAttribute(String attr_name){
		//get id for element
		for(String tag_attr : this.attributes.keySet()){
			if(tag_attr.equalsIgnoreCase(attr_name)){
				return this.attributes.get(tag_attr);
			}
		}
		
		return null;
	}
	

	public void addAttribute(String attribute, String values) {
		this.attributes.put(attribute, values);
	}
	
	public String getScreenshotUrl() {
		return this.screenshot_url;
	}

	public void setScreenshotUrl(String screenshot_url) {
		this.screenshot_url = screenshot_url;
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
		for(String style : getCssValues().keySet()) {
			key += getCssValues().get(style);
			
		}
		return "elementstate::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getTemplate()+this.getXpath());
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
		return this.getKey().equals(that.getKey());
	}


	public ElementState clone() {
		ElementState page_elem = new ElementState();
		page_elem.setAttributes(this.getAttributes());
		page_elem.setCssValues(this.getCssValues());
		page_elem.setKey(this.getKey());
		page_elem.setName(this.getName());
		page_elem.setScreenshotUrl(this.getScreenshotUrl());
		page_elem.setScreenshotChecksum(this.getScreenshotChecksum());
		page_elem.setText(this.getText());
		page_elem.setType(this.getType());
		page_elem.setXpath(this.getXpath());
		page_elem.setYLocation(this.getYLocation());
		page_elem.setXLocation(this.getXLocation());
		page_elem.setWidth(this.getWidth());
		page_elem.setHeight(this.getHeight());
		page_elem.setOuterHtml(this.getOuterHtml());
		page_elem.setTemplate(this.getTemplate());
		
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
        return this.getKey().compareTo(o.getKey());
		/*
		 if(this.getYLocation() == o.getYLocation())
             return 0;
         return this.getYLocation() < o.getYLocation() ? -1 : 1;
         */
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

	@Deprecated
	public boolean isPartOfForm() {
		return part_of_form;
	}

	@Deprecated
	public void setIsPartOfForm(boolean is_part_of_form) {
		this.part_of_form = is_part_of_form;
	}

	public boolean isLeaf() {
		return getClassification().equals(ElementClassification.CHILD);
	}

	public ElementClassification getClassification() {
		return ElementClassification.create(classification);
	}

	public void setClassification(ElementClassification classification) {
		this.classification = classification.toString();
	}
	
	public List<ElementState> getChildElements() {
		return child_elements;
	}

	public void setChildElements(List<ElementState> child_elements) {
		this.child_elements = child_elements;
	}
	
	public void addChildElement(ElementState child_element) {
		this.child_elements.add(child_element);
	}
}
