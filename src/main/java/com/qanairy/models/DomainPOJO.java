package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestUser;


/**
 * Encompasses a domain name as well as all {@link Test}s and {@link Group}s 
 * belong to this domain
 */
public class DomainPOJO extends Domain{
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
		setUrl(null);
		setTests(new ArrayList<Test>());
		setProtocol("http");
		test_users = new ArrayList<TestUser>();
		setDiscoveryBrowserName("");
		setTestCount(0);
		setKey(generateKey());
	}
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	@Deprecated
	public DomainPOJO(String url, String protocol){
		setUrl(url);
		setTests(new ArrayList<Test>());
		setProtocol(protocol);
		test_users = new ArrayList<TestUser>();
		setDiscoveryBrowserName("");
		setTestCount(0);
		setKey(generateKey());
	}
	
	/**
	 * 
	 * @param protocol web protocol ("http", "https", "file", etc.)
	 * @param url landable url
	 * @param browser name of the browser ie. chrome, firefox, etc.
	 * @param logo_url url of logo image file
	 */
	public DomainPOJO( String protocol, String url, String browser, String logo_url){
		setUrl(url);
		setLogoUrl(logo_url);
		setTests(new ArrayList<Test>());
		setProtocol(protocol);
		test_users = new ArrayList<TestUser>();
		setDiscoveryBrowserName(browser);
		setTestCount(0);
		setKey(generateKey());
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
	public DomainPOJO(String domain_url,
					String logo_url,
					List<Test> tests,
					String protocol,
					int test_count){
		setUrl(domain_url);
		setTests(tests);
		setProtocol(protocol);
		setLogoUrl(logo_url);
		test_users = new ArrayList<TestUser>();
		setDiscoveryBrowserName("");
		setTestCount(test_count);
		setKey(generateKey());
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
	public DomainPOJO(String domain_url,
					String logo_url,
					List<Test> tests,
					String protocol,
					List<TestUser> test_users,
					String browser_name,
					int test_count){
		setUrl(domain_url);
		setTests(tests);
		setProtocol(protocol);
		setLogoUrl(logo_url);
		test_users = test_users;
		setDiscoveryBrowserName(browser_name);
		setTestCount(test_count);
		setKey(generateKey());
	}
	
	@Override
	public String getUrl() {
		return domain;
	}

	@Override
	public void setUrl(String domain) {
		this.domain = domain;
	}

	@Override
	public List<Test> getTests() {
		return tests;
	}

	@Override
	public void setTests(List<Test> tests) {
		this.tests = tests;
	}

	@Override
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Override
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
	@Override
	public String getKey() {
		return this.key;
	}

	/**
	 * @param key the key to set
	 */
	@Override
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getLogoUrl() {
		return logo_url;
	}

	@Override
	public void setLogoUrl(String logo_url) {
		this.logo_url = logo_url;
	}

	@Override
	public List<TestUser> getTestUsers() {
		return test_users;
	}
	
	@Override
	public void removeTestUser(TestUser test_user) {
		this.test_users.remove(test_user);
	}
	
	@Override
	public void addTestUser(TestUser test_user) {
		this.test_users.add(test_user);
	}

	@Override
	public String getDiscoveryBrowserName() {
		return discovery_browser;
	}

	@Override
	public void setDiscoveryBrowserName(String discovery_browser) {
		this.discovery_browser = discovery_browser;
	}

	@Override
	public int getTestCount() {
		return test_cnt;
	}

	@Override
	public void setTestCount(int test_cnt) {
		this.test_cnt = test_cnt;
	}


	@Override
	public void addTest(Test test) {
		this.tests.add(test);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		return getUrl().toString();
	}
}
