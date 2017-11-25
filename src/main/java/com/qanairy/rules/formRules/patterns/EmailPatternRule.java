package com.qanairy.rules.formRules.patterns;

import java.util.regex.Pattern;

import com.qanairy.rules.formRules.FormRuleType;
import com.qanairy.rules.formRules.PatternRule;

public class EmailPatternRule extends PatternRule {

	private static String email_regex_str = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";

	public EmailPatternRule() {
		super(Pattern.compile(email_regex_str, Pattern.CASE_INSENSITIVE), FormRuleType.EMAIL_PATTERN);
	}
}
