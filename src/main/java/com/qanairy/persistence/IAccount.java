package com.qanairy.persistence;

import java.util.List;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface IAccount {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("org_name")
	public String getOrgName();
	
	@Property("org_name")
	public void setOrgName(String name);
	
	@Property("service_package")
	public String getServicePackage();

	@Property("service_package")
	public void setServicePackage(String service_package);

	@Property("customer_token")
	public String getCustomerToken();

	@Property("customer_token")
	public void setCustomerToken(String customer_token);

	@Property("subscription_token")
	public String getSubscriptionToken();

	@Property("subscription_token")
	public void setSubscriptionToken(String subscription_token);
	
	
	@Property("last_domain")
	public void setLastDomain(String domain_url);
	
	@Property("last_domain")
	public String getLastDomain();
	
	@Adjacency(direction=Direction.OUT, label="has_user")
	public Iterable<IQanairyUser> getUsers();
	
	@Adjacency(direction=Direction.OUT, label="has_user")
	public void addUser(IQanairyUser user);
	
	@Adjacency(direction=Direction.OUT, label="has_user")
	public void setUsers(List<IQanairyUser> user);
	
	@Adjacency(direction=Direction.OUT, label="has_domain")
	public Iterable<IDomain> getDomains();
	
	@Adjacency(direction=Direction.OUT, label="has_domain")
	public void addDomain(IDomain domain);
	
	@Adjacency(direction=Direction.OUT, label="has_domain")
	public void removeDomain(IDomain domain);
	
	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public Iterable<IDiscoveryRecord> getDiscoveryRecords();

	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public void setDiscoveryRecords(List<IDiscoveryRecord> discovery_records);

	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public void addDiscoveryRecord(IDiscoveryRecord discovery_record);
	
	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public Iterable<ITestRecord> getTestRecords();

	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public void setTestRecords(List<ITestRecord> test_records);

	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public void addTestRecord(ITestRecord test_record);
	
}
