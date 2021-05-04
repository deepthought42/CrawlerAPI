package com.looksee.models.message;

import com.looksee.models.Domain;
import com.looksee.models.enums.AuditStage;

/**
 * Message containing details of which sort of audit to run for the domain
 * 
 */
public class DomainAuditMessage {
	private Domain domain;
	private AuditStage stage;
	
	public DomainAuditMessage(Domain domain, AuditStage stage){
		assert domain != null;
		assert stage != null;
		
		setDomain(domain);
		setStage(stage);
	}
	
	
	public Domain getDomain() {
		return domain;
	}
	
	private void setDomain(Domain domain) {
		this.domain = domain;
	}

	public AuditStage getStage() {
		return stage;
	}

	public void setStage(AuditStage stage) {
		this.stage = stage;
	}
}
