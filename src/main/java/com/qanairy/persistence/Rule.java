package com.qanairy.persistence;

import java.util.Optional;

import com.minion.browsing.form.FormField;
import com.qanairy.models.rules.RuleType;
import com.qanairy.persistence.PageElement;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Property;

/**
 * Defines rule to be used to evaluate if a {@link FormField} has a value that satisfies the 
 * rule based on its {@link RuleType}
 *
 * @param <T> a generic value that is used to define the type of value returned
 */
public abstract class Rule extends AbstractVertexFrame {

	@Property("key")
	public abstract void setKey(String key);

	@Property("key")
	public abstract String getKey();

	@Property("rule_type")
	public abstract RuleType getType();

	@Property("rule_type")
	public abstract void setType(RuleType type);
	
	@Property("value")
	public abstract Optional<String> getValue();

	@Property("value")
	public abstract void setValue(Optional<String> value);
	
	/**
	 * evaluates the rule to determine if it is satisfied
	 * 
	 * @return boolean value indicating the rule is satisfied or not
	 */
	public abstract Boolean evaluate(PageElement val);	

	
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
