package com.qanairy.persistence;

import com.tinkerpop.frames.Property;

public interface IQanairyUser {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("email")
	public String getEmail();
	
	@Property("email")
	public void setEmail(String email);
}
