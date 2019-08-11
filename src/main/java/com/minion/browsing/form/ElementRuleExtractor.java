package com.minion.browsing.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import com.qanairy.models.rules.AlphabeticRestrictionRule;
import com.qanairy.models.rules.Clickable;
import com.qanairy.models.rules.DisabledRule;
import com.qanairy.models.rules.EmailPatternRule;
import com.qanairy.models.rules.NumericRule;
import com.qanairy.models.rules.PatternRule;
import com.qanairy.models.rules.ReadOnlyRule;
import com.qanairy.models.rules.RequirementRule;
import com.qanairy.models.rules.Rule;
import com.qanairy.models.rules.RuleFactory;
import com.qanairy.models.rules.RuleType;
import com.qanairy.models.rules.SpecialCharacterRestriction;
import com.qanairy.services.RuleService;

@Service
public class ElementRuleExtractor {
	private static Logger log = LoggerFactory.getLogger(ElementRuleExtractor.class);

	@Autowired
	private RuleService rule_service;

	public List<Rule> extractInputRules(ElementState elem){
		Map<String, Boolean> input_rules = new HashMap<String, Boolean>();
		List<Rule> rules = new ArrayList<Rule>();

		for(Attribute attr : elem.getAttributes()){
			log.warn("Attribute during rule extraction :: " + attr.getName());
			log.warn("Attribute during rule extraction :: " + attr.getName().equalsIgnoreCase("type"));
			log.warn("Attribute during rule extraction :: " + (attr.getName().equalsIgnoreCase("type") && attr.getVals().contains("email")));
			log.warn("attribute values contains email??     "+attr.getVals().contains("email"));
			Rule rule = RuleFactory.build(attr.getName().toLowerCase(), attr.getVals().get(0));
			Rule rule_record = null;

			if(rule != null){
				log.info("RULE :: "+rule);
				log.info("rule repo key :: "+rule.getKey());
				log.info("RULE RECORD :: "+rule_record);
				log.info("INPUT RULES ::  "+ input_rules.keySet().size());
				log.info("RULE TYPE   ::  "+rule.getType().toString());

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
