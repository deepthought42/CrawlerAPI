package com.qanairy.persistence;

import com.tinkerpop.frames.Property;

/**
 * Represents a {@link Group} to be stored in orientDB database
 */
public interface IGroup {
	@Property("name")
	public String getName();
	
	@Property("name")
	public void setName(String name);
	
	@Property("group")
	public String getGroup();
	
	@Property("group")
	public void setGroup(String group);
}
