package com.qanairy.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.enums.ElementClassification;


/**
 * Contains all the pertinent information for an element on a page. A ElementState
 *  may be a Parent and/or child of another ElementState. This heirarchy is not
 *  maintained by ElementState though. 
 */
public class ElementState extends LookseeObject implements Comparable<ElementState> {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementState.class);

	private String name;
	private String text;
	private String css_selector;
	private String outer_html;
	private String xpath;
	private String classification;
	private String screenshot_url;
	private int x_location;
	private int y_location;
	private int width;
	private int height;
	
	private boolean visible;

	@Properties
	private Map<String, String> rendered_css_values = new HashMap<>();
	
	@Properties
	private Map<String, String> attributes = new HashMap<>();
	
	@Relationship(type = "HAS_CHILD", direction = Relationship.OUTGOING)
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
		
		setName(name);
		setAttributes(attributes);
		setScreenshotUrl(screenshot_url);
		setText(text);
		setRenderedCssValues(css_map);
		setXLocation(x_location);
		setYLocation(y_location);
		setWidth(width);
		setHeight(height);
		setOuterHtml(outer_html);
		setCssSelector("");
		setClassification(ElementClassification.LEAF);
		setXpath(xpath);
		setKey(generateKey());
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
		
		setName(name);
		setAttributes(attributes);
		setScreenshotUrl(screenshot_url);
		setText(text);
		setRenderedCssValues(css_map);
		setXLocation(x_location);
		setYLocation(y_location);
		setWidth(width);
		setHeight(height);
		setOuterHtml(outer_html);
		setCssSelector("");
		setClassification(classification);
		setXpath(xpath);
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
		for(String propertyName : rendered_css_values.keySet()){
			if(propertyName.contains("-moz-") || propertyName.contains("-webkit-") || propertyName.contains("-o-") || propertyName.contains("-ms-")){
				continue;
			}
			if(!rendered_css_values.get(propertyName).equals(elem.getRenderedCssValues().get(propertyName))){
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

	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey() {
		String key = "";
		List<String> properties = new ArrayList<>(getRenderedCssValues().keySet());
		Collections.sort(properties);
		for(String style : properties) {
			key += getRenderedCssValues().get(style);
		}
		return "elementstate::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(key+this.getOuterHtml());
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
		page_elem.setRenderedCssValues(this.getRenderedCssValues());
		page_elem.setKey(this.getKey());
		page_elem.setName(this.getName());
		page_elem.setScreenshotUrl(this.getScreenshotUrl());
		page_elem.setText(this.getText());
		page_elem.setYLocation(this.getYLocation());
		page_elem.setXLocation(this.getXLocation());
		page_elem.setWidth(this.getWidth());
		page_elem.setHeight(this.getHeight());
		page_elem.setOuterHtml(this.getOuterHtml());
		
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

	public boolean isLeaf() {
		return getClassification().equals(ElementClassification.LEAF);
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

	public Map<String, String> getRenderedCssValues() {
		return rendered_css_values;
	}

	public void setRenderedCssValues(Map<String, String> rendered_css_values) {
		this.rendered_css_values.putAll(rendered_css_values);
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getXpath() {
		return xpath;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}
}
