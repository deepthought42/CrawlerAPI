package com.qanairy.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.api.exceptions.ExistingRuleException;
import com.qanairy.models.Domain;
import com.qanairy.models.Element;
import com.qanairy.models.repository.ElementRepository;
import com.qanairy.models.rules.Rule;

@Service
public class ElementService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementService.class);

	@Autowired
	private RuleService rule_service;

	@Autowired
	private ElementRepository element_repo;

	/**
	 * 
	 * @param element
	 * @return
	 * 
	 * @pre element != null
	 */
	public Element save(Element element){
		assert element != null;

		Element element_record = element_repo.findByKey(element.getKey());
		if(element_record == null){
			//iterate over attributes			
			Set<Rule> rule_records = new HashSet<>();
			for(Rule rule : element.getRules()){
				log.warn("adding rule to rule records :: " + rule.getType());
				rule_records.add(rule_service.save(rule));
			}
			element.setRules(rule_records);

			element_record = element_repo.save(element);
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
	public Element saveFormElement(Element element){
		assert element != null;
		Element element_record = element_repo.findByKey(element.getKey());
		if(element_record == null){			
			Set<Rule> rule_records = new HashSet<>();
			for(Rule rule : element.getRules()){
				log.warn("adding rule to rule records :: " + rule.getType());
				rule_records.add(rule_service.save(rule));
			}
			element.setRules(rule_records);

			element_record = element_repo.save(element);
		}
		
		return element_record;
	}

	public Element findByKey(String key){
		return element_repo.findByKey(key);
	}

	public Element findByKeyAndUserId(String user_id, String key){
		return element_repo.findByKeyAndUserId(user_id, key);
	}

	public void removeRule(String user_id, String element_key, String rule_key){
		element_repo.removeRule(user_id, element_key, rule_key);
	}
	
	public boolean doesElementExistInOtherPageStateWithLowerScrollOffset(Element element){
		return false;
	}

	public Element findById(long id) {
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

	public Element findByOuterHtml(String user_id, String snippet) {
		return element_repo.findByOuterHtml(user_id, snippet);
	}

	public void clearBugMessages(String user_id, String form_key) {
		element_repo.clearBugMessages(user_id, form_key);
	}

	public List<Element> getChildElementsForUser(String user_id, String element_key) {
		return element_repo.getChildElementsForUser(user_id, element_key);
	}
	
	public List<Element> getChildElements(String element_key) {
		return element_repo.getChildElements(element_key);
	}
	
	public List<Element> getChildElementForParent(String parent_key, String child_element_key) {
		return element_repo.getChildElementForParent(parent_key, child_element_key);
	}

	@Deprecated
	public Element getParentElement(String user_id, Domain domain, String page_key, String element_state_key) {
		return element_repo.getParentElement(user_id, domain, page_key, element_state_key);
	}

	/**
	 * gets parent element for given {@link Element} within the given {@link PageState}
	 * 
	 * @param page_state_key
	 * @param element_state_key
	 * @return
	 */
	public Element getParentElement(String page_state_key, String element_state_key) {
		return element_repo.getParentElement(page_state_key, element_state_key);
	}
	
	public void addChildElement(String parent_element_key, String child_element_key) {
		//check if element has child already
		if(getChildElementForParent(parent_element_key, child_element_key).isEmpty()) {
			element_repo.addChildElement(parent_element_key, child_element_key);
		}
	}
}
