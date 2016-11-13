package com.minion.browsing.element;

import com.minion.browsing.HtmlTag;

public class ErrorField {
	private HtmlTag tag;
	private String error;
	
	public HtmlTag getTag() {
		return tag;
	}
	
	public void setTag(HtmlTag tag) {
		this.tag = tag;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
}
