package com.qanairy.models.audit;

import java.util.List;

import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

public abstract class TypographyAudit extends PageStateAudit {

	public TypographyAudit(List<String> best_practices, String ada_compliance_description, String description, AuditSubcategory subcategory) {
		super(AuditCategory.TYPOGRAPHY, best_practices, ada_compliance_description, description, subcategory);
	}
}
