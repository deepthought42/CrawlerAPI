package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.PatternRuleType;

/**
 * Defines a regular expression based rule that applies to the entire text content(beginning to end) of a field.
 */
public class PatternRule implements ValueBasedFormRule<Pattern> {

	private PatternRuleType type;
	private Pattern pattern;
	
	public PatternRule(Pattern pattern){
		this.type = PatternRuleType.REGEX;
		this.pattern = pattern;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternRuleType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		String pattern = "/^" + field.getInputElement().getText() + " $/";
		Matcher matcher = this.pattern.matcher(pattern);
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
