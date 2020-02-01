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
	private String url;
	private String key;
	private String protocol;
	private String logo_url;
	private String discovery_browser;
	
	@Relationship(type = "HAS_TEST")
	private Set<Test> tests = new HashSet<>();

	@Relationship(type = "HAS")
	private Set<PageState> page_states = new HashSet<>();

	@Relationship(type = "HAS_TEST_USER")
	private Set<TestUser> test_users = new HashSet<>();
	
	@Relationship(type = "HAS_DOMAIN", direction = Relationship.INCOMING)
	private Account account;

	@Relationship(type = "HAS_DISCOVERY_RECORD")
	private Set<DiscoveryRecord> discovery_records = new HashSet<>();
	
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
	public Domain( String protocol, String url, String browser, String logo_url, String host){
		setUrl(url);
		setLogoUrl(logo_url);
		setProtocol(protocol);
		setDiscoveryBrowserName(browser);
		setHost(host);
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
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
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

	public void addTest(Test test) {
		this.tests.add(test);
	}
	
	public Set<DiscoveryRecord> getDiscoveryRecords() {
		return discovery_records;
	}

	public void setDiscoveryRecords(Set<DiscoveryRecord> discovery_records) {
		this.discovery_records = discovery_records;
	}
	
	public void addDiscoveryRecord(DiscoveryRecord record){
		this.discovery_records.add(record);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "domain::"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(getUrl().toString());
	}
	
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}
	
	public long getId(){
		return this.id;
	}

	public Set<PageState> getPageStates() {
		return this.page_states;
	}
	
	public void setPageStates(Set<PageState> page_states){
		this.page_states = page_states;
	}
	
	public boolean addPageState(PageState page_state){
		//check if page state exists
		for(PageState state : this.getPageStates()){
			if(state.getKey().equals(page_state.getKey())){
				return false;
			}
		}
		
		return this.getPageStates().add(page_state);
	}
}
