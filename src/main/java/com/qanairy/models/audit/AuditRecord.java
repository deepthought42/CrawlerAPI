package com.qanairy.models.audit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.AuditStats;
import com.qanairy.models.LookseeObject;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class AuditRecord extends LookseeObject {
	
	@Relationship(type = "HAS")
	private Set<Audit> audits;

	@Relationship(type = "HAS")
	private AuditStats audit_stats;
	
	public AuditRecord() {}
	
	/**
	 * Constructor
	 * 
	 * @param audit_stats {@link AuditStats} object with statics for audit progress
	 * 
	 * @pre audit_stats != null;
	 */
	public AuditRecord(AuditStats audit_stats) {
		assert audit_stats != null;
		
		setAudits(new HashSet<>());
		setKey(generateKey());
		setAuditStats(audit_stats);
	}

	public String generateKey() {
		return "auditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public Set<Audit> getAudits() {
		return audits;
	}

	public void setAudits(Set<Audit> audits) {
		this.audits = audits;
	}

	public void addAudit(Audit audit) {
		this.audits.add( audit );
	}
	
	public void addAudits(Set<Audit> audits) {
		this.audits.addAll( audits );
	}

	public void setAuditStats(AuditStats audit_stats) {
		this.audit_stats = audit_stats;
	}
	
	public AuditStats getAuditStats() {
		return this.audit_stats;
	}
}
