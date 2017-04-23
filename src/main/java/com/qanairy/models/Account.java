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
	public Account(String org_name, String service_package, String payment_acct_num, List<QanairyUser> users){
		assert users != null;
		
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
		this.users = users;
		this.domains = new ArrayList<Domain>();
	}
	
	public Account(String key, String org_name, String service_package, String payment_acct_num){
		this.setKey(key);
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
		this.users = new ArrayList<>();
		this.domains = new ArrayList<Domain>();
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
	public Account(String key, String org_name, String service_package, String payment_acct_num, List<QanairyUser> users){
		assert users != null;
		
		this.setKey(key);
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
		this.users = users;
		this.domains = new ArrayList<Domain>();
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
}
