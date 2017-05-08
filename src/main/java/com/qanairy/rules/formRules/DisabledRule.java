package com.qanairy.rules.formRules;

import org.apache.log4j.Logger
import org.slf4j.LoggerFactory;

import com.minion.actors.FormTestDiscoveryActor;
import com.minion.browsing.form.FormField;
import com.qanairy.models.Attribute;
import com.qanairy.rules.FormRule;

public class DisabledRule implements FormRule {
	private static Logger log = LogManager.getLogger(FormTestDiscoveryActor.class);

	private FormRuleType type;
	
	public DisabledRule() {
		this.type = FormRuleType.DISABLED;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public FormRuleType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(FormField field) {
		/* 
		 * Also check for 
		 * 
		 * display: none;
		 * visibility: hidden;
		 * 
		 */
	
		Attribute attr = field.getInputElement().getAttribute("disabled");
		log.info("!DISABLED RULE TYPE....TODO : THIS FEATURE NEEDS A PROPER IMPLEMENTATION!!!");
		return attr.getVals().size() == 0;
	}
}
