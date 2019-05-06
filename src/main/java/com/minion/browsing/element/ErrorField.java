package com.minion.browsing.element;

import com.qanairy.models.ElementState;

/**
 * Defines an error element by tag and the string contained within
 */
public class ErrorField {
	private ElementState tag;
	private String error;
	
	public ElementState getTag() {
		return tag;
	}
	
	public void setTag(ElementState tag) {
		this.tag = tag;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
}
