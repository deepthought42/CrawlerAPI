package com.looksee.models.message;

public class ElementsSaved extends Message{
	private String page_url;
	
	public ElementsSaved(String page_url,
						long audit_record_id) {
		setPageUrl(page_url);
		setAuditRecordId(audit_record_id);
	}
	public String getPageUrl() {
		return page_url;
	}
	public void setPageUrl(String page_url) {
		this.page_url = page_url;
	}
	
}
