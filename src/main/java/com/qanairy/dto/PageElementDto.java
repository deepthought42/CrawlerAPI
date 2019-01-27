package com.qanairy.dto;

import com.qanairy.models.PageElement;

/**
 * Data transfer object for {@link PageElement} object that stores data in a format for browser extension
 */
public class PageElementDto {

	private String key;
	private String xpath;
	
	public PageElementDto(PageElement elem){
		setKey(elem.getKey());
		setXpath(elem.getXpath());
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getXpath() {
		return xpath;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}
}
