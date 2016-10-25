package com.minion.persistence;

import com.tinkerpop.frames.Property;

public interface IAttribute extends IPathObject{
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("name")
	public String getName();
	
	@Property("name")
	public void setName(String name);
	
	@Property("vals")
	public String[] getVals();
	
	@Property("vals")
	public void setVals(String[] vals);
}
