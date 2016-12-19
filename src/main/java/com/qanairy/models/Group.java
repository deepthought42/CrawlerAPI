package com.qanairy.models;

import java.util.List;

import com.minion.persistence.IGroup;
import com.minion.persistence.IPersistable;
import com.minion.persistence.OrientConnectionFactory;

/**
 * Defines a name and color used to group {@link Test}s
 */
public class Group implements IPersistable<IGroup>{
	private String key;
	private String name;
	private String color;
	private List<Test> test;
	
	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param color		hexadecimal string representation of color
	 * @param test		{@link List} of {@link Test}s
	 */
	public Group(String name, String color, List<Test> test){
		this.setName(name);
		this.setColor(color);
		this.generateKey();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
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

	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		return "group:"+name;
	}

	/**
	 * {@inheritDoc}
	 */
	public IGroup convertToRecord(OrientConnectionFactory connection) {
		
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public IGroup create() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public IGroup update() {
		// TODO Auto-generated method stub
		return null;
	}
}
