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
	private String logo_url;
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
	@Deprecated
	public Domain(String url, String protocol){
		this.setUrl(url);
		this.setTests(new ArrayList<Test>());
		this.setProtocol(protocol);
	}
	
	/**
	 * 
	 * @param url
	 * @param logo_url
	 * @param protocol
	 */
	public Domain(String url, String logo_url, String protocol){
		this.setUrl(url);
		this.setLogoUrl(logo_url);
		this.setTests(new ArrayList<Test>());
		this.setProtocol(protocol);
	}
	
	/**
	 * Construct a new Domain object
	 * 
	 * @param domain_url 	- host url of the domain (eg. www.reddit.com)
	 * @param tests			- tests that belong to this domain
	 */
	@Deprecated
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
		this.setLogoUrl(logo_url);
	}
	
	/**
	 * 
	 * @param key
	 * @param domain_url
	 * @param logo_url
	 * @param tests
	 * @param protocol
	 * @param timestamp
	 */
	public Domain(	String key, 
					String domain_url,
					String logo_url,
					List<Test> tests,
					String protocol,
					Date timestamp){
		this.setKey(key);
		this.setUrl(domain_url);
		this.setTests(tests);
		this.setProtocol(protocol);
		this.setLastDiscoveryPathRanAt(timestamp);
		this.setLogoUrl(logo_url);
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

	public String getLogoUrl() {
		return logo_url;
	}

	public void setLogoUrl(String logo_url) {
		this.logo_url = logo_url;
	}
}
