package com.qanairy.dto;

import com.qanairy.models.ElementState;

/**
 * Data transfer object for {@link ElementState} object that stores data in a format for browser extension
 */
public class ElementStateDto {

	private String key;
	private String xpath;
	
	public ElementStateDto(ElementState elem){
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
