package com.qanairy.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.models.PageElement;

public class EmailPatternRule implements Rule {

	private static String email_regex_str = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
	
	public EmailPatternRule() {
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RuleType getType() {
		return RuleType.EMAIL_PATTERN;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getValue() {
		return email_regex_str;
	}

	@Override
	public Boolean evaluate(PageElement page_element) {
		String pattern = "/^" + page_element.getAttribute("vals").getVals().toString() + " $/";
		Matcher matcher = Pattern.compile(getValue()).matcher(pattern);
	    return matcher.matches();
	}
}
