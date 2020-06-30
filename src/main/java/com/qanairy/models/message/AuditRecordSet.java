package com.qanairy.models.message;

import java.util.List;

import com.qanairy.models.PageState;
import com.qanairy.models.audit.AuditRecord;

/**
 * Message that contains a {@link PageState} that is ready for analysis
 * 
 */
public class AuditRecordSet extends Message {
	private List<AuditRecord> audit_records;
	
	public AuditRecordSet(List<AuditRecord> audit_records){
		setAuditRecords(audit_records);
	}

	public List<AuditRecord> getAuditRecords() {
		return audit_records;
	}

	public void setAuditRecords(List<AuditRecord> audit_records) {
		this.audit_records = audit_records;
	}

}
