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
	
	@Property("last_path_ran_at")
	Date getLastPathRan();
	
	@Property("last_path_ran_at")
	void setLastPathRan(Date last_path_ran);
	
	@Property("test_cnt")
	int getTestCount();
	
	@Property("test_cnt")
	void setTestCount(int cnt);
	
	@Property("examined_path_cnt")
	int getExaminedPathCount();
	
	@Property("examined_path_cnt")
	void setExaminedPathCount(int path_cnt);

	@Property("total_path_cnt")
	int getTotalPathCount();
	
	@Property("total_path_cnt")
	void setTotalPathCount(int path_cnt);
	
	
}
