package com.qanairy.models;

import com.qanairy.persistence.ScreenshotSet;

/**
 * 
 */
public class ScreenshotSetPOJO extends ScreenshotSet {
	private String key;
	private String browser_name;
	private String full_screenshot;
	private String viewport_screenshot;
	
	public ScreenshotSetPOJO(String full, String viewport, String browser_name){
		this.full_screenshot = full;
		this.viewport_screenshot = viewport;
		this.setBrowser(browser_name);
		setKey(generateKey());
	}
	
	public ScreenshotSetPOJO(String key, String full, String viewport, String browser_name){
		this.key = key;
		this.full_screenshot = full;
		this.viewport_screenshot = viewport;
		this.setBrowser(browser_name);
		setKey(generateKey());
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
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getBrowser() {
		return browser_name;
	}

	public void setBrowser(String browser_name) {
		this.browser_name = browser_name;
	}
	
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(getFullScreenshot())+":"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(getViewportScreenshot());
	}
}
