package com.crawlerApi.models.rules;

import com.crawlerApi.models.Element;

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
	public Boolean evaluate(Element elem) {
		return elem.getAttributes().containsKey("required");
	}
}
