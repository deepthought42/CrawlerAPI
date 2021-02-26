package com.qanairy.models.audit;

import java.util.Map;
import java.util.Set;

import com.qanairy.models.SimpleElement;

public class AuditElementMap {
	private Map<Audit, Set<SimpleElement>> audit_map;
	
	public AuditElementMap(Map<Audit, Set<SimpleElement>> audit_map) {
		setAuditMap(audit_map);
	}

	public Map<Audit, Set<SimpleElement>> getAuditMap() {
		return audit_map;
	}

	public void setAuditMap(Map<Audit, Set<SimpleElement>> audit_map) {
		this.audit_map = audit_map;
	}
}
