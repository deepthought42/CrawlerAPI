package com.qanairy.models.message;

import com.qanairy.models.PageState;

public class PageStateAuditComplete {

	private PageState page_state;
	
	public PageStateAuditComplete(PageState page_state) {
		setPageState(page_state);
	}

	public PageState getPageState() {
		return page_state;
	}

	public void setPageState(PageState page_state) {
		this.page_state = page_state;
	}

}
