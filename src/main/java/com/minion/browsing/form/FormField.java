package com.minion.browsing.form;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.ElementState;
import com.qanairy.models.rules.Rule;

/**
 * Defines a complex element grouping of input and label for a field contained within a form. 
 * Also contains list of rules which can be enforced on the object
 * 
 */
@NodeEntity
public class FormField {
	
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private List<Rule> rules;
	private ElementState form_field;
	
	/**
	 * Constructs new FormField
	 * 
	 * @param form_field combo element defining the input grouping for this FormField
	 */
	public FormField(ElementState form_field){
		this.form_field = form_field;
		this.rules = new ArrayList<Rule>();
		setKey(generateKey());
	}
	
	private String generateKey() {
		return form_field.getKey()+"::"+rules.hashCode();
	}

	/**
	 * Constructs new FormField
	 * 
	 * @param form_field combo element defining the input grouping for this FormField
	 * @param rules list of {@link Rule} defined on this FormField
	 */
	public FormField(ElementState form_field, List<Rule> rules){
		this.form_field = form_field;
		this.rules = rules;
	}
	
	/**
	 * Adds a rule
	 * 
	 * @param rule Rule to be added
	 * 
	 * @return true if the rule was added successfully, otherwise return false;
	 */
	public boolean addRule(Rule rule){
		return this.rules.add(rule);
	}
	
	/**
	 * Adds a Collection of rules
	 * 
	 * @param rules List of {@link Rule}s to be added
	 * @return true if rules were added successfully, otherwise return false;
	 */
	public boolean addRules(List<Rule> rules){
		return this.rules.addAll(rules);
	}
	
	/**
	 * @return List of {@link Rule}s defined on this FormField
	 */
	public List<Rule> getRules() {
		return rules;
	}
	
	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}
	
	public ElementState getInputElement() {
		return form_field;
	}
	
	public void setInputElement(ElementState form_field) {
		this.form_field = form_field;
	}
	
	/**
	 * This handles the performing of a {@link Action} 
	 * 
	 * @param action
	 */
	public void performAction(String action){
		
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
