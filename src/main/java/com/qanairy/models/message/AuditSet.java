package com.qanairy.models.message;

import java.util.List;

import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;

/**
 * Message that contains a {@link PageState} that is ready for analysis
 * 
 */
public class AuditSet extends Message {
	private List<Audit> audits;
	
	public AuditSet(List<Audit> audits){
		setAudits(audits);
	}

	public List<Audit> getAudits() {
		return audits;
	}

	public void setAudits(List<Audit> audits) {
		this.audits = audits;
	}
}
