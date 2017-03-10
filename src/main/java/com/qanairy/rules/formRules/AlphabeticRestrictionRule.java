package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

/**
 * Defines a {@link FormRule} where all letters a-z are not allowed regardless of case
 */
public class AlphabeticRestrictionRule implements FormRule{

	private FormRuleType type;
	
	/**
	 * 
	 */
	public AlphabeticRestrictionRule() {
		this.type = FormRuleType.ALPHABETIC_RESTRICTION;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public FormRuleType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		Pattern pattern = Pattern.compile("[a-zA-Z]*");

        Matcher matcher = pattern.matcher(field.getInputElement().getText());
		return !matcher.matches();
	}
}
