package com.qanairy.persistence;

import com.tinkerpop.frames.Property;

/**
 * Defines user information that can be used during testing
 */
public interface ITestUser {
	
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("username")
	public String getUsername();
	
	@Property("username")
	public void setUsername(String username);
	
	@Property("password")
	public String getPassword();
	
	@Property("password")
	public void setPassword(String password);
}
