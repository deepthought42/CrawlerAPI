package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.repository.AlphabeticRestrictionRuleRepository;
import com.qanairy.models.repository.DisabledRuleRepository;
import com.qanairy.models.repository.EmailPatternRuleRepository;
import com.qanairy.models.repository.NumericRestrictionRuleRepository;
import com.qanairy.models.repository.NumericRuleRepository;
import com.qanairy.models.repository.PatternRuleRepository;
import com.qanairy.models.repository.ReadOnlyRuleRepository;
import com.qanairy.models.repository.RequiredRuleRepository;
import com.qanairy.models.repository.SpecialCharacterRestrictionRuleRepository;
import com.qanairy.models.rules.AlphabeticRestrictionRule;
import com.qanairy.models.rules.DisabledRule;
import com.qanairy.models.rules.EmailPatternRule;
import com.qanairy.models.rules.NumericRestrictionRule;
import com.qanairy.models.rules.NumericRule;
import com.qanairy.models.rules.PatternRule;
import com.qanairy.models.rules.ReadOnlyRule;
import com.qanairy.models.rules.RequirementRule;
import com.qanairy.models.rules.Rule;
import com.qanairy.models.rules.RuleType;
import com.qanairy.models.rules.SpecialCharacterRestriction;

@Service
public class RuleService {
	
	@Autowired
	private AlphabeticRestrictionRuleRepository alphabetic_restriction_rule_repo;
	
	@Autowired
	private DisabledRuleRepository disabled_rule_repo;
	
	@Autowired
	private EmailPatternRuleRepository email_pattern_rule_repo;
	
	@Autowired
	private NumericRuleRepository numeric_rule_repo;
	
	@Autowired
	private NumericRestrictionRuleRepository numeric_restriction_rule_repo;
	
	@Autowired
	private PatternRuleRepository pattern_rule_repo;
	
	@Autowired
	private ReadOnlyRuleRepository read_only_rule_repo;
	
	@Autowired
	private RequiredRuleRepository required_rule_repo;
	
	@Autowired
	private SpecialCharacterRestrictionRuleRepository special_character_restriction_rule_repo;
	
	/**
	 * Saves a given {@link Rule}
	 * 
	 * @param rule {@link Rule} to be saved
	 * 
	 * @return database record of saved rule
	 * 
	 * @pre rule != null
	 */
	public Rule save(Rule rule){
		assert rule != null;
		
		Rule rule_record = null;
		
		if(rule.getType().equals(RuleType.ALPHABETIC_RESTRICTION)){
			rule_record = alphabetic_restriction_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = alphabetic_restriction_rule_repo.save((AlphabeticRestrictionRule)rule);
			}
		}
		else if(rule.getType().equals(RuleType.DISABLED)){
			rule_record = disabled_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = disabled_rule_repo.save((DisabledRule)rule);
			}
		}
		else if(rule.getType().equals(RuleType.EMAIL_PATTERN)){
			rule_record = email_pattern_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = email_pattern_rule_repo.save((EmailPatternRule)rule);
			}
		}
		else if(rule.getType().equals(RuleType.MAX_LENGTH) || rule.getType().equals(RuleType.MIN_LENGTH) 
				|| rule.getType().equals(RuleType.MAX_VALUE) || rule.getType().equals(RuleType.MIN_VALUE)){
			rule_record = numeric_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = numeric_rule_repo.save((NumericRule)rule);
			}
		}
		else if(rule.getType().equals(RuleType.NUMERIC_RESTRICTION)){
			rule_record = numeric_restriction_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = numeric_restriction_rule_repo.save((NumericRestrictionRule)rule);
			}
		}
		else if(rule.getType().equals(RuleType.PATTERN)){
			rule_record = pattern_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = pattern_rule_repo.save((PatternRule)rule);
			}
		}
		else if(rule.getType().equals(RuleType.READ_ONLY)){
			rule_record = read_only_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = read_only_rule_repo.save((ReadOnlyRule)rule);
			}
		}
		else if(rule.getType().equals(RuleType.REQUIRED)){
			rule_record = required_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = required_rule_repo.save((RequirementRule)rule);
			}
		}
		else if(rule.getType().equals(RuleType.SPECIAL_CHARACTER_RESTRICTION)){
			rule_record = special_character_restriction_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = special_character_restriction_rule_repo.save((SpecialCharacterRestriction)rule);
			}
		}
		
		return rule_record;
	}
}
