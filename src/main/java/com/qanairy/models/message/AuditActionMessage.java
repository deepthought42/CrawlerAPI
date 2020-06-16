package com.qanairy.models.message;

import java.util.List;

import com.qanairy.models.Domain;
import com.qanairy.models.enums.AuditType;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class AuditActionMessage extends Message{
	private DiscoveryAction action;
	private List<AuditType> audits_types;
	
	public AuditActionMessage(DiscoveryAction action, Domain domain, String account_id){
		super(domain.getHost(), account_id);
		setAction(action);
	}
	
	public DiscoveryAction getAction() {
		return action;
	}
	
	private void setAction(DiscoveryAction action) {
		this.action = action;
	}

	public List<AuditType> getAuditsTypes() {
		return audits_types;
	}

	public void setAuditsTypes(List<AuditType> audits_types) {
		this.audits_types = audits_types;
	}
}
