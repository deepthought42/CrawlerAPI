package com.qanairy.persistence;

import java.util.Iterator;
import java.util.List;

import com.qanairy.models.ChildModelTempDemo;
import com.tinkerpop.frames.Adjacency;
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
	
	
	/**
	 * Gets the correctness value of the test
	 * 
	 * @return Correctness value. Null indicates value is unset.
	 */
	@Adjacency(label="belongs_to")
	public Iterator<IChildModelTempDemoDao> getGroups();

	/**
	 * Adds a record to this test connecting it via edge with label "has"
	 */
	@Adjacency(label="belongs_to")
	public void addGroup(IChildModelTempDemoDao group);
}
