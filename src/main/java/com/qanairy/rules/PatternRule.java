package com.qanairy.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	public boolean evaluate(String value) {
		 Matcher matcher = pattern.matcher(value);
	        boolean matches = matcher.matches();
		return matches;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pattern getValue() {
		return pattern;
	}

}
