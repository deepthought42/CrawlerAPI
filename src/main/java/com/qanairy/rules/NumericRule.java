package com.qanairy.rules;

import org.apache.commons.lang3.StringUtils;

import com.qanairy.models.PageElement;

/**
 * Defines a min/max value or length {@link Rule} on a {@link PageElement}
 */
public class NumericRule implements Rule{
	
	private RuleType type;
	private String value;
	
	/**
	 * @param type
	 * @param value the length of the value allowed written as a {@linkplain String}. (eg. "3" -> length 3)
	 */
	public NumericRule(RuleType type, String value){
		this.type = type;
		this.value = value;
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
	public Boolean evaluate(PageElement elem) {
		String field_value = elem.getAttribute("val").getVals().toString();
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
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getValue() {
		return this.value;
	}	
	
	public static String generateRandomAlphabeticString(int str_length){
		return StringUtils.repeat("a", str_length);
	}
}
