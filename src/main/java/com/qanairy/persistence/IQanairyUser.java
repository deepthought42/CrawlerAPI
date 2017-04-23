package com.qanairy.persistence;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
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
	
	@Adjacency(direction=Direction.IN, label="has_user")
	public Iterable<IAccount> getAccounts();
	
	@Adjacency(direction=Direction.IN, label="has_user")
	public void addAccount(IAccount account);
}
