package com.looksee.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.models.enums.ElementClassification;


/**
 * Contains all the pertinent information for an element on a page. A ElementState
 *  may be a Parent and/or child of another ElementState. This heirarchy is not
 *  maintained by ElementState though. 
 */
public class ElementState extends LookseeObject implements Comparable<ElementState> {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementState.class);

	private String name;
	private String owned_text;
	private String all_text;
	private String css_selector;
	private String outer_html;
	private String xpath;
	private String classification;
	private String screenshot_url;
	private String background_color;
	private String foreground_color;
	private int x_location;
	private int y_location;
	private int width;
	private int height;
	private double text_contrast;
	private double non_text_contrast;
	
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
	 * @param all_text TODO
	 * @param xpath
	 * @param name
	 * @param attributes
	 * @param css_map
	 * @param outer_html TODO
	 * @param css_selector TODO
	 * @param text
	 * @pre xpath != null
	 * @pre name != null
	 * @pre screenshot_url != null
	 * @pre !screenshot_url.isEmpty()
	 * @pre outer_html != null;
	 * @pre !outer_html.isEmpty()
	 */
	public ElementState(String owned_text, 
						String all_text, 
						String xpath, 
						String name, 
						Map<String, String> attributes, 
						Map<String, String> css_map, 
						String screenshot_url, 
						int x_location, 
						int y_location, 
						int width,
						int height, 
						ElementClassification classification, 
						String outer_html, 
						boolean isVisible, 
						String css_selector){
		assert name != null;
		assert xpath != null;
		assert outer_html != null;
		assert !outer_html.isEmpty();
		
		setName(name);
		setAttributes(attributes);
		setScreenshotUrl(screenshot_url);
		setOwnedText(owned_text);
		setAllText(all_text);
		setRenderedCssValues(css_map);
		setXLocation(x_location);
		setYLocation(y_location);
		setWidth(width);
		setHeight(height);
		setOuterHtml(outer_html);
		setCssSelector(css_selector);
		setClassification(classification);
		setXpath(xpath);
		setVisible(isVisible);
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
	
	public String getOwnedText() {
		return owned_text;
	}
	
	public void setOwnedText(String text) {
		this.owned_text = text;
	}
	
	public String getAllText() {
		return all_text;
	}
	
	public void setAllText(String text) {
		this.all_text = text;
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
		/*
		String key = "";
		List<String> properties = new ArrayList<>(getRenderedCssValues().keySet());
		Collections.sort(properties);
		for(String style : properties) {
			key += getRenderedCssValues().get(style);
		}
		*/
		return "elementstate"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(getScreenshotUrl());
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
		page_elem.setOwnedText(this.getOwnedText());
		page_elem.setAllText(this.getAllText());
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

	public double getTextContrast() {
		return text_contrast;
	}

	public void setTextContrast(double text_contrast) {
		this.text_contrast = text_contrast;
	}

	public double getNonTextContrast() {
		return non_text_contrast;
	}

	public void setNonTextContrast(double non_text_contrast) {
		this.non_text_contrast = non_text_contrast;
	}

	public String getBackgroundColor() {
		return background_color;
	}

	public void setBackgroundColor(String background_color) {
		this.background_color = background_color;
	}
	
	public String getForegroundColor() {
		return foreground_color;
	}

	public void setForegroundColor(String foreground_color) {
		this.foreground_color = foreground_color;
	}
}
