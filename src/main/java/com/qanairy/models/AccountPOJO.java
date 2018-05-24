package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.structure.Direction;

import com.qanairy.persistence.Account;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.TestRecord;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.annotations.Property;


/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
@GraphElement
public class AccountPOJO extends Account{
	private String key;
	private String org_name;
	private String customer_token;
	private String subscription_token;
	private List<Domain> domains;
	private String last_domain_url;
	private List<DiscoveryRecord> discovery_records;
	private List<TestRecord> test_records;
	private List<String> onboarded_steps;
	
	public AccountPOJO(){}
	
	/**
	 * 
	 * @param org_name
	 * @param customer_token
	 * @param subscription_token
	 * 
	 * @pre users != null
	 */
	public AccountPOJO(String org_name, String customer_token, String subscription_token){
		setOrgName(org_name);
		setCustomerToken(customer_token);
		setSubscriptionToken(subscription_token);
		setDiscoveryRecords(new ArrayList<DiscoveryRecord>());
		setDomains(new ArrayList<Domain>());
		setTestRecords(new ArrayList<TestRecord>());
		setOnboardedSteps(new ArrayList<String>());
		setKey(generateKey());
	}
	
	/**
	 * 
	 * @param org_name
	 * @param payment_acct_num
	 * @param users
	 * 
	 * @pre users != null
	 */
	public AccountPOJO(String org_name, String customer_token, String subscription_token, 
					List<DiscoveryRecord> discovery_records, List<TestRecord> test_records, List<String> onboarded_steps){
		
		setOrgName(org_name);
		setCustomerToken(customer_token);
		setSubscriptionToken(subscription_token);
		setDomains(new ArrayList<Domain>());
		setDiscoveryRecords(discovery_records);
		setTestRecords(test_records);
		setOnboardedSteps(onboarded_steps);
		setKey(generateKey());
	}

	/**
	 * 
	 * @param key
	 * @param org_name
	 * @param payment_acct_num
	 * @param users
	 * @param domains
	 * @param last_domain_url
	 * @param discovery_records
	 */
	public AccountPOJO(String org_name, 
					String customer_token, 
					String subscription_token, 
					List<Domain> domains, 
					String last_domain_url, 
					List<DiscoveryRecord> discovery_records,
					List<TestRecord> test_records, 
					List<String> onboarded_steps){
		
		setOrgName(org_name);
		setCustomerToken(customer_token);
		setSubscriptionToken(subscription_token);
		setDomains(domains);
		setLastDomain(last_domain_url);
		setDiscoveryRecords(discovery_records);
		setTestRecords(test_records);
		setOnboardedSteps(onboarded_steps);
		setKey(generateKey());
	}
		
	@Property("org_name")
	public String getOrgName() {
		return org_name;
	}

	@Property("org_name")
	public void setOrgName(String org_name) {
		this.org_name = org_name;
	}

	@Property("customer_token")
	public String getCustomerToken() {
		return customer_token;
	}

	@Property("customer_token")
	public void setCustomerToken(String customer_token) {
		this.customer_token = customer_token;
	}
	
	@Property("subscription_token")
	public String getSubscriptionToken() {
		return subscription_token;
	}

	@Property("subscription_token")
	public void setSubscriptionToken(String subscription_token) {
		this.subscription_token = subscription_token;
	}
	
	@Property("key")
	public String getKey() {
		return key;
	}

	@Property("key")
	public void setKey(String key) {
		this.key = generateKey();
	}

	@Property("last_domain")
	public void setLastDomain(String domain_url) {
		this.last_domain_url = domain_url;
	}
	
	@Property("last_domain")
	public String getLastDomain(){
		return this.last_domain_url;
	}

	@Property("onboarded_steps")
	public List<String> getOnboardedSteps() {
		return onboarded_steps;
	}

	@Property("onboarded_steps")
	public void setOnboardedSteps(List<String> onboarded_steps) {
		if(onboarded_steps == null){
			this.onboarded_steps = new ArrayList<String>();
		}
		else{
			this.onboarded_steps = onboarded_steps;
		}
	}
	
	@Property("onboarded_steps")
	public void addOnboardingStep(String step_name) {
		if(!this.onboarded_steps.contains(step_name)){
			this.onboarded_steps.add(step_name);
		}
	}
	
	@Adjacency(label="has_domain")
	public List<Domain> getDomains(){
		return this.domains;
	}
	
	@Adjacency(label="has_domain")
	public void setDomains(List<Domain> domains){
		this.domains = domains;
	}
	
	@Adjacency(label="has_domain")
	public void addDomain(Domain domain) {
		this.domains.add(domain);
	}
	
	@Adjacency(label="has_domain")
	public void removeDomain(Domain domain) {
		int idx = -1;
		boolean domain_found = false;
		for(Domain curr_domain : this.domains){
			if(curr_domain.getKey().equals(domain.getKey())){
				domain_found = true;
				break;
			}
			idx++;
		}
		
		if(domain_found){
			this.domains.remove(idx);
		}
	}
	
	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public List<DiscoveryRecord> getDiscoveryRecords() {
		return discovery_records;
	}

	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public void setDiscoveryRecords(List<DiscoveryRecord> discovery_records) {
		this.discovery_records = discovery_records;
	}
	
	@Adjacency(direction=Direction.OUT, label="has_discovery_record")
	public void addDiscoveryRecord(DiscoveryRecord record){
		this.discovery_records.add(record);
	}

	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public List<TestRecord> getTestRecords() {
		return test_records;
	}

	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public void setTestRecords(List<TestRecord> test_records) {
		this.test_records = test_records;
	}

	@Adjacency(direction=Direction.OUT, label="has_test_record")
	public void addTestRecord(TestRecord record) {
		this.test_records.add(record);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return getOrgName();
	}
}
