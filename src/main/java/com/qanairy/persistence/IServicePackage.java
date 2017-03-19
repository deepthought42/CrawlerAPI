package com.qanairy.persistence;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Graph DB implementation for various {@link ServicePackage}s offered by the system 
 */
@TypeValue(value="ServicePackage")
public interface IServicePackage {

	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("name")
	public String getName();
	
	@Property("name")
	public void setName(String name);
	
	@Property("price")
	public int getPrice();
	
	@Property("price")
	public void setPrice(int price);
	
	@Property("max_users")
	public int getMaxUsers();
	
	@Property("max_users")
	public void setMaxUsers(int max_user_cnt);
}
