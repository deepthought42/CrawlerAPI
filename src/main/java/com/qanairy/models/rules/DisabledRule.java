package com.qanairy.models.rules;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

public class DisabledRule extends Rule{
	private static Logger log = LoggerFactory.getLogger(DisabledRule.class);

	private String key;
	private String value;
	private RuleType type;
	
	public DisabledRule() {
		setType(RuleType.DISABLED);
		setValue(null);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElement elem) {
		/* 
		 * Also check for 
		 * 
		 * display: none;
		 * visibility: hidden;
		 * 
		 */
	
		Attribute attr = elem.getAttribute("disabled");
		System.err.println("!DISABLED RULE TYPE....TODO : THIS FEATURE NEEDS A PROPER IMPLEMENTATION!!!");
		return attr.getVals().size() == 0;
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
		return null;
	}
	
	@Override
	public void setValue(String value) {
		this.value = null;
	}
}
