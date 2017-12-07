package com.qanairy.rules;

import com.qanairy.models.PageElement;

public class RequirementRule implements Rule{
		
	/**
	 * Constructs Rule
	 */
	public RequirementRule(){
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return RuleType.REQUIRED;
	}

	@Override
	public String getValue() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElement elem) {
		return elem.getAttributes().contains("required");
	}
}
