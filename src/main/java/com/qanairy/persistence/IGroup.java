package com.qanairy.persistence;



/**
 * Represents a {@link Group} to be stored in orientDB database
 */
public interface IGroup {
	@Property("name")
	public String getName();
	
	@Property("name")
	public void setName(String name);
	
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);

	@Property("description")
	public void setDescription(String description);
	
	@Property("description")
	public String getDescription();
}
