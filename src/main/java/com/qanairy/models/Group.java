package com.qanairy.models;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines a name and color used to group {@link Test}s
 */
public class Group implements IPersistable<Group, IGroup>{
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
		this.generateKey();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public List<Test> getTest() {
		return test;
	}

	public void setTest(List<Test> test) {
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
