package com.qanairy.persistence;

import java.util.Date;

import com.tinkerpop.frames.Property;

/**
 * Tinkerpop representation of a {@link DiscoveryRecord} in graph database
 */
public interface IDiscoveryRecord {

	@Property("key")
	String getKey();
	
	@Property("key")
	void setKey(String key);

	@Property("started_at")
	Date getStartTime();

	@Property("started_at")
	void setStartTime(Date timestamp);
			
	@Property("browser_name")
	String getBrowserName();
	
	@Property("browser_name")
	void setBrowserName(String browser_name);
	
	@Property("domain_url")
	String getDomainUrl();
	
	@Property("domain_url")
	void setDomainUrl(String domain_url);
}
