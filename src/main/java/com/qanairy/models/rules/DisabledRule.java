package com.qanairy.models.rules;

import org.slf4j.LoggerFactory;

import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import org.slf4j.Logger;

public class DisabledRule extends Rule{
	private static Logger log = LoggerFactory.getLogger(DisabledRule.class);
	
	public DisabledRule() {
		setType(RuleType.DISABLED);
		setValue("");
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(ElementState elem) {
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
}
