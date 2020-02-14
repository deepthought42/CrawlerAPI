package com.minion.browsing.element;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import com.qanairy.models.rules.Rule;

public class SelectElement extends ElementState {
    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(SelectElement.class);

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setType(String type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setKey(String key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String tagName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setText(String text) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getXpath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setXpath(String xpath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, String> getCssValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCssValues(Map<String, String> cssMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getScreenshotUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setScreenshotUrl(String cssMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<Attribute> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttributes(Set<Attribute> attributes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAttribute(Attribute attribute) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRules(Set<Rule> rules) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addRule(Rule rules) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<Rule> getRules() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	private HtmlTag select_element;
	private Set<HtmlTag> option_elements;
	private HtmlTag label_element;
	
	public SelectElement(HtmlTag select_tag, Set<HtmlTag> options, HtmlTag label){
		assert select_tag.getName().equals("select");
		assert label.getName().equals("label");
		
		this.select_element = select_tag;
		this.option_elements = options;
		this.label_element = label;
	}
	
	public HtmlTag getSelectElement() {
		return select_element;
	}
	
	public void setSelectElement(HtmlTag selectElement) {
		this.select_element = selectElement;
	}

	public Set<HtmlTag> getOptionElements() {
		return option_elements;
	}

	public void setOptionElements(Set<HtmlTag> optionElements) {
		this.option_elements = optionElements;
	}

	public HtmlTag getLabelElement() {
		return label_element;
	}

	public void setLabelElement(HtmlTag labelElement) {
		this.label_element = labelElement;
	}

	@Override
	public String getScreenshot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setScreenshot(String screenshot) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean performAction(Action action, String value, WebDriver driver) throws UnreachableBrowserException {
		// TODO Auto-generated method stub
		return false;
	}
	*/
}
