package com.looksee.models.message;

import com.looksee.models.Domain;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.DiscoveryAction;

/**
 * 
 * 
 */
public class DiscoveryActionMessage {
	private DiscoveryAction action;
	private Domain domain;
	private String account_id;
	private BrowserType browser;
	
	public DiscoveryActionMessage(DiscoveryAction action, Domain domain, String account_id, BrowserType browser){
		setAction(action);
		setDomain(domain);
		setAccountId(account_id);
		setBrowser(browser);
	}
	
	public DiscoveryAction getAction() {
		return action;
	}
	
	private void setAction(DiscoveryAction action) {
		this.action = action;
	}
	
	public Domain getDomain() {
		return domain;
	}
	
	private void setDomain(Domain domain) {
		this.domain = domain;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	public void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	public String getAccountId() {
		return account_id;
	}

	public void setAccountId(String account_id) {
		this.account_id = account_id;
	}
}
