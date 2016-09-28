package com.minion.persistence;

import com.tinkerpop.frames.Property;

public interface IAction extends IPersistable<IAction> {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("name")
	public String getName();
	
	@Property("name")
	public String setName(String name);
}
