package com.qanairy.persistence;

import java.util.Iterator;
import java.util.List;

import com.qanairy.models.Group;
import com.qanairy.models.Test;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

/**
 * Represents an {@link Domain} record in OrientDB database
 */
public interface IDomain {
	@Property("key")
	String getKey();
	
	@Property("key")
	void setKey(String key);

	@Property("url")
	String getUrl();
	
	@Property("url")
	void setUrl(String url);

	@Adjacency(label="contains_test")
	Iterator<ITest> getTests();

	@Adjacency(label="contains_test")
	void setTests(List<ITest> tests);

	@Adjacency(label="group")
	Iterator<IGroup> getGroups();

	@Adjacency(label="group")
	void setGroups(List<IGroup> groups);
	
	@Adjacency(direction=Direction.IN, label="has_domain")
	Iterator<IAccount> getAccounts();

	@Adjacency(direction=Direction.IN, label="group")
	void setAccounts(List<IAccount> groups);
}
