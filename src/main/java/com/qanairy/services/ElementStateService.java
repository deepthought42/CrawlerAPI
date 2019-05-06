package com.qanairy.services;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import com.qanairy.models.repository.ElementStateRepository;
import com.qanairy.models.rules.Rule;

@Service
public class ElementStateService {
	
	@Autowired
	private AttributeService attribute_service;
	
	@Autowired
	private RuleService rule_service;
	
	@Autowired
	private ElementStateRepository page_element_repo;
	
	public ElementState save(ElementState element){
		ElementState element_record = page_element_repo.findByKey(element.getKey());
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
			
			element_record = page_element_repo.save(element);
		}
		else{
			element_record.setScreenshot(element.getScreenshot());
			element_record.setXpath(element.getXpath());
			page_element_repo.save(element_record);
		}
		
		return element_record;
	}
	
	public ElementState findByKey(String key){
		return page_element_repo.findByKey(key);
	}
	
	public ElementState findByTextAndName(String text, String name){
		return page_element_repo.findByTextAndName(text, name);
	}
	
	public boolean doesElementExistInOtherPageStateWithLowerScrollOffset(ElementState element){
		
		return false;
	}
	
	public ElementState findByScreenshotChecksum(String screenshotChecksum) {
		return page_element_repo.findByScreenshotChecksum(screenshotChecksum);
	}
}
