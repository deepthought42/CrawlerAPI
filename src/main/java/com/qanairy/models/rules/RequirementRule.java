package com.qanairy.models.rules;

import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

public class RequirementRule extends Rule{
		
	private String key;
	private RuleType type;
	private String value;

	/**
	 * Constructs Rule
	 */
	public RequirementRule(){
		setValue(null);
		setType(RuleType.REQUIRED);
		this.setKey(super.generateKey());
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElement elem) {
		return elem.getAttributes().contains("required");
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

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}
}
