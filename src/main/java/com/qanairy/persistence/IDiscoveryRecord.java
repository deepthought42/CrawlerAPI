package com.qanairy.persistence;

import java.util.Date;

import com.tinkerpop.frames.Property;

public interface IDiscoveryRecord {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("date")
	public Date getDate();
	
	@Property("date")
	public void setDate(Date date);
}
