package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

/**
 * Defines a regular expression based rule that applies to the entire text content(beginning to end) of a field.
 */
public class PatternRule extends Rule {

	private String key;
	private String value;
	private RuleType type;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElement elem) {
		String pattern = "/^" + elem.getAttributes().get(elem.getAttributes().indexOf("vals")).getVals().toString() + " $/";
		Matcher matcher = Pattern.compile(this.value).matcher(pattern);
	    return matcher.matches();
	}
	
	public PatternRule(String pattern){
		this.value = pattern;
		setType(RuleType.PATTERN);
		setKey(super.generateKey());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getValue() {
		return this.value;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}

}
