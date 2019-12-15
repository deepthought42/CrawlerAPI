package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;

/**
 * Defines a regular expression based rule that applies to the entire text content(beginning to end) of a field.
 */
public class PatternRule extends Rule {
	public PatternRule(){}
	
	public PatternRule(String pattern){
		setValue(pattern);
		setType(RuleType.PATTERN);
		setKey(generateKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(ElementState elem) {
		for(Attribute attribute: elem.getAttributes()){
			if(attribute.getName().equals("vals")){
				String pattern = "/^" + attribute.getVals().toString() + " $/";
				Matcher matcher = Pattern.compile(getValue()).matcher(pattern);
			    return matcher.matches();
			}
		}
		return null;
	}
}
