package com.qanairy.rules.formRules;

import org.apache.commons.lang3.StringUtils;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.NumericRuleType;

/**
 * Defines a min/max value or length {@link FormRule} on a {@link FormField}
 */
public class NumericRule implements ValueBasedFormRule<Integer>{
	
	private NumericRuleType type;
	private Integer value;
	
	public NumericRule(NumericRuleType type, Integer value){
		this.type = type;
		this.value = value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public NumericRuleType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		String field_value = field.getInputElement().getText();
		if(this.getType().equals(NumericRuleType.MAX_LENGTH)){
			return field_value.length() <= this.getValue();
		}
		else if(this.getType().equals(NumericRuleType.MIN_LENGTH)){
			return field_value.length() >= this.getValue();
		}
		else if(this.getType().equals(NumericRuleType.MIN_VALUE)){
			return Integer.parseInt(field_value) >= this.getValue();
		}
		else if(this.getType().equals(NumericRuleType.MAX_VALUE)){
			return Integer.parseInt(field_value)  <= this.getValue();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getValue() {
		return this.value;
	}	
	
	public static String generateRandomAlphabeticString(int str_length){
		return StringUtils.repeat("a", str_length);
	}
}
