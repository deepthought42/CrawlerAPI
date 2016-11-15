package com.qanairy.rules;

public class BooleanRule implements Rule<Boolean, Boolean> {

	public BooleanRuleType type;
	public Boolean value; 
	
	public BooleanRule(BooleanRuleType type, Boolean value){
		this.type = type;
		this.value = value;
	}
	
	@Override
	public RuleType getType() {
		return type;
	}

	@Override
	public boolean evaluate(Boolean val) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public Boolean getValue() {
		return this.value;
	}

}
