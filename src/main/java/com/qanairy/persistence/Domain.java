package com.qanairy.persistence;

import java.util.List;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.Property;

/**
 * Represents an {@link Domain} record in OrientDB database
 */
public abstract class Domain extends AbstractVertexFrame implements Persistable{

	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);

	@Property("url")
	public abstract String getUrl();
	
	@Property("url")
	public abstract void setUrl(String url);

	@Property("logo_url")
	public abstract String getLogoUrl();
	
	@Property("logo_url")
	public abstract void setLogoUrl(String logo_url);
	
	@Property("protocol")
	public abstract String getProtocol();
	
	@Property("protocol")
	public abstract void setProtocol(String protocol);
	
	@Property("discovery_browser")
	public abstract void setDiscoveryBrowserName(String browser_name);

	@Property("discovery_browser")
	public abstract String getDiscoveryBrowserName();
	
	@Property("test_cnt")
	public abstract int getTestCount();

	@Property("test_cnt")
	public abstract void setTestCount(int count);
	
	/* ADJACENCIES */
	@Adjacency(label="contains_test")
	public abstract List<Test> getTests();

	@Adjacency(label="contains_test")
	public abstract void setTests(List<Test> tests);

	@Adjacency(label="contains_test")
	public abstract void addTest(Test test);
	
	@Adjacency(label="has_test_user")
	public abstract List<TestUser> getTestUsers();

	@Adjacency(label="has_test_user")
	public abstract void removeTestUser(TestUser test_users);

	@Adjacency(label="has_test_user")
	public abstract void addTestUser(TestUser test_user);
}
