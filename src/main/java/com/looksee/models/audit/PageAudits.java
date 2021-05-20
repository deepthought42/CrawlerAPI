package com.looksee.models.audit;

import java.util.UUID;

import com.looksee.models.AuditStats;
import com.looksee.models.SimplePage;
import com.looksee.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class PageAudits {
	private ElementIssueTwoWayMapping element_issue_map;
	private SimplePage page_state;
	
	private String status;
	
	public PageAudits() {}
	
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
			ElementIssueTwoWayMapping element_issue_map, 
			SimplePage page_state
	) {
		assert element_issue_map != null;
		assert page_state != null;
		assert status != null;
		
		setElementIssueMap(element_issue_map);
		setSimplePage(page_state);
		setStatus(status);
	}

	public String generateKey() {
		return "auditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public ElementIssueTwoWayMapping getElementIssueMap() {
		return element_issue_map;
	}

	public void setElementIssueMap(ElementIssueTwoWayMapping element_issue_map) {
		this.element_issue_map = element_issue_map;
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
