package com.minion.browsing.actions;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.persistence.DataAccessObject;
import com.minion.persistence.IAction;
import com.minion.persistence.OrientConnectionFactory;
import com.qanairy.models.PathObject;

/**
 * Defines an action in name only
 */
public class Action extends PathObject<IAction>{
	private static final Logger log = LoggerFactory.getLogger(Action.class);

	private final String name;
	private final String key;
	private final String val;
	
	/**
	 * Construct empty action object
	 */
	public Action(){
		this.name = null;
		this.key = null;
		this.val = "";
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name) {
		this.name = action_name;
		this.val = "";
		this.key = generateKey();
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name, String value) {
		this.name = action_name;
		this.val = value;
		this.key = generateKey();
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

	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		return this.name + ":"+this.val.hashCode();
	}

	public String getValue() {
		return val;
	}

	/**
	 * {@inheritDoc}
	 */
	public IAction convertToRecord(OrientConnectionFactory rl_conn) {
		log.info("Creating Action path object record with key : "+this.getKey());
		@SuppressWarnings("unchecked")
		Iterable<IAction> actions = (Iterable<IAction>) DataAccessObject.findByKey(this.getKey(), IAction.class);
		
		Iterator<IAction> action_iter = actions.iterator();
		IAction action = null;

		if(!action_iter.hasNext()){
			action = rl_conn.getTransaction().addVertex("class:"+IAction.class.getCanonicalName()+","+UUID.randomUUID(), IAction.class);
			action.setName(this.name);
			action.setKey(this.key);
			action.setType(this.getClass().getName());
			action.setValue(this.getValue());
		}
		else{
			action = actions.iterator().next();
		}		
		
		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	public IAction create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		IAction action = this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	public IAction update() {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IAction action = this.convertToRecord(connection);
		connection.save();
		
		return action;
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public Action convertFromRecord(IAction data) {
		Action action = new Action(data.getName(), data.getValue());
		action.setType(Action.class.getSimpleName());
		
		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject<?> clone() {
		Action action_clone = new Action(this.getName(), this.getValue());
		//action_clone.setNext(this.getNext());
		return action_clone;
	}
}
