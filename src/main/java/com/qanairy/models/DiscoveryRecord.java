package com.qanairy.models;

import java.util.Date;

/**
 * Record detailing a "Discovery" ran by an account.
 */
public class DiscoveryRecord {
	private String key;
	private Date started_at;
	private String browser_name;
	private String domain_url;
	
	public DiscoveryRecord(Date started_timestamp, String browser_name, String domain_url){
		assert started_timestamp != null;
		assert browser_name != null;
		assert domain_url != null;
		
		setKey("");
		setStartedAt(started_timestamp);
		setBrowserName(browser_name);
		setDomainUrl(domain_url);
	}
	
	public DiscoveryRecord(String key, Date started_timestamp, String browser_name, String domain_url){
		assert key != null;
		assert started_timestamp != null;
		assert browser_name != null;
		assert domain_url != null;
		
		setKey(key);
		setStartedAt(started_timestamp);
		setBrowserName(browser_name);
		setDomainUrl(domain_url);
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

	public String getDomainUrl() {
		return domain_url;
	}

	public void setDomainUrl(String domain_url) {
		this.domain_url = domain_url;
	}
}
