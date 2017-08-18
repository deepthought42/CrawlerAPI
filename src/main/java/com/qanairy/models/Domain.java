package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;


/**
 * Encompasses a domain name as well as all {@link Test}s and {@link Group}s 
 * belong to this domain
 */
@Component
public class Domain {
	private String domain;
	private List<Account> accounts;
	private List<Test> tests;
	private List<Group> groups;
	private String key;
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(){
		this.setUrl(null);
		this.setTests(new ArrayList<Test>());
		this.setGroups(new ArrayList<Group>());
		this.accounts = new ArrayList<Account>();
	}
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(String url){
		this.setUrl(url);
		this.setTests(new ArrayList<Test>());
		this.setGroups(new ArrayList<Group>());
	}
	
	/**
	 * Construct a new Domain object
	 * 
	 * @param domain_url 	- host url of the domain (eg. www.reddit.com)
	 * @param tests			- tests that belong to this domain
	 * @param groups		- groups that belong to this domain
	 */
	public Domain(	String key, 
					String domain_url, 
					List<Test> tests, 
					List<Group> groups){
		this.setKey(key);
		this.setUrl(domain_url);
		this.setTests(tests);
		this.setGroups(groups);
	}

	public String getUrl() {
		return domain;
	}

	public void setUrl(String domain) {
		this.domain = domain;
	}

	public List<Test> getTests() {
		return tests;
	}

	public void setTests(List<Test> tests) {
		this.tests = tests;
	}

	public List<Group> getGroups() {
		return groups;
	}

	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}
	
	public boolean addAccount(Account acct){
		return this.accounts.add(acct);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof Domain){
			Domain domain = (Domain)o;
			if(domain.getUrl().equals(this.getUrl())){
				return true;
			}
		}
		
		return false;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}
}
