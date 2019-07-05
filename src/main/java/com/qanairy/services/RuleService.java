package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.minion.api.exception.RuleValueRequiredException;

@Service
public class RuleService {
	private static Logger log = LoggerFactory.getLogger(RuleService.class);

	@Autowired
	private AlphabeticRestrictionRuleRepository alphabetic_restriction_rule_repo;

	@Autowired
	private RequiredRuleRepository required_rule_repo;

	@Autowired
	private DisabledRuleRepository disabled_rule_repo;

	@Autowired
	private AlphabeticRestrictionRuleRepository alphabetic_restriction_repo;

	@Autowired
	private SpecialCharacterRestrictionRuleRepository special_character_restriction_repo;

	@Autowired
	private NumericRestrictionRuleRepository numeric_restriction_rule_repo;

	@Autowired
	private ReadOnlyRuleRepository read_only_rule_repo;

	@Autowired
	private NumericRuleRepository numeric_rule_repo;

	@Autowired
	private EmailPatternRuleRepository email_pattern_rule_repo;

	@Autowired
	private PatternRuleRepository pattern_rule_repo;

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
			rule_record = special_character_restriction_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = special_character_restriction_repo.save((SpecialCharacterRestriction)rule);
			}
		}

		return rule_record;
	}

	public Rule findByType(String rule_type, String value) throws RuleValueRequiredException {
		Rule rule_record = null;
		log.warn("looking up rule by type :: " + rule_type);
		if(rule_type.equals(RuleType.REQUIRED.toString())){
			RequirementRule rule = new RequirementRule();
			log.warn("finding rule by key" );
			rule_record = required_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = required_rule_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.DISABLED.toString())){
			DisabledRule rule = new DisabledRule();
			rule_record = disabled_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = disabled_rule_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.ALPHABETIC_RESTRICTION.toString())){
			AlphabeticRestrictionRule rule = new AlphabeticRestrictionRule();
			rule_record = alphabetic_restriction_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = alphabetic_restriction_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.SPECIAL_CHARACTER_RESTRICTION.toString())){
			SpecialCharacterRestriction rule = new SpecialCharacterRestriction();
			rule_record = special_character_restriction_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = special_character_restriction_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.READ_ONLY.toString())){
			ReadOnlyRule rule = new ReadOnlyRule();
			rule_record = read_only_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = read_only_rule_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.MIN_VALUE.toString())){
			if(value == null){
				throw new RuleValueRequiredException(RuleType.MIN_VALUE);
			}
			NumericRule rule = new NumericRule(RuleType.MIN_VALUE, value);
			rule_record = numeric_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = numeric_rule_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.MAX_VALUE.toString())){
			if(value == null){
				throw new RuleValueRequiredException(RuleType.MAX_VALUE);
			}
			NumericRule rule = new NumericRule(RuleType.MAX_VALUE, value);
			rule_record = numeric_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = numeric_rule_repo.save(rule);
			}
		}
		//minlength only works for certain frameworks such as angularjs that support it as a custom html5 attribute
		else if(rule_type.equals(RuleType.MIN_LENGTH.toString())){
			if(value == null){
				throw new RuleValueRequiredException(RuleType.MIN_LENGTH);
			}
			NumericRule rule = new NumericRule(RuleType.MIN_LENGTH, value);
			rule_record = numeric_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = numeric_rule_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.MAX_LENGTH.toString())){
			if(value == null){
				throw new RuleValueRequiredException(RuleType.MAX_LENGTH);
			}
			NumericRule rule = new NumericRule(RuleType.MAX_LENGTH, value);
			rule_record = numeric_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = numeric_rule_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.EMAIL_PATTERN.toString())){
			EmailPatternRule rule = new EmailPatternRule();
			rule_record = email_pattern_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = email_pattern_rule_repo.save(rule);
			}
		}
		else if(rule_type.equals(RuleType.PATTERN.toString())){
			if(value == null){
				throw new RuleValueRequiredException(RuleType.PATTERN);
			}
			String regex_str = value;
			PatternRule rule = new PatternRule(regex_str);
			rule_record = pattern_rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = pattern_rule_repo.save(rule);
			}
		}

		return rule_record;
	}
}
