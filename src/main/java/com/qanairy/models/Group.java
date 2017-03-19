package com.qanairy.models;

import java.util.List;

/**
 * Defines a name and color used to group {@link Test}s
 */
public class Group {
	private String key;
	private String name;
	private List<Test> test;
	private String description;
	
	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String name, List<Test> test){
		this.setName(name);
		this.setDescription("");
		this.setTests(test);
		this.setKey(null);
	}
	
	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String name, List<Test> test, String desc){
		this.setName(name);
		this.setDescription(desc);
		this.setTests(test);
		this.setKey(null);
	}

	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String key, String name, List<Test> test, String desc){
		this.setName(name);
		this.setDescription(desc);
		this.setTests(test);
		this.setKey(key);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public List<Test> getTests() {
		return test;
	}

	public void setTests(List<Test> test) {
		this.test = test;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setDescription(String description){
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}
}
