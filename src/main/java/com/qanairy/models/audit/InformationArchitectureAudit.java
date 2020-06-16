package com.qanairy.models.audit;

import java.util.List;

import com.qanairy.models.enums.AuditCategory;

public abstract class InformationArchitectureAudit extends Audit {

	public InformationArchitectureAudit(List<String> best_practices, String ada_compliance_description, String Description, String name) {
		super(AuditCategory.INFORMATION_ARCHITECTURE, best_practices, ada_compliance_description, ada_compliance_description, name);
	}
}
