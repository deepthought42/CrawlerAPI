package com.qanairy.persistence;

import java.util.List;
import org.apache.tinkerpop.gremlin.structure.Direction;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.Property;

/**
 * 
 */
public abstract class Account extends AbstractVertexFrame implements Persistable{
	
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);
	
	@Property("org_name")
	public abstract String getOrgName();
	
	@Property("org_name")
	public abstract void setOrgName(String name);

	@Property("customer_token")
	public abstract String getCustomerToken();

	@Property("customer_token")
	public abstract void setCustomerToken(String customer_token);

	@Property("subscription_token")
	public abstract String getSubscriptionToken();

	@Property("subscription_token")
	public abstract void setSubscriptionToken(String subscription_token);
	
	@Property("last_domain")
	public abstract void setLastDomain(String domain_url);
	
	@Property("last_domain")
	public abstract String getLastDomain();
	
	@Property("onboarded_steps")
	public abstract List<String> getOnboardedSteps();
	
	@Property("onboarded_steps")
	public abstract void setOnboardedSteps(List<String> onboarded_steps);
	
	@Adjacency(direction=Direction.OUT, label="has_domain")
	public abstract List<Domain> getDomains();
	
	@Adjacency(direction=Direction.OUT, label="has_domain")
	public abstract boolean addDomain(Domain domain);
	
	@Adjacency(direction=Direction.OUT, label="has_domain")
	public abstract void removeDomain(Domain domain);
	
	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public abstract List<DiscoveryRecord> getDiscoveryRecords();

	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public abstract void setDiscoveryRecords(List<DiscoveryRecord> discovery_records);

	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public abstract void addDiscoveryRecord(DiscoveryRecord discovery_record);
	
	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public abstract List<TestRecord> getTestRecords();

	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public abstract void setTestRecords(List<TestRecord> test_records);

	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public abstract void addTestRecord(TestRecord test_record);
}
