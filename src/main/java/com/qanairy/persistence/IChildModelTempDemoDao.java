package com.qanairy.persistence;

import com.tinkerpop.frames.Property;

public interface IChildModelTempDemoDao {

	@Property("key")
	public void setKey(String key);
	
	@Property("key")
	public String getKey();
}
