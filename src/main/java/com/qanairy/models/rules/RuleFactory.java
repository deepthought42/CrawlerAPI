package com.qanairy.models.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.persistence.Rule;

/**
 * 
 *
 */
public class RuleFactory {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(RuleFactory.class);

	/**
	 * 
	 * @param type
	 * @param value
	 * @return
	 */
	public static Rule build(String type, String value){
		if(type.equals(RuleType.ALPHABETIC_RESTRICTION.toString())){
			return new AlphabeticRestrictionRule();
		}
		else if(type.equals(RuleType.DISABLED.toString())){
			return new DisabledRule();
		}
		else if(type.equals(RuleType.EMAIL_PATTERN.toString())){
			//System.err.println("Creating email pattern rule");
			return new EmailPatternRule();
		}
		else if(type.equals(RuleType.MAX_LENGTH.toString())){
			return new NumericRule(RuleType.MAX_LENGTH, value);
		}
		else if(type.equals(RuleType.MAX_VALUE.toString())){
			return new NumericRule(RuleType.MAX_VALUE, value);
		}
		else if(type.equals(RuleType.MIN_LENGTH.toString())){
			return new NumericRule(RuleType.MIN_LENGTH, value);
		}
		else if(type.equals(RuleType.MIN_VALUE.toString())){
			return new NumericRule(RuleType.MIN_VALUE, value);
		}
		else if(type.equals(RuleType.NUMERIC_RESTRICTION.toString())){
			return new NumericRestrictionRule();
		}
		else if(type.equals(RuleType.PATTERN.toString())){
			return new PatternRule(value);
		}
		else if(type.equals(RuleType.READ_ONLY.toString())){
			return new ReadOnlyRule();
		}
		else if(type.equals(RuleType.REQUIRED.toString())){
			return new RequirementRule();
		}
		else if(type.equals(RuleType.SPECIAL_CHARACTER_RESTRICTION.toString())){
			return new SpecialCharacterRestriction();
		}
		//System.err.println("returning null rule");
		return null;
	}
}
