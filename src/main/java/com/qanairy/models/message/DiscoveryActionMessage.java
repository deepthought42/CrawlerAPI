package com.qanairy.models.message;

import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;

/**
 * 
 * 
 */
public class DiscoveryActionMessage {
	private DiscoveryAction action;
	private Domain domain;
	private Account account;
	private BrowserType browser;
	
	public DiscoveryActionMessage(DiscoveryAction action, Domain domain, Account account, BrowserType browser){
		setAction(action);
		setDomain(domain);
		setAccount(account);
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

	public Account getAccount() {
		return account;
	}

	private void setAccount(Account account) {
		this.account = account;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	public void setBrowser(BrowserType browser) {
		this.browser = browser;
	}
}
