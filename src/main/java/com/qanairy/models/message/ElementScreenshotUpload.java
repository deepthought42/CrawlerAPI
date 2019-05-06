package com.qanairy.models.message;

import java.awt.image.BufferedImage;
import java.net.URL;

public class ElementScreenshotUpload{
	
	private BufferedImage screenshot;
	private URL page_url;
	private String page_elem_key;
	private String browser_name;

	public ElementScreenshotUpload(BufferedImage screenshot, URL url, String page_elem_key, String browser_name){
		this.screenshot = screenshot;
		this.page_elem_key = page_elem_key;
		this.page_url = url;
		this.browser_name = browser_name;
	}
	
	public BufferedImage getScreenshot() {
		return screenshot;
	}

	public URL getPageUrl() {
		return page_url;
	}

	public String getPageElemKey() {
		return page_elem_key;
	}

	public String getBrowserName() {
		return browser_name;
	}
}
