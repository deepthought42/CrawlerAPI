package com.minion.browsing.form;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import com.qanairy.models.rules.Clickable;
import com.qanairy.models.rules.Rule;
import com.qanairy.models.rules.RuleFactory;
import com.qanairy.services.RuleService;

/**
 * Extracts rules for input {@link ElementState}s
 * @author brand
 *
 */
@Service
public class ElementRuleExtractor {
	private static Logger log = LoggerFactory.getLogger(ElementRuleExtractor.class);

	@Autowired
	private RuleService rule_service;

	public List<Rule> extractInputRules(ElementState elem){
		List<Rule> rules = new ArrayList<Rule>();

		for(Attribute attr : elem.getAttributes()){
			Rule rule = RuleFactory.build(attr.getName().toLowerCase(), attr.getVals().get(0));
			
			if(rule != null){
				rules.add(rule_service.save(rule));
			}
		}

		return rules;
	}

	public List<Rule> extractMouseRules(ElementState page_element) {
		List<Rule> rules = new ArrayList<Rule>();

		//iterate over possible mouse actions.
		//if an element action interaction causes change
			//then add the appropriate rule to the list
		Rule clickable = new Clickable();
		rules.add(clickable);
		return rules;
	}
}
