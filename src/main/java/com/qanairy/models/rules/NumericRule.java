package com.qanairy.models.rules;

import org.apache.commons.lang3.StringUtils;
import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;


/**
 * Defines a min/max value or length {@link Rule} on a {@link ElementState}
 */
public class NumericRule extends Rule{
	public NumericRule(){}
	
	/**
	 * @param type
	 * @param value the length of the value allowed written as a {@linkplain String}. (eg. "3" -> length 3)
	 */
	public NumericRule(RuleType type, String value){
		setType(type);
		setValue(value);
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(ElementState elem) {
		for(Attribute attribute: elem.getAttributes()){
			if(attribute.getName().equals("val")){
				String field_value = attribute.getVals().toString();
				if(this.getType().equals(RuleType.MAX_LENGTH)){
					return field_value.length() <= Integer.parseInt(this.getValue());
				}
				else if(this.getType().equals(RuleType.MIN_LENGTH)){
					return field_value.length() >= Integer.parseInt(this.getValue());
				}
				else if(this.getType().equals(RuleType.MIN_VALUE)){
					return Integer.parseInt(field_value) >= Integer.parseInt(this.getValue());
				}
				else if(this.getType().equals(RuleType.MAX_VALUE)){
					return Integer.parseInt(field_value)  <= Integer.parseInt(this.getValue());
				}
			}
		}
		return false;
	}
	
	public static String generateRandomAlphabeticString(int str_length){
		return StringUtils.repeat("a", str_length);
	}
}
