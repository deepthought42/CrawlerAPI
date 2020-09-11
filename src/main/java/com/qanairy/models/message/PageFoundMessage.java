package com.qanairy.models.message;

import com.qanairy.models.PageVersion;

/**
 * Message that signals a new page has been identified and is ready to be processed
 * 
 */
public class PageFoundMessage extends Message{
	private PageVersion page;
	
	public PageFoundMessage(PageVersion page){
		//super(domain.getHost(), account_id);
		setPage(page);
	}

	public PageVersion getPage() {
		return page;
	}

	public void setPage(PageVersion page) {
		this.page = page;
	}
}
