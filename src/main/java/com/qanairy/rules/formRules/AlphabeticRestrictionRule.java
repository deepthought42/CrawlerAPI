package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

public class AlphabeticRestrictionRule implements FormRule<Boolean>{

	private FormRuleType type;
	private boolean value;
	
	/**
	 * 
	 */
	public AlphabeticRestrictionRule() {
		this.type = FormRuleType.ALPHABETIC_RESTRICTION;
		this.value = true;
	}
	
	/**
	 * 
	 */
	@Override
	public FormRuleType getType() {
		return this.type;
	}

	/**
	 * 
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
		Pattern pattern = Pattern.compile("[a-zA-Z]*");

        Matcher matcher = pattern.matcher(field.getInputElement().getText());
		return matcher.matches();
	}
}
