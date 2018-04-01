package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
public class Account {
	private String key;
	private String org_name;
	private String service_package;
	private String customer_token;
	private String subscription_token;
	private List<QanairyUser> users;
	private List<Domain> domains;
	private String last_domain_url;
	private List<DiscoveryRecord> discovery_records;
	private List<TestRecord> test_records;
	
	public Account(){}
	
	/**
	 * 
	 * @param org_name
	 * @param service_package
	 * @param payment_acct_num
	 * @param users
	 * 
	 * @pre users != null
	 */
	public Account(String org_name, String service_package, String customer_token, String subscription_token){
		assert users != null;
		
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setCustomerToken(customer_token);
		this.setSubscriptionToken(subscription_token);
		this.setDiscoveryRecords(new ArrayList<DiscoveryRecord>());
		this.setUsers(new ArrayList<QanairyUser>());
		this.setDomains(new ArrayList<Domain>());
		this.setDiscoveryRecords(new ArrayList<DiscoveryRecord>());
		this.setTestRecords(new ArrayList<TestRecord>());
	}
	
	/**
	 * 
	 * @param org_name
	 * @param service_package
	 * @param payment_acct_num
	 * @param users
	 * 
	 * @pre users != null
	 */
	public Account(String org_name, String service_package, String customer_token, String subscription_token, List<QanairyUser> users, 
					List<DiscoveryRecord> discovery_records, List<TestRecord> test_records){
		assert users != null;
		
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setCustomerToken(customer_token);
		this.setSubscriptionToken(subscription_token);
		this.setDomains(new ArrayList<Domain>());
		this.setUsers(users);
		this.setDiscoveryRecords(discovery_records);
		this.setTestRecords(test_records);
	}

	/**
	 * 
	 * @param key
	 * @param org_name
	 * @param service_package
	 * @param payment_acct_num
	 * @param users
	 * @param domains
	 * @param last_domain_url
	 * @param discovery_records
	 */
	public Account(String key, String org_name, String service_package, String customer_token, String subscription_token, 
					List<QanairyUser> users, List<Domain> domains, 
					String last_domain_url, List<DiscoveryRecord> discovery_records,
					List<TestRecord> test_records){
		assert users != null;
		
		this.setKey(key);
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setCustomerToken(customer_token);
		this.setSubscriptionToken(subscription_token);
		this.setUsers(users);
		this.setDomains(domains);
		this.setLastDomain(last_domain_url);
		this.setDiscoveryRecords(discovery_records);
		this.setTestRecords(test_records);
	}
	
	/**
	 * 
	 * @param org_name
	 * @param service_package
	 * @param payment_acct_num
	 * @param users
	 * @param domains
	 * @param last_domain_url
	 * @param discovery_records
	 */
	public Account(String org_name, String service_package, String customer_token, String subscription_token, List<QanairyUser> users, List<Domain> domains, 
			String last_domain_url, List<DiscoveryRecord> discovery_records, List<TestRecord> test_records){
		assert users != null;
		
		this.setKey(null);
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setCustomerToken(customer_token);
		this.setSubscriptionToken(subscription_token);
		this.setUsers(users);
		this.setDomains(domains);
		this.setLastDomain(last_domain_url);
		this.setDiscoveryRecords(discovery_records);
		this.setTestRecords(test_records);
	}
			
	public String getOrgName() {
		return org_name;
	}

	public void setOrgName(String org_name) {
		this.org_name = org_name;
	}

	public String getServicePackage() {
		return service_package;
	}

	public void setServicePackage(String service_package) {
		this.service_package = service_package;
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
	
	public List<QanairyUser> getUsers(){
		return this.users;
	}
	
	public void setUsers(List<QanairyUser> users){
		this.users = users;
	}
	
	public boolean addUser(QanairyUser user){
		return this.users.add(user);
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<Domain> getDomains(){
		return this.domains;
	}
	
	public void setDomains(List<Domain> domains){
		this.domains = domains;
	}
	
	public boolean addDomain(Domain domain) {
		return this.domains.add(domain);
	}
	
	public Domain removeDomain(Domain domain) {
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
			return this.domains.remove(idx);
		}
		return null;
	}
	
	public void setLastDomain(String domain_url) {
		this.last_domain_url = domain_url;
	}
	
	public String getLastDomain(){
		return this.last_domain_url;
	}

	public List<DiscoveryRecord> getDiscoveryRecords() {
		return discovery_records;
	}

	public void setDiscoveryRecords(List<DiscoveryRecord> discovery_records) {
		this.discovery_records = discovery_records;
	}
	
	public void addDiscoveryRecord(DiscoveryRecord record){
		this.discovery_records.add(record);
	}

	public List<TestRecord> getTestRecords() {
		return test_records;
	}

	public void setTestRecords(List<TestRecord> test_records) {
		this.test_records = test_records;
	}

	public void addTestRecord(TestRecord record) {
		this.test_records.add(record);
	}
}
