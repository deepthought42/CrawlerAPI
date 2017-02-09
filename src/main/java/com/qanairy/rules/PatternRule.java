package com.qanairy.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 *
 */
public class PatternRule implements Rule<Pattern, String> {

	private PatternRuleType type;
	private Pattern pattern;
	
	public PatternRule(PatternRuleType type, Pattern pattern){
		this.type = type;
		this.pattern = pattern;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(String value) {
		Matcher matcher = this.pattern.matcher(value);
	    return matcher.matches();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pattern getValue() {
		return pattern;
	}

}
