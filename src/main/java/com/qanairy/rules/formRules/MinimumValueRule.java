package com.qanairy.rules.formRules;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

/**
 * Defines a minimum value allowed on a field
 */
public class MinimumValueRule implements ValueBasedFormRule<Integer> {

	private int min_value;
	
	public MinimumValueRule(int min_value) {
		this.min_value = min_value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public FormRuleType getType() {
		return FormRuleType.MINIMUM_VALUE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		return Integer.parseInt(field.getInputElement().getText()) >= this.min_value;
	}

	@Override
	public Object getValue() {
		return this.min_value;
	}
	
	

}
