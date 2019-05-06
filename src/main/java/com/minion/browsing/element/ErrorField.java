package com.minion.browsing.element;

import com.qanairy.models.PageElement;

/**
 * Defines an error element by tag and the string contained within
 */
public class ErrorField {
	private PageElement tag;
	private String error;
	
	public PageElement getTag() {
		return tag;
	}
	
	public void setTag(PageElement tag) {
		this.tag = tag;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
}
