package com.qanairy.models.message;

import com.qanairy.models.Page;

/**
 * Message that signals a new page has been identified and is ready to be processed
 * 
 */
public class PageFoundMessage extends Message{
	private Page page;
	
	public PageFoundMessage(Page page){
		//super(domain.getHost(), account_id);
		setPage(page);
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}
}
