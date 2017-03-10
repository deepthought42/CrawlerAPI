package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;
import com.qanairy.rules.RuleType;

/**
 * Defines a {@link FormRule} where the numbers 1-9 cannot appear in a given value when evaluated
 */
public class NumericRestrictionRule implements FormRule {

	private FormRuleType type;
	
	public NumericRestrictionRule() {
		this.type = FormRuleType.NUMERIC_RESTRICTION;
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
	public Boolean evaluate(FormField field) {
		Pattern pattern = Pattern.compile("[0-9]*");

        Matcher matcher = pattern.matcher(field.getInputElement().getText());
		return !matcher.matches();
	}
}
