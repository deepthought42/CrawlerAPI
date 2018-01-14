package com.qanairy.models;

import java.util.Date;

/**
 * Record detailing a "Discovery" ran by an account.
 */
public class DiscoveryRecord {
	private String key;
	private Date started_at;
	private String browser_name;
	
	public DiscoveryRecord(Date started_timestamp, String browser_name){
		setKey("");
		setStartedAt(started_timestamp);
		setBrowserName(browser_name);
	}
	
	public DiscoveryRecord(String key, Date started_timestamp, String browser_name){
		
	}

	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public Date getStartedAt() {
		return started_at;
	}
	
	public void setStartedAt(Date started_at) {
		this.started_at = started_at;
	}
	
	public String getBrowserName() {
		return browser_name;
	}
	
	public void setBrowserName(String browser_name) {
		this.browser_name = browser_name;
	}
}
