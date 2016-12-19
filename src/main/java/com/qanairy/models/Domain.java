package com.qanairy.models;

import java.util.List;

import com.minion.persistence.IDomain;
import com.minion.persistence.IPersistable;
import com.minion.persistence.OrientConnectionFactory;

/**
 * Encompasses a domain name as well as all {@link Test}s and {@link Group}s 
 * belong to this domain
 */
public class Domain implements IPersistable<IDomain>{
	private String domain;
	private List<Test> tests;
	private List<Group> groups;
	private Organization organization;
	
	/**
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(String domain_name, Organization organization){
		this.setUrl(domain);
		this.setOrganization(organization);
	}
	
	/**
	 * Construct a new Domain object
	 * 
	 * @param domain_url 	- host url of the domain (eg. www.reddit.com)
	 * @param tests			- tests that belong to this domain
	 * @param groups		- groups that belong to this domain
	 */
	public Domain(String domain_url, List<Test> tests, List<Group> groups){
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

	public Organization getOrganization() {
		return organization;
	}

	public void setOrganization(Organization organization) {
		this.organization = organization;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public IDomain convertToRecord(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public IDomain create() {
		// TODO Auto-generated method stub
		return null;
	}

	public IDomain update() {
		// TODO Auto-generated method stub
		return null;
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
	
}
