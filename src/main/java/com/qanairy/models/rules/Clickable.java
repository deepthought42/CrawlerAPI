package com.qanairy.models.rules;

import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

public class Clickable extends Rule {

	private String key;
	private RuleType type;
	private String value;
	
	public Clickable(){
		this.type = RuleType.CLICKABLE;
		this.value = "";
		setKey(generateKey());
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
	public void setType(RuleType type) {
		this.type = RuleType.CLICKABLE;
	}
	
	@Override
	public RuleType getType() {
		return RuleType.CLICKABLE;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public Boolean evaluate(PageElement val) {
		assert false;
		// TODO Auto-generated method stub
		return null;
	}
}
