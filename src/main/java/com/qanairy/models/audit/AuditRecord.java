package com.qanairy.models.audit;

import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.LookseeObject;

/**
 * Defines a set of audits that were ran together
 */
public class AuditRecord extends LookseeObject{
	@Properties
	private Map<String, Double> category_scores;
	
	@Relationship(type = "HAS")
	private List<Audit> audits;
	
	public AuditRecord(){
		super();
	}
	
	/**
	 * @param audits {@linkplain List} of {@link Audit audits}
	 */
	public AuditRecord(List<Audit> audits) {
		super();
		setAudits(audits);
		this.setKey(generateKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		String audit_key = "";
		for(Audit audit : audits) {
			audit_key += audit.getKey();
		}
		return "audit_record:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(audit_key);
	}

	public List<Audit> getAudits() {
		return audits;
	}

	public void setAudits(List<Audit> audits) {
		this.audits = audits;
	}
}
