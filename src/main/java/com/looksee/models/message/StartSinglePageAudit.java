package com.looksee.models.message;

import java.net.URL;

import com.looksee.models.audit.PageAuditRecord;

public class StartSinglePageAudit extends Message{
	private URL url;
	private PageAuditRecord audit_record;

	public StartSinglePageAudit(
							PageAuditRecord audit_record, 
							URL url
	) {
		setUrl(url);
		setAuditRecord(audit_record);
		setAuditRecordId(audit_record.getId());
	}
	
	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public PageAuditRecord getAuditRecord() {
		return audit_record;
	}

	public void setAuditRecord(PageAuditRecord audit_record) {
		this.audit_record = audit_record;
	}	
}
