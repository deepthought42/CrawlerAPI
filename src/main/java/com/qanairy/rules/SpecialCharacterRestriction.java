package com.qanairy.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qanairy.models.PageElement;

/**
 * Verifies that an element doesn't have any special characters in its value
 *
 */
public class SpecialCharacterRestriction implements Rule{

	private String value;
	
	public SpecialCharacterRestriction() {
		this.value = "[a-zA-Z0-9]*";
	}
	
	@Override
	public RuleType getType() {
		return RuleType.SPECIAL_CHARACTER_RESTRICTION;
	}

	@Override
	public String getValue() {
		return null;
	}
	
	@Override
	public Boolean evaluate(PageElement elem) {
		Pattern pattern = Pattern.compile(this.value);

        Matcher matcher = pattern.matcher(elem.getAttribute("val").getVals().toString());
		return matcher.matches();
	}
}
