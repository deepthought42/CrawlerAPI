package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

public class EmailPatternRule extends Rule {

	private String key;
	private RuleType type;
	private String value;
	
	public EmailPatternRule() {
		setType(RuleType.EMAIL_PATTERN);
		setValue("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
	}

	@Override
	public Boolean evaluate(PageElement page_element) {
		String pattern = "/^" + page_element.getAttribute("vals").getVals().toString() + " $/";
		Matcher matcher = Pattern.compile(getValue()).matcher(pattern);
	    return matcher.matches();
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
	public RuleType getType() {
		return RuleType.EMAIL_PATTERN;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getValue() {
		return this.value;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}
}
