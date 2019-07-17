package com.qanairy.models.message;

import java.net.URL;

import com.qanairy.models.enums.DomainAction;

public class DomainActionMessage {
	private URL domain;
	private DomainAction action;
	
	public DomainActionMessage(URL domain_url, DomainAction action){
		setDomain(domain_url);
		setAction(action);
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
}
