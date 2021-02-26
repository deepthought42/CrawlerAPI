package com.qanairy.models;

import java.util.Set;

import com.qanairy.models.audit.Audit;

public class PageStateAudits {
	private String url;
	private String screenshot_url;
	private String full_page_screenshot_url;
	private Set<Audit> audits;
	
	public PageStateAudits(String url, String screenshot_url, String full_page_screenshot_url, Set<Audit> audits) {
		setUrl(url);
		setScreenshotUrl(screenshot_url);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setAudits(audits);
	}

	public Set<Audit> getAudits() {
		return audits;
	}

	public void setAudits(Set<Audit> audits) {
		this.audits = audits;
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
}
