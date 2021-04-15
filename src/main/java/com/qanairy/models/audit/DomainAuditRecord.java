package com.qanairy.models.audit;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.AuditStats;
import com.qanairy.models.Domain;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class DomainAuditRecord extends AuditRecord {
	
	@Relationship(type = "HAS")
	private Set<PageAuditRecord> page_audit_records;
	
	@Relationship(type = "HAS")
	private Domain domain;
	
	public DomainAuditRecord() {
		setAudits(new HashSet<>());
		setStartTime(LocalDateTime.now());
	}
	
	/**
	 * Constructor
	 * 
	 * @param audit_stats {@link AuditStats} object with statics for audit progress
	 * @param level TODO
	 * 
	 * @pre audit_stats != null;
	 */
	public DomainAuditRecord(ExecutionStatus status) {
		assert status != null;

		setAudits(new HashSet<>());
		setKey(generateKey());
		setStatus(status);
		setLevel( AuditLevel.DOMAIN);
		setStartTime(LocalDateTime.now());
	}

	public String generateKey() {
		return "domainauditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public Set<PageAuditRecord> getAudits() {
		return page_audit_records;
	}

	public void setAudits(Set<PageAuditRecord> audits) {
		this.page_audit_records = audits;
	}

	public void addAudit(PageAuditRecord audit) {
		this.page_audit_records.add( audit );
	}
	
	public void addAudits(Set<PageAuditRecord> audits) {
		this.page_audit_records.addAll( audits );
	}

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}
}
