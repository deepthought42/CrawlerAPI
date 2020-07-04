package com.qanairy.models.audit;

import java.util.List;

import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

public abstract class InformationArchitectureAudit extends PageStateAudit {

	public InformationArchitectureAudit(List<String> best_practices, String ada_compliance_description, String description, AuditSubcategory subcategory) {
		super(AuditCategory.INFORMATION_ARCHITECTURE, best_practices, ada_compliance_description, description, subcategory);
	}
}
