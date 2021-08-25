package com.looksee.models.message;

import java.util.List;

import com.looksee.models.PageState;

public class ElementExtractionMessage {
	private PageState page_state;
	private long audit_record_id;
	private List<String> xpaths;
	
	public ElementExtractionMessage( PageState page,
									 long audit_id,
									 List<String> xpaths) {
		setPageState(page);
		setAuditRecordId(audit_id);
		setXpaths(xpaths);
	}
	
	public PageState getPageState() {
		return page_state;
	}
	public void setPageState(PageState page_state) {
		this.page_state = page_state;
	}
	public long getAuditRecordId() {
		return audit_record_id;
	}
	public void setAuditRecordId(long audit_record_id) {
		this.audit_record_id = audit_record_id;
	}
	public List<String> getXpaths() {
		return xpaths;
	}
	public void setXpaths(List<String> xpaths) {
		this.xpaths = xpaths;
	}
}
