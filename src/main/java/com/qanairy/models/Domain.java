package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;


/**
 * Encompasses a domain name as well as all {@link Test}s and {@link Group}s 
 * belong to this domain
 */
@NodeEntity
public class Domain implements Persistable{
	@Id 
	@GeneratedValue 
	private Long id;
	
	private String domain;
	
	@Relationship(type = "HAS_TEST", direction = Relationship.OUTGOING)
	private List<Test> tests;
	private int test_cnt;
	private String key;
	private String protocol;
	private String logo_url;
	
	@Relationship(type = "HAS_TEST_USER", direction = Relationship.OUTGOING)
	private List<TestUser> test_users;
	private String discovery_browser;
	
	@Relationship(type = "HAS_PAGE_STATE", direction = Relationship.OUTGOING)
	private List<PageState> page_states;
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(){
		setUrl(null);
		setTests(new ArrayList<Test>());
		setProtocol("http");
		this.test_users = new ArrayList<TestUser>();
		setPageStates(new ArrayList<PageState>());
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
	public Domain( String protocol, String url, String browser, String logo_url){
		setUrl(url);
		setLogoUrl(logo_url);
		setTests(new ArrayList<Test>());
		setProtocol(protocol);
		this.test_users = new ArrayList<TestUser>();
		setDiscoveryBrowserName(browser);
		setTestCount(0);
		setPageStates(new ArrayList<PageState>());
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
	public Domain(String domain_url,
					String logo_url,
					List<Test> tests,
					String protocol,
					int test_count){
		setUrl(domain_url);
		setTests(tests);
		setProtocol(protocol);
		setLogoUrl(logo_url);
		this.test_users = new ArrayList<TestUser>();
		setDiscoveryBrowserName("");
		setTestCount(test_count);
		setPageStates(new ArrayList<PageState>());
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
	public Domain(String domain_url,
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
		setTestUsers(test_users);
		setDiscoveryBrowserName(browser_name);
		setTestCount(test_count);
		setPageStates(new ArrayList<PageState>());
		setKey(generateKey());
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
	 * @return the key
	 */
	public String getKey() {
		return this.key;
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
	
	public void removeTestUser(TestUser test_user) {
		this.test_users.remove(test_user);
	}
	
	public void addTestUser(TestUser test_user) {
		this.test_users.add(test_user);
	}

	public void setTestUsers(List<TestUser> test_users) {
		this.test_users = new ArrayList<TestUser>(test_users);
	}
	
	public String getDiscoveryBrowserName() {
		return discovery_browser;
	}

	public void setDiscoveryBrowserName(String discovery_browser) {
		this.discovery_browser = discovery_browser;
	}

	public int getTestCount() {
		return test_cnt;
	}

	public void setTestCount(int test_cnt) {
		this.test_cnt = test_cnt;
	}

	public void addTest(Test test) {
		this.tests.add(test);
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return getUrl().toString();
	}

	public List<PageState> getPageStates() {
		return this.page_states;
	}

	public void setPageStates(List<PageState> states) {
		this.page_states = states;
	}

	public void addPageState(PageState state) {
		this.page_states.add(state);
	}
}
