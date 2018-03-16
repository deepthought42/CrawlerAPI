package com.qanairy.rules;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.minion.actors.FormTestDiscoveryActor;
import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;

public class DisabledRule implements Rule{
	private static Logger log = LoggerFactory.getLogger(FormTestDiscoveryActor.class);

	private String value;
	
	public DisabledRule() {
		this.value = null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return RuleType.DISABLED;
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
}
