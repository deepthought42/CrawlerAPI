package com.qanairy.models.rules;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.minion.browsing.form.FormField;
import com.qanairy.models.PageElement;
import com.qanairy.models.Persistable;
import com.qanairy.models.rules.RuleType;

/**
 * Defines rule to be used to evaluate if a {@link FormField} has a value that satisfies the 
 * rule based on its {@link RuleType}
 *
 * @param <T> a generic value that is used to define the type of value returned
 */
public abstract class Rule implements Persistable {
	
	public abstract void setKey(String key);

	public abstract String getKey();

	public abstract RuleType getType();

	public abstract void setType(RuleType type);
	
	public abstract String getValue();

	public abstract void setValue(String value);
	
	/**
	 * evaluates the rule to determine if it is satisfied
	 * 
	 * @return boolean value indicating the rule is satisfied or not
	 */
	abstract Boolean evaluate(PageElement val);	

	
	@Override
	public String generateKey() {
		return this.getType()+"::"+this.getValue();
	}
	
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
