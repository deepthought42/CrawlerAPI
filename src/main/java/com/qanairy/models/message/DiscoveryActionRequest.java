package com.qanairy.models.message;

import com.qanairy.models.Domain;

public class DiscoveryActionRequest {
	private Domain domain;
	
	public DiscoveryActionRequest(Domain domain) {
		this.setDomain(domain);
	}

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}	
}
