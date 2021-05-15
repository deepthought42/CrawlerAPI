package com.looksee.models.message;

import com.looksee.models.PageState;

public class PageStateMessage extends Message {

	private PageState page_state;
	
	public PageStateMessage(PageState page_state, long domain_id, String account_id, long audit_record_id) {
		super(domain_id, account_id, audit_record_id);
		setPageState(page_state);
	}

	public PageState getPageState() {
		return page_state;
	}

	public void setPageState(PageState page_state) {
		this.page_state = page_state;
	}
}
