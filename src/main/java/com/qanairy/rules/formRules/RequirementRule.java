package com.qanairy.rules.formRules;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

public class RequirementRule implements FormRule{
		
	/**
	 * Constructs Rule
	 */
	public RequirementRule(){
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public FormRuleType getType() {
		return FormRuleType.REQUIRED;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		return field.getInputElement().getAttributes().contains("required");
	}
}
