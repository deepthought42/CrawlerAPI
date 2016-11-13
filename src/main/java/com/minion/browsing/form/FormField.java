package com.minion.browsing.form;

import java.util.List;

import com.minion.browsing.element.ComboElement;
import com.qanairy.rules.Rule;

/**
 * 
 * 
 */
public class FormField {
	private List<Rule> rules;
	private ComboElement form_field;

	
	public FormField(ComboElement form_field){
		this.form_field = form_field;
	}
	
	public FormField(ComboElement form_field, List<Rule> rules){
		this.form_field = form_field;
		this.rules = rules;
	}
	
	public boolean addRule(Rule rule){
		return this.rules.add(rule);
	}
	
	public boolean addRules(List<Rule> rules){
		return this.rules.addAll(rules);
	}
	
	public List<Rule> getRules() {
		return rules;
	}
	
	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}
	
	public ComboElement getComboElement() {
		return form_field;
	}
	
	public void setComboElement(ComboElement form_field) {
		this.form_field = form_field;
	}
	
	/**
	 * This handles the performing of a {@link Action} 
	 * 
	 * @param action
	 */
	public void performAction(String action){
		
	}
}
