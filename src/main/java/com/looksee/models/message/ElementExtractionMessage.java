package com.looksee.models.message;

import java.util.List;

import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;

public class ElementExtractionMessage {
	private PageState page_state;
	private AuditRecord audit_record;
	private List<String> xpaths;
	
	public ElementExtractionMessage( PageState page,
									 AuditRecord record,
									 List<String> xpaths) {
		setPageState(page);
		setAuditRecord(record);
		setXpaths(xpaths);
	}
	
	public PageState getPageState() {
		return page_state;
	}
	public void setPageState(PageState page_state) {
		this.page_state = page_state;
	}
	public AuditRecord getAuditRecord() {
		return audit_record;
	}
	public void setAuditRecord(AuditRecord audit_record) {
		this.audit_record = audit_record;
	}
	public List<String> getXpaths() {
		return xpaths;
	}
	public void setXpaths(List<String> xpaths) {
		this.xpaths = xpaths;
	}
}
