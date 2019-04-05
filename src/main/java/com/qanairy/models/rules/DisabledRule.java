package com.qanairy.models.rules;

import org.slf4j.LoggerFactory;

import com.qanairy.models.Attribute;
import com.qanairy.models.PageElementState;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.slf4j.Logger;

@NodeEntity
public class DisabledRule extends Rule{
	private static Logger log = LoggerFactory.getLogger(DisabledRule.class);
	
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String value;
	private RuleType type;
	
	public DisabledRule() {
		setType(RuleType.DISABLED);
		setValue("");
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElementState elem) {
		/* 
		 * Also check for 
		 * 
		 * display: none;
		 * visibility: hidden;
		 * 
		 */
	
		for(Attribute attribute: elem.getAttributes()){
			if(attribute.getName().equals("disabled")){
				log.info("!DISABLED RULE TYPE....TODO : THIS FEATURE NEEDS A PROPER IMPLEMENTATION!!!");
				return attribute.getVals().size() == 0;
			}
		}
		return null;
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
