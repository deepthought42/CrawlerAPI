package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a name and color used to group {@link Test}s
 */
public class Group {
	private String key;
	private String name;
	private String description;
	private List<Test> tests;
	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String name){
		this.setName(name);
		this.setDescription("");
		this.setTests(new ArrayList<Test>());
		this.setKey(null);
	}
	
	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String name, List<Test> tests){
		this.setName(name);
		this.setTests(tests);
		this.setDescription("");
		this.setKey(null);
	}
	
	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String name, List<Test> tests, String desc){
		this.setName(name);
		this.setTests(tests);
		this.setDescription(desc);
		this.setKey(null);
	}

	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String key, String name, List<Test> tests, String desc){
		this.setName(name);
		this.setTests(tests);
		this.setDescription(desc);
		this.setKey(key);
	}
	
	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String name, String desc){
		this.setName(name);
		this.setTests(new ArrayList<Test>());
		this.setDescription(desc);
		this.setKey(null);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public List<Test> getTests() {
		return tests;
	}

	public void setTests(List<Test> tests) {
		this.tests = tests;
	}
}
