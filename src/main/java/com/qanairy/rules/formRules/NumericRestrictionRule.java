package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;
import com.qanairy.rules.RuleType;

public class NumericRestrictionRule implements FormRule<Boolean> {

	private FormRuleType type;
	private boolean value;
	
	public NumericRestrictionRule(boolean value) {
		this.type = FormRuleType.NUMERIC_RESTRICTION;
		this.value = value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean getValue() {
		return this.value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		Pattern pattern = Pattern.compile("[0-9]*");

        Matcher matcher = pattern.matcher(field.getInputElement().getText());
		return matcher.matches();
	}
}
