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
			
			log.warn("rule remove list size  ::  " + rule_removal_list.size());
			//remove removed rules
			for(Rule rule : rule_removal_list){
				log.warn("Removing rule :: " +rule.getKey());
				element_repo.removeRule(element.getKey(), rule.getKey());
			}
		}
		else{
			element_record.setScreenshot(element.getScreenshot());
			element_record.setScreenshotChecksum(element.getScreenshotChecksum());
			element_record.setXpath(element.getXpath());
			for(Rule rule : element.getRules()){
				element_record.addRule(rule_service.save(rule));
			}

			element_record = element_repo.save(element_record);
			
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
			
			log.warn("rule remove list size  ::  " + rule_removal_list.size());
			//remove removed rules
			for(Rule rule : rule_removal_list){
				log.warn("Removing rule :: " +rule.getKey());
				element_repo.removeRule(element.getKey(), rule.getKey());
			}
		}
		return element_record;
	}

	public ElementState findByKey(String key){
		return element_repo.findByKey(key);
	}

	public ElementState findByTextAndName(String text, String name){
		return element_repo.findByTextAndName(text, name);
	}

	public void removeElementState(ElementState element, String rule_key){
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
