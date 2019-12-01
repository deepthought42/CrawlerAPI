package com.qanairy.models.message;

import com.qanairy.models.Domain;

public class DiscoveryActionRequest {
	private Domain domain;
	private String account;
	
	public DiscoveryActionRequest(Domain domain) {
		this.setDomain(domain);
	}

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	public String getAccountId() {
		return account;
	}

	public void setAccountId(String account_id) {
		this.account = account_id;
	}
}
