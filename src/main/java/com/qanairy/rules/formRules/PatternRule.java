package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.form.FormField;

/**
 * Defines a regular expression based rule that applies to the entire text content(beginning to end) of a field.
 */
public class PatternRule implements ValueBasedFormRule<Pattern> {

	private FormRuleType type;
	private Pattern pattern;
	
	public PatternRule(Pattern pattern){
		this.type = FormRuleType.PATTERN;
		this.pattern = pattern;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public FormRuleType getType() {
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
