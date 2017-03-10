package com.qanairy.rules.formRules;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;

/**
 * Creates a read-only {@link FormRule} on a {@link FormField}  
 *
 */
public class ReadOnlyRule implements FormRule {

	
	public ReadOnlyRule(){
	}
	
	@Override
	public FormRuleType getType() {
		return FormRuleType.READ_ONLY;
	}

	@Override
	public Boolean evaluate(FormField field) {
		//Check if field is read-only
		return field.getInputElement().getAttributes().contains("readonly");
	}

}
