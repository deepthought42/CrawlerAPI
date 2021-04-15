package com.qanairy.models.audit;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.AuditStats;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class PageAuditRecord extends AuditRecord {
	@Relationship(type = "HAS")
	private Set<Audit> audits;
	
	@Relationship(type = "HAS")
	private PageState page_state;
	
	private String status;
	
	public PageAuditRecord() {
		setStatus(ExecutionStatus.IN_PROGRESS);
		setLevel(AuditLevel.PAGE);
		setAudits(new HashSet<>());
	}
	
	/**
	 * Constructor
	 * @param audits TODO
	 * @param page_state TODO
	 * @param audit_stats {@link AuditStats} object with statics for audit progress
	 * 
	 * @pre audits != null
	 * @pre page_state != null
	 * @pre status != null;
	 */
	public PageAuditRecord(
			ExecutionStatus status, 
			Set<Audit> audits, 
			PageState page_state
	) {
		assert audits != null;
		assert page_state != null;
		assert status != null;
		
		setAudits(audits);
		setPageState(page_state);
		setStatus(status);
		setLevel( AuditLevel.PAGE);
		setKey(generateKey());
	}

	public String generateKey() {
		return "pageauditrecord:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + page_state.getKey());
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

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}

	public PageState getPageState() {
		return page_state;
	}

	public void setPageState(PageState page_state) {
		this.page_state = page_state;
	}
}
