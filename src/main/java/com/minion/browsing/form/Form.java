package com.minion.browsing.form;

import java.util.List;

public class Form {
	private List<FormField> form_fields;
	private boolean required;
	
	public List<FormField> getFormFields() {
		return form_fields;
	}
	
	public void setFormFields(List<FormField> form_fields) {
		this.form_fields = form_fields;
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public void setRequired(boolean required) {
		this.required = required;
	}
}
