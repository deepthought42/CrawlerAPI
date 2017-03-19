package com.qanairy.persistence;

import java.util.List;

import com.qanairy.models.Test;
import com.tinkerpop.frames.Property;

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
	public void setKey(String group);

	@Property("description")
	public void setDescription(String description);
	
	@Property("description")
	public String getDescription();
	
	@Property("")
	public void setTests();
	
	@Property("")
	public List<Test> getTests();
}
