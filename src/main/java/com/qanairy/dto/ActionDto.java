package com.qanairy.dto;

import com.qanairy.models.Action;

/**
 * Data Transfer Object for {@link Action} 
 */
public class ActionDto {

	private String key;
	private String value;
	private String name;
	
	public ActionDto(Action action){
		setKey(action.getKey());
		setValue(action.getValue());
		setName(action.getName());
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
