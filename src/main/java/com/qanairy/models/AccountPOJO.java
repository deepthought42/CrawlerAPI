package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.TestRecord;
import com.qanairy.persistence.edges.HasDomain;


/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
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
		setDiscoveryRecords(new ArrayList<DiscoveryRecord>());
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
	public AccountPOJO(String org_name, String customer_token, String subscription_token, 
					List<Domain> domains, 
					String last_domain_url, List<DiscoveryRecord> discovery_records,
					List<TestRecord> test_records, List<String> onboarded_steps){
		
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
			
	public String getOrgName() {
		return org_name;
	}

	public void setOrgName(String org_name) {
		this.org_name = org_name;
	}

	public String getCustomerToken() {
		return customer_token;
	}

	public void setCustomerToken(String customer_token) {
		this.customer_token = customer_token;
	}
	
	public String getSubscriptionToken() {
		return subscription_token;
	}

	public void setSubscriptionToken(String subscription_token) {
		this.subscription_token = subscription_token;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = generateKey();
	}

	public void setLastDomain(String domain_url) {
		this.last_domain_url = domain_url;
	}
	
	public String getLastDomain(){
		return this.last_domain_url;
	}

	public List<String> getOnboardedSteps() {
		return onboarded_steps;
	}

	public void setOnboardedSteps(List<String> onboarded_steps) {
		if(onboarded_steps == null){
			this.onboarded_steps = new ArrayList<String>();
		}
		else{
			this.onboarded_steps = onboarded_steps;
		}
	}
	
	public void addOnboardingStep(String step_name) {
		if(!this.onboarded_steps.contains(step_name)){
			this.onboarded_steps.add(step_name);
		}
	}
	
	@Override
	public List<Domain> getDomains(){
		return this.domains;
	}
	
	@Override
	public void setDomains(List<Domain> domains){
		this.domains = domains;
	}
	
	@Override
	public void addDomain(Domain domain) {
		this.domains.add(domain);
	}
	
	@Override
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
	
	@Override
	public List<DiscoveryRecord> getDiscoveryRecords() {
		return discovery_records;
	}

	@Override
	public void setDiscoveryRecords(List<DiscoveryRecord> discovery_records) {
		this.discovery_records = discovery_records;
	}
	
	@Override
	public void addDiscoveryRecord(DiscoveryRecord record){
		this.discovery_records.add(record);
	}

	@Override
	public List<TestRecord> getTestRecords() {
		return test_records;
	}

	@Override
	public void setTestRecords(List<TestRecord> test_records) {
		this.test_records = test_records;
	}

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
