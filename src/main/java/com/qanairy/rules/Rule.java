package com.qanairy.rules;

/**
 * Defines rule to be used to evaluate if a {@link FormField} has a value that satisfies the 
 * rule based on its {@link RuleType}
 *
 * @param <T> a generic value that is used to define the type of value returned
 */
public interface Rule<T,Z> {
	
	/**
	 * @return the {@link RuleType} of this rule
	 */
	RuleType getType();
	
	/**
	 * @return the value of this rule based on the type of rule. 
	 * 
	 * NB: The type is defined in the implementation
	 */
	T getValue();
	
	/**
	 * evaluates the rule to determine if it is satisfied
	 * 
	 * @return boolean value indicating the rule is satisfied(true) or not satisfied(false)
	 */
	boolean evaluate(Z val);	
	
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
	 * --- INTEGER RULES  ---
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
