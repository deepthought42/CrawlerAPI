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
			Rule rule = null;
			Rule rule_record = null;
			if(attr.getName().equalsIgnoreCase("required")){
				rule = new RequirementRule();
			}
			else if(attr.getName().equalsIgnoreCase("disabled")){
				rule = new DisabledRule();
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.getVals().contains("number")){
				log.warn("adding number rule");
				AlphabeticRestrictionRule alphabetic_restriction_rule = new AlphabeticRestrictionRule();
				rules.add(rule_service.save(alphabetic_restriction_rule));
				
				Rule special_character_rule = new SpecialCharacterRestriction();
				rules.add(rule_service.save(special_character_rule));
				continue;
			}
			else if(attr.getName().equalsIgnoreCase("readonly")){
				rule = new ReadOnlyRule();
			}
			else if(attr.getName().equalsIgnoreCase("min")){
				rule = new NumericRule(RuleType.MIN_VALUE, attr.getVals().get(0));
			}
			else if(attr.getName().equalsIgnoreCase("max")){
				rule = new NumericRule(RuleType.MAX_VALUE, attr.getVals().get(0));
			}
			//minlength only works for certain frameworks such as angularjs that support it as a custom html5 attribute
			else if(attr.getName().equalsIgnoreCase("minlength")){
				rule = new NumericRule(RuleType.MIN_LENGTH, attr.getVals().get(0));
			}
			else if(attr.getName().equalsIgnoreCase("maxlength")){
				rule = new NumericRule(RuleType.MAX_LENGTH, attr.getVals().get(0));
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.getVals().contains("email")){
				rule = new EmailPatternRule();					
			
				log.info("email rule :: "+rule);
				EmailPatternRule email_rule = new EmailPatternRule();
				log.info("email pattern rule :: "+email_rule);
			}
			else if(attr.getName().equalsIgnoreCase("pattern")){
				String regex_str = attr.getVals().get(0);
				rule = new PatternRule(regex_str);
			}
			
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
