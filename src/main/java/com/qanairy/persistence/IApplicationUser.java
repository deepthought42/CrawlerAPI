package com.qanairy.persistence;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

/**
 * 
 */
public interface IApplicationUser {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("username")
	public String getUsername();
	
	@Property("username")
	public void setUsername(String name);
	
	@Property("password")
	public String getPassword();

	@Property("password")
	public void setPassword(String password);
	
	@Adjacency(direction=Direction.IN, label="has_user")
	public Iterable<IAccount> getAccounts();
	
	@Adjacency(direction=Direction.IN, label="has_user")
	public void addAccount(IAccount account);
}
