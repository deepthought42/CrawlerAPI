package com.looksee.models.message;

public class PageAuditRecordMessage extends Message {

	private long page_audit_id;
	
	public PageAuditRecordMessage(long page_audit_id, 
								  long domain_id, 
								  long account_id, 
								  long domain_audit_id
	) {
		super(domain_id, account_id, domain_audit_id);
		setPageAuditId(page_audit_id);
	}

	public long getPageAuditId() {
		return page_audit_id;
	}

	public void setPageAuditId(long page_audit_id) {
		this.page_audit_id = page_audit_id;
	}
}
