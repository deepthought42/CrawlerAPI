package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.qanairy.persistence.Domain;
import com.qanairy.persistence.Test;


/**
 * Encompasses a domain name as well as all {@link Test}s and {@link Group}s 
 * belong to this domain
 */
@Component
public class DomainPOJO {
	private String domain;
	private List<Test> tests;
	private int test_cnt;
	private String key;
	private String protocol;
	private String logo_url;
	private List<TestUser> test_users;
	private String discovery_browser;
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public DomainPOJO(){
		this.setUrl(null);
		this.setTests(new ArrayList<Test>());
		this.setProtocol("http");
		this.setTestUsers(new ArrayList<TestUser>());
		this.setDiscoveryBrowser("");
		this.setTestCount(0);
	}
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	@Deprecated
	public DomainPOJO(String url, String protocol){
		this.setUrl(url);
		this.setTests(new ArrayList<Test>());
		this.setProtocol(protocol);
		this.setTestUsers(new ArrayList<TestUser>());
		this.setDiscoveryBrowser("");
		this.setTestCount(0);
	}
	
	/**
	 * 
	 * @param protocol web protocol ("http", "https", "file", etc.)
	 * @param url landable url
	 * @param browser name of the browser ie. chrome, firefox, etc.
	 * @param logo_url url of logo image file
	 */
	public DomainPOJO( String protocol, String url, String browser, String logo_url){
		this.setUrl(url);
		this.setLogoUrl(logo_url);
		this.setTests(new ArrayList<Test>());
		this.setProtocol(protocol);
		this.setTestUsers(new ArrayList<TestUser>());
		this.setDiscoveryBrowser(browser);
		this.setTestCount(0);
	}
	
	/**
	 * 
	 * @param key
	 * @param domain_url
	 * @param logo_url
	 * @param tests
	 * @param protocol
	 * @param test_count
	 */
	public DomainPOJO(	String key, 
					String domain_url,
					String logo_url,
					List<Test> tests,
					String protocol,
					int test_count){
		this.setKey(key);
		this.setUrl(domain_url);
		this.setTests(tests);
		this.setProtocol(protocol);
		this.setLogoUrl(logo_url);
		this.setTestUsers(new ArrayList<TestUser>());
		this.setDiscoveryBrowser("");
		this.setTestCount(test_count);
	}

	
	/**
	 * 
	 * @param key
	 * @param domain_url
	 * @param logo_url
	 * @param tests
	 * @param protocol
	 * @param last_path_ran_at
	 * @param last_discovery_started_at
	 * @param test_users
	 * @param discovered_test_count
	 * @param browser_name
	 */
	public DomainPOJO(	String key, 
					String domain_url,
					String logo_url,
					List<Test> tests,
					String protocol,
					List<TestUser> test_users,
					String browser_name,
					int test_count){
		this.setKey(key);
		this.setUrl(domain_url);
		this.setTests(tests);
		this.setProtocol(protocol);
		this.setLogoUrl(logo_url);
		this.setTestUsers(test_users);
		this.setDiscoveryBrowser(browser_name);
		this.setTestCount(test_count);
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

	public String getLogoUrl() {
		return logo_url;
	}

	public void setLogoUrl(String logo_url) {
		this.logo_url = logo_url;
	}

	public List<TestUser> getTestUsers() {
		return test_users;
	}

	public void setTestUsers(List<TestUser> test_users) {
		this.test_users = test_users;
	}
	
	public void addTestUsers(TestUser test_user) {
		this.test_users.add(test_user);
	}

	public String getDiscoveryBrowser() {
		return discovery_browser;
	}

	public void setDiscoveryBrowser(String discovery_browser) {
		this.discovery_browser = discovery_browser;
	}

	public int getTestCount() {
		return test_cnt;
	}

	public void setTestCount(int test_cnt) {
		this.test_cnt = test_cnt;
	}
}
