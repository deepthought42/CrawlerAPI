package com.qanairy.models.audit;

import java.util.List;

import com.qanairy.models.enums.AuditCategory;

public abstract class ColorManagementAudit extends Audit {

	public ColorManagementAudit(List<String> best_practices, String ada_compliance_description, String Description, String name) {
		super(AuditCategory.COLOR_MANAGEMENT, best_practices, ada_compliance_description, ada_compliance_description, name);
	}
}
