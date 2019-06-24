package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.repository.RuleRepository;
import com.qanairy.models.rules.Rule;

@Service
public class RuleService {
	
	@Autowired
	private RuleRepository rule_repo;
	
	public Rule save(Rule rule){
		Rule rule_record = rule_repo.findByKey(rule.getKey());
		if(rule_record == null){
			rule_record = rule_repo.save(rule);
		}
		
		return rule_record;
	}
}
