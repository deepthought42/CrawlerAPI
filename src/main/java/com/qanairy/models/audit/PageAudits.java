package com.qanairy.models.audit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.qanairy.models.AuditStats;
import com.qanairy.models.SimplePage;
import com.qanairy.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class PageAudits {
	private Set<Audit> audits;
	private SimplePage page_state;
	
	private String status;
	
	public PageAudits() {
		setAudits(new HashSet<>());
	}
	
	/**
	 * Constructor
	 * @param audits TODO
	 * @param page_state TODO
	 * @param audit_stats {@link AuditStats} object with statics for audit progress
	 * 
	 * @pre audits != null;
	 */
	public PageAudits(
			ExecutionStatus status, 
			Set<Audit> audits, 
			SimplePage page_state
	) {
		assert audits != null;
		
		setAudits(audits);
		setSimplePage(page_state);
		setStatus(status);
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

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}

	public SimplePage getSimplePage() {
		return page_state;
	}

	public void setSimplePage(SimplePage page_state) {
		this.page_state = page_state;
	}
}
