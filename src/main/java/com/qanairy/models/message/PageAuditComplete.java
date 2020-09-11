package com.qanairy.models.message;

import com.qanairy.models.PageVersion;

public class PageAuditComplete {

	private PageVersion page;
	
	public PageAuditComplete(PageVersion page) {
		setPageState(page);
	}

	public PageVersion getPageState() {
		return page;
	}

	public void setPageState(PageVersion page) {
		this.page = page;
	}

}
