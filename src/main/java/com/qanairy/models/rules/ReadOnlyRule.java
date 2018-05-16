package com.qanairy.models.rules;

import com.qanairy.models.PageElement;
import com.qanairy.persistence.Rule;

/**
 * Creates a read-only {@link FormRule} on a {@link FormField}  
 *
 */
public class ReadOnlyRule implements Rule {

	
	public ReadOnlyRule(){
	}
	
	@Override
	public RuleType getType() {
		return RuleType.READ_ONLY;
	}

	@Override
	public String getValue() {
		return null;
	}
	
	@Override
	public Boolean evaluate(PageElement elem) {
		//Check if field is read-only
		return elem.getAttributes().contains("readonly");
	}
}
