package com.qanairy.models.rules;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

/**
 * Defines a {@link Rule} where all letters a-z are not allowed regardless of case
 */
public class AlphabeticRestrictionRule extends Rule{

	private String key;
	private String value;
	private RuleType type;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElement elem) {
		Pattern pattern = Pattern.compile(this.value);

        Matcher matcher = pattern.matcher(elem.getText());
		return !matcher.matches();
	}

	public AlphabeticRestrictionRule() {
		this.value = "[a-zA-Z]*";
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
		this.type = RuleType.ALPHABETIC_RESTRICTION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return RuleType.ALPHABETIC_RESTRICTION;
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
