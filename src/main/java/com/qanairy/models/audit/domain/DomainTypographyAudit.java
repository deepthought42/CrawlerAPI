package com.qanairy.models.audit.domain;

import java.util.List;

import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

public abstract class DomainTypographyAudit extends DomainAudit {

	public DomainTypographyAudit(List<String> best_practices, String ada_compliance_description, String description, AuditSubcategory subcategory) {
		super(AuditCategory.TYPOGRAPHY, best_practices, ada_compliance_description, description, subcategory);
	}
}
