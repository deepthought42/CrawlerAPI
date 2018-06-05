package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

/**
 * Defines a {@link Rule} where the numbers 1-9 cannot appear in a given value when evaluated
 */
public class NumericRestrictionRule extends Rule {

	private String value;
	private String key;
	private RuleType type;
	
	public NumericRestrictionRule() {
		setValue("[0-9]*");
		setType(RuleType.NUMERIC_RESTRICTION);
		setKey(generateKey());
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
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void setType(RuleType type) {
		this.type = type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return this.type;
	}

	@Override
	public String getValue() {
		return this.value;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}
}
