package com.qanairy.models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Encompasses a domain name as well as all {@link Test}s and {@link Group}s 
 * belong to this domain
 */
public class Domain implements IPersistable<IDomain>{
	private String domain;
	private List<Test> tests;
	private List<Group> groups;
	private String key;
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(String domain_name){
		this.setUrl(domain_name);
		this.setKey(this.generateKey());
		this.setTests(new ArrayList<Test>());
		this.setGroups(new ArrayList<Group>());
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
		this.setKey(this.generateKey());
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

	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		return this.getUrl().toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public IDomain convertToRecord(OrientConnectionFactory connection) {
		IDomain domain = connection.getTransaction().addVertex("class:"+IDomain.class.getCanonicalName()+","+UUID.randomUUID(), IDomain.class);
		domain.setKey(this.getKey());
		domain.setUrl(this.getUrl());
		domain.setTests(this.getTests());
		domain.setGroups(this.getGroups());
		return domain;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IDomain create(OrientConnectionFactory connection) {
		IDomain domain = this.find(connection);
		
		if(domain != null){
			domain = this.convertToRecord(connection);
			connection.save();
		}
		return domain;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IDomain update(OrientConnectionFactory connection) {
		IDomain domain = this.find(connection);
		if(domain != null){
			domain.setGroups(this.getGroups());
			domain.setTests(this.getTests());
			domain.setUrl(this.getUrl());
			connection.save();
		}
		
		return domain;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IDomain find(OrientConnectionFactory connection) {
		@SuppressWarnings("unchecked")
		Iterable<IDomain> domains = (Iterable<IDomain>) DataAccessObject.findByKey(this.getKey(), connection, IDomain.class);
		Iterator<IDomain> iter = domains.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		
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
}
