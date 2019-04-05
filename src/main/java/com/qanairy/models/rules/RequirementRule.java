package com.qanairy.models.rules;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.PageElementState;

@NodeEntity
public class RequirementRule extends Rule{
	@GeneratedValue
    @Id
	private Long id;
	
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
	public Boolean evaluate(PageElementState elem) {
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
