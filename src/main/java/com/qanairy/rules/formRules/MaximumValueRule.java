package com.qanairy.rules.formRules;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

/**
 * Rule that defines the maximum allowed value for a field
 */
public class MaximumValueRule implements ValueBasedFormRule<Integer> {

	private int max_value;
	
	public MaximumValueRule(int max_value) {
		this.max_value = max_value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public FormRuleType getType() {
		return FormRuleType.MAXIMUM_VALUE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		
		return Integer.parseInt(field.getInputElement().getText()) < this.max_value;
	}

	@Override
	public Integer getValue() {
		return this.max_value;
	}

}
