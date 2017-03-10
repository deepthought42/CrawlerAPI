package com.qanairy.rules.formRules;

import com.minion.browsing.Attribute;
import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

public class DisabledRule implements FormRule {

	private FormRuleType type;
	
	public DisabledRule() {
		this.type = FormRuleType.DISABLED;
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
		/* 
		 * Also check for 
		 * 
		 * display: none;
		 * visibility: hidden;
		 * 
		 */
	
		Attribute attr = field.getInputElement().getAttribute("disabled");
		System.err.println("!DISABLED RULE THIS FEATURE NEEDS A PROPER IMPLEMENTATION!!!");
		return attr.getVals().length == 0;
	}
}
