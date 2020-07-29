package com.qanairy.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.audit.domain.DomainAuditRecord;


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
	private String entry_path;
	private String key;
	private String protocol;
	private String logo_url;
	private String discovery_browser;
	
	@Relationship(type = "HAS_TEST")
	private Set<Test> tests;

	@Relationship(type = "HAS")
	private List<Page> pages;

	@Relationship(type = "HAS_TEST_USER")
	private Set<TestUser> test_users;
	
	@Relationship(type = "HAS_DOMAIN", direction = Relationship.INCOMING)
	private Account account;

	@Relationship(type = "HAS_DISCOVERY_RECORD")
	private Set<DiscoveryRecord> discovery_records;
	
	@Relationship(type = "HAS_AUDIT")
	private Set<DomainAuditRecord> audits;
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(){
		setTests( new HashSet<>() );
		setDiscoveryRecords( new HashSet<>() );
		setTestUsers( new HashSet<>() );
		setPages( new ArrayList<>() );
	}
	
	/**
	 * 
	 * @param protocol web protocol ("http", "https", "file", etc.)
	 * @param path landable url
	 * @param browser name of the browser ie. chrome, firefox, etc.
	 * @param logo_url url of logo image file
	 */
	public Domain( String protocol, String host, String path, String browser, String logo_url){
		setEntryPath(path);
		setLogoUrl(logo_url);
		setProtocol(protocol);
		setDiscoveryBrowserName(browser);
		setHost(host);
		setPages(new ArrayList<>());
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof Domain){
			Domain domain = (Domain)o;
			if(domain.getEntryPath().equals(this.getEntryPath())){
				return true;
			}
		}
		
		return false;
	}

	public String getEntryPath() {
		return entry_path;
	}

	public void setEntryPath(String url) {
		this.entry_path = url;
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
		return "domain::"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(getEntryPath().toString());
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

	public boolean addPage(Page page) {
		//check if page state exists
		for(Page state : this.getPages()){
			if(state.getKey().equals(page.getKey())){
				return false;
			}
		}
		
		return this.getPages().add(page);
	}

	public List<Page> getPages() {
		return this.pages;
	}
	
	public void setPages(List<Page> pages) {
		this.pages = pages;
	}
}
