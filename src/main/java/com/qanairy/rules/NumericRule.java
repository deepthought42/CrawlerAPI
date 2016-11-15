package com.qanairy.rules;

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
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getValue() {
		return this.value;
	}

	@Override
	public boolean evaluate(Integer val) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
