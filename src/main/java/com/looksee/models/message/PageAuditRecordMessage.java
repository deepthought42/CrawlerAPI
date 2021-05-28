package com.looksee.models.message;

import com.looksee.models.audit.PageAuditRecord;

public class PageAuditRecordMessage extends Message {

	private PageAuditRecord page_audit;
	
	public PageAuditRecordMessage(
			PageAuditRecord page_audit_record, 
			long domain_id, 
			long account_id, 
			long domain_audit_id) {
		super(domain_id, account_id, domain_audit_id);
		setPageAuditRecord(page_audit_record);
	}

	public PageAuditRecord getPageAuditRecord() {
		return page_audit;
	}

	public void setPageAuditRecord(PageAuditRecord page_audit_record) {
		this.page_audit = page_audit_record;
	}
}
