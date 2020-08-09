package com.qanairy.models.message;

import com.qanairy.models.Domain;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.enums.CrawlAction;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class CrawlActionMessage extends Message{
	private CrawlAction action;
	private AuditRecord audit_record;
	
	public CrawlActionMessage(CrawlAction action, Domain domain, String account_id, AuditRecord record){
		super(domain.getHost(), account_id);
		setAction(action);
		setDomain(domain);
		setAuditRecord(record);
	}
	
	public CrawlAction getAction() {
		return action;
	}
	
	private void setAction(CrawlAction action) {
		this.action = action;
	}

	public AuditRecord getAuditRecord() {
		return audit_record;
	}

	public void setAuditRecord(AuditRecord audit_record) {
		this.audit_record = audit_record;
	}	
}
