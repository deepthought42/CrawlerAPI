package com.qanairy.rules.formRules;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

/**
 * 
 *
 */
public class ReadOnlyRule implements FormRule<Boolean> {

	private FormRuleType type;
	private Boolean value;
	
	public ReadOnlyRule(){
		this.type = FormRuleType.READ_ONLY;
		this.value = true;
	}
	
	@Override
	public FormRuleType getType() {
		return this.type;
	}

	@Override
	public Boolean getValue() {
		return this.value;
	}

	@Override
	public Boolean evaluate(FormField field) {
		//Check if field is read-only
		return field.getInputElement().getAttributes().contains("readonly");
	}

}
