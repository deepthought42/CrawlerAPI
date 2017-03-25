package com.qanairy.persistence;

import java.util.List;

import com.tinkerpop.frames.Property;

/**
 * Represents {@link Attribute} to be stored in OrientDB database
 */
public interface IAttribute extends IPathObject{
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("name")
	public String records();
	
	@Property("name")
	public void setName(String name);
	
	@Property("name")
	public String getName();
	
	@Property("vals")
	public List<String> getVals();
	
	@Property("vals")
	public void setVals(List<String> vals);
}
