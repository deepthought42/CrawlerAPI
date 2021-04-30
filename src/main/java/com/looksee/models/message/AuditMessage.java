package com.looksee.models.message;

import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;

public class AuditMessage {
	
	String category;
	String name;
	String message;
	
	public AuditMessage() {}
	
	public AuditMessage(
			AuditCategory category,
			AuditName subcategory,
			String message
	) {
		setCategory(category);
	}

	public AuditCategory getCategory() {
		return AuditCategory.valueOf(category);
	}

	public void setCategory(AuditCategory category) {
		this.category = category.getShortName();
	}

	public AuditName getName() {
		return AuditName.valueOf(name);
	}

	public void setName(AuditName name) {
		this.name = name.getShortName();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
