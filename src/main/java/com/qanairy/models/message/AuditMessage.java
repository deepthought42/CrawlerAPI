package com.qanairy.models.message;

import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

public class AuditMessage {
	
	String category;
	String name;
	String message;
	
	public AuditMessage() {}
	
	public AuditMessage(
			AuditCategory category,
			AuditSubcategory subcategory,
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

	public AuditSubcategory getName() {
		return AuditSubcategory.valueOf(name);
	}

	public void setName(AuditSubcategory name) {
		this.name = name.getShortName();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
