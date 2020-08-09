package com.qanairy.models.audit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.LookseeObject;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class AuditRecord extends LookseeObject {
	
	@Relationship(type = "HAS")
	private Set<Audit> audits;

	public AuditRecord() {
		audits = new HashSet<>();
		setKey(generateKey());
	}

	public String generateKey() {
		return "auditrecord:"+UUID.randomUUID().toString();
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
}
