package com.qanairy.persistence;

import java.util.Date;
import java.util.List;

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

	@Property("logo_url")
	String getLogoUrl();
	
	@Property("logo_url")
	void setLogoUrl(String logo_url);
	
	@Property("protocol")
	String getProtocol();
	
	@Property("protocol")
	void setProtocol(String protocol);

	@Property("discovery_started_at")
	Date getDiscoveryStartTime();

	@Property("discovery_started_at")
	void setDiscoveryStartTime(Date timestamp);
	
	@Property("last_discovery_path_ran_at")
	Date getLastDiscoveryPathRanAt();

	@Property("last_discovery_path_ran_at")
	void setLastDiscoveryPathRanAt(Date timestamp);

	@Adjacency(label="contains_test")
	Iterable<ITest> getTests();

	@Adjacency(label="contains_test")
	void setTests(List<ITest> tests);

	@Adjacency(label="contains_test")
	void addTest(ITest test);
	
	@Adjacency(label="group")
	Iterable<IGroup> getGroups();

	@Adjacency(label="group")
	void setGroups(List<IGroup> groups);
	
	@Adjacency(direction=Direction.IN, label="has_domain")
	Iterable<IAccount> getAccounts();

	@Adjacency(direction=Direction.IN, label="has_domain")
	void setAccounts(List<IAccount> accounts);

	@Adjacency(direction=Direction.IN, label="has_domain")
	void addAccount(IAccount account);
	
	@Adjacency(label="has_test_user")
	Iterable<ITestUser> getTestUsers();

	@Adjacency(label="has_test_user")
	void setTestUsers(List<ITestUser> test_users);

	@Adjacency(label="has_test_user")
	void addTestUser(ITestUser test_user);
}
