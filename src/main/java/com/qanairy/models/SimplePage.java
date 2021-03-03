package com.qanairy.models;

/**
 * A simplified data set for page consisting of full page and viewport screenshots, url and the height and width
 *  of the full page screenshot
 *
 */
public class SimplePage {
	private String url;
	private String screenshot_url;
	private String full_page_screenshot_url;
	private long width;
	private long height;
	
	public SimplePage(String url, String screenshot_url, String full_page_screenshot_url, long width, long height) {
		setUrl(url);
		setScreenshotUrl(screenshot_url);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setWidth(width);
		setHeight(height);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getScreenshotUrl() {
		return screenshot_url;
	}

	public void setScreenshotUrl(String screenshot_url) {
		this.screenshot_url = screenshot_url;
	}

	public String getFullPageScreenshotUrl() {
		return full_page_screenshot_url;
	}

	public void setFullPageScreenshotUrl(String full_page_screenshot_url) {
		this.full_page_screenshot_url = full_page_screenshot_url;
	}

	public long getWidth() {
		return width;
	}

	public void setWidth(long width) {
		this.width = width;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}
}
