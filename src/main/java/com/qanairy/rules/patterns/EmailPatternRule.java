package com.qanairy.rules.patterns;

import com.qanairy.rules.PatternRule;
import com.qanairy.rules.RuleType;

public class EmailPatternRule extends PatternRule {

	private static String email_regex_str = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";

	public EmailPatternRule() {
		super(email_regex_str);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return RuleType.EMAIL_PATTERN;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getValue() {
		return email_regex_str;
	}
}
