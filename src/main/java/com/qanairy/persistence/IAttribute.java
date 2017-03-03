package com.qanairy.persistence;

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
	public String getName();
	
	@Property("name")
	public void setName(String name);
	
	@Property("vals")
	public String[] getVals();
	
	@Property("vals")
	public void setVals(String[] vals);
}
