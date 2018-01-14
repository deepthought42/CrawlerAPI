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
	private String payment_acct_num;
	private List<QanairyUser> users;
	private List<Domain> domains;
	private List<DiscoveryRecord> discovery_records;
	
	public Account(){}
	
	public Account(String key, String org_name, String service_package, String payment_acct_num){
		this.setKey(key);
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
		this.setDomains(new ArrayList<Domain>());
		this.setUsers(new ArrayList<QanairyUser>());
		this.setDiscoveryRecords(new ArrayList<DiscoveryRecord>());
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
	@Deprecated
	public Account(String org_name, String service_package, String payment_acct_num, List<QanairyUser> users){
		assert users != null;
		
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
		this.setDomains(new ArrayList<Domain>());
		this.setDiscoveryRecords(new ArrayList<DiscoveryRecord>());
		this.setUsers(users);
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
	public Account(String org_name, String service_package, String payment_acct_num, List<QanairyUser> users, List<DiscoveryRecord> discovery_records){
		assert users != null;
		
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
		this.setDomains(new ArrayList<Domain>());
		this.setUsers(users);
		this.setDiscoveryRecords(discovery_records);
	}
	
	/**
	 * 
	 * @param key
	 * @param org_name
	 * @param service_package
	 * @param payment_acct_num
	 * @param users
	 * 
	 * @pre users != null
	 */
	public Account(String key, String org_name, String service_package, String payment_acct_num, List<QanairyUser> users, List<Domain> domains){
		assert users != null;
		
		this.setKey(key);
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
		this.setUsers(users);
		this.setDomains(domains);
		this.setDiscoveryRecords(new ArrayList<DiscoveryRecord>());
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

	public String getPaymentAcctNum() {
		return payment_acct_num;
	}

	public void setPaymentAcctNum(String payment_acct_num) {
		this.payment_acct_num = payment_acct_num;
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
	
	public boolean setDomains(List<Domain> domains){
		return this.domains.addAll(domains);
	}
	
	public boolean addDomain(Domain domain) {
		return this.domains.add(domain);
	}
	
	public Domain removeDomain(Domain domain) {
		int idx = -1;
		boolean domain_found = false;
		for(Domain curr_domain : domains){
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

	public List<DiscoveryRecord> getDiscoveryRecords() {
		return discovery_records;
	}

	public void setDiscoveryRecords(List<DiscoveryRecord> discovery_records) {
		this.discovery_records = discovery_records;
	}
}
