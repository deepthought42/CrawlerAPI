package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;

/**
 * Verifies that an element doesn't have any special characters in its value
 *
 */
public class SpecialCharacterRestriction extends Rule {

	private String key;
	private String value;
	private RuleType type;
	
	public SpecialCharacterRestriction() {
		setValue("[a-zA-Z0-9]*");
		setType(RuleType.SPECIAL_CHARACTER_RESTRICTION);
		setKey(super.generateKey());
	}

	@Override
	public Boolean evaluate(PageElement elem) {
		Pattern pattern = Pattern.compile(this.value);
		for(Attribute attribute: elem.getAttributes()){
			if(attribute.getName().equals("vals")){
		        Matcher matcher = pattern.matcher(attribute.getVals().toString());
				return matcher.matches();
			}
		}
		return null;
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
