package com.qanairy.models.rules;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;


/**
 * Defines a min/max value or length {@link Rule} on a {@link PageElement}
 */
public class NumericRule extends Rule{
	
	private String key;
	private RuleType type;
	private String value;
	
	/**
	 * @param type
	 * @param value the length of the value allowed written as a {@linkplain String}. (eg. "3" -> length 3)
	 */
	public NumericRule(RuleType type, String value){
		setType(type);
		setValue(value);
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


	@Override
	public void setValue(String value) {
		this.value = value;
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
