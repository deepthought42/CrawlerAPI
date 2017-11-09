package com.qanairy.models;

/**
 * Encapsulates the html source for a {@link Page}
 *
 */
public class PageSource {
	private String key;
	private String src;
	
	public PageSource(String src){
		this.setKey(null);
		this.src = src;
	}
	
	public PageSource(String key, String src){
		this.setKey(key);
		this.src = src;
	}

	public String getSrc() {
		return src;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
