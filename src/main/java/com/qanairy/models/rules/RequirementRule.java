package com.qanairy.models.rules;

import com.qanairy.models.ElementState;

public class RequirementRule extends Rule{
	/**
	 * Constructs Rule
	 */
	public RequirementRule(){
		setValue("");
		setType(RuleType.REQUIRED);
		this.setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(ElementState elem) {
		return elem.getAttributes().containsKey("required");
	}
}
