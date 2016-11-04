package com.minion.browsing.element;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.HtmlTag;
import com.minion.browsing.PageElement;

public class SelectElement extends PageElement {
    private static final Logger log = LoggerFactory.getLogger(SelectElement.class);
	
	private HtmlTag select_element;
	private List<HtmlTag> option_elements;
	private HtmlTag label_element;
	
	public SelectElement(HtmlTag select_tag, List<HtmlTag> options, HtmlTag label){
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

	public List<HtmlTag> getOptionElements() {
		return option_elements;
	}

	public void setOptionElements(List<HtmlTag> optionElements) {
		this.option_elements = optionElements;
	}

	public HtmlTag getLabelElement() {
		return label_element;
	}

	public void setLabelElement(HtmlTag labelElement) {
		this.label_element = labelElement;
	}
}
