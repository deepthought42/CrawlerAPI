package com.minion.browsing.form;

import java.util.ArrayList;
import java.util.List;

import com.qanairy.models.PageElement;
import com.qanairy.rules.FormRule;

/**
 * Defines a complex element grouping of input and label for a field contained within a form. 
 * Also contains list of rules which can be enforced on the object
 * 
 */
public class FormField {
	private List<FormRule<?>> rules;
	private PageElement form_field;
	private PageElement field_label;
	
	/**
	 * Constructs new FormField
	 * 
	 * @param form_field combo element defining the input grouping for this FormField
	 */
	public FormField(PageElement form_field){
		this.form_field = form_field;
		this.rules = new ArrayList<FormRule<?>>();
	}
	
	/**
	 * Constructs new FormField
	 * 
	 * @param form_field combo element defining the input grouping for this FormField
	 * @param rules list of {@link Rule} defined on this FormField
	 */
	public FormField(PageElement form_field, List<FormRule<?>> rules){
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
	public boolean addRule(FormRule<?> rule){
		return this.rules.add(rule);
	}
	
	/**
	 * Adds a Collection of rules
	 * 
	 * @param rules List of {@link Rule}s to be added
	 * @return true if rules were added successfully, otherwise return false;
	 */
	public boolean addRules(List<FormRule<?>> rules){
		return this.rules.addAll(rules);
	}
	
	/**
	 * @return List of {@link Rule}s defined on this FormField
	 */
	public List<FormRule<?>> getRules() {
		return rules;
	}
	
	public void setRules(List<FormRule<?>> rules) {
		this.rules = rules;
	}
	
	public PageElement getInputElement() {
		return form_field;
	}
	
	public void setInputElement(PageElement form_field) {
		this.form_field = form_field;
	}
	
	/**
	 * This handles the performing of a {@link Action} 
	 * 
	 * @param action
	 */
	public void performAction(String action){
		
	}
	public PageElement getFieldLabel() {
		return field_label;
	}
	
	public void setFieldLabel(PageElement field_label) {
		this.field_label = field_label;
	}
}
