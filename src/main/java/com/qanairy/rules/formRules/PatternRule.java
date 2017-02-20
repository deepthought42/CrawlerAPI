package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

/**
 * 
 *
 */
public class PatternRule implements FormRule<Pattern> {

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
		Matcher matcher = this.pattern.matcher(field.getInputElement().getText());
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
