package com.qanairy.models.message;

import com.qanairy.models.Domain;
import com.qanairy.models.enums.AuditStage;

/**
 * Message containing details of which sort of audit to run for the domain
 * 
 */
public class DomainAuditMessage {
	private Domain domain;
	private AuditStage stage;
	
	public DomainAuditMessage(Domain domain, AuditStage stage){
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
