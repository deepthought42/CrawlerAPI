package com.qanairy.rules.formRules;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

public class RequirementRule implements FormRule<Boolean>{
	
	public FormRuleType type;
	public Boolean value; 
	
	public RequirementRule(){
		this.type = FormRuleType.REQUIRED;
		this.value = true;
	}
	
	@Override
	public FormRuleType getType() {
		return type;
	}

	@Override
	public Boolean evaluate(FormField field) {
		return field.getInputElement().getAttributes().contains("required");
	}
	
	@Override
	public Boolean getValue() {
		return this.value;
	}
}
