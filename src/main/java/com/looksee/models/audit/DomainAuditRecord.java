package com.looksee.models.audit;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
@Node
public class DomainAuditRecord extends AuditRecord {
	
	@Relationship(type = "HAS", direction = Direction.OUTGOING)
	private Set<PageAuditRecord> pageAuditRecords;	

	public DomainAuditRecord() {
		super();
		setPageAuditRecords(new HashSet<>());
		setAuditLabels(new HashSet<>());
	}
	
	/**
	 * Constructor
	 * 
	 * @param audit_stats {@link AuditStats} object with statics for audit progress
	 * @param level TODO
	 * 
	 * @pre audit_stats != null;
	 */
	public DomainAuditRecord(ExecutionStatus status, 
							Set<AuditName> audit_list) {
		super();
		assert status != null;
		
		setPageAuditRecords(new HashSet<>());
		setStatus(status);
		setLevel( AuditLevel.DOMAIN);
		setStartTime(LocalDateTime.now());
		setAestheticAuditProgress(0.0);
		setContentAuditProgress(0.0);
		setInfoArchitectureAuditProgress(0.0);
		setDataExtractionProgress(0.0);
		setAuditLabels(audit_list);
		setKey(generateKey());
	}

	public String generateKey() {
		return "domainauditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public Set<PageAuditRecord> getPageAuditRecords() {
		return pageAuditRecords;
	}

	public void setPageAuditRecords(Set<PageAuditRecord> audits) {
		this.pageAuditRecords = audits;
	}

	public void addPageAuditRecord(PageAuditRecord audit) {
		this.pageAuditRecords.add( audit );
	}
	
	public void addPageAuditRecords(Set<PageAuditRecord> audits) {
		this.pageAuditRecords.addAll( audits );
	}
}
