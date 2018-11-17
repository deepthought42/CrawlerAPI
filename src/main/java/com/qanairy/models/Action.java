package com.qanairy.models;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Defines an action in name only
 */
@NodeEntity
public class Action implements Persistable, PathObject{
	
	@GeneratedValue
    @Id
	private Long id;
	
	private String name;
	private String key;
	private String value;
	private String type;
	
	/**
	 * Construct empty action object
	 */
	public Action(){}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name) {
		setType("Action");
		this.name = action_name;
		this.value = "";
		this.setKey(generateKey());
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name, String value) {
		setType("Action");
		setName(action_name);
		setValue(value);
		this.setKey(generateKey());
	}
	
	/**
	 * @return the name of this action
	 */
	public String getName(){
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}

	public String getKey() {
		return this.key;
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

	/**
	 * {@inheritDoc}
	 */
	public PathObject clone() {
		Action action_clone = new Action(this.getName(), this.getValue());
		return action_clone;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "action:"+getName() + ":"+ getValue();
	}
}
