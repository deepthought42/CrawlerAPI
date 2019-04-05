package com.minion.browsing.element;

import com.qanairy.models.PageElementState;

/**
 * Defines an error element by tag and the string contained within
 */
public class ErrorField {
	private PageElementState tag;
	private String error;
	
	public PageElementState getTag() {
		return tag;
	}
	
	public void setTag(PageElementState tag) {
		this.tag = tag;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
}
