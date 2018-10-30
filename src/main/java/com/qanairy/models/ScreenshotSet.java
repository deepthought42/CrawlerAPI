package com.qanairy.models;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * 
 */
@NodeEntity
public class ScreenshotSet implements Persistable {
	
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String browser_name;
	private String viewport_screenshot;
	
	public ScreenshotSet(){}
	
	public ScreenshotSet(String viewport, String browser_name){
		setViewportScreenshot(viewport);
		setBrowser(browser_name);
		setKey(generateKey());
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
		return "screenshot::"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(getViewportScreenshot());
	}
}
