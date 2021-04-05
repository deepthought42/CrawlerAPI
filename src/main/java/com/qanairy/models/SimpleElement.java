package com.qanairy.models;

public class SimpleElement {
	private String key;
	private String screenshot_url;
	private int x_location;
	private int y_location;
	private int width;
	private int height;
	private String css_selector;
	
	public SimpleElement(String key, String screenshot_url, int x, int y, int width, int height, String css_selector) {
		setKey(key);
		setScreenshotUrl(screenshot_url);
		setXLocation(x);
		setYLocation(y);
		setWidth(width);
		setHeight(height);
		setCssSelector(css_selector);
	}
	
	public String getScreenshotUrl() {
		return screenshot_url;
	}
	public void setScreenshotUrl(String screenshot_url) {
		this.screenshot_url = screenshot_url;
	}
	
	public int getXLocation() {
		return x_location;
	}
	public void setXLocation(int x_location) {
		this.x_location = x_location;
	}
	
	public int getYLocation() {
		return y_location;
	}
	public void setYLocation(int y_location) {
		this.y_location = y_location;
	}
	
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getCssSelector() {
		return css_selector;
	}

	public void setCssSelector(String css_selector) {
		this.css_selector = css_selector;
	}	
}
