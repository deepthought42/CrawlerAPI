package com.qanairy.models;

import java.util.ArrayList;
import java.util.Date;
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
	private String key;
	private String protocol;
	private Date last_discovery_path_ran_at;
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(){
		this.setUrl(null);
		this.setTests(new ArrayList<Test>());
		this.accounts = new ArrayList<Account>();
		this.setProtocol("http");
	}
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(String url, String protocol){
		this.setUrl(url);
		this.setTests(new ArrayList<Test>());
		this.setProtocol(protocol);
	}
	
	/**
	 * Construct a new Domain object
	 * 
	 * @param domain_url 	- host url of the domain (eg. www.reddit.com)
	 * @param tests			- tests that belong to this domain
	 */
	public Domain(	String key, 
					String domain_url, 
					List<Test> tests,
					String protocol,
					Date timestamp){
		this.setKey(key);
		this.setUrl(domain_url);
		this.setTests(tests);
		this.setProtocol(protocol);
		this.setLastDiscoveryPathRanAt(timestamp);
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
	
	public boolean addAccount(Account acct){
		return this.accounts.add(acct);
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return this.protocol;
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

	public Date getLastDiscoveryPathRanAt() {
		return this.last_discovery_path_ran_at;
	}
	
	public void setLastDiscoveryPathRanAt(Date timestamp) {
		this.last_discovery_path_ran_at = timestamp;
	}
}
