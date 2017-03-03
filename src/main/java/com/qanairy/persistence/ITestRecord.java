package com.qanairy.persistence;

import java.util.Date;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

/*
 * 
 */
public interface ITestRecord extends IPersistable<ITestRecord> {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Adjacency(label="test")
	public ITest getTest();
	
	@Adjacency(label="test")
	public void setTest(ITest test);
	
	@Property("ran_at")
	public Date getRanAt();
	
	@Property("ran_at")
	public void setRanAt(Date date);
	
	@Property("passes")
	public boolean getPasses();
	
	@Property("passes")
	public void setPasses(boolean isPassing);
}
