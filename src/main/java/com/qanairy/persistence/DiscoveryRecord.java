package com.qanairy.persistence;

import java.util.Date;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Property;

/**
 * Representation of a {@link DiscoveryRecord} in graph database
 */
public abstract class DiscoveryRecord extends AbstractVertexFrame {

	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);

	@Property("started_at")
	public abstract Date getStartTime();

	@Property("started_at")
	public abstract void setStartTime(Date timestamp);
			
	@Property("browser_name")
	public abstract String getBrowserName();
	
	@Property("browser_name")
	public abstract void setBrowserName(String browser_name);
	
	@Property("domain_url")
	public abstract String getDomainUrl();
	
	@Property("domain_url")
	public abstract void setDomainUrl(String domain_url);
	
	@Property("last_path_ran_at")
	public abstract Date getLastPathRanAt();
	
	@Property("last_path_ran_at")
	public abstract void setLastPathRanAt(Date last_path_ran);
	
	@Property("test_cnt")
	public abstract int getTestCount();
	
	@Property("test_cnt")
	public abstract void setTestCount(int cnt);
	
	@Property("examined_path_cnt")
	public abstract int getExaminedPathCount();
	
	@Property("examined_path_cnt")
	public abstract void setExaminedPathCount(int path_cnt);

	@Property("total_path_cnt")
	public abstract int getTotalPathCount();
	
	@Property("total_path_cnt")
	public abstract void setTotalPathCount(int path_cnt);
}
