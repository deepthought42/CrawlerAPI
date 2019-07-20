package com.qanairy.models.message;

import java.net.URL;

import com.qanairy.models.Account;
import com.qanairy.models.enums.DomainAction;

public class DomainActionMessage {
	private URL domain;
	private DomainAction action;
	private Account account;
	
	public DomainActionMessage(URL domain_url, DomainAction actionm, Account account){
		setDomain(domain_url);
		setAction(action);
		setAccount(account);
	}
	
	public URL getDomain() {
		return domain;
	}
	
	private void setDomain(URL domain) {
		this.domain = domain;
	}
	
	public DomainAction getAction() {
		return action;
	}
	
	private void setAction(DomainAction action) {
		this.action = action;
	}

	public Account getAccount() {
		return account;
	}

	private void setAccount(Account account) {
		this.account = account;
	}
}
