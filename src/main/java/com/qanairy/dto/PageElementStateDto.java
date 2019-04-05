package com.qanairy.dto;

import com.qanairy.models.PageElementState;

/**
 * Data transfer object for {@link PageElementState} object that stores data in a format for browser extension
 */
public class PageElementStateDto {

	private String key;
	private String xpath;
	
	public PageElementStateDto(PageElementState elem){
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
