package com.qanairy.models;

/**
 * Defines an action in name only
 */
public class Action extends PathObject{
	private String name;
	private String key;
	private String val;
	
	/**
	 * Construct empty action object
	 */
	public Action(){
		super.setType("Action");
		this.name = null;
		this.key = null;
		this.val = "";
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name) {
		super.setType("Action");
		this.name = action_name;
		this.val = "";
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name, String value) {
		super.setType("Action");
		this.name = action_name;
		this.val = value;
	}
	
	/**
	 * @return the name of this action
	 */
	public String getName(){
		return this.name;
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
		return val;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject clone() {
		Action action_clone = new Action(this.getName(), this.getValue());
		//action_clone.setNext(this.getNext());
		return action_clone;
	}
}
