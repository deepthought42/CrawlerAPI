package com.qanairy.models.rules;

/**
 * Defines all types of rules that exist in the system
 */
public enum RuleType {
	PATTERN, REQUIRED, ALPHABETIC_RESTRICTION, SPECIAL_CHARACTER_RESTRICTION, NUMERIC_RESTRICTION, DISABLED, NO_VALIDATE, 
	READ_ONLY, MINIMUM_VALUE, MAXIMUM_VALUE, MINIMUM_LENGTH, MAXIMUM_LENGTH, MIN_LENGTH, MAX_LENGTH, MIN_VALUE, MAX_VALUE, 
	EMAIL_PATTERN, CLICKABLE, DOUBLE_CLICKABLE, MOUSE_RELEASE, MOUSE_OVER, SCROLLABLE;
	
	//NO VALIDATE MAY NOT BE USEFUL
	/*
	 * NB: This list is complete since the system assumes that any type of absence of a restriction is either waiting to have a 
	 * restriction discovered, or doesn't have any restriction. For example, when a field only allows numbers, we assume that such a field
	 * will have alphabetic and special character restrictions
	 */

}
