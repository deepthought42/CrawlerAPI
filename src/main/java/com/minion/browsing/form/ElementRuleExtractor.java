package com.minion.browsing.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.models.repository.AlphabeticRestrictionRuleRepository;
import com.qanairy.models.repository.DisabledRuleRepository;
import com.qanairy.models.repository.EmailPatternRuleRepository;
import com.qanairy.models.repository.NumericRuleRepository;
import com.qanairy.models.repository.PatternRuleRepository;
import com.qanairy.models.repository.ReadOnlyRuleRepository;
import com.qanairy.models.repository.RequirementRuleRepository;
import com.qanairy.models.repository.SpecialCharacterRestrictionRuleRepository;
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

@Service
public class ElementRuleExtractor {
	private static Logger log = LoggerFactory.getLogger(ElementRuleExtractor.class);

	@Autowired 
	private RequirementRuleRepository requirement_rule_repo;
	
	@Autowired 
	private DisabledRuleRepository disabled_rule_repo;
	
	@Autowired 
	private AlphabeticRestrictionRuleRepository alphabet_restriction_rule_repo;
	
	@Autowired 
	private SpecialCharacterRestrictionRuleRepository special_character_rule_repo;
	
	@Autowired 
	private ReadOnlyRuleRepository read_only_rule_repo;
	
	@Autowired 
	private NumericRuleRepository numeric_rule_repo;
	
	@Autowired 
	private EmailPatternRuleRepository email_pattern_rule_repo;
	
	@Autowired 
	private PatternRuleRepository pattern_rule_repo;
	
	public List<Rule> extractInputRules(PageElement elem){
		Map<String, Boolean> input_rules = new HashMap<String, Boolean>();
		List<Rule> rules = new ArrayList<Rule>();
		for(Attribute attr : elem.getAttributes()){
			Rule rule = null;
			Rule rule_record = null;
			if(attr.getName().trim().equalsIgnoreCase("required")){
				rule = new RequirementRule();
				rule_record = requirement_rule_repo.findByKey(rule.getKey());
			}
			else if(attr.getName().trim().equalsIgnoreCase("disabled")){
				rule = new DisabledRule();
				rule_record = disabled_rule_repo.findByKey(rule.getKey());
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.getVals().contains("number")){
				AlphabeticRestrictionRule alphabetic_restriction_rule = new AlphabeticRestrictionRule();
				rules.add(alphabetic_restriction_rule);
				rule_record = alphabet_restriction_rule_repo.findByKey(alphabetic_restriction_rule.getKey());
				if(rule_record == null){
					rules.add(alphabetic_restriction_rule);
				}
				else{
					rules.add(rule_record);
				}
				
				Rule special_character_rule = new SpecialCharacterRestriction();
				rules.add(special_character_rule);
				Rule rule_record2 = special_character_rule_repo.findByKey(rule.getKey());
				if(rule_record == null){
					rules.add(special_character_rule);
				}
				else{
					rules.add(rule_record2);
				}
				continue;
			}
			else if(attr.getName().equalsIgnoreCase("readonly")){
				rule = new ReadOnlyRule();
				rule_record = read_only_rule_repo.findByKey(rule.getKey());
			}
			else if(attr.getName().equalsIgnoreCase("min")){
				rule = new NumericRule(RuleType.MIN_VALUE, attr.getVals().get(0));
				rule_record = numeric_rule_repo.findByKey(rule.getKey());
			}
			else if(attr.getName().equalsIgnoreCase("max")){
				rule = new NumericRule(RuleType.MAX_VALUE, attr.getVals().get(0));
				rule_record = numeric_rule_repo.findByKey(rule.getKey());
			}
			//minlength only works for certain frameworks such as angularjs that support it as a custom html5 attribute
			else if(attr.getName().equalsIgnoreCase("minlength")){
				rule = new NumericRule(RuleType.MIN_LENGTH, attr.getVals().get(0));
				rule_record = numeric_rule_repo.findByKey(rule.getKey());
			}
			else if(attr.getName().equalsIgnoreCase("maxlength")){
				rule = new NumericRule(RuleType.MAX_LENGTH, attr.getVals().get(0));
				rule_record = numeric_rule_repo.findByKey(rule.getKey());
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.getVals().get(0).equalsIgnoreCase("email")){
				rule = new EmailPatternRule();					
			
				log.info("email rule :: "+rule);
				EmailPatternRule email_rule = new EmailPatternRule();
				log.info("email pattern rule :: "+email_rule);
				
				log.info("EMAIL RULE REPO :: "+email_pattern_rule_repo);

				rule_record = email_pattern_rule_repo.findByKey(rule.getKey());
			}
			else if(attr.getName().equalsIgnoreCase("pattern")){
				String regex_str = attr.getVals().get(0);
				rule = new PatternRule(regex_str);
				rule_record = pattern_rule_repo.findByKey(rule.getKey());
			}
			else{
				continue;
			}
			log.info("RULE :: "+rule);
			log.info("rule repo key :: "+rule.getKey());
			log.info("RULE RECORD :: "+rule_record);
			log.info("INPUT RULES ::  "+ input_rules.keySet().size());
			log.info("RULE TYPE   ::  "+rule.getType().toString());
			if(input_rules.containsKey(rule.getType().toString()) && input_rules.get(rule.getType().toString()) != true){
				if(rule_record == null){
					rules.add(rule);
				}
				else{
					rules.add(rule_record);
				}
				input_rules.put(rule.getType().toString(), true);
			}
		}
		
		return rules;
	}

	public List<Rule> extractMouseRules(PageElement page_element) {
		List<Rule> rules = new ArrayList<Rule>();

		//iterate over possible mouse actions. 
		//if an element action interaction causes change
			//then add the appropriate rule to the list
		Rule clickable = new Clickable();
		rules.add(clickable);
		return rules;
	}
}
