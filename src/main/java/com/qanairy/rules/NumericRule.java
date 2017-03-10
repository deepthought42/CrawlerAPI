package com.qanairy.rules;

import org.apache.commons.lang3.StringUtils;

public class NumericRule implements Rule<Integer, String>{
	
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
	public RuleType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean evaluate(String val) {
		if(this.getType().equals(NumericRuleType.MAX_LENGTH)){
			return val.length() <= this.getValue();
		}
		else if(this.getType().equals(NumericRuleType.MIN_LENGTH)){
			return val.length() >= this.getValue();
		}
		else if(this.getType().equals(NumericRuleType.MIN_VALUE)){
			return Integer.parseInt(val) >= this.getValue();
		}
		else if(this.getType().equals(NumericRuleType.MAX_VALUE)){
			return Integer.parseInt(val)  <= this.getValue();
		}
		return false;
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
