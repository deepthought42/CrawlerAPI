package com.qanairy.rules.formRules;

import com.qanairy.rules.RuleType;

public enum FormRuleType implements RuleType {
	PATTERN, REQUIRED, ALPHABETIC_RESTRICTION, SPECIAL_CHARACTER_RESTRICTION, NUMERIC_RESTRICTION, DISABLED, NO_VALIDATE, READ_ONLY, MINIMUM_VALUE, MAXIMUM_VALUE, MINIMUM_LENGTH, MAXIMUM_LENGTH;
	
	//NO VALIDATE MAY NOT BE USEFUL
	/*
	 * NB: This list is complete since the system assumes that any type of absence of a restriction is either waiting to have a 
	 * restriction discovered, or doesn't have any restriction. For example, when a field only allows numbers, we assume that such a field
	 * will have alphabetic and special character restrictions
	 */

}
