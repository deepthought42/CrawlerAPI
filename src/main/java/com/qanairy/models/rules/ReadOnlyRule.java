package com.qanairy.models.rules;

import com.qanairy.models.ElementState;

/**
 * Creates a read-only {@link FormRule} on a {@link FormField}  
 *
 */
public class ReadOnlyRule extends Rule {
	public ReadOnlyRule(){
		setValue("");
		setType(RuleType.READ_ONLY);
		setKey(generateKey());
	}
	
	@Override
	public Boolean evaluate(ElementState elem) {
		//Check if field is read-only
		return elem.getAttributes().containsKey("readonly");
	}
}
