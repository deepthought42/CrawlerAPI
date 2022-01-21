package com.looksee.models;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.audit.AuditRecord;


/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
@NodeEntity
public class Competitor extends LookseeObject{

	private String company_name;
	private String url;
	private String industry;
	
	@Relationship(type = "HAS")
	private Set<AuditRecord> competitive_audit = new HashSet<>();

	public Competitor(){
		super();
	}

	/**
	 *
	 * @param username
	 * @param customer_token
	 * @param subscription_token
	 *
	 * @pre users != null
	 */
	public Competitor(
			String company_name, 
			String url, 
			String industry
	){
		super();
		setCompanyName(company_name);
		setUrl(url);
		setIndustry(industry);
	}

	
	@Override
	public String generateKey() {
		return "competitor::"+UUID.randomUUID().toString();
	}

	public String getCompanyName() {
		return company_name;
	}

	public void setCompanyName(String company_name) {
		this.company_name = company_name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getIndustry() {
		return industry;
	}

	public void setIndustry(String industry) {
		this.industry = industry;
	}
}
