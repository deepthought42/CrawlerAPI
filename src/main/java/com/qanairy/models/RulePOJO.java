package com.qanairy.models;

import com.qanairy.persistence.Rule;

public class RulePOJO extends Rule{

	private String key;
	private String type;
	private String value;
	
	@Override
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getKey() {
		return key;
	}
	
	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
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
