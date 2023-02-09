package com.looksee.models.message;

import com.looksee.models.enums.BrowserType;


public class UrlMessage extends Message{
	private String url;
	private BrowserType browser;
	
	public UrlMessage(String url, 
					  BrowserType browser, 
					  long domain_id, 
					  long account_id, 
					  long audit_record_id)
	{
		setUrl(url);
		setBrowser(browser);
		setDomainId(domain_id);
		setAccountId(account_id);
		setAuditRecordId(audit_record_id);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	private void setBrowser(BrowserType browser) {
		this.browser = browser;
	}
}
