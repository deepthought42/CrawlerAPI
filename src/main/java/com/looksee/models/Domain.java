package com.looksee.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.audit.AuditRecord;


/**
 * Encompasses a domain name as well as all {@link Test}s and {@link Group}s 
 * belong to this domain
 */
public class Domain extends LookseeObject{
	
	private String url;
	private String logo_url;

	@Relationship(type = "HAS")
	private List<PageState> pages;

	@Relationship(type = "HAS_TEST_USER")
	private Set<TestUser> test_users;
	
	@Relationship(type = "HAS_DOMAIN", direction = Relationship.INCOMING)
	private Account account;
	
	@Relationship(type = "HAS")
	private Set<AuditRecord> audit_records;
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(){
		setTestUsers( new HashSet<>() );
		setPages( new ArrayList<>() );
		setAuditRecords(new HashSet<>());
	}
	
	/**
	 * 
	 * @param protocol web protocol ("http", "https", "file", etc.)
	 * @param path landable url
	 * @param browser name of the browser ie. chrome, firefox, etc.
	 * @param logo_url url of logo image file
	 */
	public Domain( String url){
		setUrl(url);
		setPages(new ArrayList<>());
		setAuditRecords(new HashSet<>());
		setKey(generateKey());
	}
	
	/**
	 * 
	 * @param protocol web protocol ("http", "https", "file", etc.)
	 * @param path landable url
	 * @param browser name of the browser ie. chrome, firefox, etc.
	 * @param logo_url url of logo image file
	 */
	public Domain( String protocol, String host, String path, String logo_url){
		setLogoUrl(logo_url);
		setUrl(host);
		setPages(new ArrayList<>());
		setAuditRecords(new HashSet<>());
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "domain"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(getUrl());
	}
	
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public boolean addPage(PageState page) {
		//check if page state exists
		for(PageState state : this.getPages()){
			if(state.getKey().equals(page.getKey())){
				return false;
			}
		}
		
		return this.getPages().add(page);
	}

	public List<PageState> getPages() {
		return this.pages;
	}
	
	public void setPages(List<PageState> pages) {
		this.pages = pages;
	}

	public Set<AuditRecord> getAuditRecords() {
		return audit_records;
	}

	public void setAuditRecords(Set<AuditRecord> audit_records) {
		this.audit_records = audit_records;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
