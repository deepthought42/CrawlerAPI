package com.qanairy.rules;


import com.minion.browsing.form.FormField;
import com.qanairy.models.PageElement;

/**
 * Defines rule to be used to evaluate if a {@link FormField} has a value that satisfies the 
 * rule based on its {@link RuleType}
 *
 * @param <T> a generic value that is used to define the type of value returned
 */
public interface Rule {
	
	/**
	 * @return the {@link RuleType} of this rule
	 */
	RuleType getType();
	
	/**
	 * @return the value as a string
	 */
	String getValue();
	
	/**
	 * evaluates the rule to determine if it is satisfied
	 * 
	 * @return boolean value indicating the rule is satisfied or not
	 */
	Boolean evaluate(PageElement val);	

	
	/**
	 * Rule types
	 * 
	 * 
	 * --- BOOLEAN RULES ---
	 * 
	 * REQUIRED
	 * ALPHABETIC_RESTRICTION
	 * SPECIAL_CHARACTER_RESTRICTION
	 * NUMBER_RESTRICTION
	 * NUMBERS_ONLY
	 * IS_ENABLED
	 * IS_NO_VALIDATE
	 * IS_READ_ONLY
	 * 
	 * 
	 * --- NUMERIC RULES  ---
	 * 
	 * MIN_LENGTH	-- ALL TEXT AND NUMBER
	 * MAX_LENGTH   -- ALL TEXT AND NUMBER
	 * 					
	 * MIN_VALUE   - ALL NUMBER AND DATE
	 * MAX_VALUE   - ALL NUMBER AND DATE
	 * 
	 * --- REGEX RULES ---
	 * 
	 * PATTERN - ALL TEXT, NUMBER, DATE INPUTS
	 */
}
