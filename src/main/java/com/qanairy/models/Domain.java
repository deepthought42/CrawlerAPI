package com.qanairy.models;

import java.util.HashSet;
import java.util.Set;

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
	
	@GeneratedValue
    @Id
	private Long id;
	
	private String host;
	private int test_cnt;
	private String key;
	private String protocol;
	private String logo_url;
	private String discovery_browser;
	
	@Relationship(type = "HAS_TEST")
	private Set<Test> tests = new HashSet<>();
	
	@Relationship(type = "HAS_TEST_USER")
	private Set<TestUser> test_users = new HashSet<>();
	
	@Relationship(type = "HAS_DOMAIN", direction = Relationship.INCOMING)
	private Set<Account> account = new HashSet<>();

	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(){}
	
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
		setProtocol(protocol);
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
	public Domain(String domain_url,
					String logo_url,
					Set<Test> tests,
					String protocol,
					int test_count){
		setUrl(domain_url);
		setTests(tests);
		setProtocol(protocol);
		setLogoUrl(logo_url);
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
	public Domain(String domain_url,
					String logo_url,
					Set<Test> tests,
					String protocol,
					Set<TestUser> test_users,
					String browser_name,
					int test_count){
		setUrl(domain_url);
		setTests(tests);
		setProtocol(protocol);
		setLogoUrl(logo_url);
		setTestUsers(test_users);
		setDiscoveryBrowserName(browser_name);
		setTestCount(test_count);
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
		return host;
	}

	public void setUrl(String host) {
		this.host = host;
	}

	public Set<Test> getTests() {
		return tests;
	}

	public void setTests(Set<Test> tests) {
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

	public Set<TestUser> getTestUsers() {
		return test_users;
	}
	
	public void removeTestUser(TestUser test_user) {
		this.test_users.remove(test_user);
	}
	
	public boolean addTestUser(TestUser test_user) {
		return this.test_users.add(test_user);
	}

	public void setTestUsers(Set<TestUser> test_users) {
		this.test_users = new HashSet<TestUser>(test_users);
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
		return "domain::"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(getUrl().toString());
	}
	
	public Set<Account> getAccount() {
		return account;
	}

	public void setAccount(Set<Account> account) {
		this.account = account;
	}
	
	public long getId(){
		return this.id;
	}
}
