package com.qanairy.dto;

import com.qanairy.models.Test;

/**
 * Data Transfer object for Test information to be relayed to user ide
 */
public class TestCreatedDto {

	private String key;
	private String name;

	public TestCreatedDto(Test test){
		setKey(key);
		setName(name);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
