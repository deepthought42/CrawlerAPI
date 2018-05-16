package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.models.PageElement;
import com.qanairy.persistence.Rule;

/**
 * Defines a {@link Rule} where the numbers 1-9 cannot appear in a given value when evaluated
 */
public class NumericRestrictionRule implements Rule {

	private String value;
	
	public NumericRestrictionRule() {
		this.value = "[0-9]*";
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return RuleType.NUMERIC_RESTRICTION;
	}

	@Override
	public String getValue() {
		return this.value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElement elem) {
		Pattern pattern = Pattern.compile(this.value);

        Matcher matcher = pattern.matcher(elem.getText());
		return !matcher.matches();
	}
}
