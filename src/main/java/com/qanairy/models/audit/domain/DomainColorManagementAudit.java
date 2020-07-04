package com.qanairy.models.audit.domain;

import java.util.List;

import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

public abstract class DomainColorManagementAudit extends DomainAudit {

	public DomainColorManagementAudit(List<String> best_practices, String ada_compliance_description, String description, AuditSubcategory subcategory) {
		super(AuditCategory.COLOR_MANAGEMENT, best_practices, ada_compliance_description, description, subcategory);
	}
}
