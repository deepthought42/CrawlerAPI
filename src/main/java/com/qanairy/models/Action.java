package com.qanairy.models;


/**
 * Defines an action in name only
 */
public class Action extends LookseeObject implements Persistable{
	
	private String name;
	private String value;
	
	/**
	 * Construct empty action object
	 */
	public Action(){}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name) {
		this.name = action_name;
		this.value = "";
		this.setKey(generateKey());
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name, String value) {
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
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * {@inheritDoc}
	 */
	public Action clone() {
		Action action_clone = new Action(this.getName(), this.getValue());
		return action_clone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "action:"+getName() + ":"+ getValue();
	}
}
