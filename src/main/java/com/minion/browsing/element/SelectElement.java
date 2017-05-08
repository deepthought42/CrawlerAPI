package com.minion.browsing.element;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;


import com.qanairy.models.Action;
import com.qanairy.models.PageElement;

public class SelectElement implements PageElement {
    @SuppressWarnings("unused")
    private static Logger log = LogManager.getLogger(SelectElement.class);
	
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
}
