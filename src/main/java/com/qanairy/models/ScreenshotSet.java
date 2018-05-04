package com.qanairy.models;

public class ScreenshotSet {
	private String key;
	private String browser_name;
	private String full_screenshot;
	private String viewport_screenshot;
	
	public ScreenshotSet(String full, String viewport, String browser_name){
		this.full_screenshot = full;
		this.viewport_screenshot = viewport;
		this.setBrowserName(browser_name);
	}
	
	public ScreenshotSet(String key, String full, String viewport, String browser_name){
		this.key = key;
		this.full_screenshot = full;
		this.viewport_screenshot = viewport;
		this.setBrowserName(browser_name);
	}
	
	public String getFullScreenshot() {
		return full_screenshot;
	}

	public void setFullScreenshot(String full_screenshot) {
		this.full_screenshot = full_screenshot;
	}

	public String getViewportScreenshot() {
		return viewport_screenshot;
	}

	public void setViewportScreenshot(String viewport_screenshot) {
		this.viewport_screenshot = viewport_screenshot;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getBrowserName() {
		return browser_name;
	}

	public void setBrowserName(String browser_name) {
		this.browser_name = browser_name;
	}
}
