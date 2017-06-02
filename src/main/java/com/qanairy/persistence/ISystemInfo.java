package com.qanairy.persistence;

import com.tinkerpop.frames.Property;

public interface ISystemInfo {
	@Property("browser_count")
	public void setBrowserCount(int count);
	
	@Property("browser_count")
	public int getBrowserCount();
	
	
	public int incrementBrowserCount();
	
	public int decrementBrowserCount();

	@Property("key")
	public void setKey(String key);
	
	@Property("key")
	public String getKey();
}
