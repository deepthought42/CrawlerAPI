package com.qanairy.persistence;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Property;

public abstract class TestUser extends AbstractVertexFrame implements Persistable{
	
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);
	
	@Property("username")
	public abstract String getUsername();
	
	@Property("username")
	public abstract void setUsername(String username);
	
	@Property("password")
	public abstract String getPassword();
	
	@Property("password")
	public abstract void setPassword(String password);

	@Property("role")
	public abstract String getRole();

	@Property("role")
	public abstract void setRole(String role);
}
