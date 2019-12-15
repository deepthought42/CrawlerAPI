package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.qanairy.models.repository.RuleRepository;
import com.qanairy.models.rules.Rule;
import com.qanairy.models.rules.RuleFactory;
import com.minion.api.exception.RuleValueRequiredException;

@Service
public class RuleService {
	
	@Autowired
	private RuleRepository rule_repo;

	public Rule save(Rule rule){
		assert rule != null;
		
		Rule rule_record = rule_repo.findByTypeAndValue(rule.getType().toString(), rule.getValue());
		if(rule_record == null) {
			return rule_repo.save(rule);
		}
		return rule_record;
	}

	public Rule findRule(String rule_type, String value) throws RuleValueRequiredException {
		
		Rule rule = rule_repo.findByTypeAndValue(rule_type, value);
		if(rule == null) {
			rule = RuleFactory.build(rule_type, value);
		}
		return rule;
	}
}
