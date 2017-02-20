package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.Attribute;
import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

public class DisabledRule implements FormRule<Boolean> {

	private FormRuleType type;
	private boolean value;
	
	public DisabledRule() {
		this.type = FormRuleType.DISABLED;
		this.value = value;
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
	public Boolean getValue() {
		return this.value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		
		Attribute attr = field.getInputElement().getAttribute("disabled");
		System.err.println("!DISABLED RULE THIS FEATURE NEEDS A PROPER IMPLEMENTATION!!!");
		return attr.getVals().length == 0;
	}
}
