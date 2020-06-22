package com.qanairy.models.message;

import com.qanairy.models.PageState;

/**
 * Message that contains a {@link PageState} that is ready for analysis
 * 
 */
public class PageStateAuditMessage extends Message{
	private PageState page;
	
	public PageStateAuditMessage(PageState page){
		//super(domain.getHost(), account_id);
		setPageState(page);
	}

	public PageState getPageState() {
		return page;
	}

	public void setPageState(PageState page) {
		this.page = page;
	}
}
