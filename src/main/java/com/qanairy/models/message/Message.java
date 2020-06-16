package com.qanairy.models.message;

import com.qanairy.models.Domain;

/**
 * Core Message object that defines global fields that are to be used by all Message objects
 */
public abstract class Message {
	private String domain_host;
	private String account_id;
	
	/**
	 * 
	 * @param domain eg. example.com
	 * @param account_id
	 */
	Message(String domain_host, String account_id){
		setDomainHost(domain_host);
		setAccountId(account_id);
	}
	
	public String getDomainHost() {
		return domain_host;
	}
	
	private void setDomainHost(String domain) {
		this.domain_host = domain;
	}
	
	public String getAccountId() {
		return account_id;
	}

	private void setAccountId(String account_id) {
		this.account_id = account_id;
	}
}
