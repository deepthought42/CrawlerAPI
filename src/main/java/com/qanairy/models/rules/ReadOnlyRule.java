package com.qanairy.models.rules;

import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

/**
 * Creates a read-only {@link FormRule} on a {@link FormField}  
 *
 */
public class ReadOnlyRule extends Rule {

	
	private String key;
	private RuleType type;
	private String value;


	public ReadOnlyRule(){
		setValue(null);
		setType(RuleType.READ_ONLY);
	}

	
	@Override
	public Boolean evaluate(PageElement elem) {
		//Check if field is read-only
		return elem.getAttributes().contains("readonly");
	}


	@Override
	public void setKey(String key) {
		this.key = key;
	}


	@Override
	public String getKey() {
		return this.key;
	}


	@Override
	public RuleType getType() {
		return this.type;
	}


	@Override
	public void setType(RuleType type) {
		this.type = type;
	}


	@Override
	public String getValue() {
		return this.value;
	}


	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	
}
