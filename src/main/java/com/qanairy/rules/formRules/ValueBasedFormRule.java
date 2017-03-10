package com.qanairy.rules.formRules;

import com.qanairy.rules.FormRule;

public interface ValueBasedFormRule<T> extends FormRule {
	/**
	 * @return the value of this rule based on the type of rule. 
	 * 
	 * NB: The type is defined in the implementation
	 */
	T getValue();
}
