package com.qanairy.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.driver.v1.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.kms.model.GetKeyPolicyRequest;
import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import com.qanairy.models.repository.ElementStateRepository;
import com.qanairy.models.rules.Rule;

@Service
public class ElementStateService {
	private static Logger log = LoggerFactory.getLogger(ElementStateService.class);

	@Autowired
	private AttributeService attribute_service;

	@Autowired
	private ElementStateService element_state_service;

	@Autowired
	private RuleService rule_service;

	@Autowired
	private ElementStateRepository element_repo;

	/**
	 * 
	 * @param element
	 * @return
	 * 
	 * @pre element != null
	 */
	public ElementState save(ElementState element) throws ClientException{
		assert element != null;
		
		ElementState element_record = element_repo.findByKey(element.getKey());
		if(element_record == null){
			//iterate over attributes
			Set<Attribute> new_attributes = new HashSet<Attribute>();
			for(Attribute attribute : element.getAttributes()){
				new_attributes.add(attribute_service.save(attribute));
			}
			element.setAttributes(new_attributes);
			
			Set<Rule> rule_records = new HashSet<>();
			for(Rule rule : element.getRules()){
				rule_records.add(rule_service.save(rule));
			}
			element.setRules(rule_records);

			element_record = element_repo.save(element);

			//get rules that exit in element but not in element_record
			List<Rule> rule_removal_list = new ArrayList<>();
			for(Rule rule : element_record.getRules()){
				boolean exists = false;
				for(Rule elem_rule : element.getRules()){
					if(elem_rule.getType().equals(rule.getType())){
						exists = true;
						break;
					}
				}
				
				if(!exists){
					rule_removal_list.add(rule);
				}
			}
			
			//remove removed rules
			for(Rule rule : rule_removal_list){
				element_repo.removeRule(element.getKey(), rule.getKey());
			}
		}
		else{
			log.warn("updateing attributes");
			element_record.setScreenshot(element.getScreenshot());
			element_record.setScreenshotChecksum(element.getScreenshotChecksum());
			element_record.setXpath(element.getXpath());
			log.warn("element record key :: " + element_record.getKey());
			Set<Rule> rules = element_state_service.getRules(element_record);
			element_record.setRules(rules);
			
			log.warn("element record rules : " + rules.size());
			for(Rule rule : element.getRules()){
				log.warn("adding rule :::  " +rule.getKey());
				element_record.addRule(rule_service.save(rule));
			}

			element_record = element_repo.save(element_record);
			
			//get rules that exit in element but not in element_record
			List<Rule> rule_removal_list = new ArrayList<>();
			for(Rule rule : element_record.getRules()){
				boolean exists = false;
				log.warn("######################################################################");
				log.warn("Element record Rule :: " +rule.getType() );
				log.warn("######################################################################");
				for(Rule elem_rule : element.getRules()){
					log.warn("element rule type :: " + elem_rule.getType());
					log.warn("element record rule type :: " + rule.getType());
					
					if(elem_rule.getType() == rule.getType()){
						log.warn("element rule matches record rule");
						exists = true;
						break;
					}
				}
				
				if(!exists){
					log.warn("Adding rule to removal list :: " + rule.getKey());
					rule_removal_list.add(rule);
				}
			}
			
			//remove removed rules
			for(Rule rule : rule_removal_list){
				log.warn("removing rule :: " +rule.getKey());
				element_repo.removeRule(element.getKey(), rule.getKey());
			}
		}
		return element_record;
	}

	private Set<Rule> getRules(ElementState element) {
		return element_repo.getRules(element.getKey());
	}

	public ElementState findByKey(String key){
		return element_repo.findByKey(key);
	}

	public ElementState findByTextAndName(String text, String name){
		return element_repo.findByTextAndName(text, name);
	}

	public void removeRule(ElementState element, String rule_key){
		element_repo.removeRule(element.getKey(), rule_key);
	}
	
	public boolean doesElementExistInOtherPageStateWithLowerScrollOffset(ElementState element){
		return false;
	}

	public ElementState findByScreenshotChecksum(String screenshotChecksum) {
		return element_repo.findByScreenshotChecksum(screenshotChecksum);
	}

	public ElementState findById(long id) {
		return element_repo.findById(id).get();
	}
}
