package com.looksee.models.message;

public class ElementsSaved extends Message{
	private String page_url;
	private int element_count;
	
	public ElementsSaved(long account_id,
						String page_url, 
						long audit_record_id,
						int element_count
	) {
		setAccountId(account_id);
		setPageUrl(page_url);
		setAuditRecordId(audit_record_id);
	}
	
	public String getPageUrl() {
		return page_url;
	}
	
	public void setPageUrl(String page_url) {
		this.page_url = page_url;
	}

	public int getElementCount() {
		return element_count;
	}

	public void setElementCount(int element_count) {
		this.element_count = element_count;
	}
}
