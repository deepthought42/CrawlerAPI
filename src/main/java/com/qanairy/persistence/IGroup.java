package com.qanairy.persistence;

import java.util.Iterator;
import java.util.List;

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
	
	@Property("test_group")
	public void setTests(List<ITest> tests);
	
	@Property("test_group")
	public Iterable<ITest> getTests();
	
	@Property("test_group")
	public void addTest(ITest test);
}
