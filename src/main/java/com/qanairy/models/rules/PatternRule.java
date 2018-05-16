package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.models.PageElement;
import com.qanairy.persistence.Rule;

/**
 * Defines a regular expression based rule that applies to the entire text content(beginning to end) of a field.
 */
public class PatternRule implements Rule {

	private String value;

	public PatternRule(String pattern){
		this.value = pattern;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return RuleType.PATTERN;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getValue() {
		return this.value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElement elem) {
		String pattern = "/^" + elem.getAttribute("vals").getVals().toString() + " $/";
		Matcher matcher = Pattern.compile(this.value).matcher(pattern);
	    return matcher.matches();
	}
}
