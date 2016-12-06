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
	public Domain(String domain, Organization organization){
		this.setDomain(domain);
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
		this.setDomain(domain_url);
		this.setTests(tests);
		this.setGroups(groups);
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
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
	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IDomain convertToRecord(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IDomain create() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDomain update() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
