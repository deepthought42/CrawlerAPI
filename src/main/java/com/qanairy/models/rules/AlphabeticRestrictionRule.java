package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.models.PageElement;
import com.qanairy.persistence.Rule;

/**
 * Defines a {@link Rule} where all letters a-z are not allowed regardless of case
 */
public class AlphabeticRestrictionRule implements Rule{

	private String value;

	public AlphabeticRestrictionRule() {
		this.value = "[a-zA-Z]*";
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return RuleType.ALPHABETIC_RESTRICTION;
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

	@Override
	public String getValue() {
		return this.value;
	}
}
