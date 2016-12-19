package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

import com.minion.persistence.IOrganization;
import com.minion.persistence.IPersistable;
import com.minion.persistence.OrientConnectionFactory;

/**
 * Defines an entity representing an organization registered with the system
 *
 */
public class Organization implements IPersistable<IOrganization> {
    private String key;
	private String name;
	private List<Domain> domains;
	
	public Organization(String name){
		this.setName(name);
		this.setDomains(new ArrayList<Domain>());
		this.setKey(this.generateKey());
	}
	
	public Organization(String name, List<Domain> domains){
		this.setName(name);
		this.setDomains(domains);
		this.setKey(this.generateKey());
	}
	
    public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Domain> getDomains() {
		return domains;
	}

	public void setDomains(List<Domain> domains) {
		this.domains = domains;
	}

	public String generateKey() {
		return "org:"+this.name;
	}

	public IOrganization convertToRecord(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}

	public IOrganization create() {
		// TODO Auto-generated method stub
		return null;
	}

	public IOrganization update() {
		// TODO Auto-generated method stub
		return null;
	}   
}
