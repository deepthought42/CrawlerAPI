package com.qanairy.services;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.models.repository.AttributeRepository;
import com.qanairy.models.repository.PageElementRepository;
import com.qanairy.models.repository.RuleRepository;
import com.qanairy.models.rules.Rule;

@Service
public class PageElementService {
	
	@Autowired
	private AttributeRepository attribute_repo;
	
	@Autowired
	private RuleRepository rule_repo;
	
	@Autowired
	private PageElementRepository page_element_repo;
	
	public PageElement save(PageElement element){
		//iterate over attributes
		Set<Attribute> new_attributes = new HashSet<Attribute>();
		for(Attribute attribute : element.getAttributes()){
			Attribute attribute_record = attribute_repo.findByKey(attribute.getKey());
			if(attribute_record == null){
				attribute_record = attribute_repo.save(attribute);
			}
			
			new_attributes.add(attribute_record);
		}
		element.setAttributes(new_attributes);
		
		Set<Rule> rule_records = new HashSet<>();
		for(Rule rule : element.getRules()){
			Rule rule_record = rule_repo.findByKey(rule.getKey());
			if(rule_record == null){
				rule_record = rule_repo.save(rule);
			}
			rule_records.add(rule_record);
		}
		element.setRules(rule_records);
		
		PageElement element_record = page_element_repo.findByKey(element.getKey());
		if(element_record == null){
			element_record = page_element_repo.save(element);
		}
		return element_record;
	}
}
