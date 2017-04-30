package com.qanairy.rules.formRules;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.RuleType;

/**
 * Defines a Maximum length for an input field
 */
public class MaxLengthRule implements ValueBasedFormRule<Integer> {

	private int value;

	public MaxLengthRule(int value) {
		this.value = value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return FormRuleType.MAXIMUM_LENGTH;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField val) {
		
		return val.getInputElement().getText().length() <= this.value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getValue() {
		return this.value;
	}
}
