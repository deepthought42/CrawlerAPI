package com.qanairy.models;

import java.util.List;

public class PageTransition implements PathObject {
	private List<String> screenshot_urls;
	private List<PageState> page_states;
	private String key;
	private String type;
	
	
	public List<String> getScreenshotUrlList() {
		return screenshot_urls;
	}

	/**
	 * 
	 * @param screenshot_urls
	 * 
	 * @pre screenshot_urls is in order of occurance
	 */
	public void setScreenshotUrlList(List<String> screenshot_urls) {
		this.screenshot_urls = screenshot_urls;
	}
	
	public List<PageState> getPageStates() {
		return page_states;
	}

	public void setPageStates(List<PageState> page_states) {
		this.page_states = page_states;
	}

	
	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}
}
