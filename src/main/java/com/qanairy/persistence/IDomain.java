package com.qanairy.persistence;

import java.util.List;





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
	
	@Property("discovery_browser")
	void setDiscoveryBrowserName(String browser_name);

	@Property("discovery_browser")
	String getDiscoveryBrowserName();
	
	@Property("test_cnt")
	int getDiscoveryTestCount();

	@Property("test_cnt")
	void setDiscoveryTestCount(int count);
	
	/* ADJACENCIES */
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
