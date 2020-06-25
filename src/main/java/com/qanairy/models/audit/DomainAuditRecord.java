package com.qanairy.models.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.LookseeObject;

/**
 * Defines a set of audits that were ran together
 */
public class DomainAuditRecord extends LookseeObject{
	@Properties
	private Map<String, Double> category_scores = new HashMap<String, Double>();
	
	@Relationship(type = "HAS")
	private Set<AuditRecord> audit_records;
	
	public DomainAuditRecord(){
		super();
	}
	
	/**
	 * @param audits {@linkplain Set} of {@link Audit audits}
	 */
	public DomainAuditRecord(Set<AuditRecord> audit_records) {
		super();
		setAudits(audit_records);
		calculateCategoryScores(audit_records);
		setKey(generateKey());
	}

	private void calculateCategoryScores(Set<AuditRecord> audit_records) {
		//calculate overall scores for each audit category
	   	for(AuditRecord audit_record : audit_records) {
	   		for(String category : audit_record.getCategoryScores().keySet()) {
	   			if(category_scores.containsKey(category)) {
	   				Double score = category_scores.get(category);
	   				score += audit_record.getCategoryScores().get(category);
	   			}
	   			else {
	   				category_scores.put(category, audit_record.getCategoryScores().get(category));
	   			}
	   		}
	   	}
	   	
	   	for(String category : category_scores.keySet()) {
	   		double score = category_scores.get(category)/audit_records.size();
	   		category_scores.put(category, score);
	   	}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		String audit_key = "";
		for(AuditRecord audit : audit_records) {
			audit_key += audit.getKey();
		}
		return "domain_audit_record:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(audit_key);
	}

	public Set<AuditRecord> getAudits() {
		return audit_records;
	}

	public void setAudits(Set<AuditRecord> audit_records) {
		this.audit_records = audit_records;
	}
}
