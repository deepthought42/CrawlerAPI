package com.qanairy.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.driver.v1.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.api.exceptions.ExistingRuleException;
import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import com.qanairy.models.repository.ElementStateRepository;
import com.qanairy.models.rules.Rule;

@Service
public class ElementStateService {
	@SuppressWarnings("unused")
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
	public ElementState save(String user_id, ElementState element) throws ClientException{
		assert element != null;
		ElementState element_record = element_repo.findByKey(user_id, element.getKey());
		if(element_record == null){
			//iterate over attributes
			Set<Attribute> new_attributes = new HashSet<Attribute>();
			for(Attribute attribute : element.getAttributes()){
				new_attributes.add(attribute_service.save(attribute));
			}
			element.setAttributes(new_attributes);
			
			Set<Rule> rule_records = new HashSet<>();
			for(Rule rule : element.getRules()){
				log.warn("adding rule to rule records :: " + rule.getType());
				rule_records.add(rule_service.save(rule));
			}
			element.setRules(rule_records);

			element_record = element_repo.save(element);
		}
		else{
			element_record.setScreenshot(element.getScreenshot());
			element_record.setScreenshotChecksum(element.getScreenshotChecksum());
			element_record.setXpath(element.getXpath());
			//element_record.setDisplayed(element.isDisplayed());
			element_record = element_repo.save(element_record);
		}
		return element_record;
	}
	
	/**
	 * 
	 * @param element
	 * @return
	 * 
	 * @pre element != null
	 */
	public ElementState saveFormElement(String user_id, ElementState element) throws ClientException{
		assert element != null;
		ElementState element_record = element_repo.findFormElementByKey(user_id, element.getKey());
		if(element_record == null){
			//iterate over attributes
			Set<Attribute> new_attributes = new HashSet<Attribute>();
			for(Attribute attribute : element.getAttributes()){
				new_attributes.add(attribute_service.save(attribute));
			}
			element.setAttributes(new_attributes);
			
			Set<Rule> rule_records = new HashSet<>();
			for(Rule rule : element.getRules()){
				log.warn("adding rule to rule records :: " + rule.getType());
				rule_records.add(rule_service.save(rule));
			}
			element.setRules(rule_records);

			element_record = element_repo.save(element);
		}
		else{
			element_record.setScreenshot(element.getScreenshot());
			element_record.setScreenshotChecksum(element.getScreenshotChecksum());
			element_record.setXpath(element.getXpath());

			element_record = element_repo.save(element_record);
		}
		return element_record;
	}

	public ElementState findFormElementByKey(String user_id, String key){
		return element_repo.findFormElementByKey(user_id, key);
	}

	public ElementState findByKey(String user_id, String key){
		return element_repo.findByKey(user_id, key);
	}

	public void removeRule(String user_id, String element_key, String rule_key){
		element_repo.removeRule(user_id, element_key, rule_key);
	}
	
	public boolean doesElementExistInOtherPageStateWithLowerScrollOffset(ElementState element){
		return false;
	}

	public ElementState findById(long id) {
		return element_repo.findById(id).get();
	}

	public Set<Rule> getRules(String user_id, String element_key) {
		return element_repo.getRules(user_id, element_key);
	}

	public Set<Rule> addRuleToFormElement(String user_id, String element_key, Rule rule) {
		//Check that rule doesn't already exist
		Rule rule_record = element_repo.getElementRule(user_id, element_key, rule.getKey());
		if(rule_record == null) {
			rule_record = element_repo.addRuleToFormElement(user_id, element_key, rule.getKey());
			return element_repo.getRules(user_id, element_key);
		}
		else {
			throw new ExistingRuleException(rule.getType().toString());
		}
	}

	public List<Attribute> getElementAttributes(String user_id, String element_key) {
		return element_repo.getElementAttributes( user_id, element_key);
	}

	public ElementState findByOuterHtml(String user_id, String snippet) {
		return element_repo.findByOuterHtml(user_id, snippet);
	}

	public void clearBugMessages(String user_id, String form_key) {
		element_repo.clearBugMessages(user_id, form_key);
	}
}
