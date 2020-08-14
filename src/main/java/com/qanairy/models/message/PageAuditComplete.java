package com.qanairy.models.message;

import com.qanairy.models.Page;

public class PageAuditComplete {

	private Page page;
	
	public PageAuditComplete(Page page) {
		setPageState(page);
	}

	public Page getPageState() {
		return page;
	}

	public void setPageState(Page page) {
		this.page = page;
	}

}
