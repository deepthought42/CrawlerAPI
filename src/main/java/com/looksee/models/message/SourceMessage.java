package com.looksee.models.message;

import java.net.URL;

/**
 * A message that contains a sanitized url and its page source that will be further extracted for any links it contains.
 */
public class SourceMessage extends Message {
	private URL sanitized_url;
	private String page_src;

	public SourceMessage(Message crawl_action,
					   URL sanitized_url,
					   String page_src){
		
		super(crawl_action.getDomainId(),
			  crawl_action.getAccountId(),
			  crawl_action.getAuditRecordId());
		
		setSanitizedUrl(sanitized_url);
		setPageSrc(page_src);
	}

	public URL getSanitizedUrl(){
		return this.sanitized_url;
	}

	public void setSanitizedUrl(URL sanitized_url){
		this.sanitized_url = sanitized_url;
	}

	public String getPageSrc(){
		return page_src;
	}

	public void setPageSrc(String page_src){
		this.page_src = page_src;
	}
}
