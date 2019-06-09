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
	private ElementStateRepository element_repo;
	
	public ElementState save(ElementState element){
		if(element == null){
			return null;
		}
		ElementState element_record = findByScreenshotChecksum(element.getScreenshotChecksum());
		if(element_record == null){
			element_record = element_repo.findByKey(element.getKey());
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
			}
			else{
				element_record.setScreenshot(element.getScreenshot());
				element_record.setScreenshotChecksum(element.getScreenshotChecksum());
				element_record.setXpath(element.getXpath());
				element_repo.save(element_record);
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
	
	public boolean doesElementExistInOtherPageStateWithLowerScrollOffset(ElementState element){
		
		return false;
	}
	
	public ElementState findByScreenshotChecksum(String screenshotChecksum) {
		return element_repo.findByScreenshotChecksum(screenshotChecksum);
	}
}
